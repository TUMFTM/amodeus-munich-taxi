package amodeus.amodeus.dispatcher.core;

import amodeus.amodeus.fleetsize.DynamicFleetSize;
import amodeus.amodeus.net.MatsimAmodeusDatabase;
import amodeus.amodeus.util.math.GlobalAssert;
import de.tum.mw.ftm.amod.analysis.events.fleetstatus.FleetStatusInformation;
import de.tum.mw.ftm.amod.analysis.events.fleetstatus.FleetStatusLogEvent;
import de.tum.mw.ftm.amod.analysis.events.passengerrequest.PassengerRequestAnalysisEvent;
import de.tum.mw.ftm.amod.analysis.events.trips.FinalRoboTaxiSchedulesEvent;
import org.apache.log4j.Logger;
import org.matsim.amodeus.config.AmodeusModeConfig;
import org.matsim.amodeus.config.FTMConfigGroup;
import org.matsim.amodeus.dvrp.schedule.*;
import org.matsim.amodeus.plpc.ParallelLeastCostPathCalculator;
import org.matsim.api.core.v01.network.Link;
import org.matsim.contrib.dvrp.passenger.PassengerRequest;
import org.matsim.contrib.dvrp.schedule.Schedule;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.config.Config;
import org.matsim.core.controler.events.IterationEndsEvent;
import org.matsim.core.controler.listener.IterationEndsListener;
import org.matsim.core.mobsim.framework.events.MobsimAfterSimStepEvent;
import org.matsim.core.mobsim.framework.listeners.MobsimAfterSimStepListener;
import org.matsim.core.router.util.TravelTime;

import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.stream.Collectors;

public abstract class DynamicFleetSizeDispatcher extends RebalancingDispatcher {
    protected final DynamicFleetSize dynamicFleetSize;
    protected final int fleetSizePeriod;
    protected final FTMConfigGroup ftmConfigGroup;
    private final static Logger logger = Logger.getLogger(DynamicFleetSizeDispatcher.class);
    protected DynamicFleetSizeDispatcher(Config config, AmodeusModeConfig operatorConfig, TravelTime travelTime, ParallelLeastCostPathCalculator parallelLeastCostPathCalculator, EventsManager eventsManager, MatsimAmodeusDatabase db, DynamicFleetSize dynamicFleetSize) {
        super(config, operatorConfig, travelTime, parallelLeastCostPathCalculator, eventsManager, db);

        if (Objects.isNull(dynamicFleetSize))
            throw new IllegalStateException(
                    "The DynamicFleetSize is not set. Make sure you active DynamicFleetSizeModule in the ScenarioServer, OR provide a custom DynamicFleetSize via injection.");
        this.dynamicFleetSize = dynamicFleetSize;
        ftmConfigGroup = (FTMConfigGroup) config.getModules().get("ftm_simulation");
        this.fleetSizePeriod = ftmConfigGroup.getFleetsizePeriod();
    }


    protected void adjustFleetSize(long round_now) {
        long activeTaxiTargetDiff = getActiveRoboTaxisCount() - dynamicFleetSize.getTargetFleetSize(round_now);

        if (activeTaxiTargetDiff > 0) {
            LinkedList<RoboTaxi> stayingTaxis = new LinkedList<>(getStayingTaxis());
            int i;
            for (i = 0; i < activeTaxiTargetDiff && stayingTaxis.size() > 0; i++) {
                setRoboTaxiOffService(stayingTaxis.poll());
            }
            LinkedList<RoboTaxi> emptyTaxis = new LinkedList<>(getDivertableUnassignedRoboTaxis());
            for (int j = i; j < activeTaxiTargetDiff && emptyTaxis.size() > 0; j++) {
                setRoboTaxiOffService(emptyTaxis.poll());
            }
        }
        if (activeTaxiTargetDiff < 0) {
            LinkedList<RoboTaxi> inactiveTaxis = new LinkedList<>(getInactiveRoboTaxis());
            for (int i = 0; i > activeTaxiTargetDiff && inactiveTaxis.size() > 0; i--) {
                // Rebalance Taxi to a Rank after getting active again
                setRoboTaxiActive(inactiveTaxis.poll());
            }
        }
//        logger.debug(String.format("Time: %d | FS [a:%d;t=%d] [s:%d;d:%d]", round_now, getActiveRoboTaxisCount(), dynamicFleetSize.getTargetFleetSize(round_now),getStayingTaxis().size(), getDivertableUnassignedRoboTaxis().size()));

    }

    protected final void setRoboTaxiOffService(final RoboTaxi roboTaxi, final Link depot) {
        GlobalAssert.that(roboTaxi.isWithoutCustomer());
        /** if {@link RoboTaxi} is during pickup, remove from pickup register */
        if (isInPickupRegister(roboTaxi)) {
            PassengerRequest toRemove = getPickupRoboTaxis().get(roboTaxi);
            removeFromPickupRegisters(toRemove);
        }
        setRoboTaxiDiversion(roboTaxi, depot, RoboTaxiStatus.OFFSERVICE);
        eventsManager.processEvent(SignOffVehicleEvent.create(getTimeNow(), roboTaxi, depot));
    }

    protected final void setRoboTaxiOffService(final RoboTaxi roboTaxi) {
        GlobalAssert.that(roboTaxi.isWithoutCustomer());
        GlobalAssert.that(roboTaxi.isWithoutDirective());
        /** if {@link RoboTaxi} is during pickup, remove from pickup register */
        if (isInPickupRegister(roboTaxi)) {
            PassengerRequest toRemove = getPickupRoboTaxis().get(roboTaxi);
            removeFromPickupRegisters(toRemove);
        }
//        roboTaxi.setStatus(RoboTaxiStatus.OFFSERVICE);
        setRoboTaxiDiversion(roboTaxi, roboTaxi.getDivertableLocation(), RoboTaxiStatus.OFFSERVICE);
//        final Schedule schedule = roboTaxi.getSchedule();
//        new RoboTaxiTaskAdapter(schedule.getCurrentTask()) {
//
//            @Override
//            public void handle(AmodeusDriveTask avDriveTask) {
//                FuturePathContainer futurePathContainer = futurePathFactory.createFuturePathContainer(
//                        roboTaxi.getDivertableLocation(),
//                        roboTaxi.getDivertableLocation(),
//                        roboTaxi.getDivertableTime()
//                );
//                roboTaxi.assignDirective(new DriveVehicleDiversionDirective(
//                        roboTaxi, roboTaxi.getDivertableLocation(), futurePathContainer)
//                );
//
//            }
//
//            @Override
//            public void handle(AmodeusStayTask avStayTask) {
//                FuturePathContainer futurePathContainer = futurePathFactory.createFuturePathContainer(
//                        roboTaxi.getDivertableLocation(),
//                        roboTaxi.getDivertableLocation(),
//                        roboTaxi.getDivertableTime()
//                );
//                roboTaxi.assignDirective(
//                        new StayVehicleDiversionDirective(
//                                roboTaxi, roboTaxi.getDivertableLocation(), futurePathContainer)
//                );
//            }
//
//            @Override
//            public void handle(AmodeusPickupTask avPickupTask) {
//                GlobalAssert.that(false);
//                roboTaxi.assignDirective(EmptyDirective.INSTANCE);
//            }
//
//            @Override
//            public void handle(AmodeusDropoffTask avDropoffTask) {
//                GlobalAssert.that(false);
//                roboTaxi.assignDirective(EmptyDirective.INSTANCE);
//            }
//        };

        eventsManager.processEvent(SignOffVehicleEvent.create(getTimeNow(), roboTaxi, roboTaxi.getDivertableLocation()));
    }

    protected void setRoboTaxiActive(final RoboTaxi roboTaxi) {
        GlobalAssert.that(roboTaxi.getStatus() == RoboTaxiStatus.OFFSERVICE);
        eventsManager.processEvent(SignOnVehicleEvent.create(getTimeNow(), roboTaxi, roboTaxi.getLastKnownLocation())); //TODO: Check if this causes trouble
        FuturePathContainer futurePathContainer = futurePathFactory.createFuturePathContainer(
                roboTaxi.getDivertableLocation(),
                roboTaxi.getDivertableLocation(),
                roboTaxi.getDivertableTime()
        );
        roboTaxi.setStatus(RoboTaxiStatus.STAY);
        roboTaxi.assignDirective(new StayVehicleDiversionDirective(roboTaxi, roboTaxi.getDivertableLocation(),futurePathContainer));

    }

    protected long getActiveRoboTaxisCount() {
        return getRoboTaxis().stream() //
                .filter(rt -> !rt.getStatus().equals(RoboTaxiStatus.OFFSERVICE)) //
                .count();
    }

    protected long getInactiveRoboTaxisCount() {
        return getRoboTaxis().stream() //
                .filter(rt -> rt.getStatus().equals(RoboTaxiStatus.OFFSERVICE)) //
                .count();
    }

    protected Set<RoboTaxi> getActiveRoboTaxis(){
        return getRoboTaxis().stream() //
                .filter(rt -> !(rt.getSchedule().getCurrentTask() instanceof OffServiceStayTask)) //
                .collect(Collectors.toSet());
    }
    protected Set<RoboTaxi> getInactiveRoboTaxis() {
        return getRoboTaxis().stream() //
                .filter(rt -> rt.getSchedule().getCurrentTask() instanceof OffServiceStayTask) //
                .collect(Collectors.toSet());
    }

    //TODO: This is super ugly. It would be nicer to couple this with MobSimAfterStep and IterationEndListeners,
    // but this would mean again massive changes in amodeus core. Also this opens error intrusions if developers use this entries not just for logging.
    @Override
    protected void afterStepLog() {
        super.afterStepLog();

        // Log Pickup Activities...
        //TODO: @michaelwittmann This was a try but does not work here. For now this is posponed and hard coded into the analysis.
//        for (RoboTaxi roboTaxi : getRoboTaxis()) {
//            Schedule schedule = roboTaxi.getSchedule();
//            new RoboTaxiTaskAdapter(schedule.getCurrentTask()) {
//                @Override
//                public void handle(AmodeusDriveTask amodeusDriveTask) {
//                    //TODO: @michaelwittmann This is also super ugly and causes Problems if mobSim Step size is not 1. Fix this later, works for now.
//                    if (amodeusDriveTask instanceof  AmodeusCustomerDriveTask &&  amodeusDriveTask.getBeginTime() == getTimeNow() -1){
//                        // Not nice, but works in this case as we have only single requests per vehicle
//                        PassengerRequest request = amodeusDriveTask.getRequests().iterator().next();
//                        eventsManager.processEvent(new PassengerRequestAnalysisEvent(getTimeNow(),roboTaxi.getId(),request, PassengerRequestAnalysisEvent.PassengerEventType.PASSENGER_REQUEST_PICKUP));
//                    }
//                }
//            };
//        }
        // Log Fleet Status

        if(getRoboTaxis().size()!=0) {
            FleetStatusInformation fleetStatusInformation = new FleetStatusInformation(getTimeNow(), getRoboTaxis());
            eventsManager.processEvent(new FleetStatusLogEvent(getTimeNow(), fleetStatusInformation));
        }

        // Log RoboTaxi Schedules when Simulation is finished
        if (getTimeNow()== ChronoUnit.SECONDS.between(ftmConfigGroup.getSimStartDateTime(), ftmConfigGroup.getSimEndDateTime())+ftmConfigGroup.getSimEndTimeBufferSeconds()){
            eventsManager.processEvent(new FinalRoboTaxiSchedulesEvent(getTimeNow(), getRoboTaxis()));
        }
    }

}


