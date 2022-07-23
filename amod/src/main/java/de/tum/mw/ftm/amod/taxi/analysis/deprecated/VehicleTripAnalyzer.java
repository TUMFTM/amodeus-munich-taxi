package de.tum.mw.ftm.amod.taxi.analysis.deprecated;

import amodeus.amodeus.dispatcher.core.LinkStatusPair;
import amodeus.amodeus.dispatcher.core.RoboTaxiStatus;
import amodeus.amodeus.net.MatsimAmodeusDatabase;
import amodeus.amodeus.net.VehicleContainer;
import amodeus.amodeus.util.math.GlobalAssert;
import de.tum.mw.ftm.amod.taxi.preprocessing.demand.TaxiRide;
import org.matsim.amodeus.config.FTMConfigGroup;
import org.matsim.amodeus.config.FTMRevenueConfig;
import org.matsim.api.core.v01.network.Link;
import org.matsim.core.utils.misc.OptionalTime;

import java.time.temporal.ChronoUnit;
import java.util.*;

public class VehicleTripAnalyzer {
    private final MatsimAmodeusDatabase amodeusDatabase;
    private final List<TaxiRide> taxiRides = new ArrayList<>();
    private final int publishPeriod;
    private final int taxiId;
    private LinkStatusPair startPair;
    private LinkStatusPair latestPair;
    private long tripStartTime;
    private long tripLatestTime;
    private final long ftmSimEndTime;
    private final long simEndTimeBufferSeconds;
    private final LinkedList<Link> tripTrace;
    private final FTMConfigGroup ftmConfigGroup;
    private final int simulationObjectCount;
    private int registeredObjectsCount;

    public VehicleTripAnalyzer(MatsimAmodeusDatabase amodeusDatabase, FTMConfigGroup ftmConfigGroup, int publishPeriod, int taxiId, int simulationObjectCount) {
        this.amodeusDatabase = amodeusDatabase;
        this.publishPeriod = publishPeriod;
        this.taxiId = taxiId;
        this.startPair = null;
        this.latestPair = null;
        this.ftmSimEndTime = ChronoUnit.SECONDS.between(ftmConfigGroup.getSimStartDateTime(),
                ftmConfigGroup.getSimEndDateTime());
        this.simEndTimeBufferSeconds = ftmConfigGroup.getSimEndTimeBufferSeconds();
        this.tripTrace = new LinkedList<>();
        this.ftmConfigGroup = ftmConfigGroup;
        this.simulationObjectCount = simulationObjectCount;
        this.registeredObjectsCount = 0;
    }

    public void register(VehicleContainer vehicleContainer, long now) {
        GlobalAssert.that(now <= ftmSimEndTime + simEndTimeBufferSeconds);

        registeredObjectsCount++;

        if (startPair == null) {
            startPair = new LinkStatusPair(amodeusDatabase.getOsmLink(vehicleContainer.linkTrace[0]).link,
                    vehicleContainer.statii[0]);
            tripStartTime = now;
            latestPair = new LinkStatusPair(amodeusDatabase.getOsmLink(vehicleContainer.linkTrace[0]).link,
                    vehicleContainer.statii[0]);
            tripLatestTime = now;
            tripTrace.addLast(startPair.link);

            for (int i = 1; i < vehicleContainer.statii.length; i++) {
                checkForStatusChange(vehicleContainer, now, i);
                latestPair = new LinkStatusPair(amodeusDatabase.getOsmLink(vehicleContainer.linkTrace[i]).link,
                        vehicleContainer.statii[i]);
                tripLatestTime = now;
            }
        } else {
            for (int i = 0; i < vehicleContainer.statii.length; i++) {
                checkForStatusChange(vehicleContainer, now, i);
                latestPair = new LinkStatusPair(amodeusDatabase.getOsmLink(vehicleContainer.linkTrace[i]).link,
                        vehicleContainer.statii[i]);
                tripLatestTime = now;
            }
        }

        if (registeredObjectsCount == simulationObjectCount && tripStartTime != publishPeriod * simulationObjectCount) {
            if (tripStartTime < ftmSimEndTime &&
                    (startPair.roboTaxiStatus == RoboTaxiStatus.STAY ||
                            startPair.roboTaxiStatus == RoboTaxiStatus.OFFSERVICE ||
                            startPair.roboTaxiStatus == RoboTaxiStatus.REBALANCEDRIVE)) {
                GlobalAssert.that(startPair.roboTaxiStatus == latestPair.roboTaxiStatus);
                double costs = calculateCosts(0, tripStartTime, ftmSimEndTime, startPair.roboTaxiStatus);
                TaxiRide taxiRide = new TaxiRide("null", taxiId,
                        ftmConfigGroup.getSimStartDateTime().plusSeconds(tripStartTime - publishPeriod),
                        ftmConfigGroup.getSimEndDateTime(), startPair.roboTaxiStatus,
                        startPair.link, startPair.link, 0, costs, 0);
                taxiRides.add(taxiRide);
                tripTrace.clear();
            } else if (startPair.roboTaxiStatus == RoboTaxiStatus.DRIVETOCUSTOMER ||
                    startPair.roboTaxiStatus == RoboTaxiStatus.DRIVEWITHCUSTOMER) {
                GlobalAssert.that(startPair.roboTaxiStatus == latestPair.roboTaxiStatus);
                double distance = calculateTripDistance();
                double revenue = calculateRevenue(distance, startPair.roboTaxiStatus);
                double costs = calculateCosts(distance, tripStartTime, tripLatestTime, startPair.roboTaxiStatus);
                TaxiRide taxiRide = new TaxiRide("null", taxiId,
                        ftmConfigGroup.getSimStartDateTime().plusSeconds(tripStartTime - publishPeriod),
                        ftmConfigGroup.getSimStartDateTime().plusSeconds(publishPeriod * simulationObjectCount),
                        latestPair.roboTaxiStatus, startPair.link,
                        amodeusDatabase.getOsmLink(vehicleContainer.linkTrace[vehicleContainer.linkTrace.length - 1]).link,
                        distance, costs, revenue);
                if (now == ftmSimEndTime + simEndTimeBufferSeconds) {
                    System.err.println(String.format("The trip with id %d was finished at the end of the simulation." +
                            " It will be added to the export but it should be inspected", taxiRide.getTripId()));
                }
                taxiRides.add(taxiRide);
                tripTrace.clear();
            }
        }
    }

    private void checkForStatusChange(VehicleContainer vehicleContainer, long now, int index) {
        checkForLinkChange(vehicleContainer, index);

        if (vehicleContainer.statii[index] != startPair.roboTaxiStatus) {
            GlobalAssert.that(startPair.roboTaxiStatus == latestPair.roboTaxiStatus);
            double distance = calculateTripDistance();
            double revenue = calculateRevenue(distance, startPair.roboTaxiStatus);
            double costs = calculateCosts(distance, tripStartTime, tripLatestTime, startPair.roboTaxiStatus);
            TaxiRide taxiRide = new TaxiRide("null", taxiId,
                    ftmConfigGroup.getSimStartDateTime().plusSeconds(tripStartTime - publishPeriod),
                    ftmConfigGroup.getSimStartDateTime().plusSeconds(tripLatestTime - publishPeriod),
                    latestPair.roboTaxiStatus, startPair.link, latestPair.link, distance, costs, revenue);
            taxiRides.add(taxiRide);
            tripTrace.clear();

            Link newStripStart = amodeusDatabase.getOsmLink(vehicleContainer.linkTrace[index]).link;
            startPair = new LinkStatusPair(newStripStart, vehicleContainer.statii[index]);
            tripStartTime = now;
            tripTrace.addLast(newStripStart);
        }
    }

    private void checkForLinkChange(VehicleContainer vehicleContainer, int index) {
        Link currentLink = amodeusDatabase.getOsmLink(vehicleContainer.linkTrace[index]).link;
        if (currentLink != tripTrace.getLast()) {
            tripTrace.addLast(currentLink);
        }
    }


    public void consolidate(Map<Integer, PassengerRequestInformation> requestInformationMap) {
            for (TaxiRide taxiRide : taxiRides) {
                String requestId = "null";
                taxiRide.setRequestId(requestId);
            }
    }

    private int calculateRequestId(RoboTaxiStatus roboTaxiStatus, long now, Map<Integer,
            PassengerRequestInformation> requestInformationMap) {
        if (roboTaxiStatus != RoboTaxiStatus.DRIVEWITHCUSTOMER && roboTaxiStatus != RoboTaxiStatus.DRIVETOCUSTOMER) {
            return -1;
        }

        if (roboTaxiStatus == RoboTaxiStatus.DRIVETOCUSTOMER) {
            for (Map.Entry<Integer, PassengerRequestInformation> requestInformationEntry : requestInformationMap.entrySet()) {
                if (checkIfValueIsBetweenBoundsWithPublishPeriodTolerance(now,
                        requestInformationEntry.getValue().getAssignTime(),
                        requestInformationEntry.getValue().getPickupTime())) {
                    return requestInformationEntry.getKey();
                }
            }
        }

        if (roboTaxiStatus == RoboTaxiStatus.DRIVEWITHCUSTOMER) {
            for (Map.Entry<Integer, PassengerRequestInformation> requestInformationEntry : requestInformationMap.entrySet()) {
                if (requestInformationEntry.getValue().getPickupTime().isDefined()) {
                    if (checkIfValueIsBetweenBoundsWithPublishPeriodTolerance(now,
                            requestInformationEntry.getValue().getPickupTime(),
                            requestInformationEntry.getValue().getDropoffTime())) {
                        return requestInformationEntry.getKey();
                    }
                }
            }
        }

        return -1;
    }

    private double calculateTripDistance() {
        double distance = 0;
        if (tripTrace.size() > 1) {
            distance = tripTrace.subList(1, tripTrace.size() - 1).stream().mapToDouble(Link::getLength).sum();
        }
        return distance;
    }


    private double calculateRevenue(double distance, RoboTaxiStatus status) {
        if (status == RoboTaxiStatus.DRIVEWITHCUSTOMER) {
            FTMRevenueConfig ftmRevenueConfig = ftmConfigGroup.getFtmRevenueConfig();
            if (distance <= 5000) {
                double metersPer20Cent = 1000 * 0.2 / ftmRevenueConfig.getRevenuePerKmBelow5Km();
                return Math.floor(distance/ metersPer20Cent) * 0.2 + ftmRevenueConfig.getRevenuePerTrip();
            } else if (distance <= 10000) {
                double metersPer20Cent = 1000 * 0.2 / ftmRevenueConfig.getRevenuePerKmBelow10Km();
                return 10 + Math.floor((distance - 5000) / metersPer20Cent) * 0.2 + ftmRevenueConfig.getRevenuePerTrip();
            } else {
                double metersPer20Cent = 1000 * 0.2 / ftmRevenueConfig.getRevenuePerKmAbove10Km();
                return 19 + Math.floor((distance - 10000) / metersPer20Cent) * 0.2 + ftmRevenueConfig.getRevenuePerTrip();
            }
        } else {
            return 0;
        }
    }

    private double calculateCosts(double distance, double tripStartTime, double tripEndTime, RoboTaxiStatus status) {
        GlobalAssert.that(tripEndTime >= tripStartTime);
        if (status == RoboTaxiStatus.OFFSERVICE) {
            return 0;
        } else {
            return ftmConfigGroup.getFtmCostsConfig().getCostsPerKm() * distance / 1000
                    + ftmConfigGroup.getFtmCostsConfig().getCostsPerHour() * (tripEndTime - tripStartTime) / 3600;
        }
    }

    private boolean checkIfValueIsBetweenBoundsWithPublishPeriodTolerance(long value, OptionalTime lowerBound, OptionalTime upperBound) {
        return value >= lowerBound.orElse(0) - publishPeriod && (value <= upperBound.orElse(value) + publishPeriod);
    }

//    private Integer getRelatedRequestId(String status, long time, Map<Integer, RequestInformation> requestInformationMap){
//        switch (status){
//            case TaxiTripType.APPROACH.getDescription():
//                TreeMap<Long, Integer> assignedRequests = requestInformationMap.entrySet().stream()
//                        .filter(entry -> entry.getValue().getAssignTime().isDefined())
//                        .collect(Collectors.toMap(entry -> (long)entry.getValue().getAssignTime().orElse(Long.MAX_VALUE), // This never happens, due to filter above, but ensures long type
//                                Map.Entry::getKey,
//                                (oldValue, newValue) -> newValue,
//                                TreeMap::new));
//                return assignedRequests.floorEntry(time + publishPeriod).getValue();
//            case TaxiTripType.OCCUPIED.getDescription():
//                TreeMap<Long, Integer> pickedUpRequests = requestInformationMap.entrySet().stream()
//                        .filter(entry -> entry.getValue().getPickupTime().isDefined())
//                        .collect(Collectors.toMap(entry -> (long)entry.getValue().getPickupTime().orElse(Long.MAX_VALUE), // This never happens, due to filter above, but ensures long type
//                                Map.Entry::getKey,
//                                (oldValue, newValue) -> newValue,
//                                TreeMap::new));
//                return pickedUpRequests.floorEntry(time + publishPeriod).getValue();
//            default:
//                return -1;
//        }
//    }

    public int getTaxiId() {
        return taxiId;
    }

    public List<TaxiRide> getTaxiRides() {
        return taxiRides;
    }
}
