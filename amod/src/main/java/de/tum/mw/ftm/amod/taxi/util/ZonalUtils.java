package de.tum.mw.ftm.amod.taxi.util;

import de.tum.mw.ftm.amod.taxi.preprocessing.demand.TaxiRide;
import de.tum.mw.ftm.amod.taxi.preprocessing.zones.DispatchingZone;
import de.tum.mw.ftm.amod.taxi.preprocessing.zones.DispatchingZones;
import org.apache.log4j.Logger;
import org.locationtech.jts.algorithm.Distance;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.MultiPolygon;
import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.contrib.zone.Zone;
import org.matsim.core.network.NetworkUtils;
import org.matsim.core.utils.geometry.CoordinateTransformation;
import org.matsim.core.utils.geometry.transformations.TransformationFactory;

import java.util.*;

public class ZonalUtils {
    private Network network;
    private final Map<Id<Link>, Zone> linkToZone;
    private final DispatchingZones dispatchingZones;
    private static final int MAXIMUM_NO_OF_NEIGHBOURS = 50;

    private final static Logger logger = Logger.getLogger(ZonalUtils.class);
    private static final CoordinateTransformation WGS84_UTM32N = TransformationFactory.getCoordinateTransformation("EPSG:4326", "EPSG:25832");

    public ZonalUtils(Network network, DispatchingZones dispatchingZones, Map<Id<Link>, Zone> linkToZone) {
        this.network = network;
        this.linkToZone = linkToZone;
        this.dispatchingZones = dispatchingZones;
//        setAdjacentZones(dispatchingZones);
    }


    public Id<Zone> getStartZone(TaxiRide ride) {
        Coord startCoord = WGS84_UTM32N.transform(new Coord(ride.getLocationStart().getX(), ride.getLocationStart().getY()));
        Link startLink = NetworkUtils.getNearestLink(network, startCoord);
        Zone startZone = linkToZone.get(startLink.getId());
        if (startZone != null) {
            return startZone.getId();
        }
        return null;
    }

    public static Comparator<Id<Zone>> zoneComparator() {
        return (o1, o2) -> {
            int id1 = Integer.parseInt(o1.toString());
            int id2 = Integer.parseInt(o2.toString());
            return Integer.compare(id1, id2);
        };
    }

    public DispatchingZone getNearestZoneToLink(Link link) {
        DispatchingZone nearestZone = null;
        double distanceToNearestZone = Double.POSITIVE_INFINITY;
        for (DispatchingZone zone : dispatchingZones.values()) {
            MultiPolygon zoneMultiPolygon = zone.getMultiPolygon();
            double distanceToZone = getlinkToZoneDistance(link, zoneMultiPolygon);
            if (distanceToZone < distanceToNearestZone) {
                distanceToNearestZone = distanceToZone;
                nearestZone = zone;
            }
        }
        return nearestZone;
    }

    public Optional<DispatchingZone> getContainingZone(Link link) {
        for (DispatchingZone zone : dispatchingZones.values()) {
            MultiPolygon zoneMultiPolygon = zone.getMultiPolygon();
            if (zoneMultiPolygon.contains(AmodeusUtil.matsimCoordToPoint(link.getCoord()))) {
                return Optional.of(zone);
            }
        }

        return Optional.empty();
    }

    public double getlinkToZoneDistance(Link link, MultiPolygon multiPolygon){
        Coordinate coordinate = new Coordinate(link.getCoord().getX(), link.getCoord().getY());
        Coordinate[] line = multiPolygon.getCoordinates();
        return Distance.pointToSegmentString(coordinate, line);
    }

    public Map<Id<Zone>, DispatchingZone> getZones() {
        return dispatchingZones;
    }

    public void removeAirportZone(){
        dispatchingZones.remove(Id.create(9999, Zone.class));
    }
}
