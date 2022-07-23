package de.tum.mw.ftm.amod.analysis.events.fleetstatus;

import org.matsim.api.core.v01.events.Event;

public class FleetStatusLogEvent extends Event {
    private static final String TYPE = "FleetStatusLogEvent";
    private FleetStatusInformation fleetStatusInformation;

    public FleetStatusLogEvent(double time, FleetStatusInformation fleetStatusInformation) {
        super(time);
        this.fleetStatusInformation = fleetStatusInformation;
    }

    public FleetStatusInformation getFleetStatusInformation() {
        return fleetStatusInformation;
    }

    @Override
    public String getEventType() {
        return TYPE;
    }
}
