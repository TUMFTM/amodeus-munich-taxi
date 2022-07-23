package de.tum.mw.ftm.amod.taxi.dispatcher;

import amodeus.amodeus.dispatcher.core.DispatcherConfigWrapper;
import amodeus.amodeus.dispatcher.core.DynamicFleetSizeDispatcher;
import amodeus.amodeus.dispatcher.core.RoboTaxi;
import amodeus.amodeus.dispatcher.core.RoboTaxiStatus;
import amodeus.amodeus.dispatcher.util.DistanceCost;
import amodeus.amodeus.dispatcher.util.DistanceHeuristics;
import amodeus.amodeus.dispatcher.util.GlobalBipartiteMatching;
import amodeus.amodeus.fleetsize.DynamicFleetSize;
import amodeus.amodeus.net.MatsimAmodeusDatabase;
import amodeus.amodeus.routing.DistanceFunction;
import amodeus.amodeus.util.math.GlobalAssert;
import amodeus.amodeus.util.matsim.SafeConfig;
import de.tum.mw.ftm.amod.geom.GridCell;
import de.tum.mw.ftm.amod.taxi.lp.RebalancingSolver;
import de.tum.mw.ftm.amod.taxi.preprocessing.demandprediction.UniformGridWithBorderPredictions;
import de.tum.mw.ftm.amod.taxi.preprocessing.ranks.TaxiRank;
import de.tum.mw.ftm.amod.taxi.preprocessing.ranks.TaxiRanks;
import de.tum.mw.ftm.amod.taxi.util.AmodeusUtil;
import de.tum.mw.ftm.amod.taxi.util.MathUtils;
import gurobi.GRBException;
import org.apache.commons.lang.NotImplementedException;
import org.apache.commons.lang3.ArrayUtils;
import org.apache.log4j.Logger;
import org.locationtech.jts.algorithm.Centroid;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.Polygon;
import org.matsim.amodeus.components.AmodeusDispatcher;
import org.matsim.amodeus.components.AmodeusRouter;
import org.matsim.amodeus.config.AmodeusModeConfig;
import org.matsim.amodeus.config.FTMConfigGroup;
import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.contrib.dvrp.run.ModalProviders;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.config.Config;
import org.matsim.core.network.NetworkUtils;
import org.matsim.core.router.util.TravelTime;

import java.time.temporal.ChronoUnit;
import java.util.*;

public class RebalancingLPDispatcher extends DynamicFleetSizeDispatcher {
    private final static Logger logger = Logger.getLogger(RebalancingLPDispatcher.class);

    private final int rebalancingPeriodSeconds;
    private final int dispatchPeriod;
    private final GridCell[] gridCells;
    private final int numberOfCells;
    private final RebalancingSolver rebalancingSolver;
    private final double[][] freespeedTravelTimes;
    private final double[][] targetProbabilities;
    private final Network network;
    private final IFTMDispatcher dispatcher;
    private final int timestepsPerRebalancing;
    private final GlobalBipartiteMatching borderRebalancer;
    UniformGridWithBorderPredictions predictions;
    private int rebalancingStep;
    private boolean started;
    private int[][][] calculatedRebalancing;
    private final Random random= new Random(42);

    private final double allowedBorderDeviation;
    private final Map<AmodeusUtil.BorderOrientation, Polygon> borderPolygons;
    private final long maxCustomerWaitingTime;
    private final long maxCustomerAssignmentTime;
    private final long lastRebalancingTimeStep;

    private final TaxiRanks taxiRanks;
    private final Map<GridCell, Set<TaxiRank>> gridTaxiRanksMap;
    private final Map<AmodeusUtil.BorderOrientation, Set<TaxiRank>> borderTaxiRanksMap;

    protected RebalancingLPDispatcher(Config config, AmodeusModeConfig operatorConfig, TravelTime travelTime,
                                      AmodeusRouter router, EventsManager eventsManager,
                                      MatsimAmodeusDatabase db, DynamicFleetSize dynamicFleetSize,
                                      UniformGridWithBorderPredictions predictions, double[][] freespeedTravelTimes,
                                      double[][] cellDistances, double[][] targetProbabilities,
                                      Network network, TaxiRanks taxiRanks) {
        super(config, operatorConfig, travelTime, router, eventsManager, db, dynamicFleetSize);

        SafeConfig safeDispatcherConfig = SafeConfig.wrap(operatorConfig.getDispatcherConfig());

        FTMConfigGroup ftmConfigGroup = (FTMConfigGroup) config.getModules().get("ftm_simulation");

        this.network = network;
        int numberOfColumns = predictions.getNumberOfColumns();
        int numberOfRows = predictions.getNumberOfRows();
        this.gridCells = predictions.getGridCells();
        this.numberOfCells = gridCells.length;
        this.borderPolygons = predictions.getBorderPolygons();
        this.predictions = predictions;
        this.dispatchPeriod = ftmConfigGroup.getDispatchingPeriodSeconds();
        this.taxiRanks = taxiRanks;
        initializeTaxiRanks(this.taxiRanks);
        this.gridTaxiRanksMap = getGridTaxiRankMap();
        this.borderTaxiRanksMap = getBorderTaxiRankMap();
        rebalancingPeriodSeconds = ftmConfigGroup.getRebalancingPeriodSeconds();
        int rebalancingSteps = predictions.getPredictionWindows();

        double alpha = ftmConfigGroup.getAlpha();
        double lambda = ftmConfigGroup.getLambda();

        logger.info(String.format("Using alpha=%f, and lambda=%f", alpha, lambda));

        GlobalAssert.that(this.rebalancingPeriodSeconds == this.predictions.getPredictionHorizon());

        this.rebalancingSolver = new RebalancingSolver(rebalancingPeriodSeconds, numberOfRows,
                numberOfColumns,
                rebalancingSteps,
                cellDistances,
                alpha,
                lambda);
        try {
            rebalancingSolver.initialize();
        } catch (GRBException e) {
            e.printStackTrace();
            throw new RuntimeException();
        }

        this.maxCustomerWaitingTime = ftmConfigGroup.getMaxCustomerWaitingTime();
        this.maxCustomerAssignmentTime = ftmConfigGroup.getMaxCustomerAssignmentTime();

        this.allowedBorderDeviation = 0.1;

        this.freespeedTravelTimes = freespeedTravelTimes;
        this.targetProbabilities = targetProbabilities;
        this.rebalancingStep = 0;
        this.started = false;
        this.timestepsPerRebalancing = rebalancingSteps - 1;
        this.calculatedRebalancing = new int[timestepsPerRebalancing][gridCells.length][gridCells.length];

        this.lastRebalancingTimeStep = ChronoUnit.SECONDS.between(ftmConfigGroup.getSimStartDateTime(), ftmConfigGroup.getSimEndDateTime());

        this.borderRebalancer = new GlobalBipartiteMatching(new DistanceCost(DistanceHeuristics.EUCLIDEAN.getDistanceFunction(network)));

        // Decide whether to use BPM or NTNR dispatching based on config
        if (ftmConfigGroup.getDispatcherType() == FTMConfigGroup.DispatcherType.GBM) {
            logger.info("Using global bipartite matching as dispatcher.");
            DispatcherConfigWrapper dispatcherConfig = DispatcherConfigWrapper.wrap(operatorConfig.getDispatcherConfig());
            DistanceHeuristics distanceHeuristics = //
                    dispatcherConfig.getDistanceHeuristics(DistanceHeuristics.EUCLIDEAN);
            logger.info("Using DistanceHeuristics: " + distanceHeuristics.name());
            DistanceFunction distanceFunction = distanceHeuristics.getDistanceFunction(network);
            dispatcher = new FTMBipartiteMatchingDispatcher(this, network, distanceFunction,
                    safeDispatcherConfig, ftmConfigGroup);
        } else if (ftmConfigGroup.getDispatcherType() == FTMConfigGroup.DispatcherType.NTNR) {
            logger.info("Using <nearest taxi nearest request> as dispatcher.");
            dispatcher = new FTM_NTNR_Dispatcher(this, network, config);
        } else {
            throw new NotImplementedException("Invalid dispatcher type. Please check the FTMConfigGroup");
        }

    }

    @Override
    protected String getInfoLine() {
        int idleTaxis = getRoboTaxiSubset(RoboTaxiStatus.STAY).size();
        int insideTaxis = Arrays.stream(AmodeusUtil.getNumberOfTaxisInsideCells(getRoboTaxiSubset(RoboTaxiStatus.STAY), gridCells)).sum();
        return String.format("%s Taxis: OffService=%d, Idle(Outside)=%d, Idle(Inside)=%d, Rebalancing=%d", //
                super.getInfoLine(), //
                getRoboTaxiSubset(RoboTaxiStatus.OFFSERVICE).size(),
                idleTaxis-insideTaxis,
                insideTaxis,
                getRoboTaxiSubset(RoboTaxiStatus.REBALANCEDRIVE).size());
    }

    private Map<GridCell, Set<TaxiRank>> getGridTaxiRankMap(){
        Map<GridCell, Set<TaxiRank>> resultMap = new HashMap<>();

        for(GridCell cell : gridCells) {
            Set<TaxiRank> zoneRanks = new HashSet<>();
            for(TaxiRank taxiRank: taxiRanks) {
                if (cell.contains(taxiRank.getGeom().getCoordinate())) {
                    zoneRanks.add(taxiRank);
                }
            }
            resultMap.put(cell, zoneRanks);
        }
        return resultMap;
    }

    private Map<AmodeusUtil.BorderOrientation, Set<TaxiRank>> getBorderTaxiRankMap(){
        Map<AmodeusUtil.BorderOrientation, Set<TaxiRank>> resultMap = new HashMap<>();

        for(Map.Entry<AmodeusUtil.BorderOrientation, Polygon> entry : borderPolygons.entrySet()) {
            Set<TaxiRank> zoneRanks = new HashSet<>();
            for(TaxiRank taxiRank: taxiRanks) {
                if (entry.getValue().contains(taxiRank.getGeom())) {
                    zoneRanks.add(taxiRank);
                }
            }
            resultMap.put(entry.getKey(), zoneRanks);
        }
        return resultMap;
    }

    private void initializeTaxiRanks(Set<TaxiRank> taxiRanks) {

        for (TaxiRank rank : taxiRanks) {
            rank.setNearestLink(network);
        }
    }

    @Override
    protected void redispatch(double now) {
        long round_now = Math.round(now);
        if (!started) {
            if (getRoboTaxis().size() == 0) /* return if the roboTaxis are not ready yet */
                return;
            /* as soon as the roboTaxis are ready, make sure to execute rebalancing and dispatching for now=0 */
            round_now = 0;

            started = true;
        }

        /* Adjust fleet size at every fleetSizePeriod seconds */
        if (round_now % fleetSizePeriod == 0) {
            adjustFleetSize(round_now);
        }

        /* dispatch taxis at every dispatchPeriod seconds */
        if (round_now % dispatchPeriod == 0) {
            // Cancel all pending requests which not served within in maxCustomerWaitingTime
            getPendingRequests().stream().filter(r -> (getTimeNow()- r.getSubmissionTime() )> maxCustomerWaitingTime).forEach(this::cancelRequest);

            // Cancel all pending requests which not assigned within in maxCustomerAssignmentTime
            getUnassignedPassengerRequests().stream().filter(request -> (getTimeNow()- request.getSubmissionTime()) > maxCustomerAssignmentTime).forEach(this::cancelRequest);
            
            dispatcher.dispatch(getDivertableUnassignedRoboTaxis(), getUnassignedPassengerRequests(), getTimeNow());
        }

        /* Send taxis that are outside of grid and borders to cell with highest demand at every second */
        if (round_now % 30 == 0) {
            sendBackOutsideTaxis();
        }

        /* Solve LP rebalancing problem at every rebalancingPeriodSeconds and redirect taxis according to it */
        if (round_now % rebalancingPeriodSeconds == 0 && round_now < lastRebalancingTimeStep) {
            rebalance();
        }
    }

    /**
     * Because taxis outside of the grid and the borders are not taken into consideration, they need to be handled
     * separately.Taxis are always send to the cell with the highest demand during the next rebalancing timestep.
     */
    private void sendBackOutsideTaxis() {
        List<RoboTaxi> taxisOutsideOfGridAndBorders = AmodeusUtil.getTaxisOutsideOfGridAndBorders(gridCells,
                new ArrayList<>(borderPolygons.values()), getStayingTaxis());
        rebalanceTaxisBasedOnFutureSupplyDemandRatio(taxisOutsideOfGridAndBorders);
    }

    private void rebalanceTaxisBasedOnFutureSupplyDemandRatio(Collection<RoboTaxi> taxisToRebalance){
        if (!taxisToRebalance.isEmpty()) {
            logger.info(String.format("Rebalance %d taxis from outside of grid to cell with highest demand ",
                    taxisToRebalance.size()));

            double[] predictedDemand = predictions.getDemandPredictionsForThisTimeStepForAllCells(getTimeNow());
            int[] numberStayingTaxisPerCell = AmodeusUtil.getNumberOfTaxisInsideCells(getStayingTaxis(), gridCells);
            int[] numberOfArrivingTaxisPerCell = AmodeusUtil.getNumberOfFutureTaxisArrivingInGridCell(getActiveRoboTaxis(), gridCells, (long) getTimeNow(), (long) getTimeNow() + rebalancingPeriodSeconds);
            int[] numberOfTaxisPerCell = MathUtils.ebeAdd(numberStayingTaxisPerCell, numberOfArrivingTaxisPerCell);

            for(RoboTaxi taxi : taxisToRebalance){
                int minCellIdx = getMinIdx(MathUtils.ebeDivide(numberOfTaxisPerCell, predictedDemand));
                rebalanceTaxiToGridCell(taxi, gridCells[minCellIdx]);
                numberOfTaxisPerCell[minCellIdx]++;
            }
        }
    }

    private int getMinIdx(double[] array){
        int minIdx = 0;
        double minVal = array[0];
        for(int i=1; i< array.length; i++){
            if (minVal > array[i]){
                minVal = array[i];
                minIdx = i;
            }
        }
        return minIdx;
    }
    private int getMaxIdx(double[] array){
        int minIdx = 0;
        double minVal = array[0];
        for(int i=1; i< array.length; i++){
            if (minVal > array[i]){
                minVal = array[i];
                minIdx = i;
            }
        }
        return minIdx;
    }


    private void rebalanceTaxisToCellWithHighestDemand(Collection<RoboTaxi> roboTaxis){
        if (roboTaxis.isEmpty()) return;
        //TODO @michaelwittmann @maxispeicher Double check that we use the right values here
        GridCell gridCellWithHighestDemand = predictions.getCellWithHighestDemand(getTimeNow());
        for (RoboTaxi taxi: roboTaxis){
            rebalanceTaxiToGridCell(taxi, gridCellWithHighestDemand);
        }
    }

    private void rebalanceTaxiToGridCell(RoboTaxi taxi, GridCell gridCell){
        Set<TaxiRank> ranks = gridTaxiRanksMap.get(gridCell);
        if(!ranks.isEmpty()){
            TaxiRank rank = AmodeusUtil.getRandomTaxiRank(ranks, random);
            setRoboTaxiRebalance(taxi, rank.getNearestLink());
        }
        else{
            Link destinationLink = NetworkUtils.getNearestLink(network,
                    new Coord(gridCell.getCenterX(), gridCell.getCenterY()));
            // Rebalance Taxis according to LP solution
            setRoboTaxiRebalance(taxi, destinationLink);
        }
    }

    private void rebalanceTaxiToBorderCell(RoboTaxi taxi, AmodeusUtil.BorderOrientation borderOrientation){
        Set<TaxiRank> ranks = borderTaxiRanksMap.get(borderOrientation);
        if(!ranks.isEmpty()){
            TaxiRank rank =  AmodeusUtil.getRandomTaxiRank(ranks, random);
            setRoboTaxiRebalance(taxi, rank.getNearestLink());
        }
        else{
            Polygon borderPolygon = borderPolygons.get(borderOrientation);
            Coordinate center = Centroid.getCentroid(borderPolygon);
            Link destinationLink = NetworkUtils.getNearestLink(network,
                    new Coord(center.x, center.y));
            // Rebalance Taxis according to LP solution
            setRoboTaxiRebalance(taxi, destinationLink);
        }
    }

    /**
     * Updates the LP model and solves it if needed. (Every timestepsPerRebalancing function call)
     * Always executes the rebalancing based on the result of the LP model.
     *
     * The border areas are handled after the grid which is taken care of by the LP model.
     * Depending on the number of taxis in a border zone, taxis are either send back to the inner grid or the closest
     * taxis are send to the border zone.
     */
    private void rebalance() {
        List<RoboTaxi> activeUnassignedTaxis = (List<RoboTaxi>) getDivertableUnassignedRoboTaxis();

        // STEP 1: Rebalance inside the Grid
        int rebalancingTimestepResidual = rebalancingStep % timestepsPerRebalancing;
        if (rebalancingTimestepResidual == 0) {
           // solve LP Problem every timestepsPerRebalancing timesteps
            updateLPSolver(activeUnassignedTaxis);
        }

        // Get Rebalancing for current timestep
        int[][] currentRebalancing = calculatedRebalancing[rebalancingTimestepResidual];
        // Get Taxis per cell
        List<List<RoboTaxi>> taxisPerCell = AmodeusUtil.getTaxisInsideCells(activeUnassignedTaxis, gridCells);
        for (int startCellIndex = 0; startCellIndex < gridCells.length; startCellIndex++) {
            List<RoboTaxi> availableTaxis = taxisPerCell.get(startCellIndex);
            for (int endCellIndex = 0; endCellIndex < gridCells.length; endCellIndex++) {
                // Cannot rebalance more taxis than available
                int minRebalancingAvailableTaxis = Math.min(currentRebalancing[startCellIndex][endCellIndex], availableTaxis.size());
                List<RoboTaxi> rebalancedTaxis = new ArrayList<>();
                if(minRebalancingAvailableTaxis > 0 ){
                    logger.info(String.format("Rebalancing %d Taxis from Cell-%d to Cell-%d", minRebalancingAvailableTaxis, startCellIndex, endCellIndex));
                }
                if(minRebalancingAvailableTaxis < currentRebalancing[startCellIndex][endCellIndex]){
                    logger.warn(String.format("There are less taxis for rebalancing available than expected (Planned: %d, There: %d)", currentRebalancing[startCellIndex][endCellIndex], availableTaxis.size()));
                }

                for (int i = 0; i < minRebalancingAvailableTaxis; i++) {
                    RoboTaxi taxi = availableTaxis.get(i);
                    // Rebalance Taxis according to LP solution
                    rebalanceTaxiToGridCell(taxi,gridCells[endCellIndex]);
                    rebalancedTaxis.add(taxi);
                }
                availableTaxis.removeAll(rebalancedTaxis);
                activeUnassignedTaxis.removeAll(rebalancedTaxis);
            }
        }


        // STEP 2: Border Rebalancing
        // Recalculate Taxis in cells after sending away rebalancing taxis
        List<List<RoboTaxi>> remainingTaxisInCells = AmodeusUtil.getTaxisInsideCells(activeUnassignedTaxis, gridCells);
        int[] gridSupply = AmodeusUtil.getNumberOfTaxisInsideCells(activeUnassignedTaxis, gridCells);
        int[] gridDemand = Arrays.stream(predictions.getDemandPredictionsForThisTimeStepForAllCells(getTimeNow())).mapToInt(value -> (int)value).toArray();
        int[] gridSupplyMinusDemand = MathUtils.ebeSub(gridSupply, gridDemand);

        List<RoboTaxi> remainingTaxisForBorderRebalancing = new ArrayList<>();

        // Add number oversupply Taxis into possible rebalancing Set for Border rebalancing
        for (int i=0 ; i<gridSupplyMinusDemand.length ;i++) {
            // If there is oversupply add i taxis from cell to rebalancing set
            if(gridSupplyMinusDemand[i]>0){
                for (int j = 0; j < gridSupplyMinusDemand[i]; j++) {
                    RoboTaxi taxi = remainingTaxisInCells.get(i).get(j);
                    remainingTaxisForBorderRebalancing.add(taxi);
                }
            }
        }

        List<Link> borderTargets = new ArrayList<>();
        // Add all Taxis in Border into rebalancing Set for Border rebalancing
        Map<AmodeusUtil.BorderOrientation, Collection<RoboTaxi>> taxisInBorder = AmodeusUtil.getTaxisByBorderZone(borderPolygons, activeUnassignedTaxis);
        for (AmodeusUtil.BorderOrientation orientation : AmodeusUtil.BorderOrientation.values()) {
            remainingTaxisForBorderRebalancing.addAll(taxisInBorder.get(orientation));
            long borderDemand = (long) predictions.getDemandPredictionForBorderCell(getTimeNow(), orientation)[0];
            long arrivingTaxis  = AmodeusUtil.getNumberOfFutureTaxisArrivingInPolygon(getActiveRoboTaxis(), borderPolygons.get(orientation),(long) getTimeNow(), (long) getTimeNow()+rebalancingPeriodSeconds);

            for(int i = 0; i< borderDemand-arrivingTaxis; i++){
                TaxiRank rank = AmodeusUtil.getRandomTaxiRank(borderTaxiRanksMap.get(orientation),random);
                borderTargets.add(rank.getNearestLink());
            }
        }

        // REBALANCE UNDERSUPPLY
        Map<RoboTaxi, Link> borderRebalancing = borderRebalancer.matchLink(remainingTaxisForBorderRebalancing, borderTargets);
        for (Map.Entry<RoboTaxi, Link> roboTaxiLinkEntry : borderRebalancing.entrySet()) {
            setRoboTaxiRebalance(roboTaxiLinkEntry.getKey(),roboTaxiLinkEntry.getValue());
            activeUnassignedTaxis.remove(roboTaxiLinkEntry.getKey());
        }

        Map<AmodeusUtil.BorderOrientation, Collection<RoboTaxi>> OversupplyInBorder = AmodeusUtil.getTaxisByBorderZone(borderPolygons, activeUnassignedTaxis);
        for (Collection<RoboTaxi> oversupplyTaxis : OversupplyInBorder.values()) {
            rebalanceTaxisBasedOnFutureSupplyDemandRatio(oversupplyTaxis);
        }



//        //TODO: CHECK IF ROBOTAXIS STAYING AT RANK FREQUENTLY REBALANCE TO ANOTHER RANK
//        for (AmodeusUtil.BorderOrientation orientation : AmodeusUtil.BorderOrientation.values()) {
//            int predictedDemandInBorder = (int) predictions.getDemandPredictionForBorderCell(getTimeNow(), orientation)[0];
//            Set<TaxiRank> ranks = borderTaxiRanksMap.get(orientation);
//            for(int i=0;i<predictedDemandInBorder; i++){
//                if(!ranks.isEmpty()) {
//                    TaxiRank rank = AmodeusUtil.getRandomTaxiRank(ranks, random);
//                    borderTargets.add(rank.getNearestLink());
//                }
//                else{
//                    Coordinate borderCenter = borderPolygons.get(orientation).getCentroid().getCoordinate();
//                    Link link = NetworkUtils.getNearestLink(network, new Coord(borderCenter.getX(), borderCenter.getY()));
//                    borderTargets.add(link);
//                }
//            }
//        }





//        for (AmodeusUtil.BorderOrientation orientation : AmodeusUtil.BorderOrientation.values()) {
//
//
//            double predictedDemandInBorder = predictions.getDemandPredictionForBorderCell(getTimeNow(), orientation)[0];
//
//            double predictionRatio = taxisInBorder.size() / predictedDemandInBorder;
//
//            if (predictionRatio > 1 + allowedBorderDeviation) {
//                // Send Taxis to center grid
//                int numberOfTaxisToRebalance = (int) (taxisInBorder.size() - predictedDemandInBorder);
//                logger.info(String.format("Rebalancing %d taxis from " + orientation.name() + " border to center grid",
//                        numberOfTaxisToRebalance));
//                List<RoboTaxi> rebalancedTaxis = new ArrayList<>();
//                for (int i = 0; i < numberOfTaxisToRebalance; i++) {
//                    RoboTaxi taxi = taxisInBorder.get(i);
//                    GridCell closestCell = AmodeusUtil.getClosestGridCell(this.gridCells,
//                            taxi.getDivertableLocation().getCoord());
//                    // Rebalance Taxis according to LP solution
//                    rebalanceTaxiToGridCell(taxi, closestCell);
//                    rebalancedTaxis.add(taxi);
//                }
//                taxisInBorder.removeAll(rebalancedTaxis);
//                activeUnassignedTaxis.removeAll(rebalancedTaxis);
//            } else if (predictionRatio < 1 - allowedBorderDeviation) {
//                // send taxis to border
//                Polygon borderPolygon = borderPolygons.get(orientation);
//                int numberOfTaxisToRebalance = (int) (predictedDemandInBorder - taxisInBorder.size());
//                logger.info(String.format("Rebalancing %d taxis from center grid to " + orientation.name() + " border",
//                        numberOfTaxisToRebalance));
//                List<Map.Entry<RoboTaxi, Coordinate>> taxiDestCoordinatePairs = AmodeusUtil.getMaxNClosestTaxis(borderPolygon,
//                        activeUnassignedTaxis, numberOfTaxisToRebalance);
//                for (Map.Entry<RoboTaxi, Coordinate> taxiCoordinatePair : taxiDestCoordinatePairs) {
//                    // Shift intersection point a little bit to the center to make sure that destination is inside the border
////                    Coordinate intersectCoord = taxiCoordinatePair.getValue();
////                    Coord destination = shiftCoordinateToPolygonCenter(intersectCoord, borderPolygon);
////                    Link destinationLink = NetworkUtils.getNearestLink(network, destination);
//                    rebalanceTaxiToBorderCell(taxiCoordinatePair.getKey(),orientation);
//                }
//                activeUnassignedTaxis.removeAll(taxiDestCoordinatePairs.stream().map(Map.Entry::getKey).collect(Collectors.toList()));
//            }
//        }
        // Increase rebalancing counter
        rebalancingStep += 1;
    }

    /**
     * Shifts the given coordinate by 0.1 of the overall distance to the center of the given polygon.
     * @return The shifted coordinate
     */
    private Coord shiftCoordinateToPolygonCenter(Coordinate coordinate, Polygon polygon) {
        double xDifference = polygon.getCentroid().getX() - coordinate.getX();
        double yDifference = polygon.getCentroid().getY() - coordinate.getY();
        double shiftFactor = 0.1;
        return new Coord(coordinate.getX() + shiftFactor * xDifference,
                coordinate.getY() + shiftFactor * yDifference);
    }

    /**
     * Updates the LP model with the values which can be extracted from the current simulation state.
     * @param activeTaxis Taxis which are currently active in the simulation.
     */
    private void updateLPSolver(List<RoboTaxi> activeTaxis) {
        // Get initial Taxi Placement
        List<List<RoboTaxi>> taxisPerCell = AmodeusUtil.getTaxisInsideCells(activeTaxis, gridCells);
        int[] numberOfTaxisPerCell = ArrayUtils.toPrimitive(taxisPerCell.stream().map(List::size).toArray(Integer[]::new));
        GlobalAssert.that(numberOfTaxisPerCell.length == numberOfCells);

        // Get subset of predicted demand
//        List<TaxiDemandProtos.TaxiDemand.SingleTimestepDemand> demand = gridDemand.getDemandSeriesList().subList(
//                rebalancingStep, rebalancingStep + timestepsPerRebalancing + 1);


        // Check which taxis will become unoccupied
        List<RoboTaxi> occupiedTaxis = getRoboTaxiSubset(RoboTaxiStatus.DRIVEWITHCUSTOMER);
        int[][] occupiedTaxisBecomingUnoccupied = new int[timestepsPerRebalancing][gridCells.length];
        for (int i = 0; i < timestepsPerRebalancing; i++) {
            occupiedTaxisBecomingUnoccupied[i] = AmodeusUtil.getNumberOfFutureTaxisArrivingInGridCell(occupiedTaxis, gridCells,
                    (rebalancingStep + i) * rebalancingPeriodSeconds, (rebalancingStep + i + 1) * rebalancingPeriodSeconds);
        }

        // Update Rebalancing solver and retrieve solution
        ArrayList<double[]> demand = predictions.getDemandPredictionsForAllCells(getTimeNow());
        try {
            rebalancingSolver.updateModel(demand, numberOfTaxisPerCell, occupiedTaxisBecomingUnoccupied, targetProbabilities, freespeedTravelTimes);
            calculatedRebalancing = rebalancingSolver.calculateRebalancing();
        } catch (GRBException e) {
            logger.error("Gurobi could not solve LP Problem");
            e.printStackTrace();
            for (int[][] outerInts : calculatedRebalancing) {
                for (int[] ints : outerInts) {
                    Arrays.fill(ints, 0);
                }
            }
            try {
                rebalancingSolver.initialize();
            } catch (GRBException grbException) {
                grbException.printStackTrace();
                throw new RuntimeException("Could not reinitialize Gurobi Model, which is a fatal error and should never happen.");
            }
        }
    }


    public static class Factory implements AmodeusDispatcher.AVDispatcherFactory {

        @Override
        public AmodeusDispatcher createDispatcher(ModalProviders.InstanceGetter inject) {
            Config config = inject.get(Config.class);
            AmodeusModeConfig operatorConfig = inject.getModal(AmodeusModeConfig.class);
            AmodeusRouter router = inject.getModal(AmodeusRouter.class);
            MatsimAmodeusDatabase db = inject.get(MatsimAmodeusDatabase.class);
            TravelTime travelTime = inject.getModal(TravelTime.class);
            Network network = inject.getModal(Network.class);
            EventsManager eventsManager = inject.get(EventsManager.class);
            DynamicFleetSize dynamicFleetSize = inject.get(DynamicFleetSize.class);
            UniformGridWithBorderPredictions predictedDemand = inject.get(UniformGridWithBorderPredictions.class);
            double[][] freespeedTravelTimes = inject.getNamed(double[][].class, "adaptedTravelTimes");
            double[][] cellDistances = inject.getNamed(double[][].class, "cellDistances");
            double[][] targetProbabilities = inject.getNamed(double[][].class, "targetProbabilities");
            TaxiRanks taxiRanks = inject.get(TaxiRanks.class);

            return new RebalancingLPDispatcher(config, operatorConfig, travelTime, router,
                    eventsManager, db, dynamicFleetSize, predictedDemand, freespeedTravelTimes,
                    cellDistances, targetProbabilities, network, taxiRanks);
        }
    }
}
