package de.tum.mw.ftm.amod.taxi.dispatcher;

import amodeus.amodeus.dispatcher.core.DynamicFleetSizeDispatcher;
import amodeus.amodeus.dispatcher.core.RoboTaxi;
import amodeus.amodeus.fleetsize.DynamicFleetSize;
import amodeus.amodeus.net.MatsimAmodeusDatabase;
import amodeus.amodeus.util.matsim.SafeConfig;
import org.matsim.amodeus.components.AmodeusDispatcher;
import org.matsim.amodeus.components.AmodeusRouter;
import org.matsim.amodeus.config.AmodeusModeConfig;
import org.matsim.amodeus.config.FTMConfigGroup;
import org.matsim.amodeus.plpc.ParallelLeastCostPathCalculator;
import org.matsim.contrib.dvrp.passenger.PassengerRequest;
import org.matsim.contrib.dvrp.run.ModalProviders;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.config.Config;
import org.matsim.core.config.ReflectiveConfigGroup;
import org.matsim.core.router.util.TravelTime;

import java.util.LinkedList;
import java.util.List;


public class FleetSizeDemoDispatcher extends DynamicFleetSizeDispatcher {

    private final int dispatchPeriod;
    private final int fleetSizePeriod;
    private final DynamicFleetSize fleetSize;

    protected FleetSizeDemoDispatcher(Config config, AmodeusModeConfig operatorConfig, TravelTime travelTime,
                                      ParallelLeastCostPathCalculator parallelLeastCostPathCalculator,
                                      EventsManager eventsManager, MatsimAmodeusDatabase db,
                                      DynamicFleetSize dynamicFleetSize) {
        super(config, operatorConfig, travelTime, parallelLeastCostPathCalculator, eventsManager, db, dynamicFleetSize);
        FTMConfigGroup ftmConfigGroup = (FTMConfigGroup) config.getModules().get("ftm_simulation");
        this.dispatchPeriod = ftmConfigGroup.getDispatchingPeriodSeconds();
        this.fleetSize = dynamicFleetSize;

        SafeConfig saveFTMConfig = SafeConfig.wrap((ReflectiveConfigGroup) config.getModules().get("ftm_simulation"));
        this.fleetSizePeriod = saveFTMConfig.getInteger("fleetsizePeriod", 60);
    }

    @Override
    protected void redispatch(double now) {
        final long round_now = Math.round(now);

        if (round_now % dispatchPeriod == 0) {
            List<PassengerRequest> requests = getUnassignedPassengerRequests();
            LinkedList<RoboTaxi> emptyTaxis = new LinkedList<>(getDivertableUnassignedRoboTaxis()
            );
            for (PassengerRequest request : requests) {
                setRoboTaxiPickup(emptyTaxis.poll(), request);
            }
        }
        if (round_now % fleetSizePeriod == 0) {
            long activeTaxiTargetDiff = getActiveRoboTaxisCount() - fleetSize.getTargetFleetSize(round_now);

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
                    setRoboTaxiActive(inactiveTaxis.poll());
                }
            }
        }
    }

    public static class Factory implements AVDispatcherFactory {
        @Override
        public AmodeusDispatcher createDispatcher(ModalProviders.InstanceGetter inject) {
            Config config = inject.get(Config.class);
            AmodeusModeConfig operatorConfig = inject.getModal(AmodeusModeConfig.class);
            AmodeusRouter router = inject.getModal(AmodeusRouter.class);
            MatsimAmodeusDatabase db = inject.get(MatsimAmodeusDatabase.class);
            TravelTime travelTime = inject.getModal(TravelTime.class);
            EventsManager eventsManager = inject.get(EventsManager.class);
            DynamicFleetSize dynamicFleetSize = inject.get(DynamicFleetSize.class);

            return new FleetSizeDemoDispatcher(config, operatorConfig, travelTime, router,
                    eventsManager, db, dynamicFleetSize) {
            };
        }
    }
}
