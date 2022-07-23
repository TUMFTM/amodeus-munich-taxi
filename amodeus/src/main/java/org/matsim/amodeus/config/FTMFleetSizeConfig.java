package org.matsim.amodeus.config;

import org.matsim.core.config.ReflectiveConfigGroup;

public class FTMFleetSizeConfig extends ReflectiveConfigGroup {
    public enum FleetSizeStrategy {
        Static,
        Dynamic,
        DynamicShifts,
        Predictive
    }

    static public final String GROUP_NAME = "fleetsize";

    static public final String STRATEGY = "strategy";
    static public final String PERCENTAGE = "percentage";
    static public final String STATIC_NUMBER_OF_VEHICLES = "staticNumberOfVehicles";

    private FleetSizeStrategy strategy = FleetSizeStrategy.Dynamic;
    private String percentage = "1";
    private int staticNumberOfVehicles = 350;

    public FTMFleetSizeConfig() {
        super(GROUP_NAME);
    }

    public FleetSizeStrategy getStrategy() {
        return strategy;
    }

    @StringGetter(STRATEGY)
    public String getStrategyLowercase() {
        return strategy.toString().toLowerCase();
    }

    @StringSetter(STRATEGY)
    public void setStrategy(String strategy) {
        switch (strategy) {
            case "static":
                this.strategy = FleetSizeStrategy.Static;
                break;
            case "dynamic":
                this.strategy = FleetSizeStrategy.Dynamic;
                break;
            case "predictive":
                this.strategy = FleetSizeStrategy.Predictive;
                break;
            case "dynamic-shifts":
                this.strategy = FleetSizeStrategy.DynamicShifts;
                break;
            default:
                System.err.println("Invalid Fleetsize strategy. Will use static fleet size.");
                this.strategy = FleetSizeStrategy.Static;
                break;
        }
    }

    @StringGetter(PERCENTAGE)
    public String getPercentage() {
        return percentage;
    }

    @StringSetter(PERCENTAGE)
    public void setPercentage(String percentage) {
        this.percentage = percentage;
    }

    @StringGetter(STATIC_NUMBER_OF_VEHICLES)
    public int getStaticNumberOfVehicles() {
        return staticNumberOfVehicles;
    }

    @StringSetter(STATIC_NUMBER_OF_VEHICLES)
    public void setStaticNumberOfVehicles(int staticNumberOfVehicles) {
        this.staticNumberOfVehicles = staticNumberOfVehicles;
    }
}
