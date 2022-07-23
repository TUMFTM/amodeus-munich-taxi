package de.tum.mw.ftm.amod.taxi.preprocessing.linkspeeds;

import amodeus.amod.ext.Static;
import amodeus.amodeus.net.MatsimAmodeusDatabase;
import amodeus.amodeus.options.ScenarioOptions;
import amodeus.amodeus.options.ScenarioOptionsBase;
import amodeus.amodeus.taxitrip.ShortestDurationCalculator;
import amodeus.amodeus.taxitrip.TaxiTrip;
import amodeus.amodeus.util.io.MultiFileTools;
import amodeus.amodeus.util.math.GlobalAssert;
import amodeus.amodeus.util.matsim.NetworkLoader;
import amodeus.amodtaxi.linkspeed.iterative.DurationCompare;
import amodeus.amodtaxi.linkspeed.iterative.IterativeLinkSpeedEstimator;
import com.google.common.base.Stopwatch;
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
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

public class MunichLinkSpeedsGenerator {
    private final static Logger logger = Logger.getLogger(MunichLinkSpeedsGenerator.class);
    private static final Random RANDOM = new Random(123);

    public static void main(String[] args) throws IOException {
        Stopwatch stopwatch = Stopwatch.createStarted();

        File workingDirectory = MultiFileTools.getDefaultWorkingDirectory();

        Config config = AmodeusUtil.loadMatSimConfig();
        FTMConfigGroup ftmConfigGroup = (FTMConfigGroup) config.getModules().get("ftm_simulation");
        MunichLinkSpeedsGenerator munichLinkSpeedsGenerator = new MunichLinkSpeedsGenerator();

        logger.info("Loading network...");
        Network network = NetworkLoader.fromNetworkFile(new File(workingDirectory, config.network().getInputFile()));

        logger.info("Reading trips from historic trips file...");
        List<TaxiRide> rides = CSVTaxiRide.parseTaxiTripsFromCSV("historic_taxi_rides.csv").stream().map(l->l.toTaxiRide()).collect(Collectors.toList());
        List<TaxiTrip> amodeusTaxiTrips = rides.parallelStream().map(
                r -> r.toTaxiTrip(ftmConfigGroup)).collect(Collectors.toList());

        munichLinkSpeedsGenerator.run(workingDirectory, amodeusTaxiTrips);
        stopwatch.stop();
        logger.info("Time elapsed: " + stopwatch.elapsed(TimeUnit.MINUTES) + " minutes");
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
                t -> DurationCompare.getInstance(t, shortestDurationCalculator)
        ).filter(Objects::nonNull).collect(Collectors.toList());

        List<TaxiTrip> filtered_trips = (durationCompares.stream().filter(
                dc -> ((dc.nwPathDistanceRatio.number().doubleValue() >= 0.75 && dc.nwPathDistanceRatio.number().doubleValue() <= 1.25 && dc.tripDistance.number().doubleValue() >= 1000) ||
                        (dc.nwPathDistanceRatio.number().doubleValue() >= 0.25 && dc.nwPathDistanceRatio.number().doubleValue() <= 1.25 && dc.tripDistance.number().doubleValue() < 1000)) &&
                        dc.nwPathDurationRatio.number().doubleValue() >= 0.25 && dc.nwPathDurationRatio.number().doubleValue() <= 1.5).map(DurationCompare::getTaxiTrip).collect(Collectors.toList()));

        System.out.println(taxiTrips.size());
        System.out.println(filtered_trips.size());

        final int epochs = 7;
        IterativeLinkSpeedEstimator iterativeLinkSpeedEstimator = new IterativeLinkSpeedEstimator(epochs, RANDOM);
        iterativeLinkSpeedEstimator.compute(processingDir, network, db, filtered_trips);
    }

}

