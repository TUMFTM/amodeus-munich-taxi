package amodeus.amodeus.dispatcher.core;

import org.matsim.amodeus.dvrp.schedule.AmodeusLoginTask;
import org.matsim.amodeus.dvrp.schedule.OffServiceStayTask;

public interface FleetSizingRoboTaxiTaskListener extends RoboTaxiTaskListener{

    public void handle(AmodeusLoginTask avStayTask);
    public void handle(OffServiceStayTask avStayTask);
}
