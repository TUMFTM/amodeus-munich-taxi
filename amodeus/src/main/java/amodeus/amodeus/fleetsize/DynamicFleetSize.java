package amodeus.amodeus.fleetsize;

import org.matsim.core.api.internal.MatsimFactory;
import org.matsim.core.api.internal.MatsimToplevelContainer;

import java.util.TreeMap;

public class DynamicFleetSize extends TreeMap<Long, Long> {


    public DynamicFleetSize(TreeMap<Long, Long> dynamicFleetSize) {
        super(dynamicFleetSize);
    }

    public DynamicFleetSize() {

    }

    public long getTargetFleetSize(long simulationTime) {
        if (!this.isEmpty()) {
            return this.floorEntry(simulationTime).getValue();
        } else {
            return 0;
        }
    }

    @Override
    public Long put(Long simulationTime, Long targetFleetSize) {
        return super.put(simulationTime, targetFleetSize);
    }

}

