package org.matsim.amodeus.config;

import org.matsim.core.config.ReflectiveConfigGroup;

public class FTMCostsConfig extends ReflectiveConfigGroup {
    static public final String GROUP_NAME = "costs";

    static public final String COSTS_PER_KM = "costsPerKm";
    static public final String COSTS_PER_HOUR = "costsPerHour";

    private double costsPerKm = 0.18;
    private double costsPerHour = 13.31;

    public FTMCostsConfig() {
        super(GROUP_NAME);
    }

    @StringGetter(COSTS_PER_KM)
    public double getCostsPerKm() {
        return costsPerKm;
    }

    @StringSetter(COSTS_PER_KM)
    public void setCostsPerKm(double costsPerKm) {
        this.costsPerKm = costsPerKm;
    }

    @StringGetter(COSTS_PER_HOUR)
    public double getCostsPerHour() {
        return costsPerHour;
    }

    @StringSetter(COSTS_PER_HOUR)
    public void setCostsPerHour(double costsPerHour) {
        this.costsPerHour = costsPerHour;
    }
}
