/* amodeus - Copyright (c) 2018, ETH Zurich, Institute for Dynamic Systems and Control */
package amodeus.amodtaxi.linkspeed.iterative;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Random;
import java.util.zip.DataFormatException;

import amodeus.amodeus.linkspeed.LinkSpeedDataContainer;
import amodeus.amodeus.linkspeed.LinkSpeedUtils;
import amodeus.amodeus.net.MatsimAmodeusDatabase;
import amodeus.amodeus.options.ScenarioOptions;
import amodeus.amodeus.options.ScenarioOptionsBase;
import amodeus.amodeus.taxitrip.ImportTaxiTrips;
import amodeus.amodeus.taxitrip.TaxiTrip;
import amodeus.amodeus.util.math.GlobalAssert;
import amodeus.amodeus.util.matsim.NetworkLoader;
import org.matsim.api.core.v01.network.Network;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;

import amodeus.amodtaxi.linkspeed.TaxiLinkSpeedEstimator;
import ch.ethz.idsc.tensor.RealScalar;
import ch.ethz.idsc.tensor.Scalar;
import ch.ethz.idsc.tensor.io.HomeDirectory;

public class IterativeLinkSpeedEstimator implements TaxiLinkSpeedEstimator {
    private final int epochs;
    private final Scalar tolerance = RealScalar.of(0.005);
    /**
     * this is a value in (0,1] which determines the convergence
     * speed of the algorithm, a value close to 1 may lead to
     * loss of convergence, it is advised to choose slow values for
     * epsilon. No changes are applied for epsilon == 0.
     */
    private final Scalar epsilon1Start = RealScalar.of(0.4);
    private final Scalar epsilon1Target = RealScalar.of(0.1);
    /**
     * probability of taking a new trip
     */
    private final Scalar epsilon2 = RealScalar.of(0.8);
    private final Random random;
    private final int dt;
    private LinkSpeedDataContainer lsData;

    public IterativeLinkSpeedEstimator(int epochs, Random random) {
        this(epochs, random, 450);
    }

    public IterativeLinkSpeedEstimator(int epochs, Random random, int dt) {
        this.epochs = epochs;
        this.random = random;
        this.dt = dt;
    }

    public static void main(String[] args) throws IOException {
        File processingDir = HomeDirectory.file("data/TaxiComparison_ChicagoScCr/Scenario");
        File finalTripsFile = HomeDirectory.file("data/TaxiComparison_ChicagoScCr/Scenario/"//
                + "tripData/Taxi_Trips_2019_07_19_prepared_filtered_modified_final.csv");

        /** creating finding network and Matsimamodeusdatabase */
        // network and database
        ScenarioOptions scenarioOptions = new ScenarioOptions(processingDir, ScenarioOptionsBase.getDefault());
        File configFile = new File(scenarioOptions.getPreparerConfigName());
        System.out.println(configFile.getAbsolutePath());
        GlobalAssert.that(configFile.exists());
        Config configFull = ConfigUtils.loadConfig(configFile.toString());
        Network network = NetworkLoader.fromNetworkFile(new File(processingDir, configFull.network().getInputFile()));
        MatsimAmodeusDatabase db = //
                MatsimAmodeusDatabase.initialize(network, scenarioOptions.getLocationSpec().referenceFrame());

        /** generating the trips file */
        // List<TaxiTrip> trips = new ArrayList<>();
        // ImportTaxiTrips.fromFile(finalTripsFile).forEach(trips::add);
        List<TaxiTrip> trips = new ArrayList<>(ImportTaxiTrips.fromFile(finalTripsFile));
        new IterativeLinkSpeedEstimator(200000, new Random(123), 450).compute(processingDir, network, db, trips);
    }

    public void compute(File processingDir, Network network, MatsimAmodeusDatabase db, List<TaxiTrip> trips) {
        /** create link speed data container */
        File initialLinkSpeedData = new File(processingDir, "initialLinkSpeedData");

        try {
            lsData = LinkSpeedUtils.loadLinkSpeedData(initialLinkSpeedData);
            System.out.println("Using initial link speed data");
        } catch (Exception e) {
            System.out.println("Could not load inital link speed file. Using a fresh one...");
            e.printStackTrace();
            lsData = new LinkSpeedDataContainer(dt);
        }

        /** load initial trips */
        System.out.println("Number of trips: " + trips.size());
        FindCongestionIterative findCongestionIterative = new FindCongestionIterative(network, db, processingDir,
                lsData, trips, epochs, tolerance, epsilon1Start,
                epsilon1Target, epsilon2, random, Cost::max, trips.size());
        findCongestionIterative.runTripIterations();

        /** final export */
        StaticHelper.export(processingDir, lsData, "");
    }

    // -------

    @Override
    public LinkSpeedDataContainer getLsData() {
        return Objects.requireNonNull(lsData);
    }
}
