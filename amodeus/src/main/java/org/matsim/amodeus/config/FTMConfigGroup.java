package org.matsim.amodeus.config;

import org.matsim.core.config.ConfigGroup;
import org.matsim.core.config.ReflectiveConfigGroup;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;

public class FTMConfigGroup extends ReflectiveConfigGroup {
    public enum DispatcherType {
        GBM,
        NTNR
    }

    static public final String GROUP_NAME = "ftm_simulation";

    static public final String REBALANCING_PERIOD_SECONDS = "rebalancingPeriodSeconds";
    static public final String DISPATCHING_PERIOD_SECONDS = "dispatchingPeriodSeconds";
    static public final String DISPATCHER_TYPE = "dispatcherType";
    static public final String REBALANCING_STEPS = "rebalancingSteps";
    static public final String FLEETSIZE_PERIOD = "fleetsizePeriod";
    static public final String GRID_INFORMATION_FILE = "gridInformationFile";
    static public final String REBALANCING_DEMAND_FILE = "rebalancingDemandFile";
    static public final String AREA_GEOJSON_FILE = "areaGeoJSONFile";
    static public final String SIM_START_DATE_TIME = "simStartDateTime";
    static public final String SIM_END_DATE_TIME = "simEndDateTime";
    static public final String SIM_END_TIME_BUFFER_SECONDS = "simEndTimeBufferSeconds";
    static public final String DYNAMIC_FLEET_SIZE_FILE = "dynamicFleetSizeFile";
    static public final String TARGET_PROBABILITIES_FILE = "targetProbabilitiesFile";
    static public final String DISPATCHING_ZONES_FILE = "dispatchingZonesFile";
    static public final String TAXI_RANKS_FILE = "taxiRanksFile";
    static public final String MAX_CUSTOMER_WAITING_TIME = "maxCustomerWaitingTime";
    static public final String MAX_CUSTOMER_ASSIGNMENT_TIME = "maxCustomerAssignmentTime";
    static public final String AIRPORT_PICKUPS = "airportPickups";
    static public final String ALPHA = "alpha";
    static public final String LAMBDA = "lambda";

    private int rebalancingPeriodSeconds = 20 * 60;
    private int dispatchingPeriodSeconds = 10;
    private DispatcherType dispatcherType = DispatcherType.GBM;
    private int rebalancingSteps = 2;
    private int fleetsizePeriod = 60;
    private String gridInformationFile = "grid_information.xml";
    private String rebalancingDemandFile = "taxi_demand_proto";
    private String targetProbabilitiesFile = "target_probabilities.xml";
    private String areaGeoJSONFile = "munich_area.geojson";
    private LocalDateTime simStartDateTime = LocalDateTime.parse("2019-11-04T06:00:00.000+01:00[Europe/Berlin]", DateTimeFormatter.ISO_ZONED_DATE_TIME);
    private LocalDateTime simEndDateTime = LocalDateTime.parse("2019-11-11T06:00:00.000+01:00[Europe/Berlin]", DateTimeFormatter.ISO_ZONED_DATE_TIME);
    private int simEndTimeBufferSeconds = 7200;
    private String dynamicFleetSizeFile = "dynamicFleetSize.xml";
    private String dispatchingZonesFile = "dispatchingZones.xml";
    private String taxiRanksFile = "taxiRanks.xml";
    private long maxCustomerWaitingTime = 30*60; // max customer waiting time in s
    private long maxCustomerAssignmentTime = 30*60;
    private boolean airportPickups = false;

    private double alpha = 0.1855;
    private double lambda= 12.26;


    private final FTMRevenueConfig ftmRevenueConfig = new FTMRevenueConfig();
    private final FTMCostsConfig ftmCostsConfig = new FTMCostsConfig();
    private final FTMFleetSizeConfig ftmFleetSizeConfig = new FTMFleetSizeConfig();


    public FTMConfigGroup() {
        super(GROUP_NAME, true);

        super.addParameterSet(ftmRevenueConfig);
        super.addParameterSet(ftmCostsConfig);
        super.addParameterSet(ftmFleetSizeConfig);
    }

    @Override
    public ConfigGroup createParameterSet(String type) {
        switch (type) {
            case FTMRevenueConfig.GROUP_NAME:
                return ftmRevenueConfig;
            case FTMCostsConfig.GROUP_NAME:
                return ftmCostsConfig;
            case FTMFleetSizeConfig.GROUP_NAME:
                return ftmFleetSizeConfig;
            default:
                throw new IllegalStateException("FTMConfigGroup does not support parameter set type: " + type);
        }
    }

    @Override
    public void addParameterSet(ConfigGroup parameterSet) {
        if (parameterSet instanceof FTMRevenueConfig) {
            return;
        }
        if (parameterSet instanceof FTMCostsConfig) {
            return;
        }
        if (parameterSet instanceof FTMFleetSizeConfig) {
            return;
        }
        throw new IllegalStateException("AMoDeus configuration module only accepts parameter sets of type AmodeusModeConfig or LinkSpeedsConfig");
    }


    public FTMRevenueConfig getFtmRevenueConfig() {
        return ftmRevenueConfig;
    }

    public FTMCostsConfig getFtmCostsConfig() {
        return ftmCostsConfig;
    }

    public FTMFleetSizeConfig getFtmFleetSizeConfig() {
        return ftmFleetSizeConfig;
    }


    @StringGetter(REBALANCING_PERIOD_SECONDS)
    public int getRebalancingPeriodSeconds() {
        return rebalancingPeriodSeconds;
    }

    @StringSetter(REBALANCING_PERIOD_SECONDS)
    public void setRebalancingPeriodSeconds(int rebalancingPeriodSeconds) {
        this.rebalancingPeriodSeconds = rebalancingPeriodSeconds;
    }

    @StringGetter(AIRPORT_PICKUPS)
    public boolean isAirportPickups() {
        return airportPickups;
    }
    @StringSetter(AIRPORT_PICKUPS)
    public void setAirportPickups(boolean maxTaxisAtAirport) {
        this.airportPickups = maxTaxisAtAirport;
    }

    @StringGetter(DISPATCHING_PERIOD_SECONDS)
    public int getDispatchingPeriodSeconds() {
        return dispatchingPeriodSeconds;
    }

    @StringSetter(DISPATCHING_PERIOD_SECONDS)
    public void setDispatchingPeriodSeconds(int dispatchingPeriodSeconds) {
        this.dispatchingPeriodSeconds = dispatchingPeriodSeconds;
    }

    @StringGetter(DISPATCHER_TYPE)
    public String getDispatcherTypeAsString() {
        return dispatcherType.toString();
    }

    public DispatcherType getDispatcherType() {
        return dispatcherType;
    }

    @StringSetter(DISPATCHER_TYPE)
    public void setDispatcherType(String dispatcherType) {
        switch (dispatcherType) {
            case "GBM":
                this.dispatcherType = DispatcherType.GBM;
                break;
            case "NTNR":
                this.dispatcherType = DispatcherType.NTNR;
                break;
            default:
                System.err.println("Invalid dispatcher type in FTMConfigGroup. Will use GBM.");
                this.dispatcherType = DispatcherType.GBM;
        }
    }

    @StringGetter(REBALANCING_STEPS)
    public int getRebalancingSteps() {
        return rebalancingSteps;
    }

    @StringSetter(REBALANCING_STEPS)
    public void setRebalancingSteps(int rebalancingSteps) {
        this.rebalancingSteps = rebalancingSteps;
    }

    @StringGetter(FLEETSIZE_PERIOD)
    public int getFleetsizePeriod() {
        return fleetsizePeriod;
    }

    @StringSetter(FLEETSIZE_PERIOD)
    public void setFleetsizePeriod(int fleetsizePeriod) {
        this.fleetsizePeriod = fleetsizePeriod;
    }

    @StringGetter(GRID_INFORMATION_FILE)
    public String getGridInformationFile() {
        return gridInformationFile;
    }

    @StringSetter(GRID_INFORMATION_FILE)
    public void setGridInformationFile(String gridInformationFile) {
        this.gridInformationFile = gridInformationFile;
    }

    @StringGetter(REBALANCING_DEMAND_FILE)
    public String getRebalancingDemandFile() {
        return rebalancingDemandFile;
    }

    @StringSetter(REBALANCING_DEMAND_FILE)
    public void setRebalancingDemandFile(String rebalancingDemandFile) {
        this.rebalancingDemandFile = rebalancingDemandFile;
    }

    @StringGetter(TARGET_PROBABILITIES_FILE)
    public String getTargetProbabilitiesFile() {
        return targetProbabilitiesFile;
    }

    @StringSetter(TARGET_PROBABILITIES_FILE)
    public void setTargetProbabilitiesFile(String targetProbabilitiesFile) {
        this.targetProbabilitiesFile = targetProbabilitiesFile;
    }

    @StringGetter(AREA_GEOJSON_FILE)
    public String getAreaGeoJSONFile() {
        return areaGeoJSONFile;
    }

    @StringSetter(AREA_GEOJSON_FILE)
    public void setAreaGeoJSONFile(String areaGeoJSONFile) {
        this.areaGeoJSONFile = areaGeoJSONFile;
    }

    @StringGetter(SIM_START_DATE_TIME)
    public LocalDateTime getSimStartDateTime() {
        return simStartDateTime;
    }

    @StringSetter(SIM_START_DATE_TIME)
    public void setSimStartDateTime(String startDateTimeString) {
        try {
            this.simStartDateTime = LocalDateTime.parse(startDateTimeString);
        } catch (DateTimeParseException e) {
            this.simStartDateTime = LocalDateTime.parse(startDateTimeString, DateTimeFormatter.ISO_ZONED_DATE_TIME);
        }
    }

    @StringGetter(SIM_END_DATE_TIME)
    public LocalDateTime getSimEndDateTime() {
        return simEndDateTime;
    }

    @StringSetter(SIM_END_DATE_TIME)
    public void setSimEndDateTime(String endDateTimeString) {
        try {
            this.simEndDateTime = LocalDateTime.parse(endDateTimeString);
        } catch (DateTimeParseException e) {
            this.simEndDateTime = LocalDateTime.parse(endDateTimeString, DateTimeFormatter.ISO_ZONED_DATE_TIME);
        }
    }

    @StringGetter(SIM_END_TIME_BUFFER_SECONDS)
    public int getSimEndTimeBufferSeconds() {
        return simEndTimeBufferSeconds;
    }

    @StringSetter(SIM_END_TIME_BUFFER_SECONDS)
    public void setSimEndTimeBufferSeconds(int simEndTimeBufferSeconds) {
        this.simEndTimeBufferSeconds = simEndTimeBufferSeconds;
    }


    @StringGetter(DYNAMIC_FLEET_SIZE_FILE)
    public String getDynamicFleetSizeFile() {
        return dynamicFleetSizeFile;
    }

    @StringSetter(DYNAMIC_FLEET_SIZE_FILE)
    public void setDynamicFleetSizeFile(String dynamicFleetSizeFile) {
        this.dynamicFleetSizeFile = dynamicFleetSizeFile;
    }

    @StringGetter(DISPATCHING_ZONES_FILE)
    public String getDispatchingZonesFile() {
        return dispatchingZonesFile;
    }

    @StringSetter(DISPATCHING_ZONES_FILE)
    public void setDispatchingZonesFile(String dispatchingZonesFile) {
        this.dispatchingZonesFile = dispatchingZonesFile;
    }

    @StringGetter(TAXI_RANKS_FILE)
    public String getTaxiRanksFile() {
        return taxiRanksFile;
    }

    @StringSetter(TAXI_RANKS_FILE)
    public void setTaxiRanksFile(String taxiRanksFile) {
        this.taxiRanksFile = taxiRanksFile;
    }

    @StringGetter(MAX_CUSTOMER_WAITING_TIME)
    public long getMaxCustomerWaitingTime() {
        return maxCustomerWaitingTime;
    }

    @StringSetter(MAX_CUSTOMER_WAITING_TIME)
    public void setMaxCustomerWaitingTime(long maxCustomerWaitingTime) {
        this.maxCustomerWaitingTime = maxCustomerWaitingTime;
    }

    @StringGetter(MAX_CUSTOMER_ASSIGNMENT_TIME)
    public long getMaxCustomerAssignmentTime() {
        return maxCustomerAssignmentTime;
    }

    @StringSetter(MAX_CUSTOMER_ASSIGNMENT_TIME)
    public void setMaxCustomerAssignmentTime(long maxCustomerAssignmentTime) {
        this.maxCustomerAssignmentTime = maxCustomerAssignmentTime;
    }

    @StringGetter(ALPHA)
    public double getAlpha() {
        return alpha;
    }

    @StringSetter(ALPHA)
    public void setAlpha(double alpha) {
        this.alpha = alpha;
    }

    @StringGetter(LAMBDA)
    public double getLambda() {
        return lambda;
    }

    @StringSetter(LAMBDA)
    public void setLambda(double lambda) {
        this.lambda = lambda;
    }



}
