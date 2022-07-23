package de.tum.mw.ftm.amod.taxi.preprocessing.zones;

import org.locationtech.jts.geom.MultiPolygon;
import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.contrib.zone.Zone;

import java.util.ArrayList;
import java.util.List;

public class DispatchingZone extends Zone {
    private List<Id<Zone>> adjacentZones = new ArrayList<>();

    public DispatchingZone(Id<Zone> id, String type) {
        super(id, type);
    }

    public DispatchingZone(Id<Zone> id, String type, Coord centroid) {
        super(id, type, centroid);
    }

    public DispatchingZone(Id<Zone> id, String type, MultiPolygon multiPolygon) {
        super(id, type, multiPolygon);
    }

    public DispatchingZone(Id<Zone> id, String type, MultiPolygon multiPolygon, List<Id<Zone>>adjacentZones) {
        super(id, type, multiPolygon);
        this.adjacentZones = adjacentZones;
    }

    public List<Id<Zone>> getAdjacentZones() {
        return adjacentZones;
    }

    public void setAdjacentZones(List<Id<Zone>> adjacentZones) {
        this.adjacentZones = adjacentZones;
    }

    public boolean addAdjacentZones(Id<Zone> adjacentZone){
        return this.adjacentZones.add(adjacentZone);
    }
}
