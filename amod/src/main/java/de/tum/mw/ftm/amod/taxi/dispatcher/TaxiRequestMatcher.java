package de.tum.mw.ftm.amod.taxi.dispatcher;

import amodeus.amodeus.dispatcher.core.RoboTaxi;
import org.matsim.contrib.dvrp.passenger.PassengerRequest;

import java.util.Collection;
import java.util.Map;

public interface TaxiRequestMatcher {

    Map<RoboTaxi, PassengerRequest> matchRequests(Collection<RoboTaxi> roboTaxis, Collection<PassengerRequest> requests);

}
