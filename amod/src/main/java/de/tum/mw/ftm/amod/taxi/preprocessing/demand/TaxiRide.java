package de.tum.mw.ftm.amod.taxi.preprocessing.demand;

import amodeus.amodeus.taxitrip.TaxiTrip;
import amodeus.amodeus.util.math.SI;
import ch.ethz.idsc.tensor.Tensors;
import ch.ethz.idsc.tensor.qty.Quantity;
import amodeus.amod.ext.UserReferenceFrames;
import amodeus.amodeus.dispatcher.core.RoboTaxiStatus;
import de.tum.mw.ftm.amod.analysis.AnalysisUtils;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.Point;
import org.locationtech.jts.geom.impl.CoordinateArraySequence;
import org.matsim.amodeus.config.FTMConfigGroup;
import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.network.Link;

import javax.persistence.*;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicInteger;

public class TaxiRide implements Comparable<TaxiRide> {

    private TaxiRideID taxiRideID;
    private LocalDateTime timestampStart;
    private LocalDateTime timestampStop;
    private Point locationStart;
    private Point locationStop;
    private Double distance;
    private String type;
    private Double costsDistance;
    private Double costsTime;
    private Double fareDistance;
    private Double fareWaiting;
    private Double fareBase;
    private Double avgSpeed;
    private static final ZoneId timezone = ZoneId.of("Europe/Berlin");
    private static final String TYPE_APPROACH = "approach";
    private static final String TYPE_OCCUPIED = "occupied";
    private static final String TYPE_REBALANCING = "rebalancing";
    private static final String TYPE_IDLE = "idle";
    private static final String TYPE_WAITING_AT_PICKUP = "waiting_at_pickup";



    private final Double overallCosts;
    private final Double overallFare;
    private final Integer tripId;
    private String requestId;
    private final Integer vehicleId;

    private static final AtomicInteger uniqueId = new AtomicInteger(0);

    public TaxiRide() {
        overallCosts = null;
        overallFare = null;
        tripId = null;
        requestId = null;
        vehicleId = null;
    }

    public TaxiRide(String requestId, int vehicleId, LocalDateTime timestampStart, LocalDateTime timestampStop,
                    RoboTaxiStatus roboTaxiStatus, Link startLink, Link endLink,
                    double distance, double costs, double fare) {
        this.tripId = uniqueId.getAndIncrement();
        this.requestId = requestId;
        this.vehicleId = vehicleId;
        this.timestampStart = timestampStart;
        this.timestampStop = timestampStop;
        this.type = roboTaxiStatus.getFTMStatus();

        GeometryFactory geometryFactory = new GeometryFactory();
        Coord WGS84Start = UserReferenceFrames.MUNICH.coords_toWGS84().transform(startLink.getCoord());
        Coord WGS84End = UserReferenceFrames.MUNICH.coords_toWGS84().transform(endLink.getCoord());
        Point startPoint = new Point(new CoordinateArraySequence(
                new Coordinate[]{new Coordinate(WGS84Start.getX(), WGS84Start.getY())}), geometryFactory);
        Point endPoint = new Point(new CoordinateArraySequence(
                new Coordinate[]{new Coordinate(WGS84End.getX(), WGS84End.getY())}), geometryFactory);
        this.locationStart = startPoint;
        this.locationStop = endPoint;
        this.distance = distance;
        this.overallCosts = costs;
        this.overallFare = fare;
    }

    public TaxiRide(String requestId, int vehicleId, LocalDateTime timestampStart, LocalDateTime timestampStop,
                    AnalysisUtils.TaxiTripType taxiTripType, Link startLink, Link endLink,
                    double distance, double costs, double fare) {
        this.tripId = uniqueId.getAndIncrement();
        this.requestId = requestId;
        this.vehicleId = vehicleId;
        this.timestampStart = timestampStart;
        this.timestampStop = timestampStop;
        this.type = taxiTripType.getDescription();

        GeometryFactory geometryFactory = new GeometryFactory();
        Coord WGS84Start = UserReferenceFrames.MUNICH.coords_toWGS84().transform(startLink.getCoord());
        Coord WGS84End = UserReferenceFrames.MUNICH.coords_toWGS84().transform(endLink.getCoord());
        Point startPoint = new Point(new CoordinateArraySequence(
                new Coordinate[]{new Coordinate(WGS84Start.getX(), WGS84Start.getY())}), geometryFactory);
        Point endPoint = new Point(new CoordinateArraySequence(
                new Coordinate[]{new Coordinate(WGS84End.getX(), WGS84End.getY())}), geometryFactory);
        this.locationStart = startPoint;
        this.locationStop = endPoint;
        this.distance = distance;
        this.overallCosts = costs;
        this.overallFare = fare;
    }


    public TaxiRideID getTaxiRideId() {
        return taxiRideID;
    }

    public void setTaxiRideId(TaxiRideID taxiRideID) {
        this.taxiRideID = taxiRideID;
    }

    public LocalDateTime getTimestampStart() {
        return timestampStart;
    }

    public void setTimestampStart(LocalDateTime timestampStart) {
        this.timestampStart = timestampStart;
    }

    public LocalDateTime getTimestampStop() {
        return timestampStop;
    }

    public void setTimestampStop(LocalDateTime timestampStop) {
        this.timestampStop = timestampStop;
    }


    public Point getLocationStart() {
        return locationStart;
    }

    public void setLocationStart(Point locationStart) {
        this.locationStart = locationStart;
    }

    public Point getLocationStop() {
        return locationStop;
    }

    public void setLocationStop(Point locationStop) {
        this.locationStop = locationStop;
    }


    public Double getDistance() {
        return distance;
    }

    public void setDistance(Double distance) {
        this.distance = distance;
    }


    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }


    public Double getCostsDistance() {
        return costsDistance;
    }

    public void setCostsDistance(Double costsDistance) {
        this.costsDistance = costsDistance;
    }


    public Double getCostsTime() {
        return costsTime;
    }

    public void setCostsTime(Double costsTime) {
        this.costsTime = costsTime;
    }


    public Double getFareDistance() {
        return fareDistance;
    }

    public void setFareDistance(Double fareDistance) {
        this.fareDistance = fareDistance;
    }


    public Double getFareWaiting() {
        return fareWaiting;
    }

    public void setFareWaiting(Double fareWaiting) {
        this.fareWaiting = fareWaiting;
    }


    public Double getFareBase() {
        return fareBase;
    }

    public void setFareBase(Double fareBase) {
        this.fareBase = fareBase;
    }


    public Double getAvgSpeed() {
        return avgSpeed;
    }

    public void setAvgSpeed(Double avgSpeed) {
        this.avgSpeed = avgSpeed;
    }


    public double getOverallCosts() {
        return overallCosts != null ? overallCosts : costsDistance + costsTime;
    }


    public Double getOverallFare() {
        return overallFare != null ? overallFare : fareBase + fareDistance + fareWaiting;
    }


    public Integer getTripId() {
        return tripId;
    }


    public void setRequestId(String requestId) {
        this.requestId = requestId;
    }


    public long getTripEndTimeInSimulation(LocalDateTime simulationStartDateTime) {
        return ChronoUnit.SECONDS.between(simulationStartDateTime, this.timestampStop);
    }


    public String getRequestId() {
        return requestId;
    }


    public Integer getVehicleId() {
        return vehicleId;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        TaxiRide that = (TaxiRide) o;
        return Objects.equals(taxiRideID, that.taxiRideID) &&
                Objects.equals(timestampStart, that.timestampStart) &&
                Objects.equals(timestampStop, that.timestampStop) &&
                Objects.equals(locationStart, that.locationStart) &&
                Objects.equals(locationStop, that.locationStop) &&
                Objects.equals(distance, that.distance) &&
                Objects.equals(type, that.type) &&
                Objects.equals(costsDistance, that.costsDistance) &&
                Objects.equals(costsTime, that.costsTime) &&
                Objects.equals(fareDistance, that.fareDistance) &&
                Objects.equals(fareWaiting, that.fareWaiting) &&
                Objects.equals(fareBase, that.fareBase) &&
                Objects.equals(avgSpeed, that.avgSpeed);
    }

    @Override
    public int hashCode() {
        return Objects.hash(taxiRideID, timestampStart, timestampStop, locationStart, locationStop, distance, type, costsDistance, costsTime, fareDistance, fareWaiting, fareBase, avgSpeed);
    }

    @Override
    public int compareTo(TaxiRide o) {
        return this.timestampStart.compareTo(o.timestampStart);
    }

    public TaxiTrip toTaxiTrip(FTMConfigGroup ftmConfigGroup) {
        return TaxiTrip.of(
                this.getTaxiRideId().getTrackId().toString(),
                null,
                Tensors.vector(this.locationStart.getX(), locationStart.getY()),
                Tensors.vector(this.locationStop.getX(), locationStop.getY()),
                Quantity.of(distance, SI.METER),
                null,
                this.timestampStart,
                this.timestampStop,
                ftmConfigGroup);
    }

}
