package org.matsim.amodeus.dvrp.schedule;

import org.matsim.contrib.dvrp.passenger.PassengerRequest;
import org.matsim.contrib.dvrp.path.VrpPathWithTravelData;

import java.util.Collection;

public class AmodeusCustomerDriveTask extends AmodeusDriveTask{
    public AmodeusCustomerDriveTask(VrpPathWithTravelData path) {
        super(path);
    }

    public AmodeusCustomerDriveTask(VrpPathWithTravelData path, Collection<PassengerRequest> requests) {
        super(path, requests);
    }
}
