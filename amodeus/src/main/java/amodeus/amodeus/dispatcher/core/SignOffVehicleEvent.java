package amodeus.amodeus.dispatcher.core;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.events.Event;
import org.matsim.api.core.v01.events.HasLinkId;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.population.Person;
import org.matsim.core.api.internal.HasPersonId;

/* package */ class SignOffVehicleEvent extends Event implements HasPersonId, HasLinkId {
    public static final String ACTTYPE = "AVSignOff"; //TODO:check usage

    private final Id<Person> personId;
    private final Id<Link> linkId;

    public static SignOffVehicleEvent create(double time, RoboTaxi roboTaxi, Link link) {
        // get the id of the AV -related agent (driver) as id of vehicle not
        // possible to access directly
        return new SignOffVehicleEvent(time, Id.createPersonId(roboTaxi.getId()), link.getId());
    }

    // ---
    private SignOffVehicleEvent(double time, Id<Person> agentId, Id<Link> linkId) {
        super(time);

        this.personId = agentId;
        this.linkId = linkId;
    }

    @Override
    public String getEventType() {
        return ACTTYPE;
    }

    @Override
    public Id<Link> getLinkId() {
        return linkId;
    }

    @Override
    public Id<Person> getPersonId() {
        return personId;
    }
}

