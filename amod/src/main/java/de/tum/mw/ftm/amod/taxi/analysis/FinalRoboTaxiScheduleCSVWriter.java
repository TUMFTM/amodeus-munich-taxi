package de.tum.mw.ftm.amod.taxi.analysis;

import de.tum.mw.ftm.amod.taxi.preprocessing.demand.TaxiRide;

import java.io.*;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class FinalRoboTaxiScheduleCSVWriter {
    private final List<TaxiRide> trips;
    private final DateTimeFormatter dtf = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    public FinalRoboTaxiScheduleCSVWriter(FinalRoboTaxiSchedulesEventListener finalRoboTaxiSchedulesEventListener) {
        this.trips = finalRoboTaxiSchedulesEventListener.getTaxiRides();
    }

    public void wirteTripsCSV(File path) throws IOException {
        BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(path)));

        writer.write(String.join(",", new String[] { //
                "trip_id",
                "request_id",
                "vehicle_id",
                "start_date_time",
                "end_date_time",
                "taxi_status",
                "start_location",
                "end_location",
                "distance",
                "costs",
                "revenue"
        }) + "\n");

        for (TaxiRide trip : trips) {
            writer.write(String.join(",", new String[] { //
                    String.valueOf(trip.getTripId()),
                    trip.getRequestId(),
                    String.valueOf(trip.getVehicleId()),
                    trip.getTimestampStart().format(dtf),
                    trip.getTimestampStop().format(dtf),
                    trip.getType(),
                    trip.getLocationStart().toText(),
                    trip.getLocationStop().toText(),
                    String.valueOf(trip.getDistance()),
                    String.valueOf(trip.getOverallCosts()),
                    String.valueOf(trip.getOverallFare())
            }) + "\n");
        }

        writer.close();
    }
    public void writeDistanceByStatus(File path) throws IOException {
        Map<String, Double> distanceByStatus = trips.stream().collect(Collectors.groupingBy(t -> t.getType(), Collectors.summingDouble(t -> t.getDistance())));
        String[] keys = distanceByStatus.keySet().toArray(new String[0]);
        String[] values = new String[keys.length];
        int i = 0;
        for(String key:keys){
            values[i] = String.valueOf(distanceByStatus.get(key));
            i++;
        }
        BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(path)));
        writer.write(String.join(",", keys) + "\n");
        writer.write(String.join(",", values) + "\n");


        writer.close();
    }
}
