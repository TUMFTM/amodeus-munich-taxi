package de.tum.mw.ftm.amod.taxi.analysis;

import de.tum.mw.ftm.amod.analysis.AnalysisUtils;
import de.tum.mw.ftm.amod.analysis.events.fleetstatus.FleetStatusLogEvent;
import de.tum.mw.ftm.amod.analysis.events.fleetstatus.FleetStatusLogEventHandler;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

public class FleetStatusLogEventListener implements FleetStatusLogEventHandler {
    private TreeMap<Double, Map<AnalysisUtils.TaxiTripType, Long>> fleetStatusMap = new TreeMap<>();
    private Long fleetSize;

    @Override
    public void handleEvent(FleetStatusLogEvent event) {
        fleetStatusMap.put(event.getTime(), event.getFleetStatusInformation());
        if(this.fleetSize == null){
            this.fleetSize = event.getFleetStatusInformation().getFleetSize();
        }
        else{
            if (fleetSize != event.getFleetStatusInformation().getFleetSize()){
                throw new IllegalStateException("FleetSize changed during simulation");
            }
        }
    }

    public Long getFleetSize() {
        return fleetSize;
    }



    public TreeMap<Double, Map<AnalysisUtils.TaxiTripType, Long>> getFleetStatusMap() {
        return fleetStatusMap;
    }
}
