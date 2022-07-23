package org.matsim.amodeus.dvrp.schedule;

import amodeus.amodeus.dispatcher.core.DynamicFleetSizeRoboTaxiAdapter;
import org.matsim.api.core.v01.network.Link;
import org.matsim.contrib.dvrp.schedule.StayTask;


public class AmodeusLoginTask extends StayTask {

    public AmodeusLoginTask(TaskType taskType, double beginTime, double endTime, Link link) {
        super(DynamicFleetSizeRoboTaxiAdapter.FleetSizingTask.LOGIN, beginTime, endTime, link);
    }
}
