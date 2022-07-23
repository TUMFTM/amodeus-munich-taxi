/* amodeus - Copyright (c) 2018, ETH Zurich, Institute for Dynamic Systems and Control */
package amodeus.amodtaxi.linkspeed.iterative;

import amodeus.amodeus.taxitrip.ShortestDurationCalculator;
import amodeus.amodeus.taxitrip.TaxiTrip;
import amodeus.amodeus.util.math.SI;
import ch.ethz.idsc.tensor.RealScalar;
import ch.ethz.idsc.tensor.Scalar;
import ch.ethz.idsc.tensor.io.StringScalar;
import ch.ethz.idsc.tensor.qty.Quantity;
import org.matsim.api.core.v01.network.Link;
import org.matsim.core.router.util.LeastCostPathCalculator.Path;

import java.io.*;
import java.util.List;
import java.util.Objects;

/**
 * Helper class to compare the duration of a taxi trip (from live data)
 * and a network path. If the nwPathdurationRatio is larger than 1,
 * the trip is slower in the simulatio network than in the original data.
 */
public class DurationCompare {

    public final Path path;
    public final TaxiTrip taxiTrip;
    public final Scalar tripDuration;
    public final Scalar tripDistance;
    public final Scalar pathTime;
    public final Scalar pathDist;

    /**
     * =1 simulation duration identical t.recorded duration
     * < 1 simulation duration faster than recorded duration
     * > 1 simulation duration slower than recorded duration
     */
    public final Scalar nwPathDurationRatio;
    public final Scalar nwPathDistanceRatio;

    public static DurationCompare getInstance(TaxiTrip trip, ShortestDurationCalculator calc) {
        Path path = calc.computePath(trip);
        if (path == null) {
            return null;
        } else {
            return new DurationCompare(trip, path);
        }
    }

    private DurationCompare(TaxiTrip trip, Path path) {
        this.taxiTrip = trip;
        this.path = path;
        this.pathTime = Quantity.of(this.path.travelTime, SI.SECOND);
        this.pathDist = Quantity.of(this.path.links.stream().filter(Objects::nonNull)
                .mapToDouble(Link::getLength).sum(), SI.METER);
        this.tripDuration = trip.driveTime;
        if (this.tripDuration instanceof StringScalar && this.tripDuration.toString().equals("null")) {
            this.nwPathDurationRatio = RealScalar.of(1);
        } else {
            this.nwPathDurationRatio = this.pathTime.divide(this.tripDuration);
        }
        this.tripDistance = trip.distance;
        if (this.tripDistance instanceof StringScalar && this.tripDistance.toString().equals("null")) {
            this.nwPathDistanceRatio = RealScalar.of(1);
        } else {
            this.nwPathDistanceRatio = this.pathDist.divide(this.tripDistance);
        }
    }

    public DurationCompare(TaxiTrip trip, ShortestDurationCalculator calc) {
        this.taxiTrip = trip;
        this.path = calc.computePath(trip);
        this.pathTime = Quantity.of(this.path.travelTime, SI.SECOND);
        this.pathDist = Quantity.of(this.path.links.stream().mapToDouble(Link::getLength).sum(), SI.METER);
        this.tripDuration = trip.driveTime;
        this.tripDistance = trip.distance;
        this.nwPathDurationRatio = this.pathTime.divide(this.tripDuration);
        if (this.tripDistance instanceof StringScalar && this.tripDistance.toString().equals("null")) {
            this.nwPathDistanceRatio = RealScalar.of(1);
        } else {
            this.nwPathDistanceRatio = this.pathDist.divide(this.tripDistance);
        }

    }

    public static void writeToCSV(List<DurationCompare> durationCompares, String filename) {
        final String CSV_SEPARATOR = ",";
        try {
            BufferedWriter bw = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(filename), "UTF-8"));
            StringBuffer oneLine = new StringBuffer();
            oneLine = new StringBuffer();
            oneLine.append("trip_id");
            oneLine.append(CSV_SEPARATOR);
            oneLine.append("trip_duration");
            oneLine.append(CSV_SEPARATOR);
            oneLine.append("trip_distance");
            oneLine.append(CSV_SEPARATOR);
            oneLine.append("path_duration");
            oneLine.append(CSV_SEPARATOR);
            oneLine.append("path_dist");
            bw.write(oneLine.toString());
            bw.newLine();
            for (DurationCompare durationCompare : durationCompares) {
                oneLine = new StringBuffer();
                oneLine.append(durationCompare.taxiTrip.localId);
                oneLine.append(CSV_SEPARATOR);
                oneLine.append(durationCompare.tripDuration.number().floatValue());
                oneLine.append(CSV_SEPARATOR);
                oneLine.append(durationCompare.tripDistance.number().floatValue());
                oneLine.append(CSV_SEPARATOR);
                oneLine.append(durationCompare.pathTime.number().floatValue());
                oneLine.append(CSV_SEPARATOR);
                oneLine.append(durationCompare.pathDist.number().floatValue());
                bw.write(oneLine.toString());
                bw.newLine();
            }
            bw.flush();
            bw.close();
        } catch (UnsupportedEncodingException e) {
        } catch (FileNotFoundException e) {
        } catch (IOException e) {
        }
    }

    public TaxiTrip getTaxiTrip() {
        return taxiTrip;
    }
}
