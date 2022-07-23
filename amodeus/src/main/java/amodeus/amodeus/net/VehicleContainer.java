/* amodeus - Copyright (c) 2018, ETH Zurich, Institute for Dynamic Systems and Control */
package amodeus.amodeus.net;

import java.io.Serializable;

import amodeus.amodeus.dispatcher.core.RoboTaxiStatus;

public class VehicleContainer implements Serializable {
    public static final int LINK_UNSPECIFIED = -1;

    /** WARNING:
     * 
     * ANY MODIFICATION IN THIS CLASS EXCEPT COMMENTS
     * WILL INVALIDATE PREVIOUS SIMULATION RECORDINGS
     * 
     * DO NOT MODIFY THIS CLASS UNLESS
     * THERE IS A VERY GOOD REASON */

    public int vehicleIndex = -1; // for tracking of individual vehicles

    /** Links the RoboTaxi traveled (ordered w.r.t time), the integer
     * linkIndex is related to MATSIM's LinkId, the value -1 is set in
     * case the information is not available, dupcliates are removed in
     * the process of generation */
    public int[] linkTrace = new int[] { LINK_UNSPECIFIED };

    /** Statii associated to the links in linkTrace
     * there might be an additional {@link RoboTaxiStatus} if the status
     * changes on the last link */
    public RoboTaxiStatus[] statii = new RoboTaxiStatus[] {};

    /** value -1 in case no particular destination */
    public int destinationLinkIndex = LINK_UNSPECIFIED;
}
