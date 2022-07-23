package org.matsim.amodeus.dvrp.schedule;

import org.matsim.contrib.dvrp.passenger.PassengerRequest;
import org.matsim.contrib.dvrp.path.VrpPathWithTravelData;
import org.matsim.contrib.dvrp.schedule.DriveTask;

import java.util.Collection;

public class AmodeusApproachTask extends AmodeusDriveTask {

    public AmodeusApproachTask(VrpPathWithTravelData path) {
        super(path);
    }

    public AmodeusApproachTask(VrpPathWithTravelData path, Collection<PassengerRequest> requests) {
        super(path, requests);
    }
}
