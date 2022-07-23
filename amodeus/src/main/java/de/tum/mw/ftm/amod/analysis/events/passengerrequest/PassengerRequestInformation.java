package de.tum.mw.ftm.amod.analysis.events.passengerrequest;

import amodeus.amodeus.util.math.GlobalAssert;
import org.locationtech.jts.geom.Point;
import org.matsim.core.utils.misc.OptionalTime;

public class PassengerRequestInformation {
    private final long requestId;
    private OptionalTime submissionTime = OptionalTime.undefined();
    private OptionalTime assignTime = OptionalTime.undefined();
    private OptionalTime pickupTime = OptionalTime.undefined();
    private OptionalTime startTime = OptionalTime.undefined();
    private OptionalTime dropoffTime = OptionalTime.undefined();
    private long associatedTaxiId;
    private Point startLocation;
    private Point stopLocation;
    private boolean canceled = false;


    public PassengerRequestInformation(long requestId, double submissionTime) {
        this.requestId = requestId;
        this.submissionTime = OptionalTime.defined(submissionTime);

    }
    public PassengerRequestInformation(long requestId, long associatedTaxiId, double submissionTime) {
        this(requestId, submissionTime);
        this.associatedTaxiId = associatedTaxiId;
    }

    public long getRequestId() {
        return requestId;
    }

    public OptionalTime getSubmissionTime() {
        return submissionTime;
    }

    public void setSubmissionTime(OptionalTime submissionTime) {
        this.submissionTime = submissionTime;
    }
    public void setSubmissionTime(Double submissionTime) {
        this.submissionTime = OptionalTime.defined(submissionTime);
    }


    public OptionalTime getAssignTime() {
        return assignTime;
    }

    public void setAssignTime(OptionalTime assignTime) {
        this.assignTime = assignTime;
    }
    public void setAssignTime(Double assignTime) {
        this.assignTime = OptionalTime.defined((assignTime));
    }

    public OptionalTime getPickupTime() {
        return pickupTime;
    }

    public void setPickupTime(OptionalTime pickupTime) {
        this.pickupTime = pickupTime;
    }
    public void setPickupTime(Double pickupTime) {
        this.pickupTime = OptionalTime.defined(pickupTime);
    }

    public OptionalTime getStartTime() {
        return startTime;
    }

    public void setStartTime(OptionalTime startTime) {
        this.startTime = startTime;
    }

    public void setStartTime(Double startTime) {
        this.startTime = OptionalTime.defined(startTime);
    }

    public OptionalTime getDropoffTime() {
        return dropoffTime;
    }

    public void setDropoffTime(OptionalTime dropoffTime) {
        this.dropoffTime = dropoffTime;
    }
    public void setDropoffTime(Double dropoffTime) {
        this.dropoffTime = OptionalTime.defined(dropoffTime);
    }

    public void setAssociatedTaxiId(int associatedTaxiId) {
        this.associatedTaxiId = associatedTaxiId;
    }

    public long getAssociatedTaxiId() {
        return associatedTaxiId;
    }

    public Point getStartLocation() {
        return startLocation;
    }

    public Point getStopLocation() {
        return stopLocation;
    }

    public void setStartLocation(Point startLocation) {
        this.startLocation = startLocation;
    }

    public void setStopLocation(Point stopLocation) {
        this.stopLocation = stopLocation;
    }

    public boolean isCanceled() {
        return canceled;
    }

    public void setCanceled(boolean canceled) {
        if(this.canceled && !canceled){
            System.err.println("A already canceled request was assigend canceled == false");
            GlobalAssert.that(false);
        }
        this.canceled = canceled;
    }

    public boolean isTripConsistent() {
        if (submissionTime.isDefined() && assignTime.isDefined()) {
            if (assignTime.seconds() < submissionTime.seconds())
                return false;
            if (pickupTime.isDefined()) {
                if (pickupTime.seconds() < assignTime.seconds())
                    return false;
                if (startTime.isDefined()) {
                    if (startTime.seconds() < pickupTime.seconds())
                        return false;
                    if (dropoffTime.isDefined()) {
                        return dropoffTime.seconds() >= startTime.seconds();
                    }
                }
            }
        }
        return true;
    }
}
