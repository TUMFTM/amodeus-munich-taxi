/* amodeus - Copyright (c) 2018, ETH Zurich, Institute for Dynamic Systems and Control */
package amodeus.amodeus.dispatcher.core;


import org.apache.log4j.Logger;
import org.matsim.amodeus.dvrp.schedule.*;
import org.matsim.api.core.v01.network.Link;
import org.matsim.contrib.dvrp.path.VrpPathWithTravelData;
import org.matsim.contrib.dvrp.schedule.Schedule;
import org.matsim.contrib.dvrp.schedule.Task;

import amodeus.amodeus.util.math.GlobalAssert;

/** for vehicles that are in stay task:
 * 1) stop stay task by setting stop time to 'now'
 * 2) append drive task
 * 3) append stay task for later */
/* package */ final class StayVehicleDiversionDirective extends VehicleDiversionDirective {
    private final static Logger logger = Logger.getLogger(StayVehicleDiversionDirective.class);
    StayVehicleDiversionDirective(RoboTaxi vehicleLinkPair, Link destination, FuturePathContainer futurePathContainer) {
        super(vehicleLinkPair, destination, futurePathContainer);
    }

    @Override
    void executeWithPath(VrpPathWithTravelData vrpPathWithTravelData) {
        final Schedule schedule = roboTaxi.getSchedule();
        final AmodeusStayTask avStayTask = (AmodeusStayTask) schedule.getCurrentTask(); // <- implies that task is started
        final double scheduleEndTime = avStayTask.getEndTime(); // typically 108000.0
        GlobalAssert.that(scheduleEndTime == schedule.getEndTime());

        final Task nextTask;
        switch (roboTaxi.getStatus()){
            case DRIVETOCUSTOMER:
                nextTask= new AmodeusApproachTask(vrpPathWithTravelData);
                break;
            case REBALANCEDRIVE:
                nextTask= new AmodeusRebalanceTask(vrpPathWithTravelData);
                break;
            case DRIVEWITHCUSTOMER:
                nextTask= new AmodeusCustomerDriveTask(vrpPathWithTravelData);
                break;
            case OFFSERVICE:
                nextTask = new OffServiceStayTask(roboTaxi.getDivertableTime(), scheduleEndTime, roboTaxi.getDivertableLocation());
                break;
            case STAY:
                nextTask = new AmodeusStayTask(roboTaxi.getDivertableTime(), scheduleEndTime, roboTaxi.getDivertableLocation());
                break;
            default:
                logger.warn("Invalid Status during drive");
                nextTask= new AmodeusDriveTask(vrpPathWithTravelData);
                break;
        }

        final double endDriveTask = nextTask.getEndTime();


        if (endDriveTask < scheduleEndTime) {
            GlobalAssert.that(avStayTask.getStatus() == Task.TaskStatus.STARTED);
            avStayTask.setEndTime(roboTaxi.getDivertableTime());
            schedule.addTask(nextTask);
            ScheduleUtils.makeWhole(roboTaxi, endDriveTask, scheduleEndTime, destination);
        } else if (endDriveTask== scheduleEndTime && nextTask instanceof AmodeusStayTask){
            GlobalAssert.that(avStayTask.getStatus() == Task.TaskStatus.STARTED);
            avStayTask.setEndTime(roboTaxi.getDivertableTime());
            schedule.addTask(nextTask);
//            logger.info("Sucessfully added LogOffTask");
        }else{
            reportExecutionBypass(endDriveTask - scheduleEndTime);
        }

    }

}
