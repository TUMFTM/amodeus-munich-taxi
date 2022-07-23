/* amodeus - Copyright (c) 2018, ETH Zurich, Institute for Dynamic Systems and Control */
package amodeus.amodeus.dispatcher.core;

import java.util.*;

import org.apache.log4j.Logger;
import org.matsim.amodeus.dvrp.schedule.*;
import org.matsim.api.core.v01.network.Link;
import org.matsim.contrib.dvrp.path.VrpPath;
import org.matsim.contrib.dvrp.path.DivertedVrpPath;
import org.matsim.contrib.dvrp.path.VrpPathWithTravelData;
import org.matsim.contrib.dvrp.path.VrpPathWithTravelDataImpl;
import org.matsim.contrib.dvrp.schedule.Schedule;
import org.matsim.contrib.dvrp.schedule.Schedules;
import org.matsim.contrib.dvrp.schedule.Task;
import org.matsim.contrib.dvrp.tracker.OnlineDriveTaskTracker;
import org.matsim.contrib.dvrp.util.LinkTimePair;

import amodeus.amodeus.util.math.GlobalAssert;

/** for vehicles that are currently driving, but should go to a new destination:
 * 1) change path of current drive task
 * 2) remove former stay task with old destination
 * 3) append new stay task */
/* package */ final class DriveVehicleDiversionDirective extends VehicleDiversionDirective {
    private final static Logger logger = Logger.getLogger(DriveVehicleDiversionDirective.class);
    DriveVehicleDiversionDirective(RoboTaxi roboTaxi, Link destination, FuturePathContainer futurePathContainer) {
        super(roboTaxi, destination, futurePathContainer);
    }

    @Override
    void executeWithPath(VrpPathWithTravelData vrpPathWithTravelData) {
        final Schedule schedule = roboTaxi.getSchedule();
        final AmodeusDriveTask actualAvDriveTask = (AmodeusDriveTask) schedule.getCurrentTask(); // <- implies that task is started
        final AmodeusStayTask avStayTask = (AmodeusStayTask) Schedules.getLastTask(schedule);
        final double scheduleEndTime = avStayTask.getEndTime();

        OnlineDriveTaskTracker taskTracker = (OnlineDriveTaskTracker) actualAvDriveTask.getTaskTracker();
        LinkTimePair diversionPoint = Objects.requireNonNull(taskTracker.getDiversionPoint());
        
        boolean isCurrentLinkDiversion = diversionPoint.link == taskTracker.getPath().getLink(taskTracker.getCurrentLinkIdx());
        final int diversionLinkIndex = taskTracker.getCurrentLinkIdx() + (isCurrentLinkDiversion ? 0 : 1);
        
        final int lengthOfDiversion = vrpPathWithTravelData.getLinkCount();
        OnlineDriveTaskTracker onlineDriveTaskTracker = (OnlineDriveTaskTracker) taskTracker;
        final double newEndTime = vrpPathWithTravelData.getArrivalTime();

        //TODO: @michaelwittmann WHY do we need that?
        if (newEndTime < scheduleEndTime)
            try {
//                onlineDriveTaskTracker.divertPath(vrpPathWithTravelData);
//                GlobalAssert.that(VrpPathUtils.isConsistent(actualAvDriveTask.getPath()));
//
//                final int lengthOfCombination = actualAvDriveTask.getPath().getLinkCount();
//                // System.out.println(String.format("[@%d of %d]", diversionLinkIndex, lengthOfCombination));
//                if (diversionLinkIndex + lengthOfDiversion != lengthOfCombination)
//                    throw new RuntimeException("mismatch " + diversionLinkIndex + "+" + lengthOfDiversion + " != " + lengthOfCombination);

//                GlobalAssert.that(actualAvDriveTask.getEndTime() == newEndTime);

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
                        nextTask =new OffServiceStayTask(roboTaxi.getDivertableTime(), scheduleEndTime, roboTaxi.getDivertableLocation());
                        break;
                    default:
                        logger.warn(String.format("Invalid Status during drive. [RoboTaxi: %s , Status: %s, ActualDriveTask: %s ",
                                roboTaxi.getId().toString(),
                                roboTaxi.getStatus().description(),
                                actualAvDriveTask.getClass().getName()));
                        nextTask= new AmodeusDriveTask(vrpPathWithTravelData);
                        break;
                }


                // Overwrite actual Path with new one
                VrpPath originalPath = actualAvDriveTask.getPath();
                Link[] cuttedPathLinks = new Link[1];
                double[] cuttedPathTT = new double[1];
                cuttedPathLinks[0] = originalPath.getLink(diversionLinkIndex);
                cuttedPathTT[0] = originalPath.getLinkTravelTime(diversionLinkIndex);
                VrpPathWithTravelData cuttedPath = new VrpPathWithTravelDataImpl(roboTaxi.getDivertableTime(), Arrays.stream(cuttedPathTT).sum(), cuttedPathLinks, cuttedPathTT);
                onlineDriveTaskTracker.divertPath(cuttedPath);
                actualAvDriveTask.setEndTime(roboTaxi.getDivertableTime());

                long plannedTasks = schedule.getTasks().stream().filter(task -> task.getStatus() == Task.TaskStatus.PLANNED).count();
                for (int i = 0; i < plannedTasks; i++) {
                    schedule.removeLastTask();
                }
                // schedule.removeLastTask(); // remove former stay task with old destination
                schedule.addTask(nextTask);

                if(roboTaxi.getStatus() != RoboTaxiStatus.OFFSERVICE) {
                    ScheduleUtils.makeWhole(roboTaxi, newEndTime, scheduleEndTime, destination);
                }
            } catch (Exception e) {
                System.err.println("Robotaxi ID: " + roboTaxi.getId().toString());
                System.err.println("====================================");
                System.err.println("Found problem with diversionLinkIdx!");
                System.err.println("====================================");
                throw new IllegalStateException();
            }
        else
            reportExecutionBypass(newEndTime - scheduleEndTime);
    }

}
