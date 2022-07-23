package de.tum.mw.ftm.amod.taxi.preprocessing.zones;

import org.locationtech.jts.geom.Envelope;
import org.locationtech.jts.geom.Point;
import org.locationtech.jts.index.SpatialIndex;
import org.locationtech.jts.index.quadtree.Quadtree;
import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.contrib.zone.Zone;
import org.matsim.contrib.zone.util.ZoneFinder;
import org.matsim.core.utils.geometry.geotools.MGC;

import java.util.List;
import java.util.Map;

public class DispatchingZoneFinderImpl implements ZoneFinder {
    private final SpatialIndex quadTree = new Quadtree();
    private final double expansionDistance;

    public DispatchingZoneFinderImpl(Map<Id<Zone>, DispatchingZone> zones, double expansionDistance) {
        this.expansionDistance = expansionDistance;

        for (Zone z : zones.values()) {
            quadTree.insert(z.getMultiPolygon().getEnvelopeInternal(), z);
        }
    }

    @Override
    @SuppressWarnings("unchecked")
    public Zone findZone(Coord coord) {
        Point point = MGC.coord2Point(coord);
        Envelope env = point.getEnvelopeInternal();

        DispatchingZone zone = getSmallestZoneContainingPoint(quadTree.query(env), point);
        if (zone != null) {
            return zone;
        }

        if (expansionDistance > 0) {
            env.expandBy(expansionDistance);
            zone = getNearestZone(quadTree.query(env), point);
        }

        return zone;
    }

    private DispatchingZone getSmallestZoneContainingPoint(List<DispatchingZone> zones, Point point) {
        if (zones.size() == 1) {// almost 100% cases
            return zones.get(0);
        }

        double minArea = Double.MAX_VALUE;
        DispatchingZone smallestZone = null;

        for (DispatchingZone z : zones) {
            if (z.getMultiPolygon().contains(point)) {
                double area = z.getMultiPolygon().getArea();
                if (area < minArea) {
                    minArea = area;
                    smallestZone = z;
                }
            }
        }

        return smallestZone;
    }

    private DispatchingZone getNearestZone(List<DispatchingZone> zones, Point point) {
        if (zones.size() == 1) {
            return zones.get(0);
        }

        double minDistance = Double.MAX_VALUE;
        DispatchingZone nearestZone = null;

        for (DispatchingZone z : zones) {
            double distance = z.getMultiPolygon().distance(point);
            if (distance <= expansionDistance) {
                if (distance < minDistance) {
                    minDistance = distance;
                    nearestZone = z;
                }
            }
        }

        return nearestZone;
    }

}
