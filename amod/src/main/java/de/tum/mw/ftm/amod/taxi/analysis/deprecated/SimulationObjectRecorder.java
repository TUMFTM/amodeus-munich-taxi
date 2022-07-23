package de.tum.mw.ftm.amod.taxi.analysis.deprecated;

import amodeus.amod.ext.UserReferenceFrames;
import amodeus.amodeus.analysis.element.AnalysisElement;
import amodeus.amodeus.dispatcher.core.RequestStatus;
import amodeus.amodeus.net.MatsimAmodeusDatabase;
import amodeus.amodeus.net.RequestContainer;
import amodeus.amodeus.net.SimulationObject;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.Point;
import org.locationtech.jts.geom.impl.CoordinateArraySequence;
import org.matsim.amodeus.config.AmodeusConfigGroup;
import org.matsim.amodeus.config.FTMConfigGroup;
import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.network.Link;
import org.matsim.core.config.Config;
import org.matsim.core.utils.misc.OptionalTime;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

public class SimulationObjectRecorder implements AnalysisElement {
    private final Map<Integer, VehicleTripAnalyzer> tripAnalyzerMap;
    private final Map<Integer, PassengerRequestInformation> requestInformationMap = new HashMap<>();
    private final MatsimAmodeusDatabase amodeusDatabase;
    private final GeometryFactory geometryFactory;

    public SimulationObjectRecorder(Set<Integer> vehicleIndices, MatsimAmodeusDatabase amodeusDatabase, Config config, int simulationObjectCount) {
        FTMConfigGroup ftmConfigGroup = (FTMConfigGroup) config.getModules().get("ftm_simulation");
        AmodeusConfigGroup amodeusConfigGroup = (AmodeusConfigGroup) config.getModules().get("amodeus");
        int publishPeriod = amodeusConfigGroup.getModes().get("av").getDispatcherConfig().getPublishPeriod();
        tripAnalyzerMap = vehicleIndices.stream().collect(Collectors.toMap(Function.identity(),
                id -> new VehicleTripAnalyzer(amodeusDatabase, ftmConfigGroup, publishPeriod, id, simulationObjectCount)));
        this.amodeusDatabase = amodeusDatabase;
        this.geometryFactory = new GeometryFactory();
    }

    @Override
    public void register(SimulationObject simulationObject) {
        updateRequestTimeInformation(simulationObject.requests, simulationObject.now);

        simulationObject.vehicles.parallelStream().forEach(vehicleContainer ->
                tripAnalyzerMap.get(vehicleContainer.vehicleIndex).register(vehicleContainer, simulationObject.now));
    }

    @Override
    public void consolidate() {

        tripAnalyzerMap.values().forEach(vehicleTripAnalyzer -> {
            Map<Integer, PassengerRequestInformation> requestTimeInformationMap = requestInformationMap
                    .entrySet().stream().filter(entry ->
                            entry.getValue().getAssociatedTaxiId() ==
                                    vehicleTripAnalyzer.getTaxiId()).collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));

            if (!requestTimeInformationMap.isEmpty()) vehicleTripAnalyzer.consolidate(requestTimeInformationMap);
        });
    }

    private void updateRequestTimeInformation(List<RequestContainer> requestContainers, long now) {
        for (RequestContainer requestContainer : requestContainers) {
            PassengerRequestInformation passengerRequestInformation;

            if (requestContainer.requestStatus.contains(RequestStatus.REQUESTED)) {
                if (requestInformationMap.containsKey(requestContainer.passengerId)) {
                    passengerRequestInformation = requestInformationMap.get(requestContainer.passengerId);
//                    if (requestInformation.getPickupTime().isDefined() || requestInformation.getDropoffTime().isDefined()) {
//                        System.err.printf("Passenger with id %d exists more than once. Check the corresponding trips%n",
//                                requestContainer.passengerId);
//                    }
                } else {
                    passengerRequestInformation = createRequestInformation(requestContainer);
                    passengerRequestInformation.setSubmissionTime(OptionalTime.defined(now));
                    requestInformationMap.put(requestContainer.passengerId, passengerRequestInformation);
                }
            }

            if (requestContainer.requestStatus.contains(RequestStatus.ASSIGNED)) {
                if (!requestInformationMap.containsKey(requestContainer.passengerId)) {
                    System.err.printf("The trip for passenger with id %d was assigned before it was requested" +
                            " Check the corresponding trip%n", requestContainer.passengerId);
                    passengerRequestInformation = createRequestInformation(requestContainer);
                    passengerRequestInformation.setAssignTime(OptionalTime.defined(now));
                    requestInformationMap.put(requestContainer.passengerId, passengerRequestInformation);
                } else {
                    passengerRequestInformation = requestInformationMap.get(requestContainer.passengerId);
                    passengerRequestInformation.setAssociatedTaxiId(requestContainer.associatedVehicle);
                    if (passengerRequestInformation.getAssignTime().isUndefined()) {
                        passengerRequestInformation.setAssignTime(OptionalTime.defined(now));
                        requestInformationMap.put(requestContainer.passengerId, passengerRequestInformation);
                    }
                }
            }

            if (requestContainer.requestStatus.contains(RequestStatus.PICKUPDRIVE)) {
                if (!requestInformationMap.containsKey(requestContainer.passengerId)) {
                    System.err.printf("The passenger with id %d was pickuped before the trip was assigned." +
                            " Check the corresponding trip%n", requestContainer.passengerId);
                    passengerRequestInformation = createRequestInformation(requestContainer);
                    passengerRequestInformation.setPickupTime(OptionalTime.defined(now));
                    requestInformationMap.put(requestContainer.passengerId, passengerRequestInformation);
                } else {
                    passengerRequestInformation = requestInformationMap.get(requestContainer.passengerId);

                        passengerRequestInformation.setPickupTime(OptionalTime.defined(now));
                        requestInformationMap.put(requestContainer.passengerId, passengerRequestInformation);

                }
            }

            if (requestContainer.requestStatus.contains(RequestStatus.DRIVING)) {
                if (!requestInformationMap.containsKey(requestContainer.passengerId)) {
                    System.err.printf("The passenger with id %d started driving before being the trip was assigned " +
                            "or he was picked up. Check the corresponding trip%n", requestContainer.passengerId);
                    passengerRequestInformation = createRequestInformation(requestContainer);
                    passengerRequestInformation.setStartTime(OptionalTime.defined(now));
                    requestInformationMap.put(requestContainer.passengerId, passengerRequestInformation);
                } else {
                    passengerRequestInformation = requestInformationMap.get(requestContainer.passengerId);
                    if (passengerRequestInformation.getStartTime().isUndefined()) {
                        passengerRequestInformation.setStartTime(OptionalTime.defined(now));
                        requestInformationMap.put(requestContainer.passengerId, passengerRequestInformation);
                    }
                }
            }

            if (requestContainer.requestStatus.contains(RequestStatus.DROPOFF)) {
                if (!requestInformationMap.containsKey(requestContainer.passengerId)) {
                    System.err.printf("The passenger with id %d was dropoffed before the trip was " +
                            "assigned or started. Check the corresponding trip%n", requestContainer.passengerId);
                    passengerRequestInformation = createRequestInformation(requestContainer);
                    passengerRequestInformation.setDropoffTime(OptionalTime.defined(now));
                    requestInformationMap.put(requestContainer.passengerId, passengerRequestInformation);
                } else {
                    passengerRequestInformation = requestInformationMap.get(requestContainer.passengerId);
                    if (passengerRequestInformation.getDropoffTime().isUndefined()) {
                        passengerRequestInformation.setDropoffTime(OptionalTime.defined(now));
                        requestInformationMap.put(requestContainer.passengerId, passengerRequestInformation);
                    }
                }
            }

            if (requestContainer.requestStatus.contains(RequestStatus.CANCELLED)) {
                if (!requestInformationMap.containsKey(requestContainer.passengerId)) {
                    System.err.printf("The passenger with id %d was cancelled before the trip was " +
                            "requested. Check the corresponding trip%n", requestContainer.passengerId);
                    passengerRequestInformation = createRequestInformation(requestContainer);
                    passengerRequestInformation.setCanceled(true);
                    requestInformationMap.put(requestContainer.passengerId, passengerRequestInformation);
                } else {
                    passengerRequestInformation = requestInformationMap.get(requestContainer.passengerId);
                    passengerRequestInformation.setCanceled(true);
                }
            }
        }
    }

    private PassengerRequestInformation createRequestInformation(RequestContainer requestContainer) {
        Link startLink = amodeusDatabase.getOsmLink(requestContainer.fromLinkIndex).link;
        Link stopLink = amodeusDatabase.getOsmLink(requestContainer.toLinkIndex).link;
        Coord WGS84Start = UserReferenceFrames.MUNICH.coords_toWGS84().transform(startLink.getCoord());
        Coord WGS84End = UserReferenceFrames.MUNICH.coords_toWGS84().transform(stopLink.getCoord());
        Point startPoint = new Point(new CoordinateArraySequence(
                new Coordinate[]{new Coordinate(WGS84Start.getX(), WGS84Start.getY())}), geometryFactory);
        Point stopPoint = new Point(new CoordinateArraySequence(
                new Coordinate[]{new Coordinate(WGS84End.getX(), WGS84End.getY())}), geometryFactory);
        return new PassengerRequestInformation(requestContainer.associatedVehicle, startPoint, stopPoint);
    }

    public Map<Integer, VehicleTripAnalyzer> getTripAnalyzerMap() {
        return tripAnalyzerMap;
    }

    public Map<Integer, PassengerRequestInformation> getRequestInformationMap() {
        return requestInformationMap;
    }
}
