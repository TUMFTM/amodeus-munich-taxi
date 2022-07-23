package org.matsim.amodeus.components.dispatcher.multi_od_heuristic.aggregation;

import java.util.Map;

import org.matsim.api.core.v01.events.Event;
import org.matsim.contrib.dvrp.passenger.PassengerRequest;

public class AggregationEvent extends Event {
    final private PassengerRequest master;
    final private PassengerRequest slave;

    public AggregationEvent(PassengerRequest master, PassengerRequest slave, double time) {
        super(time);

        this.master = master;
        this.slave = slave;
    }

    @Override
    public String getEventType() {
        return "ODRSAggregation";
    }

    @Override
    public Map<String, String> getAttributes() {
        Map<String, String> attr = super.getAttributes();
        attr.put("master", master.getPassengerId().toString());
        attr.put("slave", slave.getPassengerId().toString());
        return attr;
    }
}
