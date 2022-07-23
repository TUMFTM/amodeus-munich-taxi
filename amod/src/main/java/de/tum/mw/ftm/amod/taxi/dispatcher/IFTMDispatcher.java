package de.tum.mw.ftm.amod.taxi.dispatcher;

import amodeus.amodeus.dispatcher.core.RoboTaxi;
import org.matsim.contrib.dvrp.passenger.PassengerRequest;

import java.util.Collection;

public interface IFTMDispatcher {
    void dispatch(Collection<RoboTaxi> availableTaxis, Collection<PassengerRequest> requests, double timNow);
}
