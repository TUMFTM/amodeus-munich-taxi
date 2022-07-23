package org.matsim.amodeus.dvrp.schedule;

import org.matsim.api.core.v01.network.Link;

public class OffServiceStayTask extends AmodeusStayTask {
    public OffServiceStayTask(double beginTime, double endTime, Link link) {
        super(beginTime, endTime, link);
        }
}
