package de.tum.mw.ftm.amod.taxi.preprocessing.linkspeeds;

import amodeus.amod.ext.Static;
import amodeus.amodeus.linkspeed.LinkSpeedDataContainer;
import amodeus.amodeus.linkspeed.LinkSpeedUtils;
import amodeus.amodeus.net.MatsimAmodeusDatabase;
import amodeus.amodeus.options.ScenarioOptions;
import amodeus.amodeus.options.ScenarioOptionsBase;
import amodeus.amodeus.taxitrip.ShortestDurationCalculator;
import amodeus.amodeus.taxitrip.TaxiTrip;
import amodeus.amodeus.util.io.MultiFileTools;
import amodeus.amodeus.util.matsim.NetworkLoader;
import amodeus.amodtaxi.linkspeed.iterative.DurationCompare;
import amodeus.amodtaxi.linkspeed.iterative.LinkSpeedLeastPathCalculatorFastAStarLandmark;
import amodeus.amodtaxi.scenario.ScenarioLabels;
import de.tum.mw.ftm.amod.taxi.preprocessing.demand.CSVTaxiRide;
import de.tum.mw.ftm.amod.taxi.preprocessing.demand.TaxiRide;
import de.tum.mw.ftm.amod.taxi.util.AmodeusUtil;
import org.apache.log4j.Logger;
import org.matsim.amodeus.config.FTMConfigGroup;
import org.matsim.api.core.v01.network.Network;
import org.matsim.core.config.Config;
import org.matsim.core.router.util.LeastCostPathCalculator;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

public class LinkSpeedAnalyser {
    private final static Logger logger = Logger.getLogger(MunichLinkSpeedsGenerator.class);

    public static void main(String[] args) throws IOException {
        Static.setup();

        File workingDirectory = MultiFileTools.getDefaultWorkingDirectory();

        Config config = AmodeusUtil.loadMatSimConfig();
        FTMConfigGroup ftmConfigGroup = (FTMConfigGroup) config.getModules().get("ftm_simulation");

        // load necessary files
        ScenarioOptions scenarioOptions = new ScenarioOptions(workingDirectory, //
                ScenarioOptionsBase.getDefault());

        logger.info("Loading network...");
        Network network = NetworkLoader.fromNetworkFile(new File(workingDirectory, config.network().getInputFile()));
        MatsimAmodeusDatabase db = MatsimAmodeusDatabase.initialize(network, scenarioOptions.getLocationSpec().referenceFrame());


        logger.info("Receiving trips from database...");
        List<TaxiRide> rides = CSVTaxiRide.parseTaxiTripsFromCSV("historic_taxi_rides.csv").stream().map(l->l.toTaxiRide()).collect(Collectors.toList());
        Set<TaxiTrip> taxiTrips = rides.parallelStream().map(r -> r.toTaxiTrip(ftmConfigGroup)).collect(Collectors.toSet());

        logger.info("Loading Link Speed File...");
        File linkSpeedsFile = new File(workingDirectory, ScenarioLabels.linkSpeedData);
        LinkSpeedDataContainer lsData = null;
        try {
           lsData = LinkSpeedUtils.loadLinkSpeedData(linkSpeedsFile);
        }
        catch (Exception e){
            logger.error("Unable to load linkSpeedData File");
            logger.error(e);
            System.exit(-1);
        }


        //TODO: Change hard coded value
        LeastCostPathCalculator lcpc = LinkSpeedLeastPathCalculatorFastAStarLandmark.from(network, lsData, 450);
        ShortestDurationCalculator calc = new ShortestDurationCalculator(lcpc, network, db);

        logger.info("Routing trips...");


        long start = System.currentTimeMillis();
        List<DurationCompare> durationCompares = taxiTrips.stream().map(t -> DurationCompare.getInstance(t, calc))
                .filter(Objects::nonNull)
                .collect(Collectors.toList());
        logger.error(String.format("Time needed %d ms", System.currentTimeMillis() - start));
        logger.info("Exporting csv...");
        DurationCompare.writeToCSV(durationCompares, "duration_compares_link_speeds_adjusted.csv");
    }
}
