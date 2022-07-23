package de.tum.mw.ftm.amod.analysis.events.passengerrequest;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.events.Event;
import org.matsim.contrib.dvrp.fleet.DvrpVehicle;
import org.matsim.contrib.dvrp.passenger.PassengerRequest;

public class PassengerRequestAnalysisEvent extends Event {
    public enum PassengerEventType {
        PASSENGER_REQUEST_ASSIGNED,
        PASSENGER_REQUEST_ARRIVED,
        PASSENGER_REQUEST_PICKUP,
        PASSENGER_REQUEST_DROPOFF,
        PASSENGER_REQUEST_CANCELLED};

    private final double timestamp;
    private final Id<DvrpVehicle> relatedVehicle;
    private final PassengerRequest relatedPassengerRequest;
    private final PassengerEventType type;


    public PassengerRequestAnalysisEvent(double timestamp,
                                         Id<DvrpVehicle> relatedVehicle,
                                         PassengerRequest relatedPassengerRequest,
                                         PassengerEventType type
                                 ) {
        super(timestamp);
        this.timestamp = timestamp;
        this.relatedVehicle = relatedVehicle;
        this.relatedPassengerRequest = relatedPassengerRequest;
        this.type = type;
    }

    public double getTimestamp() {
        return timestamp;
    }

    public Id<DvrpVehicle> getRelatedVehicle() {
        return relatedVehicle;
    }

    public PassengerRequest getRelatedPassengerRequest() {
        return relatedPassengerRequest;
    }

    public PassengerEventType getType() {
        return type;
    }

    @Override
    public String getEventType() {
        return type.toString();
    }
}
