/* amodeus - Copyright (c) 2019, ETH Zurich, Institute for Dynamic Systems and Control */
package amodeus.amodeus.dispatcher.shared.tshare;

import org.matsim.contrib.dvrp.passenger.PassengerRequest;

import amodeus.amodeus.routing.NetworkTimeDistInterface;
import amodeus.amodeus.util.math.SI;
import ch.ethz.idsc.tensor.Scalar;
import ch.ethz.idsc.tensor.Scalars;
import ch.ethz.idsc.tensor.qty.Quantity;

/* package */ enum LatestArrival {
    ;

    /** @return latest arrival of {@link PassengerRequest} @param avRequest given a maximum
     *         tolerable {@link Scalar} delay @param maxDropoffDelay, the nominal travle time is
     *         computed with {@link NetworkTimeDistInterface} @param travelTimeCashed from the current
     *         time @param timeNow */
    public static Scalar of(PassengerRequest avRequest, Scalar maxDropoffDelay, //
            NetworkTimeDistInterface travelTimeCashed, double timeNow) {
        /** in the T-Share publication, the end time window is supplied by the user, here
         * it is computed as the expeted arrival time plus a fixed delay. */
        return Quantity.of(avRequest.getSubmissionTime(), SI.SECOND) //
                .add(travelTimeCashed.travelTime(avRequest.getFromLink(), avRequest.getToLink(), timeNow)) //
                .add(maxDropoffDelay);
    }

    /** @return {@link Scalar} time left to dop off {@link PassengerRequest} @param avRequest with
     *         the {@link Scalar} @param maxDropoffDelay given the current time @param timeNow and
     *         the expected travel distance calculated with @param travelTimeCashed, returns zero
     *         if already overdue. */
    public static Scalar timeTo(PassengerRequest avRequest, Scalar maxDropoffDelay, //
            NetworkTimeDistInterface travelTimeCashed, double timeNow) {
        Scalar latestArrival = of(avRequest, maxDropoffDelay, travelTimeCashed, timeNow);
        Scalar time = Quantity.of(timeNow, SI.SECOND);
        Scalar slack = latestArrival.subtract(time);
        Scalar timeTo = Scalars.lessThan(slack, Quantity.of(0, SI.SECOND)) ? //
                Quantity.of(0, "s") : slack;
        return timeTo;
    }

}
