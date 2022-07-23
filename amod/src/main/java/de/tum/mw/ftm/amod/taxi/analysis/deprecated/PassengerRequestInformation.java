package de.tum.mw.ftm.amod.taxi.analysis.deprecated;

import amodeus.amodeus.util.math.GlobalAssert;
import org.locationtech.jts.geom.Point;
import org.matsim.core.utils.misc.OptionalTime;

public class PassengerRequestInformation {
    private OptionalTime submissionTime = OptionalTime.undefined();
    private OptionalTime assignTime = OptionalTime.undefined();
    private OptionalTime pickupTime = OptionalTime.undefined();
    private OptionalTime startTime = OptionalTime.undefined();
    private OptionalTime dropoffTime = OptionalTime.undefined();
    private int associatedTaxiId;
    private final Point startLocation;
    private final Point stopLocation;
    private boolean canceled = false;

    public PassengerRequestInformation(int associatedTaxiId, Point startLocation, Point stopLocation) {
        this.associatedTaxiId = associatedTaxiId;
        this.startLocation = startLocation;
        this.stopLocation = stopLocation;
    }

    public OptionalTime getSubmissionTime() {
        return submissionTime;
    }

    public void setSubmissionTime(OptionalTime submissionTime) {
        this.submissionTime = submissionTime;
    }

    public OptionalTime getAssignTime() {
        return assignTime;
    }

    public void setAssignTime(OptionalTime assignTime) {
        this.assignTime = assignTime;
    }

    public OptionalTime getPickupTime() {
        return pickupTime;
    }

    public void setPickupTime(OptionalTime pickupTime) {
        this.pickupTime = pickupTime;
    }

    public OptionalTime getStartTime() {
        return startTime;
    }

    public void setStartTime(OptionalTime startTime) {
        this.startTime = startTime;
    }

    public OptionalTime getDropoffTime() {
        return dropoffTime;
    }

    public void setDropoffTime(OptionalTime dropoffTime) {
        this.dropoffTime = dropoffTime;
    }

    public void setAssociatedTaxiId(int associatedTaxiId) {
        this.associatedTaxiId = associatedTaxiId;
    }

    public int getAssociatedTaxiId() {
        return associatedTaxiId;
    }

    public Point getStartLocation() {
        return startLocation;
    }

    public Point getStopLocation() {
        return stopLocation;
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
