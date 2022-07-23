package de.tum.mw.ftm.amod.taxi.preprocessing.grid;

import amodeus.amodeus.util.math.GlobalAssert;
import com.google.common.base.Stopwatch;
import de.tum.mw.ftm.amod.geom.GridCell;
import de.tum.mw.ftm.amod.taxi.util.AmodeusUtil;
import org.apache.log4j.Logger;
import org.matsim.amodeus.config.FTMConfigGroup;
import org.matsim.amodeus.plpc.DefaultParallelLeastCostPathCalculator;
import org.matsim.amodeus.plpc.ParallelLeastCostPathCalculator;
import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.contrib.dvrp.path.VrpPathWithTravelData;
import org.matsim.contrib.dvrp.path.VrpPaths;
import org.matsim.core.network.NetworkUtils;
import org.matsim.core.router.DijkstraFactory;
import org.matsim.core.router.FastAStarLandmarksFactory;
import org.matsim.core.router.costcalculators.OnlyTimeDependentTravelDisutility;
import org.matsim.core.router.util.LeastCostPathCalculator;
import org.matsim.core.router.util.TravelTime;
import org.matsim.core.trafficmonitoring.FreeSpeedTravelTime;

import java.io.IOException;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import static de.tum.mw.ftm.amod.taxi.util.MatsimNetworkUtil.vrpPathIsConsistent;

public class GridInformationCalculator {
    private final static Logger logger = Logger.getLogger(GridInformationCalculator.class);
    private GridCell[] gridCells;
    private final int numberOfColumns;
    private final int numberOfRows;
    private final int numberOfCells;
    private final Network network;
    private final double[][] distancesBetweenCells;
    private final double[][] freeSpeedTravelTimes;
    private boolean informationCalculated;


    private GridInformationCalculator(int numberOfColumns, int numberOfRows, Network network) {
        this.numberOfColumns = numberOfColumns;
        this.numberOfRows = numberOfRows;
        this.network = network;
        this. numberOfCells = numberOfColumns * numberOfRows;
        this.distancesBetweenCells = new double[numberOfCells][numberOfCells];
        this.freeSpeedTravelTimes = new double[numberOfCells][numberOfCells];
        for (int i = 0; i < numberOfCells; i++) {
            Arrays.fill(distancesBetweenCells[i], 0);
            Arrays.fill(freeSpeedTravelTimes[i], 0);
        }
        this.informationCalculated = false;
    }

    public GridInformationCalculator(GridCell[] gridCells, int numberOfRows, int numberOfColumns, Network network) {
        this(numberOfColumns, numberOfRows, network);
        this.gridCells = gridCells;

    }

    public GridInformationCalculator(double minX, double maxX, double minY, double maxY, int numberOfRows, int numberOfColumns, Network network) {
        this(numberOfColumns, numberOfRows, network);
        this.gridCells = AmodeusUtil.createEvenSpacedGrid(minX, maxX, minY, maxY, numberOfColumns, numberOfRows);
        int numberOfCells = gridCells.length;
        GlobalAssert.that(numberOfCells == numberOfColumns * numberOfRows);

    }

    public void calculateGridInformation() throws IOException, ExecutionException, InterruptedException {
        TravelTime travelTime = new FreeSpeedTravelTime();

        ParallelLeastCostPathCalculator plcpc = DefaultParallelLeastCostPathCalculator.create(Runtime.getRuntime().availableProcessors(),
                new FastAStarLandmarksFactory(2), network, new OnlyTimeDependentTravelDisutility(travelTime), travelTime);




        int counter = 0;
        Stopwatch stopwatch = Stopwatch.createStarted();

        LinkedList<Future<LeastCostPathCalculator.Path>> futurePaths = new LinkedList<>();
        for (int i = 0; i < numberOfCells; i++) {
            for (int j = i + 1; j < numberOfCells; j++) {
                Coord startCoord = new Coord(gridCells[i].getCenterX(), gridCells[i].getCenterY());
                Coord endCoord = new Coord(gridCells[j].getCenterX(), gridCells[j].getCenterY());

                Link startLink = NetworkUtils.getNearestLink(network, startCoord);
                Link endLink = NetworkUtils.getNearestLink(network, endCoord);

                futurePaths.addLast(plcpc.calcLeastCostPath(startLink.getToNode(), endLink.getFromNode(), 0.0, null, null));
            }
        }

        for (int i = 0; i < numberOfCells; i++) {
            for (int j = i + 1; j < numberOfCells; j++) {
                Coord startCoord = new Coord(gridCells[i].getCenterX(), gridCells[i].getCenterY());
                Coord endCoord = new Coord(gridCells[j].getCenterX(), gridCells[j].getCenterY());

                Link startLink = NetworkUtils.getNearestLink(network, startCoord);
                Link endLink = NetworkUtils.getNearestLink(network, endCoord);

                Future<LeastCostPathCalculator.Path> futurePath = futurePaths.removeFirst();
                LeastCostPathCalculator.Path path = futurePath.get();
                VrpPathWithTravelData vrpPathWithTravelData = VrpPaths.createPath(startLink, endLink, 0.0, path, travelTime);
                GlobalAssert.that(vrpPathIsConsistent(vrpPathWithTravelData));

                double distance = VrpPaths.calcDistance(vrpPathWithTravelData);
                double freespeedTravelTime = vrpPathWithTravelData.getTravelTime();

                distancesBetweenCells[i][j] = distance;
                distancesBetweenCells[j][i] = distance;

                freeSpeedTravelTimes[i][j] = freespeedTravelTime;
                freeSpeedTravelTimes[j][i] = freespeedTravelTime;

                counter++;
            }
        }
        plcpc.close();
        stopwatch.stop();

        informationCalculated = true;

        logger.info(String.format("Processed %d distances in %d seconds", counter, stopwatch.elapsed(TimeUnit.SECONDS)));
    }

    public double[][] getDistancesBetweenCells() {
        if (!informationCalculated) {
            throw new ExecutionOrderException("Calculate grid information before trying to retrieve it");
        }
        return distancesBetweenCells;
    }

    public double[][] getFreeSpeedTravelTimes() {
        if (!informationCalculated) {
            throw new ExecutionOrderException("Calculate grid information before trying to retrieve it");
        }
        return freeSpeedTravelTimes;
    }
}
