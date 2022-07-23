package de.tum.mw.ftm.amod.taxi.lp;

import gurobi.GRBException;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Random;

import static org.junit.Assert.assertArrayEquals;

public class GurobiRebalancingTest {
    private static int numberOfCells;
    private static int rebalancingSteps;
    private static RebalancingSolver rebalancingSolver;
    private static Random random;
    private static int[][][] expectedRebalancing;
    private static ArrayList<double[]> zeroDemand;
    private static int[] initialTaxiPlacement;
    private static int[][] occupiedTaxisBecomingUnoccupied;
    private static double[][] probabilityMatrix;
    private static double[][] distanceMatrix;
    private static double[][] travelTimes;


    @BeforeClass
    public static void setUpOnce() throws GRBException {
        int rebalancingSeconds = 45 * 60;
        int numberOfRows = 2;
        int numberOfColumns = 2;
        numberOfCells = numberOfRows * numberOfColumns;
        rebalancingSteps = 3;
        distanceMatrix = new double[numberOfCells][numberOfCells];
        probabilityMatrix = new double[numberOfCells][numberOfCells];
        travelTimes = new double[numberOfCells][numberOfCells];

        for (double[] matrix : distanceMatrix) {
            Arrays.fill(matrix, 1000);
        }

        for (double[] matrix : travelTimes) {
            Arrays.fill(matrix, 100);
        }

        rebalancingSolver = new RebalancingSolver(rebalancingSeconds,
                numberOfRows,
                numberOfColumns,
                rebalancingSteps,
                distanceMatrix,
                0.1855,
                12.26);
        rebalancingSolver.initialize();
        random = new Random(42);

        expectedRebalancing = new int[rebalancingSteps][numberOfCells][numberOfCells];


        initialTaxiPlacement = new int[numberOfCells];
        occupiedTaxisBecomingUnoccupied = new int[rebalancingSteps - 1][numberOfCells];
    }

    @Before
    public void resetArrays() {
        for (int timestep = 0; timestep < rebalancingSteps; timestep++) {
            for (int cellIndex = 0; cellIndex < numberOfCells; cellIndex++) {
                Arrays.fill(expectedRebalancing[timestep][cellIndex], 0);
            }

            if (timestep < rebalancingSteps - 1) {
                Arrays.fill(occupiedTaxisBecomingUnoccupied[timestep], 0);
            }
        }

        Arrays.fill(initialTaxiPlacement, 0);

        for (int i = 0; i < distanceMatrix.length; i++) {
            Arrays.fill(probabilityMatrix[i], 0);
            probabilityMatrix[i][i] = 1;
        }

        zeroDemand = new ArrayList<>();
        for (int i = 0; i < numberOfCells; i++) {
            zeroDemand.add(new double[rebalancingSteps]);
        }
    }

    @Test
    public void noDemandAndThereforeNoRebalancing() throws GRBException {
        for (int i = 0; i < initialTaxiPlacement.length; i++) {
            initialTaxiPlacement[i] = random.nextInt(10);
        }

        for (int i = 0; i < rebalancingSteps - 1; i++) {
            for (int j = 0; j < numberOfCells; j++) {
                occupiedTaxisBecomingUnoccupied[i][j] = random.nextInt(10);
            }
        }

        rebalancingSolver.updateModel(zeroDemand, initialTaxiPlacement, occupiedTaxisBecomingUnoccupied,
                probabilityMatrix, travelTimes);

        int[][][] rebalancing = rebalancingSolver.calculateRebalancing();
        assertArrayEquals(expectedRebalancing, rebalancing);
    }

    @Test
    public void supplyInDifferentCellAndThereforeOneTaxiRebalancing() throws GRBException {
        ArrayList<double[]> predictedDemand = zeroDemand;
        predictedDemand.get(0)[0] = 1;

        initialTaxiPlacement[1] = 1;

        expectedRebalancing[0][1][0] = 1;

        rebalancingSolver.updateModel(predictedDemand, initialTaxiPlacement, occupiedTaxisBecomingUnoccupied,
                probabilityMatrix, travelTimes);

        int[][][] rebalancing = rebalancingSolver.calculateRebalancing();

        assertArrayEquals(expectedRebalancing, rebalancing);
    }

    @Test
    public void undersupplyAndThereforeNoRebalancing() throws GRBException {
        ArrayList<double[]> predictedDemand = zeroDemand;

        for (int i = 0; i < numberOfCells; i++) {
            Arrays.fill(predictedDemand.get(i), 2);
        }


        Arrays.fill(initialTaxiPlacement, 1);

        rebalancingSolver.updateModel(predictedDemand, initialTaxiPlacement, occupiedTaxisBecomingUnoccupied,
                probabilityMatrix, travelTimes);

        int[][][] rebalancing = rebalancingSolver.calculateRebalancing();

        assertArrayEquals(expectedRebalancing, rebalancing);
    }

    @Test
    public void oversupplyAndThereforeNoRebalancing() throws GRBException {
        ArrayList<double[]> predictedDemand = zeroDemand;

        for (int i = 0; i < numberOfCells; i++) {
            Arrays.fill(predictedDemand.get(i), 1);
        }


        Arrays.fill(initialTaxiPlacement, 2);

        rebalancingSolver.updateModel(predictedDemand, initialTaxiPlacement, occupiedTaxisBecomingUnoccupied,
                probabilityMatrix, travelTimes);

        int[][][] rebalancing = rebalancingSolver.calculateRebalancing();

        assertArrayEquals(expectedRebalancing, rebalancing);
    }

    @Test
    public void oversupplyInOneCellRebalancingToOtherCells() throws GRBException {
        ArrayList<double[]> predictedDemand = zeroDemand;

        for (int i = 0; i < numberOfCells; i++) {
            predictedDemand.get(i)[0] = 1;
        }


        initialTaxiPlacement[0] = 20;

        rebalancingSolver.updateModel(predictedDemand, initialTaxiPlacement, occupiedTaxisBecomingUnoccupied,
                probabilityMatrix, travelTimes);

        expectedRebalancing[0][0][1] = 1;
        expectedRebalancing[0][0][2] = 1;
        expectedRebalancing[0][0][3] = 1;

        int[][][] rebalancing = rebalancingSolver.calculateRebalancing();

        assertArrayEquals(expectedRebalancing, rebalancing);
    }

    @Test
    public void taxiBecomingUnoccupiedAndThereforeNoRebalancing() throws GRBException {
        ArrayList<double[]> predictedDemand = zeroDemand;

        predictedDemand.get(0)[0] = 1;


        initialTaxiPlacement[1] = 1;

        occupiedTaxisBecomingUnoccupied[0][0] = 1;

        rebalancingSolver.updateModel(predictedDemand, initialTaxiPlacement, occupiedTaxisBecomingUnoccupied,
                probabilityMatrix, travelTimes);

        int[][][] rebalancing = rebalancingSolver.calculateRebalancing();

        assertArrayEquals(expectedRebalancing, rebalancing);
    }

    @Test
    public void rebalancingDistanceTooLargeAndThereforeNoRebalancing() throws GRBException {
        int numberOfRows = 2;
        int numberOfColumns = 2;
        double[][] distanceMatrix = new double[numberOfCells][numberOfCells];
        double[][] probabilityMatrix = new double[numberOfCells][numberOfCells];
        for (int i = 0; i < distanceMatrix.length; i++) {
            Arrays.fill(distanceMatrix[i], 10000);
            Arrays.fill(probabilityMatrix[i], 1.0 / (numberOfColumns * numberOfRows));
        }

        RebalancingSolver rebalancingSolver = new RebalancingSolver(60,
                numberOfRows,
                numberOfColumns,
                rebalancingSteps,
                distanceMatrix,
                0.1855,
                12.26);
        rebalancingSolver.initialize();

        ArrayList<double[]> predictedDemand = zeroDemand;

        predictedDemand.get(0)[0] = 1;

        initialTaxiPlacement[1] = 1;

        rebalancingSolver.updateModel(predictedDemand, initialTaxiPlacement, occupiedTaxisBecomingUnoccupied,
                probabilityMatrix, travelTimes);

        int[][][] rebalancing = rebalancingSolver.calculateRebalancing();

        assertArrayEquals(expectedRebalancing, rebalancing);
    }

    @Test
    public void tripFromPreviousTimestepFulfillsDemand() throws GRBException {
        for (int i = 0; i < distanceMatrix.length; i++) {
            Arrays.fill(probabilityMatrix[i], 0);
        }

        probabilityMatrix[1][0] = 1;

        rebalancingSolver.initialize();

        ArrayList<double[]> predictedDemand = zeroDemand;

        predictedDemand.get(0)[0] = 1;
        predictedDemand.get(1)[0] = 1;

        initialTaxiPlacement[1] = 1;
        initialTaxiPlacement[2] = 1;

        rebalancingSolver.updateModel(predictedDemand, initialTaxiPlacement, occupiedTaxisBecomingUnoccupied,
                probabilityMatrix, travelTimes);

        int[][][] rebalancing = rebalancingSolver.calculateRebalancing();

        assertArrayEquals(expectedRebalancing, rebalancing);
    }

}
