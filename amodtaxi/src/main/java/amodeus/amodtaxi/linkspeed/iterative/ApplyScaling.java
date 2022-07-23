/* amodeus - Copyright (c) 2018, ETH Zurich, Institute for Dynamic Systems and Control */
package amodeus.amodtaxi.linkspeed.iterative;

import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import amodeus.amodeus.linkspeed.LinkSpeedDataContainer;
import amodeus.amodeus.linkspeed.LinkSpeedTimeSeries;
import amodeus.amodeus.taxitrip.TaxiTrip;
import amodeus.amodeus.util.AmodeusTimeConvert;
import amodeus.amodeus.util.math.GlobalAssert;
import com.fasterxml.jackson.annotation.ObjectIdGenerators;
import org.matsim.api.core.v01.network.Link;
import org.matsim.core.router.util.LeastCostPathCalculator.Path;

import ch.ethz.idsc.tensor.RealScalar;
import ch.ethz.idsc.tensor.Scalar;

/* package */ enum ApplyScaling {
    ;

    public static void to(LinkSpeedDataContainer lsData, TaxiTrip trip, Path path, Scalar rescalefactor, boolean allowIncrease) {
        for (Link link : path.links) {
            /** get link properties */
            double freeSpeed = link.getFreespeed();

            Set<Integer> relevantTimes = getRelevantTimes(trip.getSimPickupTime(), trip.getSimDropoffTime(), lsData.getDt());

            /** if no recordings are present, initialize with free speed for duration of trip */
            if (lsData.get(link) == null) {
                for (int time : relevantTimes) {
                    lsData.addData(link, time, freeSpeed);
                }
            }

            /** if there are recordings, but not for the whole trip, add freeflowspeeds for them */
            Set<Integer> missingIntervals = new HashSet<>(relevantTimes);
            missingIntervals.removeAll(lsData.get(link).getRecordedTimes());
            for (int time : missingIntervals) {
                lsData.addData(link, time, freeSpeed);
            }


//            lsTime = lsData.get(link);
//            Objects.requireNonNull(lsTime);
//
//            List<Integer> relevantTimes = lsTime.getRecordedTimes().stream() //
//                    .filter(time -> tripStart <= time && time <= tripEnd).collect(Collectors.toList());


            //TODO: @michaelwittmann check if this still works
//            if (relevantTimes.size() == 0) // must have at least one entry for convergence
//                relevantTimes.add(lsTime.getTimeFloor(tripStart));
//
//            GlobalAssert.that(relevantTimes.size() > 0);

            // TODO: @michaelwittmann remove just for debug.,
            int counterIncrease = 0;

            for (int time : relevantTimes) {
                Scalar speedNow = RealScalar.of(freeSpeed);
                Double recorded = lsData.get(link).getSpeedsAt(time);
                if (Objects.nonNull(recorded))
                    speedNow = RealScalar.of(recorded);
                Scalar newSpeedS = speedNow.multiply(rescalefactor);
                double newSpeed = newSpeedS.number().doubleValue();

                // NOW
                if (newSpeed <= link.getFreespeed() || allowIncrease)
                    lsData.get(link).setSpeed(time, newSpeed);
                else {
                    counterIncrease++;
                }
            }
        }

    }

    private static Set<Integer> getRelevantTimes(int tripStart, int tripEnd, Integer dt) {
        /** if dt is Null, set to one second as minimal interval*/
        if (dt == null) dt = 1;
        Set<Integer> relevantTimes = new HashSet<Integer>();
        for (int i = tripStart / dt * dt; i <= tripEnd / dt * dt; i += dt) {
            relevantTimes.add(i);
        }
        return relevantTimes;
    }

}
