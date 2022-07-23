package de.tum.mw.ftm.amod.taxi.dispatcher;

import amodeus.amodeus.dispatcher.core.DispatcherConfigWrapper;
import amodeus.amodeus.dispatcher.util.DistanceHeuristics;
import amodeus.amodeus.fleetsize.DynamicFleetSize;
import amodeus.amodeus.net.MatsimAmodeusDatabase;
import amodeus.amodeus.options.ScenarioOptions;
import amodeus.amodeus.routing.DistanceFunction;
import amodeus.amodeus.util.matsim.SafeConfig;
import de.tum.mw.ftm.amod.taxi.preprocessing.ranks.TaxiRanks;
import de.tum.mw.ftm.amod.taxi.util.ZonalUtils;
import org.apache.commons.lang.NotImplementedException;
import org.apache.log4j.Logger;
import org.matsim.amodeus.components.AmodeusDispatcher;
import org.matsim.amodeus.components.AmodeusRouter;
import org.matsim.amodeus.config.AmodeusModeConfig;
import org.matsim.amodeus.config.FTMConfigGroup;
import org.matsim.api.core.v01.network.Network;
import org.matsim.contrib.dvrp.run.ModalProviders;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.config.Config;
import org.matsim.core.router.util.TravelTime;


public class ReferenceRebalancer extends ReferenceDispatcher{
    private final static Logger logger = Logger.getLogger(ReferenceRebalancer.class);
    private final IFTMDispatcher dispatcher;

    protected ReferenceRebalancer(Config config, AmodeusModeConfig operatorConfig, TravelTime travelTime, AmodeusRouter router, EventsManager eventsManager, Network network, MatsimAmodeusDatabase db, ZonalUtils zonalUtils, TaxiRanks taxiRanks, ScenarioOptions scenarioOptions, DynamicFleetSize dynamicFleetSize) {
        super(config, operatorConfig, travelTime, router, eventsManager, network, db, zonalUtils, taxiRanks, scenarioOptions, dynamicFleetSize);
        SafeConfig safeDispatcherConfig = SafeConfig.wrap(operatorConfig.getDispatcherConfig());
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
    protected void dispatch() {
        // Cancel all pending requests which not served within in maxCustomerWaitingTime
        getPendingRequests().stream().filter(r -> (getTimeNow()- r.getSubmissionTime() )> maxCustomerWaitingTime).forEach(this::cancelRequest);

        // Cancel all pending requests which not assigned within in maxCustomerAssignmentTime
        getUnassignedPassengerRequests().stream().filter(request -> (getTimeNow()- request.getSubmissionTime()) > maxCustomerAssignmentTime).forEach(this::cancelRequest);

        dispatcher.dispatch(getDivertableUnassignedRoboTaxis(), getUnassignedPassengerRequests(), getTimeNow());
    }

    public static class Factory implements AVDispatcherFactory {
        @Override
        public AmodeusDispatcher createDispatcher(ModalProviders.InstanceGetter inject) {
            Config config = inject.get(Config.class);
            AmodeusModeConfig operatorConfig = inject.getModal(AmodeusModeConfig.class);
            AmodeusRouter router = inject.getModal(AmodeusRouter.class);
            MatsimAmodeusDatabase db = inject.get(MatsimAmodeusDatabase.class);
            TravelTime travelTime = inject.getModal(TravelTime.class);
            Network network = inject.getModal(Network.class);
            EventsManager eventsManager = inject.get(EventsManager.class);
            ZonalUtils zonalUtils = inject.get(ZonalUtils.class);
            TaxiRanks taxiRanks = inject.get(TaxiRanks.class);
            ScenarioOptions scenarioOptions = inject.get(ScenarioOptions.class);
            DynamicFleetSize dynamicFleetSize = inject.get(DynamicFleetSize.class);

            return new ReferenceRebalancer(config, operatorConfig, travelTime, router,
                    eventsManager, network, db, zonalUtils, taxiRanks, scenarioOptions, dynamicFleetSize);
        }
    }
}


