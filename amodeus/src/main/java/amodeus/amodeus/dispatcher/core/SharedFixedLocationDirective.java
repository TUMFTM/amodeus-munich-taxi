/* amodeus - Copyright (c) 2018, ETH Zurich, Institute for Dynamic Systems and Control */
package amodeus.amodeus.dispatcher.core;

import org.matsim.contrib.dvrp.passenger.PassengerRequest;

/** for vehicles that are in stay task and should dropoff a customer at the link:
 * 1) finish stay task 2) append dropoff task 3) if more customers planned append drive task
 * 4) append new stay task */
// TODO @sebhoerl check if we could make that possible to use such a directive for pickup and dropoff
/* package */ abstract class SharedFixedLocationDirective implements DirectiveInterface {
    final RoboTaxi roboTaxi;
    final PassengerRequest avRequest;
    final double getTimeNow;
    final double durationOfTask;

    public SharedFixedLocationDirective(RoboTaxi roboTaxi, PassengerRequest avRequest, double getTimeNow, double durationOfTask) {
        this.roboTaxi = roboTaxi;
        this.avRequest = avRequest;
        this.getTimeNow = getTimeNow;
        this.durationOfTask = durationOfTask;
    }

}
