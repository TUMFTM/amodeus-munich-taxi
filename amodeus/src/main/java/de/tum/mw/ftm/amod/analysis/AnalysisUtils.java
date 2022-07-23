package de.tum.mw.ftm.amod.analysis;


import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.Point;
import org.locationtech.jts.geom.impl.CoordinateArraySequence;
import org.matsim.amodeus.dvrp.schedule.*;
import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.network.Link;
import org.matsim.contrib.dvrp.path.DivertedVrpPath;
import org.matsim.contrib.dvrp.path.VrpPath;
import org.matsim.contrib.dvrp.schedule.StayTask;
import org.matsim.contrib.dvrp.schedule.Task;

import java.util.stream.StreamSupport;

public  class AnalysisUtils {

    public enum TaxiTripType {
        OCCUPIED("dwc", "occupied"), //
        APPROACH( "d2c", "approach"), //
        REBALANCING( "reb", "rebalancing"), //
        IDLE( "sty", "stay"), //
        OFFSERVICE( "off", "off service"),
        PICKUP("pu", "waiting_at_pickup"),
        DROPOFF("do", "dropoff");
        private final String tag;
        private final String description;

        TaxiTripType(String xmlTag, String description) {
            this.tag = xmlTag;
            this.description = description;
        }

        public String getTag() {
            return tag;
        }

        public String getDescription() {
            return description;
        }

    }
    public static TaxiTripType getTaxiTripTypeFromTask(Task task) {
        if (task instanceof StayTask) {
            if (task instanceof OffServiceStayTask) {
//                    return "off_service";
                return TaxiTripType.OFFSERVICE;
            } else if (task instanceof AmodeusLoginTask) {
                return TaxiTripType.IDLE;
            } else if (task instanceof AmodeusStayTask) {
                return TaxiTripType.IDLE;
            } else if (task instanceof AmodeusPickupTask) {
                return TaxiTripType.PICKUP;
            } else if (task instanceof AmodeusDropoffTask) {
                return TaxiTripType.DROPOFF;
            } else {
                //TODO: @michaelwittmann verify that we did not forget any cases.
                return TaxiTripType.IDLE;
            }

        } else if (task instanceof AmodeusDriveTask) {
            if (task instanceof AmodeusRebalanceTask) {
                // This is a rebalaning task
                return TaxiTripType.REBALANCING;
            } else if (task instanceof AmodeusApproachTask) {
                // This is an  approach task
                return TaxiTripType.APPROACH;
            } else {
                // This is a regular Driving task with customer
                return TaxiTripType.OCCUPIED;
            }
        } else {
            throw new IllegalStateException("Unknown and unhandled Task in RoboTaxi Schedule");
        }
    }

    public static Link getEndLinkFromTask(Task task) {
        if(task instanceof StayTask){
            return ((StayTask) task).getLink();
        }
        else if (task instanceof AmodeusDriveTask){
            return ((AmodeusDriveTask) task).getPath().getToLink();
        }
        else{
            throw new IllegalStateException("Unknown and unhandled Task in RoboTaxi Schedule");
        }
    }

    public static Link getStatLinkFromTask(Task task) {
        if(task instanceof StayTask){
            return ((StayTask) task).getLink();
        }
        else if (task instanceof AmodeusDriveTask){
            return ((AmodeusDriveTask) task).getPath().getFromLink();
        }
        else{
            throw new IllegalStateException("Unknown and unhandled Task in RoboTaxi Schedule");
        }
    }

    public static double getDistanceFromTask(Task task) {

        if (task instanceof StayTask){
            return 0;
        }
        else if (task instanceof AmodeusDriveTask){
            double distance = 0;
            VrpPath path = ((AmodeusDriveTask) task).getPath();
            if (path instanceof DivertedVrpPath){
                for (int i=0; i < ((DivertedVrpPath) path).getDiversionLinkIdx(); i++){
                    distance+= ((DivertedVrpPath) path).getOriginalPath().getLink(i).getLength();
                }
                distance += StreamSupport.stream(((DivertedVrpPath) path).getNewSubPath().spliterator(), false).mapToDouble(l -> l.getLength()).sum();
            }
            else {
                distance= StreamSupport.stream(path.spliterator(), false).mapToDouble(l -> l.getLength()).sum();
            }
            return distance;
        }
        else{
            throw new IllegalStateException("Unknown and unhandled Task in RoboTaxi Schedule");
        }
    }

}