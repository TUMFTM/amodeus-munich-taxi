package de.tum.mw.ftm.amod.taxi.preprocessing.demand;

import amodeus.amod.ext.UserReferenceFrames;
import com.opencsv.bean.CsvBindByName;
import com.opencsv.bean.CsvToBeanBuilder;
import de.tum.mw.ftm.amod.analysis.AnalysisUtils;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.Point;
import org.locationtech.jts.geom.impl.CoordinateArraySequence;
import org.matsim.api.core.v01.Coord;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

public class CSVTaxiRide {

    public CSVTaxiRide() {
    }

    @CsvBindByName(column = "id")
    private int id;

    @CsvBindByName(column = "timestamp_start")
    private String timestampStart;

    @CsvBindByName(column = "timestamp_stop")
    private String timestampStop;

    @CsvBindByName(column = "start_lng")
    private double startLng;

    @CsvBindByName(column = "start_lat")
    private double startLat;

    @CsvBindByName(column = "stop_lng")
    private double stopLng;

    @CsvBindByName(column = "stop_lat")
    private double stopLat;

    @CsvBindByName(column = "distance")
    private double distance;

    public int getId() {
        return id;
    }

    public String getTimestampStart() {
        return timestampStart;
    }

    public String getTimestampStop() {
        return timestampStop;
    }

    public double getStartLng() {
        return startLng;
    }

    public double getStartLat() {
        return startLat;
    }

    public double getStopLng() {
        return stopLng;
    }

    public double getStopLat() {
        return stopLat;
    }

    public double getDistance() {
        return distance;
    }

    public static List<CSVTaxiRide> parseTaxiTripsFromCSV(String file) throws FileNotFoundException {
        return parseTaxiTripsFromCSV(new File(file));
    }
    public static List<CSVTaxiRide> parseTaxiTripsFromCSV(File file) throws FileNotFoundException {
        List<CSVTaxiRide> csvTrips = new CsvToBeanBuilder(new FileReader(file))
                .withType(CSVTaxiRide.class)
                .build()
                .parse();

        return csvTrips;
    }
    public TaxiRide toTaxiRide(){

        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
        LocalDateTime rideStartTime = LocalDateTime.parse(this.timestampStart, formatter);
        LocalDateTime rideStopTime = LocalDateTime.parse(this.timestampStop, formatter);

        GeometryFactory geometryFactory = new GeometryFactory();
        Point startPoint = new Point(new CoordinateArraySequence(
                new Coordinate[]{new Coordinate(this.startLng, this.startLat)}), geometryFactory);
        Point endPoint = new Point(new CoordinateArraySequence(
                new Coordinate[]{new Coordinate(this.stopLng, this.stopLat)}), geometryFactory);


        TaxiRide ride = new TaxiRide();
        ride.setTaxiRideId(new TaxiRideID((long) this.id, 0L));
        ride.setTimestampStart(rideStartTime);
        ride.setTimestampStop(rideStopTime);
        ride.setLocationStart(startPoint);
        ride.setLocationStop(endPoint);
        ride.setDistance(this.distance);
        return ride;
    }
}
