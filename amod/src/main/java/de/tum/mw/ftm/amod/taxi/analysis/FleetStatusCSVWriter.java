package de.tum.mw.ftm.amod.taxi.analysis;

import de.tum.mw.ftm.amod.analysis.AnalysisUtils;
import org.matsim.amodeus.config.FTMConfigGroup;

import java.io.*;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

public class FleetStatusCSVWriter {
    TreeMap<Double, Map<AnalysisUtils.TaxiTripType, Long>> fleetStatusMap;
    private final DateTimeFormatter dtf = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private final FTMConfigGroup ftmConfigGroup;
    private final LocalDateTime simStartTime;

    public FleetStatusCSVWriter(FleetStatusLogEventListener fleetStatusLogEventListener, FTMConfigGroup ftmConfigGroup) {
        this.fleetStatusMap = fleetStatusLogEventListener.getFleetStatusMap();
        this.ftmConfigGroup = ftmConfigGroup;
        this.simStartTime = ftmConfigGroup.getSimStartDateTime();

    }


    public void writeCSV(File path) throws IOException {
        BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(path)));


        String[] columns = Arrays.stream(AnalysisUtils.TaxiTripType.class.getEnumConstants()).map(Enum::name).toArray(String[]::new);
        writer.write(String.join(",", new String[] { //
                "time",
                "count_occupied",
                "count_approach",
                "count_rebalancing",
                "count_idle",
                "count_inactive"
        }) + "\n");


        for (Map.Entry<Double, Map<AnalysisUtils.TaxiTripType, Long>> fleetStatus : fleetStatusMap.entrySet()) {
            writer.write(String.join(",", new String[] { //
                   getDateTimeStringForOptionalTime(fleetStatus.getKey()),
                   String.valueOf(getCountOccupied(fleetStatus.getValue())),
                   String.valueOf(getCountApproach(fleetStatus.getValue())),
                   String.valueOf(getCountRebalancing(fleetStatus.getValue())),
                   String.valueOf(getCountIdle(fleetStatus.getValue())),
                   String.valueOf(getCountInactive(fleetStatus.getValue())),

            }) + "\n");
        }

        writer.close();
    }

    // Aggregate this in order to be compatible with analysis Script
    private long getCountOccupied(Map<AnalysisUtils.TaxiTripType, Long> fleetStatus){
        long count = 0;
        count += fleetStatus.getOrDefault(AnalysisUtils.TaxiTripType.OCCUPIED,0L);
        count += fleetStatus.getOrDefault(AnalysisUtils.TaxiTripType.PICKUP,0L);
        count += fleetStatus.getOrDefault(AnalysisUtils.TaxiTripType.DROPOFF,0L);
        return count;
    }
    private long getCountApproach(Map<AnalysisUtils.TaxiTripType, Long> fleetStatus){
        return fleetStatus.getOrDefault(AnalysisUtils.TaxiTripType.APPROACH,0L);
    }
    private long getCountRebalancing(Map<AnalysisUtils.TaxiTripType, Long> fleetStatus){
        return fleetStatus.getOrDefault(AnalysisUtils.TaxiTripType.REBALANCING,0L);
    }
    private long getCountIdle(Map<AnalysisUtils.TaxiTripType, Long> fleetStatus){
        return fleetStatus.getOrDefault(AnalysisUtils.TaxiTripType.IDLE,0L);
    }
    private long getCountInactive(Map<AnalysisUtils.TaxiTripType, Long> fleetStatus){
        return fleetStatus.getOrDefault(AnalysisUtils.TaxiTripType.OFFSERVICE,0L);
    }


    private String getDateTimeStringForOptionalTime(double time) {
            return simStartTime.plusSeconds((long) time).format(dtf);

    }
}
