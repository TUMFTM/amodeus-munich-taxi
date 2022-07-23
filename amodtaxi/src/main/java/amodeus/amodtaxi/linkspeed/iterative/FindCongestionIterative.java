/* amodeus - Copyright (c) 2018, ETH Zurich, Institute for Dynamic Systems and Control */
package amodeus.amodtaxi.linkspeed.iterative;

import amodeus.amodeus.linkspeed.LinkSpeedDataContainer;
import amodeus.amodeus.net.FastLinkLookup;
import amodeus.amodeus.net.MatsimAmodeusDatabase;
import amodeus.amodeus.taxitrip.ShortestDurationCalculator;
import amodeus.amodeus.taxitrip.TaxiTrip;
import ch.ethz.idsc.tensor.RealScalar;
import ch.ethz.idsc.tensor.Scalar;
import org.matsim.api.core.v01.network.Network;
import org.matsim.core.router.util.LeastCostPathCalculator;

import java.io.File;
import java.util.List;
import java.util.Objects;
import java.util.Random;
import java.util.function.Function;

/* package */ class FindCongestionIterative {
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
    private final Scalar epsilon1target;
    private final Scalar epsilon1start;
    /**
     * probability of taking a new trip
     */
    private final Scalar epsilon2;
    private final int maxIter;
    private final Random random;

    public FindCongestionIterative(Network network, MatsimAmodeusDatabase db, File processingDir, //
                                   LinkSpeedDataContainer lsData, List<TaxiTrip> allTrips, //
                                   int maxIter, Scalar tol, Scalar epsilon1target, Scalar epsilon2, Random random, //
                                   Function<List<Scalar>, Scalar> costFunction, int checkHorizon) {
        this(network, db, processingDir, lsData, allTrips, maxIter, tol,
                epsilon1target, epsilon1target, epsilon2, random, costFunction, checkHorizon);

    }

    public FindCongestionIterative(Network network, MatsimAmodeusDatabase db, File processingDir, //
                                   LinkSpeedDataContainer lsData, List<TaxiTrip> allTrips, //
                                   int epochs, Scalar tol, Scalar epsilon1target, Scalar epsilon1start, Scalar epsilon2, Random random, //
                                   Function<List<Scalar>, Scalar> costFunction, int checkHorizon) {
        this.processingDir = processingDir;
        this.network = network;
        this.tolerance = Objects.requireNonNull(tol);
        this.lsData = lsData;
        this.epsilon1target = epsilon1target;
        this.epsilon2 = epsilon2;
        // FIXME: This is a empirical calculation to speed up tests, but have enough iterations for real calculations
        int minimalIterations = Math.min(200000, 3 * epochs * allTrips.size());
        this.maxIter = Math.max(epochs * allTrips.size(), minimalIterations);
        this.random = random;
        this.fastLinkLookup = new FastLinkLookup(network, db);
        this.epsilon1start = epsilon1start;
        /** export the initial distribution of ratios */
        this.randomTrips = new RandomTripMaintainer(allTrips, checkHorizon, costFunction, random);
        this.tripMaintainer = new TripComparisonMaintainer(randomTrips, network, lsData, fastLinkLookup);

        /** export initial distribution */
        File diff = new File(processingDir, "diff");
        File plot = new File(processingDir, "plot");
        diff.mkdir();
        plot.mkdir();
        StaticHelper.exportRatioMap(diff, tripMaintainer.getLookupMap(), "Initial");
        StaticHelper.plotRatioMap(plot, randomTrips.getRatios(), "Initial");

        /** show initial score */
        System.out.println("Cost initial: " + randomTrips.getRatioCost());
    }

    public void runTripIterations() {
        int iterationCount = 0;
        Scalar lastCost = randomTrips.getRatioCost();
        System.out.println("Last cost before start: " + lastCost);
        System.out.println("Tolerance:              " + tolerance);

        while (iterationCount < maxIter) {
            try {
                ++iterationCount;
                Scalar epsilon1Scaled;
                epsilon1Scaled = RealScalar.of(epsilon1start.number().doubleValue() - ((epsilon1start.subtract(epsilon1target).number().doubleValue()) * iterationCount / maxIter));
                /** taking random trip */
                boolean isRandomTrip = random.nextDouble() <= epsilon2.number().doubleValue();
                TaxiTrip trip = isRandomTrip //
                        ? randomTrips.nextRandom() //
                        : tripMaintainer.getWorst(); // take currently worst trip

                /** create the shortest duration calculator using the linkSpeed data,
                 * must be done again to take into account newest updates */
                DurationCompare compareBefore = getPathDurationRatio(trip);
                if (compareBefore == null) {
                    continue;
                }
                Scalar nwPathDurationRatio = compareBefore.nwPathDurationRatio;
                Scalar ratioBefore = compareBefore.nwPathDurationRatio;

                /** if it is a random trip, record the ratio */
                if (isRandomTrip)
                    randomTrips.addRecordedRatio(ratioBefore);

                /** update cost based on random trips */
                lastCost = randomTrips.getRatioCost();

                /** rescale factor such that epsilon in [0,1] maps to [f,1] */
                Scalar rescaleFactor = RealScalar.ONE.subtract( //
                        (RealScalar.ONE.subtract(nwPathDurationRatio)).multiply(epsilon1Scaled));

                /** rescale links to approach desired link speed */
                ApplyScaling.to(lsData, trip, compareBefore.path, rescaleFactor, true);
                tripMaintainer.update(trip, nwPathDurationRatio);

                /** assess every 20 trips if ok */
                if (iterationCount % 100 == 0) {
                    System.out.println("iterationCount:       " + iterationCount);
                    System.out.println("worst cost: " + tripMaintainer.getWorstCost());
                    System.out.println("worst trip: " + tripMaintainer.getWorst().localId);
                    System.out.println("cost:       " + lastCost);
                }

                // DEBUGGING
                /** DEBUGGING every interval trips, export cost map */
                if (iterationCount % 10000 == 0) {
                    StaticHelper.exportRatioMap(new File(processingDir, "diff"), tripMaintainer.getLookupMap(), Integer.toString(iterationCount));
                    StaticHelper.plotRatioMap(new File(processingDir, "plot"), randomTrips.getRatios(), Integer.toString(iterationCount));
                }
                // DEBUGGING END
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        System.out.println("---- " + iterationCount + " ----");

        System.out.println("Cost End: " + randomTrips.getRatioCost());
    }

    private DurationCompare getPathDurationRatio(TaxiTrip trip) {
        /** create the shortest duration calculator using the linkSpeed data,
         * must be done again to take into account newest updates */
        LeastCostPathCalculator lcpc = LinkSpeedLeastPathCalculator.from(network, lsData);
        ShortestDurationCalculator calc = new ShortestDurationCalculator(lcpc, fastLinkLookup);

        /** compute ratio of network path and trip duration f */
        return DurationCompare.getInstance(trip, calc);
    }
}
