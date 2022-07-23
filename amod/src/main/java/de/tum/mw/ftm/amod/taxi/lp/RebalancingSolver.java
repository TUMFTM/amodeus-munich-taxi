package de.tum.mw.ftm.amod.taxi.lp;

import amodeus.amodeus.util.math.GlobalAssert;
import gurobi.GRBException;
import org.apache.log4j.Logger;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Utility class which helps interacting with the underlying Gurobi LP model.
 */
public class RebalancingSolver {
    private final int rebalancingSeconds;
    private final int numberOfCells;
    private final int rebalancingSteps;
    private final double[][] distanceMatrix;
    private RebalancingGRBModel rebalancingGRBModel;
    private boolean updatedSinceLastOptimization;
    private final double alpha;
    private final double lambda;
    private final Logger logger = Logger.getLogger(RebalancingSolver.class);

    public RebalancingSolver(int rebalancingSeconds,
                             int numberOfRows,
                             int numberOfColumns,
                             int rebalancingSteps,
                             double[][] distanceMatrix,
                             double alpha,
                             double lambda)
    {
        this.rebalancingSeconds = rebalancingSeconds;
        this.numberOfCells = numberOfRows * numberOfColumns;
        this.rebalancingSteps = rebalancingSteps;
        this.updatedSinceLastOptimization = false;
        this.distanceMatrix = distanceMatrix;
        // Cost parameter in this case variable costs per km.
        this.alpha = alpha * Math.pow(10.0, -3.0);;
        // Discount, EUR per unserved Request
        this.lambda = lambda;
    }

    public void initialize() throws GRBException {
        GlobalAssert.that(distanceMatrix.length == numberOfCells);
        for (double[] matrix : distanceMatrix) {
            GlobalAssert.that(matrix.length == numberOfCells);
        }

        rebalancingGRBModel = new RebalancingGRBModel();

        rebalancingGRBModel.createModelVariables(numberOfCells, rebalancingSteps);

        rebalancingGRBModel.addObjectiveFunction(numberOfCells, rebalancingSteps,
                1, alpha, distanceMatrix);
        rebalancingGRBModel.addConstraints(numberOfCells, rebalancingSteps);
    }

    /**
     * Updates the underlying model with the given data.
     * @param predictedDemand: predicted demand for the next rebalancing steps.
     * @param initialTaxiPlacement: current placement of the available taxis. Index is the current cell index.
     * @param occupiedTaxisBecomingUnoccupied: Number of occupied taxis becoming unoccupied during the next
     *                                         rebalancing steps. Index: [rebalancingStep][cellIndex]
     * @param probabilityMatrix: array which contains the probability that trips end in a specific cell when they've
     *                           started in a specific one. Index: [startCell][endCell]
     * @param travelTimes: travel durations between different cells. Index: [startCell][endCell]
     */
    public void updateModel(ArrayList<double[]> predictedDemand, int[] initialTaxiPlacement,
                            int[][] occupiedTaxisBecomingUnoccupied, double[][] probabilityMatrix,
                            double[][] travelTimes) throws GRBException {
        GlobalAssert.that(rebalancingGRBModel != null);
        logger.info(String.format("Updating Model:\n" +
                "InitialTaxiPlacement: %s \n" +
                "Sum of available Taxis: %s", Arrays.toString(initialTaxiPlacement), Arrays.stream(initialTaxiPlacement).sum()));
        rebalancingGRBModel.updateModel(numberOfCells, rebalancingSteps, predictedDemand,
                initialTaxiPlacement, occupiedTaxisBecomingUnoccupied, probabilityMatrix,
                rebalancingSeconds, travelTimes);

        updatedSinceLastOptimization = true;
    }

    /**
     * Solves the LP problem and resets the updatedSinceLastOptimization flag.
     * @return Returns a integer array [time][startCell][endCell] with the number of vehicles to rebalance.
     */
    public int[][][] calculateRebalancing() throws GRBException {
        GlobalAssert.that(updatedSinceLastOptimization);
        GlobalAssert.that(rebalancingGRBModel != null);

        boolean optimizationSuccessful = rebalancingGRBModel.optimizeModel();
        int[][][] rebalancingAmount = rebalancingGRBModel.getRebalancingAmount(numberOfCells, rebalancingSteps);

        // Additional constraints were added for feasibility --> Need to recreate Gurobi model
        if (!optimizationSuccessful) {
            this.initialize();
        }

        updatedSinceLastOptimization = false;

        return rebalancingAmount;
    }

}
