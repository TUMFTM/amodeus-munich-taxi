package de.tum.mw.ftm.amod.analysis.events.passengerrequest;

import org.matsim.core.events.handler.EventHandler;

public interface PassengerRequestAnalysisEventHandler extends EventHandler {
    void handleEvent (PassengerRequestAnalysisEvent event);

}
