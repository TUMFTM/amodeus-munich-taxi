package de.tum.mw.ftm.amod.taxi.preprocessing.zones;

import org.matsim.api.core.v01.Id;
import org.matsim.contrib.zone.Zone;

import java.util.Comparator;
import java.util.TreeMap;

public class DispatchingZones extends TreeMap<Id<Zone>, DispatchingZone> {

    public DispatchingZones() {
    }

    public DispatchingZones(Comparator<? super Id<Zone>> comparator) {
        super(comparator);
    }


}
