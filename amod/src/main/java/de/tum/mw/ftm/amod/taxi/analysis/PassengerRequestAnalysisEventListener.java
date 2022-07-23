package de.tum.mw.ftm.amod.taxi.analysis;

import amodeus.amod.ext.UserReferenceFrames;
import de.tum.mw.ftm.amod.analysis.events.passengerrequest.PassengerRequestAnalysisEvent;
import de.tum.mw.ftm.amod.analysis.events.passengerrequest.PassengerRequestAnalysisEventHandler;
import de.tum.mw.ftm.amod.analysis.events.passengerrequest.PassengerRequestInformation;
import org.apache.log4j.Logger;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.Point;
import org.locationtech.jts.geom.impl.CoordinateArraySequence;
import org.matsim.amodeus.config.AmodeusConfigGroup;
import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.population.Person;
import org.matsim.contrib.dvrp.fleet.DvrpVehicle;
import org.matsim.contrib.dvrp.passenger.PassengerRequest;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

public class PassengerRequestAnalysisEventListener implements PassengerRequestAnalysisEventHandler {
    private final static Logger logger = Logger.getLogger(PassengerRequestAnalysisEventListener.class);
    Map<Id<Person>, PassengerRequestInformation> passengerRequestInformationMap = new HashMap<>();
    private AmodeusConfigGroup amodeusConfigGroup;

    public PassengerRequestAnalysisEventListener(AmodeusConfigGroup amodeusConfigGroup) {
        this.amodeusConfigGroup = amodeusConfigGroup;
    }

    @Override
    public void handleEvent(PassengerRequestAnalysisEvent event) {
        PassengerRequest passengerRequest = event.getRelatedPassengerRequest();
        Id<DvrpVehicle> taxi = event.getRelatedVehicle();

        PassengerRequestInformation passengerRequestInformation;
        passengerRequestInformation = passengerRequestInformationMap.getOrDefault(passengerRequest.getPassengerId(),
                new PassengerRequestInformation(Long.valueOf(passengerRequest.getPassengerId().toString()),
                        passengerRequest.getSubmissionTime()));

        //        PassengerRequestInformation passengerRequestInformation = new PassengerRequestInformation(passengerRequest.toString());

        passengerRequestInformation.setStartLocation(linkToPoint(event.getRelatedPassengerRequest().getFromLink()));
        passengerRequestInformation.setStopLocation(linkToPoint(event.getRelatedPassengerRequest().getToLink()));

        switch (event.getType()){
            case PASSENGER_REQUEST_ASSIGNED:
                if(passengerRequestInformation.getAssignTime().isUndefined()) passengerRequestInformation.setAssignTime(event.getTimestamp());
                if(taxi != null) {
                    passengerRequestInformation.setAssociatedTaxiId(Integer.valueOf(taxi.toString().split("\\:")[2]));
                }
                else{
                    logger.warn("Taxi is null");
                }
                break;
            case PASSENGER_REQUEST_ARRIVED:
                passengerRequestInformation.setPickupTime(event.getTimestamp());
                double pickupduration = Math.max(amodeusConfigGroup.getMode("av").getTimingConfig().getMinimumPickupDurationPerStop(),
                        amodeusConfigGroup.getMode("av").getTimingConfig().getPickupDurationPerPassenger());
                passengerRequestInformation.setStartTime(event.getTimestamp()+ pickupduration +1);
                break;
            case PASSENGER_REQUEST_PICKUP:
                passengerRequestInformation.setStartTime(event.getTimestamp());
                break;
            case PASSENGER_REQUEST_DROPOFF:
                passengerRequestInformation.setDropoffTime(event.getTimestamp());
                break;
            case PASSENGER_REQUEST_CANCELLED:
                passengerRequestInformation.setCanceled(true);
                break;
            default:
                logger.warn("Unhandled PassengerRequestEventType");
        }
        passengerRequestInformationMap.put(passengerRequest.getPassengerId(), passengerRequestInformation);
    }

    public Collection<PassengerRequestInformation> getPassengerRequests() {
        return passengerRequestInformationMap.values();
    }

    //TODO: @michaelwittmann move this to an better place
    public static Point linkToPoint(Link link){
        GeometryFactory geometryFactory = new GeometryFactory();
        Coord WGS84Coords = UserReferenceFrames.MUNICH.coords_toWGS84().transform(link.getCoord());
        return new Point(new CoordinateArraySequence(
                new Coordinate[]{new Coordinate(WGS84Coords.getX(), WGS84Coords.getY())}), geometryFactory);
    }
}
