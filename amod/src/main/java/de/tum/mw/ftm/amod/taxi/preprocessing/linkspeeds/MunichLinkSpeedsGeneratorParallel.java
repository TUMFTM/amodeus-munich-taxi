package de.tum.mw.ftm.amod.taxi.preprocessing.linkspeeds;

import amodeus.amod.ext.Static;
import amodeus.amodeus.linkspeed.LinkSpeedDataContainer;
import amodeus.amodeus.net.MatsimAmodeusDatabase;
import amodeus.amodeus.options.ScenarioOptions;
import amodeus.amodeus.options.ScenarioOptionsBase;
import amodeus.amodeus.taxitrip.ShortestDurationCalculator;
import amodeus.amodeus.taxitrip.TaxiTrip;
import amodeus.amodeus.util.io.MultiFileTools;
import amodeus.amodeus.util.math.GlobalAssert;
import amodeus.amodeus.util.matsim.NetworkLoader;
import amodeus.amodtaxi.linkspeed.iterative.Cost;
import amodeus.amodtaxi.linkspeed.iterative.DurationCompare;
import amodeus.amodtaxi.linkspeed.iterative.FindCongestionIterativeParallel;
import ch.ethz.idsc.tensor.RealScalar;
import ch.ethz.idsc.tensor.Scalar;
import de.tum.mw.ftm.amod.taxi.preprocessing.demand.CSVTaxiRide;
import de.tum.mw.ftm.amod.taxi.preprocessing.demand.TaxiRide;
import de.tum.mw.ftm.amod.taxi.util.AmodeusUtil;
import org.apache.log4j.Logger;
import org.matsim.amodeus.config.FTMConfigGroup;
import org.matsim.api.core.v01.network.Network;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.Objects;
import java.util.Random;
import java.util.stream.Collectors;

public class MunichLinkSpeedsGeneratorParallel {
    private final static Logger logger = Logger.getLogger(MunichLinkSpeedsGeneratorParallel.class);
    private static final Random RANDOM = new Random(123);

    public static void main(String[] args) throws IOException {


        File workingDirectory = MultiFileTools.getDefaultWorkingDirectory();

        Config config = AmodeusUtil.loadMatSimConfig();
        FTMConfigGroup ftmConfigGroup = (FTMConfigGroup) config.getModules().get("ftm_simulation");
        MunichLinkSpeedsGeneratorParallel munichLinkSpeedsGenerator = new MunichLinkSpeedsGeneratorParallel();

        logger.info("Reading trips from historic trips file...");
        List<TaxiRide> rides = CSVTaxiRide.parseTaxiTripsFromCSV("historic_taxi_rides.csv").stream().map(l->l.toTaxiRide()).collect(Collectors.toList());
        List<TaxiTrip> amodeusTaxiTrips = rides.parallelStream().map(r -> r.toTaxiTrip(ftmConfigGroup)).collect(Collectors.toList());


        munichLinkSpeedsGenerator.run(workingDirectory, amodeusTaxiTrips);
    }

    private void run(File processingDir, List<TaxiTrip> taxiTrips) throws IOException {
        Static.setup();

        // load necessary files
        ScenarioOptions scenarioOptions = new ScenarioOptions(processingDir, //
                ScenarioOptionsBase.getDefault());
        File configFile = new File(scenarioOptions.getPreparerConfigName());
        System.out.println(configFile.getAbsolutePath());
        GlobalAssert.that(configFile.exists());
        Config configFull = ConfigUtils.loadConfig(configFile.toString());
        Network network = NetworkLoader.fromNetworkFile(new File(processingDir, configFull.network().getInputFile()));
        MatsimAmodeusDatabase db = MatsimAmodeusDatabase.initialize(network, scenarioOptions.getLocationSpec().referenceFrame());

        ShortestDurationCalculator shortestDurationCalculator = new ShortestDurationCalculator(network, db);


        List<DurationCompare> durationCompares = taxiTrips.stream().map(
                t -> DurationCompare.getInstance(t, shortestDurationCalculator))
                .filter(Objects::nonNull)
                .collect(Collectors.toList());

        DurationCompare.writeToCSV(durationCompares, "duration_compares.csv");

        List<TaxiTrip> filtered_trips = (durationCompares.stream().filter(
                dc -> ((dc.nwPathDistanceRatio.number().doubleValue() >= 0.75 && dc.nwPathDistanceRatio.number().doubleValue() <= 1.25 && dc.tripDistance.number().doubleValue() >= 1000) ||
                        (dc.nwPathDistanceRatio.number().doubleValue() >= 0.25 && dc.nwPathDistanceRatio.number().doubleValue() <= 1.25 && dc.tripDistance.number().doubleValue() < 1000)) &&
                        dc.nwPathDurationRatio.number().doubleValue() >= 0.25 && dc.nwPathDurationRatio.number().doubleValue() <= 1.5).map(DurationCompare::getTaxiTrip).collect(Collectors.toList()));

        System.out.println(taxiTrips.size());
        System.out.println(filtered_trips.size());

        final int maxIter = 10000;
        final int dt = 450;
        final Scalar tolerance = RealScalar.of(0.005);
        /**
         * this is a value in (0,1] which determines the convergence
         * speed of the algorithm, a value close to 1 may lead to
         * loss of convergence, it is advised to choose slow values for
         * epsilon. No changes are applied for epsilon == 0.
         */
        final Scalar epsilon1Start = RealScalar.of(0.3);
        final Scalar epsilon1Target = RealScalar.of(0.05);

        /** create link speed data container */

        LinkSpeedDataContainer lsData = new LinkSpeedDataContainer(dt);

        /** load initial trips */
        System.out.println("Number of trips: " + filtered_trips.size());
        new FindCongestionIterativeParallel(network, db, processingDir, lsData, filtered_trips, maxIter, //
                tolerance, epsilon1Start, epsilon1Target,
                RANDOM, dt, Cost::max, filtered_trips.size());
    }

}


