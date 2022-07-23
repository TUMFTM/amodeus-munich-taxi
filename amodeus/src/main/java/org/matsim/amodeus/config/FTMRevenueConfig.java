package org.matsim.amodeus.config;

import org.matsim.core.config.ReflectiveConfigGroup;

public class FTMRevenueConfig extends ReflectiveConfigGroup {
    static public final String GROUP_NAME = "revenue";

    static public final String REVENUE_PER_KM_BELOW_5_KM = "revenuePerKmBelow5Km";
    static public final String REVENUE_PER_KM_BELOW_10_KM = "revenuePerKmBelow10Km";
    static public final String REVENUE_PER_KM_ABOVE_10_KM = "revenuePerKmAbove10Km";
    static public final String REVENUE_PER_TRIP = "revenuePerTrip";

    private double revenuePerKmBelow5Km;
    private double revenuePerKmBelow10Km;
    private double revenuePerKmAbove10Km;
    private double revenuePerTrip;


    public FTMRevenueConfig() {
        super(GROUP_NAME);
    }

    @StringGetter(REVENUE_PER_KM_BELOW_5_KM)
    public double getRevenuePerKmBelow5Km() {
        return revenuePerKmBelow5Km;
    }

    @StringSetter(REVENUE_PER_KM_BELOW_5_KM)
    public void setRevenuePerKmBelow5Km(double revenuePerKmBelow5Km) {
        this.revenuePerKmBelow5Km = revenuePerKmBelow5Km;
    }

    @StringGetter(REVENUE_PER_KM_BELOW_10_KM)
    public double getRevenuePerKmBelow10Km() {
        return revenuePerKmBelow10Km;
    }

    @StringSetter(REVENUE_PER_KM_BELOW_10_KM)
    public void setRevenuePerKmBelow10Km(double revenuePerKmBelow10Km) {
        this.revenuePerKmBelow10Km = revenuePerKmBelow10Km;
    }

    @StringGetter(REVENUE_PER_KM_ABOVE_10_KM)
    public double getRevenuePerKmAbove10Km() {
        return revenuePerKmAbove10Km;
    }

    @StringSetter(REVENUE_PER_KM_ABOVE_10_KM)
    public void setRevenuePerKmAbove10Km(double revenuePerKmAbove10Km) {
        this.revenuePerKmAbove10Km = revenuePerKmAbove10Km;
    }

    @StringGetter(REVENUE_PER_TRIP)
    public double getRevenuePerTrip() {
        return revenuePerTrip;
    }

    @StringSetter(REVENUE_PER_TRIP)
    public void setRevenuePerTrip(double revenuePerTrip) {
        this.revenuePerTrip = revenuePerTrip;
    }
}
