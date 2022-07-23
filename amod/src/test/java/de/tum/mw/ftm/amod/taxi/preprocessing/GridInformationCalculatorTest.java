package de.tum.mw.ftm.amod.taxi.preprocessing;

import amodeus.amodeus.util.matsim.NetworkLoader;
import de.tum.mw.ftm.amod.geom.GridCell;
import de.tum.mw.ftm.amod.taxi.preprocessing.demandprediction.GridDemandPrediction;
import de.tum.mw.ftm.amod.taxi.preprocessing.demandprediction.UniformGridWithBorderPredictions;
import de.tum.mw.ftm.amod.taxi.preprocessing.grid.ExecutionOrderException;
import de.tum.mw.ftm.amod.taxi.preprocessing.grid.GridInformationCalculator;
import org.junit.BeforeClass;
import org.junit.Test;
import org.matsim.amodeus.config.FTMConfigGroup;
import org.matsim.api.core.v01.network.Network;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.config.groups.NetworkConfigGroup;

import java.io.File;
import java.io.IOException;
import java.util.concurrent.ExecutionException;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class GridInformationCalculatorTest {
    private static double allowedDoubleDeviation;
    private static Network network;
    private static FTMConfigGroup ftmConfigGroup;
    private static GridDemandPrediction gridDemandPrediction;

    @BeforeClass
    public static void setupOnce() {
        allowedDoubleDeviation = 0.001;
        Config config = ConfigUtils.loadConfig("src/test/resources/config_full.xml", new FTMConfigGroup());
        ftmConfigGroup = (FTMConfigGroup) config.getModules().get("ftm_simulation");
        NetworkConfigGroup networkConfigGroup = config.network();
        network = NetworkLoader.fromNetworkFile(new File("src/test/resources", networkConfigGroup.getInputFile()));
        gridDemandPrediction = UniformGridWithBorderPredictions.fromXML("src/test/resources/prediction.xml");
    }

    @Test(expected = ExecutionOrderException.class)
    public void tryToRetrieveDistancesBeforeCalculationShouldThrowExecutionOrderException() {
        GridInformationCalculator gridInformationCalculator = new GridInformationCalculator(gridDemandPrediction.getGridCells(),
                gridDemandPrediction.getNumberOfRows(), gridDemandPrediction.getNumberOfColumns(), network);
        gridInformationCalculator.getDistancesBetweenCells();
    }

    @Test(expected = ExecutionOrderException.class)
    public void tryToRetrieveTravelTimesBeforeCalculationShouldThrowExecutionOrderException() {
        GridInformationCalculator gridInformationCalculator = new GridInformationCalculator(gridDemandPrediction.getGridCells(),
                gridDemandPrediction.getNumberOfRows(), gridDemandPrediction.getNumberOfColumns(), network);
        gridInformationCalculator.getFreeSpeedTravelTimes();
    }

    @Test
    public void calculateGridInformationAndDistanceArrayShouldBeSymmetric() throws InterruptedException, ExecutionException, IOException {
        GridInformationCalculator gridInformationCalculator = new GridInformationCalculator(gridDemandPrediction.getGridCells(),
                gridDemandPrediction.getNumberOfRows(), gridDemandPrediction.getNumberOfColumns(), network);
        gridInformationCalculator.calculateGridInformation();
        double[][] distancesBetweenCells = gridInformationCalculator.getDistancesBetweenCells();

        checkSymmetry(distancesBetweenCells);
    }

    @Test
    public void calculateGridInformationAndTravelTimeArrayShouldBeSymmetric() throws InterruptedException, ExecutionException, IOException {
        GridInformationCalculator gridInformationCalculator = new GridInformationCalculator(gridDemandPrediction.getGridCells(),
                gridDemandPrediction.getNumberOfRows(), gridDemandPrediction.getNumberOfColumns(), network);
        gridInformationCalculator.calculateGridInformation();
        double[][] freeSpeedTravelTimes = gridInformationCalculator.getFreeSpeedTravelTimes();

        checkSymmetry(freeSpeedTravelTimes);
    }

    @Test
    public void calculateGridInformationAndDistanceArrayShouldBePositive() throws InterruptedException, ExecutionException, IOException {
        GridInformationCalculator gridInformationCalculator = new GridInformationCalculator(gridDemandPrediction.getGridCells(),
                gridDemandPrediction.getNumberOfRows(), gridDemandPrediction.getNumberOfColumns(), network);
        gridInformationCalculator.calculateGridInformation();
        double[][] distancesBetweenCells = gridInformationCalculator.getDistancesBetweenCells();

        checkPositivity(distancesBetweenCells);
    }

    @Test
    public void calculateGridInformationAndTravelTimeArrayShouldBePositive() throws InterruptedException, ExecutionException, IOException {
        GridInformationCalculator gridInformationCalculator = new GridInformationCalculator(gridDemandPrediction.getGridCells(),
                gridDemandPrediction.getNumberOfRows(), gridDemandPrediction.getNumberOfColumns(), network);
        gridInformationCalculator.calculateGridInformation();
        double[][] freeSpeedTravelTimes = gridInformationCalculator.getFreeSpeedTravelTimes();

        checkPositivity(freeSpeedTravelTimes);
    }


    private static void checkSymmetry(double[][] matrix) {
        for (int i = 0; i < matrix.length; i++) {
            assertEquals(matrix.length, matrix[i].length);
            for (int j = 0; j < matrix[i].length; j++) {
                assertEquals(matrix[i][j], matrix[j][i], allowedDoubleDeviation);
            }
        }
    }

    private static void checkPositivity(double[][] matrix) {
        for (int i = 0; i < matrix.length; i++) {
            for (int j = 0; j < matrix[i].length; j++) {
                if (i == j) {
                    assertEquals(matrix[i][j], 0.0, allowedDoubleDeviation);
                } else {
                    assertTrue(matrix[i][j] >= 0.0);
                }
            }
        }
    }
}
