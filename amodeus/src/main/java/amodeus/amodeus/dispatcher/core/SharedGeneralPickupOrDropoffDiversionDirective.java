/* amodeus - Copyright (c) 2018, ETH Zurich, Institute for Dynamic Systems and Control */
package amodeus.amodeus.dispatcher.core;

import org.matsim.amodeus.dvrp.schedule.AmodeusDriveTask;
import org.matsim.amodeus.dvrp.schedule.AmodeusStayTask;
import org.matsim.contrib.dvrp.path.VrpPathWithTravelData;
import org.matsim.contrib.dvrp.schedule.Schedule;
import org.matsim.contrib.dvrp.schedule.Schedules;

import amodeus.amodeus.util.math.GlobalAssert;

/** for vehicles that are in dropoff or pickup task and new request is assigned.
 * 1) finish pickup or dropoff task 2) append drive task 3) append new stay task */
/* package */ final class SharedGeneralPickupOrDropoffDiversionDirective extends FuturePathDirective {
    final RoboTaxi roboTaxi;
    final double getTimeNow;

    /** for vehicles that are in dropoff or pickup task and new request is assigned.
     * 1) finish pickup or dropoff task 2) append drive task 3) append new stay task */
    public SharedGeneralPickupOrDropoffDiversionDirective(RoboTaxi roboTaxi, //
            FuturePathContainer futurePathContainer, final double getTimeNow) {
        super(futurePathContainer);
        this.roboTaxi = roboTaxi;
        this.getTimeNow = getTimeNow;
    }

    @Override
    void executeWithPath(final VrpPathWithTravelData vrpPathWithTravelData) {
        final Schedule schedule = roboTaxi.getSchedule();
        final AmodeusStayTask avStayTask = (AmodeusStayTask) Schedules.getLastTask(schedule);
        final double scheduleEndTime = avStayTask.getEndTime();
        final double endTaskTime = vrpPathWithTravelData.getArrivalTime();
        GlobalAssert.that(scheduleEndTime == schedule.getEndTime());

        if (endTaskTime < scheduleEndTime) {
            // Remove all pending tasks in the future
            int counter = 0;
            while (Schedules.getLastTask(schedule).getEndTime() != schedule.getCurrentTask().getEndTime()) {
                schedule.removeLastTask();
                counter++;
            }
            GlobalAssert.that(counter == 1); // WE make sure that there was only the stay Task at the end removed.

            // Add new drive task
            schedule.addTask(new AmodeusDriveTask( //
                    vrpPathWithTravelData));

            ScheduleUtils.makeWhole(roboTaxi, endTaskTime, scheduleEndTime, vrpPathWithTravelData.getToLink());

            // jan: following computation is mandatory for the internal scoring
            // function
            // final double distance = VrpPathUtils.getDistance(vrpPathWithTravelData);
            // nextRequest.getRoute().setDistance(distance);
        } else
            reportExecutionBypass(endTaskTime - scheduleEndTime);
    }

}
