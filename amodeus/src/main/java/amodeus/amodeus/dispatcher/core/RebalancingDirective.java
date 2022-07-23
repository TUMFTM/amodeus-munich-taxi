package amodeus.amodeus.dispatcher.core;


import org.matsim.amodeus.dvrp.schedule.AmodeusRebalanceTask;
import org.matsim.contrib.dvrp.path.VrpPathWithTravelData;
import org.matsim.contrib.dvrp.schedule.Schedule;
import org.matsim.contrib.dvrp.tracker.OnlineDriveTaskTracker;
import org.matsim.contrib.dvrp.tracker.TaskTracker;

public class RebalancingDirective  extends FuturePathDirective {
    private final RoboTaxi roboTaxi;

    RebalancingDirective(FuturePathContainer futurePathContainer, RoboTaxi roboTaxi) {
        super(futurePathContainer);
        this.roboTaxi = roboTaxi;
    }

    @Override
    void executeWithPath(VrpPathWithTravelData vrpPathWithTravelData) {
        final Schedule schedule = roboTaxi.getSchedule();
        final AmodeusRebalanceTask avDriveTask = (AmodeusRebalanceTask) schedule.getCurrentTask(); // <- implies that task is started
        TaskTracker taskTracker = avDriveTask.getTaskTracker();
        OnlineDriveTaskTracker onlineDriveTaskTracker = (OnlineDriveTaskTracker) taskTracker;
        onlineDriveTaskTracker.divertPath(vrpPathWithTravelData);
    }
}
