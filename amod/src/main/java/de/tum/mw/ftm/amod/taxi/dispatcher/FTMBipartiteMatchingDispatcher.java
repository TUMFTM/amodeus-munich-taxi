package de.tum.mw.ftm.amod.taxi.dispatcher;

import amodeus.amodeus.dispatcher.core.RoboTaxi;
import amodeus.amodeus.dispatcher.core.UniversalDispatcher;
import amodeus.amodeus.dispatcher.util.BipartiteMatcher;
import amodeus.amodeus.dispatcher.util.ConfigurableBipartiteMatcher;
import amodeus.amodeus.dispatcher.util.DistanceCost;
import amodeus.amodeus.routing.DistanceFunction;
import amodeus.amodeus.util.matsim.SafeConfig;
import org.apache.log4j.Logger;
import org.matsim.amodeus.config.FTMConfigGroup;
import org.matsim.api.core.v01.network.Network;
import org.matsim.contrib.dvrp.passenger.PassengerRequest;
import org.matsim.core.config.Config;

import java.util.Collection;
import java.util.Map;

public class FTMBipartiteMatchingDispatcher implements IFTMDispatcher {
    private final UniversalDispatcher universalDispatcher;
    private final TaxiRequestMatcher bipartiteMatcher;
    private final DistanceFunction distanceFunction;
    private final Network network;
    private final long maxCustomerWaitingTime;
    private final static Logger logger = Logger.getLogger(FTMBipartiteMatchingDispatcher.class);

    public FTMBipartiteMatchingDispatcher(UniversalDispatcher universalDispatcher, Network network,
                                          DistanceFunction distanceFunction, SafeConfig safeDispatcherConfig, FTMConfigGroup ftmConfigGroup) {
        this.universalDispatcher = universalDispatcher;
        this.distanceFunction = distanceFunction;
        this.network = network;
        this.bipartiteMatcher = new FTMBipartiteMatcher(new DistanceCost(distanceFunction),
                safeDispatcherConfig);
        this.maxCustomerWaitingTime = ftmConfigGroup.getMaxCustomerWaitingTime();
    }

    @Override
    public void dispatch(Collection<RoboTaxi> availableTaxis, Collection<PassengerRequest> requests, double timeNow) {
        Map<RoboTaxi, PassengerRequest> matchedRequests =  bipartiteMatcher.matchRequests(availableTaxis,requests);

        for (Map.Entry<RoboTaxi, PassengerRequest> matchedRequest: matchedRequests.entrySet()){
            RoboTaxi roboTaxi = matchedRequest.getKey();
            PassengerRequest request = matchedRequest.getValue();
            double arrivalTime = universalDispatcher.getEstimatedArrivalTime(roboTaxi.getLastKnownLocation(), request.getFromLink(), timeNow);
            // Assing request only if estimated arrival time is within maxCustomerWaitingTime
            if (arrivalTime-request.getSubmissionTime()<=maxCustomerWaitingTime*0.9) {
                universalDispatcher.setRoboTaxiPickup(roboTaxi, request);
                availableTaxis.remove(roboTaxi);
            }
            else {
                logger.warn(String.format("Request %s has not been assigned, cause timeToArrival exceeds maxCustomerWaitingTime", request.getPassengerId()));
                logger.debug(String.format("Estimated arrival time for Passenger Request %s, at %f is %f",
                        request.getPassengerId(),
                        timeNow,
                        arrivalTime));
            }

        }
    }


}
