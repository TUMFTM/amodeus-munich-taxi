package amodeus.amodtaxi.linkspeed.iterative;

import amodeus.amodeus.linkspeed.LSDataTravelTime;
import amodeus.amodeus.linkspeed.LinkSpeedDataContainer;
import amodeus.amodeus.net.FastLinkLookup;
import amodeus.amodeus.net.MatsimAmodeusDatabase;
import amodeus.amodeus.net.TensorCoords;
import amodeus.amodeus.taxitrip.ShortestDurationCalculator;
import amodeus.amodeus.taxitrip.TaxiTrip;
import amodeus.amodeus.util.math.SI;
import ch.ethz.idsc.tensor.RealScalar;
import ch.ethz.idsc.tensor.Scalar;
import ch.ethz.idsc.tensor.Scalars;
import ch.ethz.idsc.tensor.qty.Quantity;
import org.matsim.amodeus.plpc.DefaultParallelLeastCostPathCalculator;
import org.matsim.amodeus.plpc.ParallelLeastCostPathCalculator;
import org.matsim.api.core.v01.network.Network;
import org.matsim.core.router.DijkstraFactory;
import org.matsim.core.router.costcalculators.OnlyTimeDependentTravelDisutility;
import org.matsim.core.router.util.LeastCostPathCalculator;
import org.matsim.core.router.util.TravelTime;

import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.function.Function;

/* package */ public class FindCongestionIterativeParallel {
    private final TripComparisonMaintainer tripMaintainer;
    private final RandomTripMaintainer randomTrips;
    private final File processingDir;
    /**
     * settings and data
     */
    private final Scalar tolerance;
    private final Network network;
    private final LinkSpeedDataContainer lsData;
    private final FastLinkLookup fastLinkLookup;
    /**
     * this is a value in (0,1] which determines the convergence
     * speed of the algorithm, a value close to 1 may lead to
     * loss of convergence, it is advised to chose slow. No changes
     * are applied for epsilon == 0.
     */
    private final double startEpsilon1;
    private final double targetEpsilon1;
    /**
     * probability of taking a new trip
     */
    private final int maxIter;
    private final int dt;

    public FindCongestionIterativeParallel(Network network, MatsimAmodeusDatabase db, File processingDir, //
                                           LinkSpeedDataContainer lsData, List<TaxiTrip> allTrips, //
                                           int maxIter, Scalar tol, Scalar startEpsilon1, Scalar targetEpsilon1, Random random, int dt, //
                                           Function<List<Scalar>, Scalar> costFunction, int checkHorizon) {
        this.processingDir = processingDir;
        this.network = network;
        this.tolerance = Objects.requireNonNull(tol);
        this.lsData = lsData;
        this.maxIter = maxIter;
        this.dt = dt;
        this.fastLinkLookup = new FastLinkLookup(network, db);
        this.startEpsilon1 = startEpsilon1.number().doubleValue();
        this.targetEpsilon1 = targetEpsilon1.number().doubleValue();

        /** export the initial distribution of ratios */
        this.randomTrips = new RandomTripMaintainer(allTrips, checkHorizon, costFunction, random);
        this.tripMaintainer = new TripComparisonMaintainer(randomTrips, network, db);

        /** export initial distribution */
        File diff = new File(processingDir, "diff");
        File plot = new File(processingDir, "plot");
        diff.mkdir();
        plot.mkdir();
        StaticHelper.exportRatioMap(diff, tripMaintainer.getLookupMap(), "Initial");
        StaticHelper.plotRatioMap(plot, randomTrips.getRatios(), "Initial");

        /** show initial score */
        System.out.println("Cost initial: " + randomTrips.getRatioCost());

        try {
            runTripIterations();
        } catch (ExecutionException | InterruptedException e) {
            e.printStackTrace();
            System.err.println(getClass().getSimpleName() + " stopped unexpected. The linkspeeds are not final.");
        }

        System.out.println("Cost End: " + randomTrips.getRatioCost());
        /** final export */
        StaticHelper.export(this.processingDir, this.lsData, "");
    }

    private void runTripIterations() throws ExecutionException, InterruptedException {
        int iterationCount = 0;
        Scalar lastCost = randomTrips.getRatioCost();
        System.out.println("Last cost before start: " + lastCost);
        System.out.println("Tolerance:              " + tolerance);

        List<TaxiTrip> tripBatch = new ArrayList<>();
        List<Map.Entry<TaxiTrip, Future<LeastCostPathCalculator.Path>>> tripFuturePathPairs;
        int tripCounter;
        Scalar epsilon1;

        while (iterationCount < maxIter) {
            ++iterationCount;
            epsilon1 = RealScalar.of(startEpsilon1 - ((startEpsilon1 - targetEpsilon1) * iterationCount / maxIter));

            int numberOfThreads = 6;
            tripBatch.addAll(tripMaintainer.getNWorst(2 * numberOfThreads));

            int batchSize = 5 * numberOfThreads;
            for (int i = numberOfThreads; i < batchSize; i++) {
                tripBatch.add(randomTrips.nextRandom());
            }

            TravelTime travelTime = new LSDataTravelTime(lsData);
            ParallelLeastCostPathCalculator plcpc = DefaultParallelLeastCostPathCalculator.create(numberOfThreads,
                    new DijkstraFactory(), network, new OnlyTimeDependentTravelDisutility(travelTime), travelTime);

            tripFuturePathPairs = getFuturePaths(tripBatch, plcpc);

            tripCounter = 0;
            for (Map.Entry<TaxiTrip, Future<LeastCostPathCalculator.Path>> entry : tripFuturePathPairs) {
                LeastCostPathCalculator.Path path = entry.getValue().get();
                Scalar pathTime = Quantity.of(path.travelTime, SI.SECOND);
                Scalar pathDurationRatio = pathTime.divide(entry.getKey().driveTime);
                Scalar rescaleFactor = RealScalar.ONE.subtract((RealScalar.ONE.subtract(pathDurationRatio)).multiply(epsilon1));
                ApplyScaling.to(lsData, entry.getKey(), path, rescaleFactor, true);

                if (tripCounter >= numberOfThreads)
                    randomTrips.addRecordedRatio(pathDurationRatio);

                tripMaintainer.update(entry.getKey(), pathDurationRatio);
                tripCounter++;
            }

            /** update cost based on random trips */
            lastCost = randomTrips.getRatioCost();


            /** assess every 20 trips if ok */
            if (iterationCount % 100 == 0) {
                System.out.println("iterationCount:       " + iterationCount);
                System.out.println("worst cost: " + tripMaintainer.getWorstCost());
                System.out.println("worst trip: " + tripMaintainer.getWorst().localId);
                System.out.println("cost:       " + lastCost);
            }

            // DEBUGGING
            /** DEBUGGING every interval trips, export cost map */
            if (iterationCount % 500 == 0) {
                StaticHelper.exportRatioMap(new File(processingDir, "diff"), tripMaintainer.getLookupMap(), Integer.toString(iterationCount));
                StaticHelper.plotRatioMap(new File(processingDir, "plot"), randomTrips.getRatios(), Integer.toString(iterationCount));
            }
            // DEBUGGING END

            /** intermediate export */
            if (iterationCount % 30000 == 0)
                StaticHelper.export(processingDir, lsData, "_" + Integer.toString(iterationCount));

            try {
                plcpc.close();
            } catch (IOException e) {
                System.err.println("ParallelLeastCostPathCalculator could not be released!");
                e.printStackTrace();
            }
            tripBatch.clear();
            tripFuturePathPairs.clear();
        }
    }

    private List<Map.Entry<TaxiTrip, Future<LeastCostPathCalculator.Path>>> getFuturePaths(List<TaxiTrip> taxiTrips,
                                                                                           ParallelLeastCostPathCalculator plcpc) {
        List<Map.Entry<TaxiTrip, Future<LeastCostPathCalculator.Path>>> futurePaths = new ArrayList<>();

        for (TaxiTrip taxiTrip : taxiTrips) {
            futurePaths.add(new AbstractMap.SimpleEntry<>(taxiTrip, plcpc.calcLeastCostPath(
                    fastLinkLookup.linkFromWGS84(TensorCoords.toCoord(taxiTrip.pickupLoc)).getFromNode(),
                    fastLinkLookup.linkFromWGS84(TensorCoords.toCoord(taxiTrip.dropoffLoc)).getToNode(),
                    taxiTrip.getSimPickupTime(), null, null))
            );
        }

        return futurePaths;
    }
}
