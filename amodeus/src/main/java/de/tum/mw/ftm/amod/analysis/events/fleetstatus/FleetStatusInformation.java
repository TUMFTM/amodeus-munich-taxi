package de.tum.mw.ftm.amod.analysis.events.fleetstatus;

import amodeus.amodeus.dispatcher.core.RoboTaxi;
import de.tum.mw.ftm.amod.analysis.AnalysisUtils;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;

public class FleetStatusInformation extends HashMap<AnalysisUtils.TaxiTripType, Long> {
    private double time ;
    private long fleetSize;

    public FleetStatusInformation(double time) {
        this.time = time;
    }

    public FleetStatusInformation(double time, Collection<RoboTaxi> roboTaxis){
        this(time);
        Map<AnalysisUtils.TaxiTripType, Long> counts = roboTaxis.stream()
                .collect(Collectors.groupingBy(r -> AnalysisUtils.getTaxiTripTypeFromTask(r.getSchedule().getCurrentTask()),
                        Collectors.counting()));
       fleetSize = roboTaxis.size();
       this.putAll(counts);
    }

    public double getTime() {
        return time;
    }

    public long getFleetSize() {
        return fleetSize;
    }
}
