package de.tum.mw.ftm.amod.analysis.events.trips;

import amodeus.amodeus.dispatcher.core.RoboTaxi;
import org.matsim.api.core.v01.events.Event;

import java.util.Collection;

public class FinalRoboTaxiSchedulesEvent extends Event {

    private static final String TYPE = "FinalRoboTaxiSchedulesEvent";
    private Collection<RoboTaxi> roboTaxis;
    public FinalRoboTaxiSchedulesEvent(double time, Collection<RoboTaxi> roboTaxis) {
        super(time);
        this.roboTaxis = roboTaxis;
    }

    public Collection<RoboTaxi> getRoboTaxis() {
        return roboTaxis;
    }

    @Override
    public String getEventType() {
        return TYPE;
    }
}
