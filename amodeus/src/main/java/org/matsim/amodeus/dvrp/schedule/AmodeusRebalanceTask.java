package org.matsim.amodeus.dvrp.schedule;

import org.matsim.contrib.dvrp.passenger.PassengerRequest;
import org.matsim.contrib.dvrp.path.VrpPathWithTravelData;

import java.util.Collection;

public class AmodeusRebalanceTask extends AmodeusDriveTask{
    public AmodeusRebalanceTask(VrpPathWithTravelData path) {
        super(path);
    }

    public AmodeusRebalanceTask(VrpPathWithTravelData path, Collection<PassengerRequest> requests) {
        super(path, requests);
    }
}
