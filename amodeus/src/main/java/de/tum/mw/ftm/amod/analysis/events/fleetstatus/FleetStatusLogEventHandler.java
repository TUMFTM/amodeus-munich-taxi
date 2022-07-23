package de.tum.mw.ftm.amod.analysis.events.fleetstatus;

import org.matsim.core.events.handler.EventHandler;

public interface FleetStatusLogEventHandler extends EventHandler {
     void handleEvent (FleetStatusLogEvent event);
}
