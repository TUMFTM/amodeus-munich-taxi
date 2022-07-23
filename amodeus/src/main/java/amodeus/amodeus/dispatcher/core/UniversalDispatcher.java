/* amodeus - Copyright (c) 2018, ETH Zurich, Institute for Dynamic Systems and Control */
package amodeus.amodeus.dispatcher.core;

import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import org.apache.log4j.Logger;
import de.tum.mw.ftm.amod.analysis.events.passengerrequest.PassengerRequestAnalysisEvent;
import org.matsim.amodeus.components.AmodeusDispatcher;
import org.matsim.amodeus.components.AmodeusGenerator;
import org.matsim.amodeus.config.AmodeusModeConfig;
import org.matsim.amodeus.dvrp.schedule.AmodeusDriveTask;
import org.matsim.amodeus.dvrp.schedule.AmodeusDropoffTask;
import org.matsim.amodeus.dvrp.schedule.AmodeusPickupTask;
import org.matsim.amodeus.dvrp.schedule.AmodeusStayTask;
import org.matsim.amodeus.plpc.ParallelLeastCostPathCalculator;
import org.matsim.api.core.v01.network.Link;
import org.matsim.contrib.dvrp.fleet.DvrpVehicle;
import org.matsim.contrib.dvrp.passenger.PassengerRequest;
import org.matsim.contrib.dvrp.schedule.Schedule;
import org.matsim.contrib.dvrp.schedule.Schedules;
import org.matsim.contrib.dvrp.schedule.Task;
import org.matsim.contrib.dvrp.tracker.OnlineDriveTaskTracker;
import org.matsim.contrib.dvrp.util.LinkTimePair;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.config.Config;
import org.matsim.core.router.util.TravelTime;

import amodeus.amodeus.net.MatsimAmodeusDatabase;
import amodeus.amodeus.net.SimulationObjectCompiler;
import amodeus.amodeus.util.math.GlobalAssert;

/** purpose of {@link UniversalDispatcher} is to collect and manage
 * {@link PassengerRequest}s alternative implementation of {@link AmodeusDispatcher};
 * supersedes {@link BasicUniversalDispatcher}. */
public abstract class UniversalDispatcher extends BasicUniversalDispatcher {
    private final Map<PassengerRequest, RoboTaxi> pickupRegister = new HashMap<>();
    private final Map<PassengerRequest, RoboTaxi> rqstDrvRegister = new HashMap<>();
    private final Map<PassengerRequest, RoboTaxi> periodFulfilledRequests = new HashMap<>();
    private final Set<PassengerRequest> periodAssignedRequests = new HashSet<>();
    private final Set<PassengerRequest> periodPickedUpRequests = new HashSet<>();

    private final static Logger logger = Logger.getLogger(UniversalDispatcher.class);
    protected UniversalDispatcher(Config config, AmodeusModeConfig operatorConfig, //
            TravelTime travelTime, ParallelLeastCostPathCalculator parallelLeastCostPathCalculator, //
            EventsManager eventsManager, MatsimAmodeusDatabase db) {
        super(eventsManager, config, operatorConfig, travelTime, //
                parallelLeastCostPathCalculator, db);
    }

    // ===================================================================================
    // Methods to use EXTERNALLY in derived dispatchers

    /** @return {@link PassengerRequest}s currently not assigned to a vehicle */
    protected synchronized final List<PassengerRequest> getUnassignedPassengerRequests() {
        return pendingRequests.stream() //
                .filter(r -> !pickupRegister.containsKey(r)) //
                .collect(Collectors.toList());
    }

    /** @return {@link PassengerRequest}s currently pending for pickup. Can be either assigned or not assidned so far */
    protected synchronized final List<PassengerRequest> getPendingRequests() {
        return pendingRequests.stream().collect(Collectors.toList());
    }

    /** @return divertable {@link RoboTaxi}s which currently not on a pickup drive */
    protected final Collection<RoboTaxi> getDivertableUnassignedRoboTaxis() {
        Collection<RoboTaxi> divertableUnassignedRoboTaxis = getDivertableRoboTaxis().stream() //
                .filter(rt -> !pickupRegister.containsValue(rt)) //
                .collect(Collectors.toList());
        GlobalAssert.that(divertableUnassignedRoboTaxis.stream().noneMatch(pickupRegister::containsValue));
        GlobalAssert.that(divertableUnassignedRoboTaxis.stream().allMatch(RoboTaxi::isWithoutCustomer));
        return divertableUnassignedRoboTaxis;
    }

    /** @return {@link Collection<RoboTaxi>}s which is in stay task (idling) */
    protected final Collection<RoboTaxi> getStayingTaxis() {
        return getDivertableUnassignedRoboTaxis().stream() //
                .filter(RoboTaxi::isInStayTask) //
                .collect(Collectors.toList());
    }

    /** @return immutable and inverted copy of pickupRegister, displays which
     *         vehicles are currently scheduled to pickup which request */
    protected final Map<RoboTaxi, PassengerRequest> getPickupRoboTaxis() {
        Map<RoboTaxi, PassengerRequest> pickupPairs = pickupRegister.entrySet().stream() //
                .collect(Collectors.toMap(Map.Entry::getValue, Map.Entry::getKey));

        GlobalAssert.that(pickupPairs.keySet().stream().allMatch(rt -> rt.getStatus().equals(RoboTaxiStatus.DRIVETOCUSTOMER)));
        return pickupPairs;
    }

    /** Diverts {@link RoboTaxi} to {@link Link} of {@link PassengerRequest} and adds pair
     * to pickupRegister. If the {@link RoboTaxi} was scheduled to pickup another
     * {@link PassengerRequest}, then this pair is silently revmoved from the pickup
     * register.
     * 
     * @param roboTaxi
     * @param avRequest */
    public final void setRoboTaxiPickup(RoboTaxi roboTaxi, PassengerRequest avRequest) {
        GlobalAssert.that(roboTaxi.isWithoutCustomer());
        GlobalAssert.that(pendingRequests.contains(avRequest));

        /** for some dispatchers, reassignment is permanently invoked again, the
         * {@link RoboTaxi} should appear under only at the time step of assignment */
        if (!pickupRegister.containsKey(avRequest))
            periodAssignedRequests.add(avRequest);

        // 1) enter information into pickup table
        if (pickupRegister.containsValue(roboTaxi)) {
            PassengerRequest toRemove = pickupRegister.entrySet().stream()//
                    .filter(e -> e.getValue().equals(roboTaxi)).findAny().get().getKey();
            pickupRegister.remove(toRemove); // remove PassengerRequest/RoboTaxi pair served before by roboTaxi
            pickupRegister.remove(avRequest); // remove PassengerRequest/RoboTaxi pair corresponding to avRequest
        }
        pickupRegister.put(avRequest, roboTaxi); // add new pair
        GlobalAssert.that(pickupRegister.size() == pickupRegister.values().stream().distinct().count());

        // 2) set vehicle diversion
        setRoboTaxiDiversion(roboTaxi, avRequest.getFromLink(), RoboTaxiStatus.DRIVETOCUSTOMER);
        eventsManager.processEvent(new PassengerRequestAnalysisEvent(getTimeNow(),roboTaxi.getId(),avRequest, PassengerRequestAnalysisEvent.PassengerEventType.PASSENGER_REQUEST_ASSIGNED));
    }

    /**
     * Cancel a pending, or already assigned customer request, if it is not served by a {@link RoboTaxi} so far.
     * @param request
     */
    public final void cancelRequest(PassengerRequest request){
        pickupRegister.remove(request);
        onRequestCanceled(request);
        logger.debug(String.format("Request %s has been canceled by customer due to waiting time", request.getPassengerId()));
        logger.warn(String.format("Total requests cancelled so far: %d", getCanceledRequests().size()));
        eventsManager.processEvent(new PassengerRequestAnalysisEvent(getTimeNow(),null, request, PassengerRequestAnalysisEvent.PassengerEventType.PASSENGER_REQUEST_CANCELLED));
    }

    /**
     * Get estimated arrivel time for the given start/dest Link pair at time start time
     * @param startLink start link of planned route
     * @param destLink dest link of planned route
     * @param startTime start time of planned route
     * @return estimated time in seconds based on travel time information
     */
    public double getEstimatedArrivalTime(Link startLink, Link destLink, double startTime){
        FuturePathContainer futurePath  = futurePathFactory.createFuturePathContainer(startLink, destLink, startTime);
        return futurePath.getVrpPathWithTravelData().getArrivalTime();
    }

    // ===================================================================================
    // INTERNAL Methods, do not call from derived dispatchers.

    /** For {@link UniversalDispatcher}, {@link RoboTaxiMaintainer} internal use only.
     * Use {@link UniversalDispatcher#setRoboTaxiPickup} or
     * {@link RebalancingDispatcher#setRoboTaxiRebalance} from dispatchers. Assigns new destination to
     * vehicle, if vehicle is already located at destination, nothing happens. In
     * one pass of {@redispatch(...)} in {@VehicleMaintainer}, the function
     * setVehicleDiversion(...) may only be invoked once for a single
     * {@link RoboTaxi} vehicle
     *
     * @param roboTaxi {@link RoboTaxi} supplied with a getFunction,e.g.,
     *            {@link this.getDivertableRoboTaxis}
     * @param destination {@link Link} the {@link RoboTaxi} should be diverted to
     * @param status {@link} the {@link RoboTaxiStatus} the {@link RoboTaxi}
     *            has after the diversion, depends if used from
     *            {@link UniversalDispatcher#setRoboTaxiPickup} or {@link RebalancingDispatcher#setRoboTaxiRebalance} */
    final void setRoboTaxiDiversion(RoboTaxi roboTaxi, Link destination, RoboTaxiStatus status) {
        /** update {@link RoboTaxiStatus} of {@link RoboTaxi} */
        GlobalAssert.that(roboTaxi.isWithoutCustomer());
        GlobalAssert.that(roboTaxi.isWithoutDirective());
        roboTaxi.setStatus(status);

        routingForDiversion(roboTaxi, destination, false);
    }

    /** this function will re-route the taxi if it is not in stay task (for
     * congestion relieving purpose) */
    protected final void reRoute(RoboTaxi roboTaxi) {
        if (!roboTaxi.isInStayTask() && roboTaxi.canReroute())
            routingForDiversion(roboTaxi, roboTaxi.getCurrentDriveDestination(), true);
    }

    // the function below is for internal use only!
    private final void routingForDiversion(RoboTaxi roboTaxi, Link destination, boolean reRoute) {
        /** update {@link Schedule} of {@link RoboTaxi} */
        // the 3rd parameter "reRoute" is added for re-routing the taxi to avoid
        // congestion
        final Schedule schedule = roboTaxi.getSchedule();
        Task task = schedule.getCurrentTask();
        new RoboTaxiTaskAdapter(task) {
            @Override
            public void handle(AmodeusDriveTask avDriveTask) {
                if (reRoute || !avDriveTask.getPath().getToLink().equals(destination)) { // ignore when vehicle is
                                                                                         // already going there
                    FuturePathContainer futurePathContainer = futurePathFactory.createFuturePathContainer( //
                            roboTaxi.getDivertableLocation(), destination, roboTaxi.getDivertableTime());
                    if (reRoute)
                        roboTaxi.assignDirective(new DriveVehicleRerouteDirective(futurePathContainer, roboTaxi));
                    else
                        roboTaxi.assignDirective(new DriveVehicleDiversionDirective(roboTaxi, destination, futurePathContainer));
                } else
                    roboTaxi.assignDirective(EmptyDirective.INSTANCE);
            }

            @Override
            public void handle(AmodeusStayTask avStayTask) {
                // if (!reRoute) {/** a staying vehicle cannot be rerouted */
                if (!avStayTask.getLink().equals(destination) || roboTaxi.getStatus() == RoboTaxiStatus.OFFSERVICE) { // ignore request where location == target
                    FuturePathContainer futurePathContainer = futurePathFactory.createFuturePathContainer( //
                            roboTaxi.getDivertableLocation(), destination, roboTaxi.getDivertableTime());
                    roboTaxi.assignDirective(new StayVehicleDiversionDirective(roboTaxi, destination, futurePathContainer));
                } else
                    roboTaxi.assignDirective(EmptyDirective.INSTANCE);
                // }
            }
        };
    }

    /** Function called from {@link UniversalDispatcher#executePickups} if a
     * {@link RoboTaxi} scheduled for pickup has reached the
     * {@link PassengerRequest#getFromLink} of the {@link PassengerRequest}.
     * 
     * @param roboTaxi
     * @param avRequest */
    private synchronized final void setAcceptRequest(RoboTaxi roboTaxi, PassengerRequest avRequest) {
        roboTaxi.setStatus(RoboTaxiStatus.DRIVEWITHCUSTOMER);
        roboTaxi.setCurrentDriveDestination(avRequest.getToLink());
        /** request not pending anymore */
        boolean statusPen = pendingRequests.remove(avRequest);
        GlobalAssert.that(statusPen);

        /** request not during pickup anymore */
        RoboTaxi formerpckp = pickupRegister.remove(avRequest);
        GlobalAssert.that(roboTaxi == formerpckp);

        /** now during drive */
        RoboTaxi formerrqstDrv = rqstDrvRegister.put(avRequest, roboTaxi);
        GlobalAssert.that(Objects.isNull(formerrqstDrv));

        /** ensure recorded in {@link SimulationObject} */
        periodPickedUpRequests.add(avRequest);
        consistencySubCheck();

        final Schedule schedule = roboTaxi.getSchedule();
        GlobalAssert.that(schedule.getCurrentTask() == Schedules.getLastTask(schedule));

        final double endPickupTime = getTimeNow() + pickupDurationPerStop;
        FuturePathContainer futurePathContainer = futurePathFactory.createFuturePathContainer(avRequest.getFromLink(), avRequest.getToLink(), endPickupTime);

        roboTaxi.assignDirective(new AcceptRequestDirective(roboTaxi, avRequest, futurePathContainer, getTimeNow(), dropoffDurationPerStop));
        eventsManager.processEvent(new PassengerRequestAnalysisEvent(getTimeNow(),roboTaxi.getId(),avRequest, PassengerRequestAnalysisEvent.PassengerEventType.PASSENGER_REQUEST_ARRIVED));
        //TODO: @michaelwittmann This is not possible here
        //eventsManager.processEvent(new PassengerRequestEvent(getTimeNow() + pickupDurationPerStop,roboTaxi.getId(),avRequest, PassengerRequestEvent.PassengerEventType.PASSENGER_REQUEST_PICKUP));
        ++total_matchedRequests;
    }

    /** Function called from {@link UniversalDispatcher#executeDropoffs} if a
     * {@link RoboTaxi} scheduled for dropoff has reached the
     * {@link PassengerRequest#getToLink} of the {@link PassengerRequest}.
     * 
     * @param roboTaxi
     * @param avRequest */
    private synchronized final void setPassengerDropoff(RoboTaxi roboTaxi, PassengerRequest avRequest) {
        RoboTaxi former = rqstDrvRegister.remove(avRequest);
        GlobalAssert.that(roboTaxi == former);

        /** save avRequests which are matched for one publishPeriod to ensure requests
         * appear in {@link SimulationObject}s */
        periodFulfilledRequests.put(avRequest, roboTaxi);

        /** check that current task is last task in schedule */
        final Schedule schedule = roboTaxi.getSchedule();
        GlobalAssert.that(schedule.getCurrentTask() == Schedules.getLastTask(schedule));
        eventsManager.processEvent(new PassengerRequestAnalysisEvent(getTimeNow(),roboTaxi.getId(),avRequest, PassengerRequestAnalysisEvent.PassengerEventType.PASSENGER_REQUEST_DROPOFF));
    }

    protected final boolean isInPickupRegister(RoboTaxi robotaxi) {
        return pickupRegister.containsValue(robotaxi);
    }

    /* package */ final boolean removeFromPickupRegisters(PassengerRequest avRequest) {
        return Objects.isNull(pickupRegister.remove(avRequest));
    }

    /** @param avRequest
     * @return {@link RoboTaxi} assigned to given avRequest, or empty if no taxi is
     *         assigned to avRequest Used by BipartiteMatching in
     *         euclideanNonCyclic, there a comparison to the old av assignment is
     *         needed */
    public final Optional<RoboTaxi> getPickupTaxi(PassengerRequest avRequest) {
        return Optional.ofNullable(pickupRegister.get(avRequest));
    }

    /** complete all matchings if a {@link RoboTaxi} has arrived at the fromLink of
     * an {@link PassengerRequest} */
    @Override
    final void executePickups() {
        Map<PassengerRequest, RoboTaxi> pickupRegisterCopy = new HashMap<>(pickupRegister);
        for (Entry<PassengerRequest, RoboTaxi> entry : pickupRegisterCopy.entrySet()) {
            PassengerRequest avRequest = entry.getKey();
            GlobalAssert.that(pendingRequests.contains(avRequest));
            RoboTaxi pickupVehicle = entry.getValue();
            Link pickupVehicleLink = pickupVehicle.getDivertableLocation();
            boolean isOk = pickupVehicle.getSchedule().getCurrentTask() == Schedules.getLastTask(pickupVehicle.getSchedule());
            if (avRequest.getFromLink().equals(pickupVehicleLink) && isOk)
                setAcceptRequest(pickupVehicle, avRequest);
        }
    }

    /** complete all matchings if a {@link RoboTaxi} has arrived at the toLink of an
     * {@link PassengerRequest} */
    @Override
    protected void executeDropoffs() {
        Map<PassengerRequest, RoboTaxi> requestRegisterCopy = new HashMap<>(rqstDrvRegister);
        for (Entry<PassengerRequest, RoboTaxi> entry : requestRegisterCopy.entrySet())
            if (Objects.nonNull(entry.getValue())) {
                PassengerRequest avRequest = entry.getKey();
                RoboTaxi dropoffVehicle = entry.getValue();
                Link dropoffVehicleLink = dropoffVehicle.getDivertableLocation();
                boolean isOk = dropoffVehicle.getSchedule().getCurrentTask() == Schedules.getLastTask(dropoffVehicle.getSchedule());
                if (avRequest.getToLink().equals(dropoffVehicleLink) && isOk)
                    setPassengerDropoff(dropoffVehicle, avRequest);
            }
    }

    /**
     * Callback function for derived dispatchers. Can be used to define the next step after a drop off, independently of redispatch. Default is stay
     * @param taxi
     */
    protected void scheduleAfterDropoffAction(RoboTaxi taxi){

    }

    /**
     * Callback function for derived dispatchers. Can be used to define the next step after a taxi gets active again. Default behaviour is stay.
     * @param taxi
     */
    protected void scheduleAfterGettingActiveAction(RoboTaxi taxi){
        // TODO: @michaelwittmann this may be an enhancement point
    }

    /** function stops {@link RoboTaxi} which are still heading towards an
     * {@link PassengerRequest} but another {@link RoboTaxi} was scheduled to pickup this
     * {@link PassengerRequest} in the meantime */
    @Override
    /* package */ final void stopAbortedPickupRoboTaxis() {

        /** stop vehicles still driving to a request but other taxi serving that request
         * already */
        getRoboTaxis().stream()//
                .filter(rt -> rt.getStatus().equals(RoboTaxiStatus.DRIVETOCUSTOMER)) //
                .filter(rt -> !pickupRegister.containsValue(rt)) //
                .filter(RoboTaxi::isWithoutCustomer) //
                .filter(RoboTaxi::isWithoutDirective) //
                .forEach(rt -> setRoboTaxiDiversion(rt, rt.getDivertableLocation(), RoboTaxiStatus.REBALANCEDRIVE));
        GlobalAssert.that(pickupRegister.size() <= pendingRequests.size());
    }

    /** Consistency checks to be called by {@link RoboTaxiMaintainer}
     * in each iteration. */
    @Override
    protected final void consistencySubCheck() {
        GlobalAssert.that(pickupRegister.size() <= pendingRequests.size());

        /** containment check pickupRegister and pendingRequests */
        pickupRegister.keySet().forEach(r -> GlobalAssert.that(pendingRequests.contains(r)));

        /** ensure no robotaxi is scheduled to pickup two requests */
        GlobalAssert.that(pickupRegister.size() == pickupRegister.values().stream().distinct().count());
    }

    @Override
    /* package */ final void insertRequestInfo(SimulationObjectCompiler simulationObjectCompiler) {
        /** pickup register must be after pending requests, request is pending from
         * moment it appears until it is picked up, this period may contain several not
         * connected pickup periods (cancelled pickup attempts) */
        simulationObjectCompiler.insertRequests(pendingRequests, RequestStatus.REQUESTED);
        simulationObjectCompiler.insertRequests(pickupRegister.keySet(), RequestStatus.PICKUPDRIVE);
        simulationObjectCompiler.insertRequests(rqstDrvRegister.keySet(), RequestStatus.DRIVING);
        simulationObjectCompiler.insertRequests(canceledRequests, RequestStatus.CANCELLED);
        /** the request is only contained in these three maps durnig 1 time step, which
         * is why they must be inserted after the first three which (potentially) are
         * for multiple time steps. */
        simulationObjectCompiler.insertRequests(periodAssignedRequests, RequestStatus.ASSIGNED);
        simulationObjectCompiler.insertRequests(periodPickedUpRequests, RequestStatus.PICKUP);
        simulationObjectCompiler.insertRequests(periodFulfilledRequests.keySet(), RequestStatus.DROPOFF);

        /** insert information of association of {@link RoboTaxi}s and {@link PassengerRequest}s */
        simulationObjectCompiler.addRequestRoboTaxiAssoc(pickupRegister);
        simulationObjectCompiler.addRequestRoboTaxiAssoc(rqstDrvRegister);
        simulationObjectCompiler.addRequestRoboTaxiAssoc(periodFulfilledRequests);

        periodFulfilledRequests.clear();
        periodAssignedRequests.clear();
        periodPickedUpRequests.clear();
    }

    @Override
    final void redispatchInternal(double now) {
        // deliberately empty
    }

    @Override
    final void executeRedirects() {
        // deliberately empty
    }

    /** adding a vehicle during setup of simulation, handeled by {@link AmodeusGenerator} */
    @Override
    public final void addVehicle(DvrpVehicle vehicle) {
        super.addVehicle(vehicle, RoboTaxiUsageType.SINGLEUSED);
    }

    /** updates the divertable locations, i.e., locations from which a
     * {@link RoboTaxi} can deviate its path according to the current Tasks in the
     * MATSim engine */
    @Override
    protected final void updateDivertableLocations() {
        for (RoboTaxi roboTaxi : getRoboTaxis()) {
            GlobalAssert.that(roboTaxi.isWithoutDirective());
            Schedule schedule = roboTaxi.getSchedule();
            new RoboTaxiTaskAdapter(schedule.getCurrentTask()) {
                @Override
                public void handle(AmodeusDriveTask avDriveTask) {
                    // for empty cars the drive task is second to last task
                    OnlineDriveTaskTracker taskTracker = (OnlineDriveTaskTracker) avDriveTask.getTaskTracker();
                    LinkTimePair linkTimePair = Objects.requireNonNull(taskTracker.getDiversionPoint());
                    roboTaxi.setDivertableLinkTime(linkTimePair); // contains null check
                    roboTaxi.setCurrentDriveDestination(avDriveTask.getPath().getToLink());
//                    GlobalAssert.that(ScheduleUtils.isNextToLastTask(schedule, avDriveTask) != roboTaxi.getStatus().equals(RoboTaxiStatus.DRIVEWITHCUSTOMER));
//                    GlobalAssert.that(ScheduleUtils.isNextToLastTask(schedule, avDriveTask) != roboTaxi.getStatus().equals(RoboTaxiStatus.DRIVEWITHCUSTOMER));
                }

                @Override
                public void handle(AmodeusPickupTask avPickupTask) {
                    GlobalAssert.that(roboTaxi.getStatus().equals(RoboTaxiStatus.DRIVEWITHCUSTOMER));
                    roboTaxi.setCurrentDriveDestination(avPickupTask.getLink());
                }

                @Override
                public void handle(AmodeusDropoffTask avDropOffTask) {
                    GlobalAssert.that(roboTaxi.getStatus().equals(RoboTaxiStatus.DRIVEWITHCUSTOMER));
                    roboTaxi.setCurrentDriveDestination(avDropOffTask.getLink());

                }

                @Override
                public void handle(AmodeusStayTask avStayTask) {
                    // for empty vehicles the current task has to be the last task
                    if (ScheduleUtils.isLastTask(schedule, avStayTask) && !isInPickupRegister(roboTaxi)) {
                        GlobalAssert.that(avStayTask.getBeginTime() <= getTimeNow());
                        GlobalAssert.that(avStayTask.getLink() != null);
                        roboTaxi.setDivertableLinkTime(new LinkTimePair(avStayTask.getLink(), getTimeNow()));
                        roboTaxi.setCurrentDriveDestination(avStayTask.getLink());
                        switch (roboTaxi.getStatus()){
                            case DRIVEWITHCUSTOMER:
                                roboTaxi.setStatus(RoboTaxiStatus.STAY);
                                scheduleAfterDropoffAction(roboTaxi);
                                break;
                            case OFFSERVICE:
//                                //TODO: michaelwittmann This may be an enhancement, needs to be checked in future reworks
                                break;
                            default:
                                roboTaxi.setStatus(RoboTaxiStatus.STAY);
                        }

//                        if (!roboTaxi.getStatus().equals(RoboTaxiStatus.OFFSERVICE)) {
//                            roboTaxi.setStatus(RoboTaxiStatus.STAY); //TODO: verifiy that
//                        }


                    }
                }
            };
        }
    }
}
