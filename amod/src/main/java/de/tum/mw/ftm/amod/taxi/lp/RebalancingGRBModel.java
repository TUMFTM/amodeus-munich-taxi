package de.tum.mw.ftm.amod.taxi.lp;

import gurobi.*;
import org.apache.log4j.Logger;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class RebalancingGRBModel {
    private final static Logger logger = Logger.getLogger(RebalancingGRBModel.class);

    private final GRBModel grbModel;
    private final List<GRBConstr> constrainedToBeRemovedOnUpdate;

    // GRBVar[time][cell_origin][cell_destination]
    private GRBVar[][][] rebalancingVars;

    // GRBVar[time][cell]
    private GRBVar[][] freeTaxis;
    private GRBVar[][] supplyVars;
    private GRBVar[][] demandVars;
    private GRBVar[][] supplyMinusDemandVars;
    private GRBVar[][] demandMinusSupplyVars;
    private GRBVar[][] minSupplyMinusDemandAndZero;
    private GRBVar[][] maxSupplyMinusDemandAndZero;
    private GRBVar[][] maxDemandMinusSupplyAndZero;
    private GRBVar[][] occupiedTaxisBecomingUnoccupied;
    private GRBVar[][] minSupplyDemandVars;

    public RebalancingGRBModel() throws GRBException {
        this.grbModel = new GRBModel(GurobiEnvSingleton.getInstance().getGrbEnv());
        this.constrainedToBeRemovedOnUpdate = new ArrayList<>();
    }

    /**
     * Initializes all model variables which are needed by Gurobi.
     * @param numberOfCells: Absolute number of grid cells.
     * @param rebalancingSteps: Number of rebalancing steps which the model should calculate/output.
     */
    public void createModelVariables(int numberOfCells, int rebalancingSteps) throws GRBException {
        rebalancingVars = new GRBVar[rebalancingSteps][numberOfCells][numberOfCells];
        supplyVars = new GRBVar[rebalancingSteps][numberOfCells];
        demandVars = new GRBVar[rebalancingSteps][numberOfCells];
        supplyMinusDemandVars = new GRBVar[rebalancingSteps][numberOfCells];
        demandMinusSupplyVars = new GRBVar[rebalancingSteps][numberOfCells];
        minSupplyMinusDemandAndZero = new GRBVar[rebalancingSteps][numberOfCells];
        maxSupplyMinusDemandAndZero = new GRBVar[rebalancingSteps][numberOfCells];
        maxDemandMinusSupplyAndZero = new GRBVar[rebalancingSteps][numberOfCells];
        freeTaxis = new GRBVar[rebalancingSteps][numberOfCells];
        occupiedTaxisBecomingUnoccupied = new GRBVar[rebalancingSteps - 1][numberOfCells];
        minSupplyDemandVars = new GRBVar[rebalancingSteps][numberOfCells];


        for (int timestep = 0; timestep < rebalancingSteps; timestep++) {
            for (int startCell = 0; startCell < numberOfCells; startCell++) {
                // Variable for the supply in different cells
                supplyVars[timestep][startCell] = grbModel.addVar(Double.NEGATIVE_INFINITY, Double.POSITIVE_INFINITY, 0.0, GRB.CONTINUOUS,
                        String.format("supply_%d_%d", timestep, startCell));
                // Variable for the demand in different cells
                demandVars[timestep][startCell] = grbModel.addVar(Double.NEGATIVE_INFINITY, Double.POSITIVE_INFINITY, 0.0, GRB.CONTINUOUS,
                        String.format("demand_%d_%d", timestep, startCell));
                // helper variable for calculating supply - demand
                supplyMinusDemandVars[timestep][startCell] = grbModel.addVar(Double.NEGATIVE_INFINITY, Double.POSITIVE_INFINITY, 0.0, GRB.CONTINUOUS,
                        String.format("supply-demand_%d_%d", timestep, startCell));
                // helper variable for calculating demand - supply
                demandMinusSupplyVars[timestep][startCell] = grbModel.addVar(Double.NEGATIVE_INFINITY, Double.POSITIVE_INFINITY, 0.0, GRB.CONTINUOUS,
                        String.format("demand-supply_%d_%d", timestep, startCell));
                // helper variable for calculating min(supply - demand, 0)
                minSupplyMinusDemandAndZero[timestep][startCell] = grbModel.addVar(Double.NEGATIVE_INFINITY, Double.POSITIVE_INFINITY, 0.0, GRB.CONTINUOUS,
                        String.format("min_supply-demand_and_0_%d_%d", timestep, startCell));
                // helper variable for calculating max(supply - demand, 0)
                maxSupplyMinusDemandAndZero[timestep][startCell] = grbModel.addVar(Double.NEGATIVE_INFINITY, Double.POSITIVE_INFINITY, 0.0, GRB.CONTINUOUS,
                        String.format("max_supply-demand_and_0_%d_%d", timestep, startCell));
                // helper variable for calculating max(demand - supply, 0)
                maxDemandMinusSupplyAndZero[timestep][startCell] = grbModel.addVar(Double.NEGATIVE_INFINITY, Double.POSITIVE_INFINITY, 0.0, GRB.CONTINUOUS,
                        String.format("max_demand-supply_and_0_%d_%d", timestep, startCell));
                // variable for free taxis in different cells
                freeTaxis[timestep][startCell] = grbModel.addVar(Double.NEGATIVE_INFINITY, Double.POSITIVE_INFINITY, 0.0, GRB.CONTINUOUS,
                        String.format("free_%d_%d", timestep, startCell));
                // helper variable for calculating min(supply, demand)
                minSupplyDemandVars[timestep][startCell] = grbModel.addVar(Double.NEGATIVE_INFINITY, Double.POSITIVE_INFINITY, 0.0, GRB.CONTINUOUS,
                        String.format("min_supply_demand_%d_%d", timestep, startCell));

                // Add variable for describing how many occupied taxis are becoming unoccupied
                if (timestep < rebalancingSteps - 1) {
                    occupiedTaxisBecomingUnoccupied[timestep][startCell] = grbModel.addVar(Double.NEGATIVE_INFINITY,
                            Double.POSITIVE_INFINITY, 0.0, GRB.CONTINUOUS,
                            String.format("occupiedBecomingUnoccupied_%d_%d", timestep, startCell));
                }

                // variable for the rebalancing itself
                for (int endCell = 0; endCell < numberOfCells; endCell++) {
                    rebalancingVars[timestep][startCell][endCell] =
                            grbModel.addVar(0, Double.POSITIVE_INFINITY, 0.0, GRB.CONTINUOUS,
                                    String.format("rebalancing_%d_%d_%d", timestep, startCell, endCell));
                }
            }

        }

    }

    /**
     * Creates the target function which should be maximized by the model
     * @param numberOfCells: absolute number of cells
     * @param rebalancingSteps: number of rebalancing steps of the model
     * @param lambda: weighting factor
     * @param alpha: distance cost factor
     * @param distanceMatrix: distances between the cells
     */
    public void addObjectiveFunction(int numberOfCells, int rebalancingSteps, double lambda,
                                     double alpha, double[][] distanceMatrix) throws GRBException {
        GRBLinExpr expr = new GRBLinExpr();

        // Target function from Wittmann 2020 eq 1;
        for (int timestep = 0; timestep < rebalancingSteps; timestep++) {
            double gamma = 1 - timestep * 0.9;
            for (int startCell = 0; startCell < numberOfCells; startCell++) {
                expr.addTerm(gamma * lambda, minSupplyMinusDemandAndZero[timestep][startCell]);
                for (int endCell = 0; endCell < numberOfCells; endCell++) {
                    if (startCell != endCell) {
                        expr.addTerm(-gamma * alpha * distanceMatrix[startCell][endCell],
                                rebalancingVars[timestep][startCell][endCell]);
                    }
                }
            }
        }
        grbModel.setObjective(expr, GRB.MAXIMIZE);
    }

    /**
     * Adds constraints which are always valid and are not needed to be updated every time.
     * @param numberOfCells: absolute number of cells
     * @param rebalancingSteps: number of rebalancing steps of the model
     */
    public void addConstraints(int numberOfCells, int rebalancingSteps) throws GRBException {
        GRBLinExpr leftHandExpr;
        GRBLinExpr rightHandExpr;

        // There cannot be more rebalancing than free taxis are available in a cell.
        for (int timestep = 0; timestep < rebalancingSteps; timestep++) {
            for (int startCell = 0; startCell < numberOfCells; startCell++) {
                leftHandExpr = new GRBLinExpr();
                rightHandExpr = new GRBLinExpr();
                double[] coeffArray = new double[numberOfCells];
                Arrays.fill(coeffArray, 1.0);
                leftHandExpr.addTerms(coeffArray, rebalancingVars[timestep][startCell]);

                rightHandExpr.addTerm(1.0, freeTaxis[timestep][startCell]);
                grbModel.addConstr(leftHandExpr, GRB.LESS_EQUAL, rightHandExpr,
                        String.format("2_%d_%d", timestep, startCell));
            }
        }

        // Supply = free taxis - rebalanced taxis
        for (int timestep = 0; timestep < rebalancingSteps; timestep++) {
            for (int startCell = 0; startCell < numberOfCells; startCell++) {
                leftHandExpr = new GRBLinExpr();
                leftHandExpr.addTerm(1, supplyVars[timestep][startCell]);

                rightHandExpr = new GRBLinExpr();
                rightHandExpr.addTerm(1.0, freeTaxis[timestep][startCell]);
                double[] coeffArray = new double[numberOfCells];
                Arrays.fill(coeffArray, -1.0);
                rightHandExpr.addTerms(coeffArray, rebalancingVars[timestep][startCell]);


                grbModel.addConstr(leftHandExpr, GRB.EQUAL, rightHandExpr,
                        String.format("4_%d_%d", timestep, startCell));
            }
        }

        // Helper variable initialization
        for (int timestep = 0; timestep < rebalancingSteps; timestep++) {
            for (int startCell = 0; startCell < numberOfCells; startCell++) {
                leftHandExpr = new GRBLinExpr();
                leftHandExpr.addTerm(1, supplyMinusDemandVars[timestep][startCell]);

                rightHandExpr = new GRBLinExpr();
                rightHandExpr.addTerm(1.0, supplyVars[timestep][startCell]);
                rightHandExpr.addTerm(-1.0, demandVars[timestep][startCell]);

                grbModel.addConstr(leftHandExpr, GRB.EQUAL, rightHandExpr,
                        String.format("supply-demand-constraint_%d_%d", timestep, startCell));
            }
        }

        // Helper variable initialization
        for (int timestep = 0; timestep < rebalancingSteps; timestep++) {
            for (int startCell = 0; startCell < numberOfCells; startCell++) {
                leftHandExpr = new GRBLinExpr();
                leftHandExpr.addTerm(1, demandMinusSupplyVars[timestep][startCell]);

                rightHandExpr = new GRBLinExpr();
                rightHandExpr.addTerm(-1.0, supplyVars[timestep][startCell]);
                rightHandExpr.addTerm(1.0, demandVars[timestep][startCell]);

                grbModel.addConstr(leftHandExpr, GRB.EQUAL, rightHandExpr,
                        String.format("demand-supply_constraint_%d_%d", timestep, startCell));
            }
        }

        // Helper variable initialization
        for (int timestep = 0; timestep < rebalancingSteps; timestep++) {
            for (int startCell = 0; startCell < numberOfCells; startCell++) {
                grbModel.addGenConstrMax(maxSupplyMinusDemandAndZero[timestep][startCell],
                        new GRBVar[]{supplyMinusDemandVars[timestep][startCell]}, 0,
                        String.format("max_supply-demand_constraint_%d_%d", timestep, startCell));

                grbModel.addGenConstrMin(minSupplyMinusDemandAndZero[timestep][startCell],
                        new GRBVar[]{supplyMinusDemandVars[timestep][startCell]}, 0,
                        String.format("min_supply-demand_constraint_%d_%d", timestep, startCell));

                grbModel.addGenConstrMax(maxDemandMinusSupplyAndZero[timestep][startCell],
                        new GRBVar[]{demandMinusSupplyVars[timestep][startCell]}, 0,
                        String.format("max_demand-supply_constraint_%d_%d", timestep, startCell));

                grbModel.addGenConstrMin(minSupplyDemandVars[timestep][startCell],
                        new GRBVar[]{supplyVars[timestep][startCell], demandVars[timestep][startCell]},
                        1000, String.format("min_demand_supply_constraint_%d_%d", timestep, startCell));
            }
        }
    }

    /**
     * Updates the dynamic constraints of the Gurobi model.
     * @param numberOfCells: absolute number of cells
     * @param rebalancingSteps: number of rebalancing steps of the model
     * @param predictedDemand: predicted demand for the next rebalancing steps.
     * @param initialTaxiPlacement: current placement of the available taxis. Index is the current cell index.
     * @param occupiedTaxisBecomingUnoccupied: Number of occupied taxis becoming unoccupied during the next
     *                                         rebalancing steps. Index: [rebalancingStep][cellIndex]
     * @param probabilityMatrix: array which contains the probability that trips end in a specific cell when they've
     *                           started in a specific one. Index: [startCell][endCell]
     * @param travelTimes: travel durations between different cells. Index: [startCell][endCell]
     */
    public void updateModel(int numberOfCells, int rebalancingSteps,
                            ArrayList<double[]> predictedDemand, int[] initialTaxiPlacement,
                            int[][] occupiedTaxisBecomingUnoccupied,
                            double[][] probabilityMatrix, int rebalancingPeriodSeconds,
                            double[][] travelTimes) throws GRBException {

        for (GRBConstr constr : constrainedToBeRemovedOnUpdate) {
            grbModel.remove(constr);
        }

        // Remove constraints from last time step
        constrainedToBeRemovedOnUpdate.clear();
        grbModel.reset();

        GRBLinExpr leftHandExpr;

        // rebalancing is 0 if start and end cell are the same, the end cell can't be reached in time or it is the last
        // rebalancing timestep
        for (int timestep = 0; timestep < rebalancingSteps; timestep++) {
            for (int startCell = 0; startCell < numberOfCells; startCell++) {
                for (int endCell = 0; endCell < numberOfCells; endCell++) {
                    if (startCell == endCell
                            || travelTimes[startCell][endCell] > rebalancingPeriodSeconds
                            || timestep == rebalancingSteps - 1) {
                        leftHandExpr = new GRBLinExpr();
                        leftHandExpr.addTerm(1.0, rebalancingVars[timestep][startCell][endCell]);
                        constrainedToBeRemovedOnUpdate.add(
                                grbModel.addConstr(leftHandExpr, GRB.EQUAL, 0.0,
                                        String.format("3_%d_%d_%d", timestep, startCell, endCell)));
                    }
                }
            }
        }

        // Demand is predicted demand + unfulfilled demand from last timestep
        for (int timestep = 0; timestep < rebalancingSteps; timestep++) {
            for (int cellIndex = 0; cellIndex < numberOfCells; cellIndex++) {
                leftHandExpr = new GRBLinExpr();
                leftHandExpr.addTerm(1.0, demandVars[timestep][cellIndex]);
                leftHandExpr.addConstant(-predictedDemand.get(cellIndex)[timestep]);

                if (timestep > 0) {
                    leftHandExpr.addTerm(-1.0, maxDemandMinusSupplyAndZero[timestep - 1][cellIndex]);
                }

                constrainedToBeRemovedOnUpdate.add(
                        grbModel.addConstr(leftHandExpr, GRB.EQUAL, 0.0,
                                String.format("demand_%d_%d", timestep, cellIndex)));
            }
        }

        // Set occupiedTaxisBecomingUnoccupied variable based on input
        for (int timestep = 0; timestep < rebalancingSteps - 1; timestep++) {
            for (int cellIndex = 0; cellIndex < numberOfCells; cellIndex++) {
                leftHandExpr = new GRBLinExpr();
                leftHandExpr.addTerm(1.0, this.occupiedTaxisBecomingUnoccupied[timestep][cellIndex]);

                leftHandExpr.addConstant(-occupiedTaxisBecomingUnoccupied[timestep][cellIndex]);

                constrainedToBeRemovedOnUpdate.add(
                        grbModel.addConstr(leftHandExpr, GRB.EQUAL, 0.0,
                                String.format("taxis_becoming_unoccupied_%d_%d", timestep, cellIndex)));
            }
        }


        for (int timestep = 1; timestep < rebalancingSteps; timestep++) {
            for (int startCell = 0; startCell < numberOfCells; startCell++) {
                leftHandExpr = new GRBLinExpr();
                leftHandExpr.addTerm(1, freeTaxis[timestep][startCell]);

                GRBLinExpr rightHandExpr = new GRBLinExpr();
                rightHandExpr.addTerm(1.0, maxSupplyMinusDemandAndZero[timestep - 1][startCell]);

                for (int endCell = 0; endCell < numberOfCells; endCell++) {
                    rightHandExpr.addTerm(1.0, rebalancingVars[timestep - 1][endCell][startCell]);

                    if (travelTimes[endCell][startCell] < timestep * ((double) rebalancingPeriodSeconds / rebalancingSteps)
                            && travelTimes[endCell][startCell] > (timestep - 1) * ((double) rebalancingPeriodSeconds / rebalancingSteps)) {
                        rightHandExpr.addTerm(probabilityMatrix[endCell][startCell],
                                minSupplyDemandVars[timestep - 1][endCell]);
                    }
                }

                rightHandExpr.addTerm(1.0, this.occupiedTaxisBecomingUnoccupied[timestep - 1][startCell]);

                constrainedToBeRemovedOnUpdate.add(
                        grbModel.addConstr(leftHandExpr, GRB.EQUAL, rightHandExpr,
                                String.format("6_%d_%d", timestep, startCell)));
            }
        }

        // Free taxis are equal to initial taxi placement for first timestep
        for (int startCell = 0; startCell < numberOfCells; startCell++) {
            leftHandExpr = new GRBLinExpr();
            leftHandExpr.addTerm(1.0, freeTaxis[0][startCell]);
            leftHandExpr.addConstant(-initialTaxiPlacement[startCell]);

            constrainedToBeRemovedOnUpdate.add(
                    grbModel.addConstr(leftHandExpr, GRB.EQUAL, 0.0,
                            String.format("5_%d", startCell)));
        }

    }

    /**
     * Optimizes the Gurobi model. If the models is infeasible, try to relax to the feasibility tolerances.
     * @return Returns true if the model was optimized successfully. If false is returned, the model needs to be
     * recreated as additional constraints needed to be added for feasibility.
     */
    public boolean optimizeModel() throws GRBException {
        logger.info("Optimizing gurobi model.");
        grbModel.optimize();
        int statusCode = grbModel.get(GRB.IntAttr.Status);
        if (statusCode == GRB.INFEASIBLE) {
            logger.warn("Gurobi model is infeasible. Try to relax feasibility.");
            double returnCode = grbModel.feasRelax(GRB.FEASRELAX_LINEAR, false, false, true);
            if (returnCode >= 0) {
                logger.info("Succeeded to relax feasibility. Will solve relaxed model.");
                grbModel.optimize();
            } else {
                logger.error("Failed to relax feasibility of gurobi model.");
            }
            return false;
        }
        return true;
    }

    /**
     * Returns the solution of the linear problem
     * @return Returns a integer array [time][startCell][endCell] with the number of vehicles to rebalance.
     */
    public int[][][] getRebalancingAmount(int numberOfCells, int rebalancingSteps) throws GRBException {
        int[][][] rebalancingAmount = new int[rebalancingSteps][numberOfCells][numberOfCells];
        for (int timestep = 0; timestep < rebalancingSteps; timestep++) {
            for (int startCell = 0; startCell < numberOfCells; startCell++) {
                for (int endCell = 0; endCell < numberOfCells; endCell++) {
                    rebalancingAmount[timestep][startCell][endCell] =
                            (int) Math.round(rebalancingVars[timestep][startCell][endCell].get(GRB.DoubleAttr.X));
                }
            }
        }

        return rebalancingAmount;
    }
}
