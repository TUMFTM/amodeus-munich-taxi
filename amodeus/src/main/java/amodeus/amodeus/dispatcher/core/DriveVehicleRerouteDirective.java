/* amodeus - Copyright (c) 2018, ETH Zurich, Institute for Dynamic Systems and Control */
package amodeus.amodeus.dispatcher.core;

import org.apache.log4j.Logger;
import org.matsim.amodeus.dvrp.schedule.AmodeusApproachTask;
import org.matsim.amodeus.dvrp.schedule.AmodeusCustomerDriveTask;
import org.matsim.amodeus.dvrp.schedule.AmodeusDriveTask;
import org.matsim.amodeus.dvrp.schedule.AmodeusRebalanceTask;
import org.matsim.contrib.dvrp.path.VrpPathWithTravelData;
import org.matsim.contrib.dvrp.schedule.Schedule;
import org.matsim.contrib.dvrp.tracker.OnlineDriveTaskTracker;
import org.matsim.contrib.dvrp.tracker.TaskTracker;

/** for vehicles that are currently driving, but should go to a new destination:
 * 1) change path of current drive task */
/* package */ final class DriveVehicleRerouteDirective extends FuturePathDirective {
    private final RoboTaxi roboTaxi;
    private final static Logger logger = Logger.getLogger(DriveVehicleRerouteDirective.class);
    DriveVehicleRerouteDirective(FuturePathContainer futurePathContainer, RoboTaxi roboTaxi) {
        super(futurePathContainer);
        this.roboTaxi = roboTaxi;
    }

    @Override
    void executeWithPath(VrpPathWithTravelData vrpPathWithTravelData) {
        final Schedule schedule = roboTaxi.getSchedule();
        final AmodeusDriveTask avDriveTask = (AmodeusDriveTask) schedule.getCurrentTask(); // <- implies that task is started
        TaskTracker taskTracker = avDriveTask.getTaskTracker();
        OnlineDriveTaskTracker onlineDriveTaskTracker = (OnlineDriveTaskTracker) taskTracker;
        onlineDriveTaskTracker.divertPath(vrpPathWithTravelData);

        avDriveTask.setEndTime(roboTaxi.getDivertableTime());
        final AmodeusDriveTask newAvDriveTask;
        switch (roboTaxi.getStatus()){
            case DRIVETOCUSTOMER:
                newAvDriveTask= new AmodeusApproachTask(vrpPathWithTravelData);
                break;
            case REBALANCEDRIVE:
                newAvDriveTask= new AmodeusRebalanceTask(vrpPathWithTravelData);
                break;
            case DRIVEWITHCUSTOMER:
                newAvDriveTask= new AmodeusCustomerDriveTask(vrpPathWithTravelData);
                break;
            default:
                logger.warn("Invalid Status during drive");
                newAvDriveTask= new AmodeusDriveTask(vrpPathWithTravelData);
                break;
        }
        schedule.addTask(newAvDriveTask);
    }
}
