/* amodeus - Copyright (c) 2018, ETH Zurich, Institute for Dynamic Systems and Control */
package amodeus.amodeus.linkspeed;

import com.google.inject.Singleton;
import org.matsim.api.core.v01.network.Link;

import java.util.Objects;

@Singleton
public class DefaultTaxiTrafficData implements TaxiTrafficData {
    private final LinkSpeedDataContainer lsData;

    public DefaultTaxiTrafficData(LinkSpeedDataContainer lsData) {
        this.lsData = Objects.requireNonNull(lsData);
    }

    public DefaultTaxiTrafficData(LinkSpeedDataContainer lsData, int dt) {
        this.lsData = Objects.requireNonNull(lsData);
    }

    @Override
    public double getTrafficSpeed(Link link, double now) {
        return LinkSpeedUtils.getLinkSpeedForTime(lsData, link, now);
    }

    @Override
    public LinkSpeedDataContainer getLSData() {
        return lsData;
    }
}
