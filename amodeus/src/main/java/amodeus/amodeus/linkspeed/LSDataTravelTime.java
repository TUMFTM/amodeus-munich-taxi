/* amodeus - Copyright (c) 2018, ETH Zurich, Institute for Dynamic Systems and Control */
package amodeus.amodeus.linkspeed;

import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.population.Person;
import org.matsim.core.router.util.TravelTime;
import org.matsim.vehicles.Vehicle;

public class LSDataTravelTime implements TravelTime {
    private final LinkSpeedDataContainer lsData;


    // TODO @clruch see if can be converted into 1 class together with
    // amodeus.amodeus.linkspeed.AmodeusLinkSpeedCalculator
    public LSDataTravelTime(LinkSpeedDataContainer lsData) {
        this.lsData = lsData;
    }


    @Override
    public double getLinkTravelTime(Link link, double time, Person person, Vehicle vehicle) {
        double speed = LinkSpeedUtils.getLinkSpeedForTime(lsData, link, time);
        return link.getLength() / speed;
    }
}
