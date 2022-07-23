package de.tum.mw.ftm.amod.taxi.di;

import amodeus.amodeus.fleetsize.DynamicFleetSize;
import amodeus.amodeus.linkspeed.LinkSpeedDataContainer;
import amodeus.amodeus.linkspeed.LinkSpeedUtils;
import amodeus.amodeus.options.ScenarioOptions;
import amodeus.amodeus.options.ScenarioOptionsBase;
import amodeus.amodeus.util.io.MultiFileTools;
import com.google.inject.Provides;
import com.google.inject.Singleton;
import de.tum.mw.ftm.amod.taxi.analysis.PythonAnalysisOutputListener;
import de.tum.mw.ftm.amod.taxi.preprocessing.demandprediction.UniformGridWithBorderPredictions;
import de.tum.mw.ftm.amod.taxi.preprocessing.fleetsize.DynamicFleetSizeXMLReader;
import de.tum.mw.ftm.amod.taxi.preprocessing.grid.GridInformationXMLReader;
import de.tum.mw.ftm.amod.taxi.preprocessing.grid.TargetProbabilityXMLReader;
import de.tum.mw.ftm.amod.taxi.preprocessing.ranks.TaxiRanks;
import de.tum.mw.ftm.amod.taxi.preprocessing.ranks.TaxiRanksXMLReader;
import de.tum.mw.ftm.amod.taxi.preprocessing.zones.DispatchingZoneFinderImpl;
import de.tum.mw.ftm.amod.taxi.preprocessing.zones.DispatchingZones;
import de.tum.mw.ftm.amod.taxi.preprocessing.zones.DispatchingZonesXMLReader;
import de.tum.mw.ftm.amod.taxi.util.ZonalUtils;
import org.apache.log4j.Logger;
import org.matsim.amodeus.config.AmodeusConfigGroup;
import org.matsim.amodeus.config.FTMConfigGroup;
import org.matsim.amodeus.config.modal.LinkSpeedsConfig;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.contrib.zone.Zone;
import org.matsim.contrib.zone.util.NetworkWithZonesUtils;
import org.matsim.core.config.Config;
import org.matsim.core.controler.AbstractModule;

import javax.inject.Named;
import java.io.File;
import java.io.IOException;
import java.util.Map;

public class SimulationModule extends AbstractModule {
    private final Network network;
    private final Logger logger = Logger.getLogger("SimulationModule");
    public SimulationModule(Network network) {
        this.network = network;
    }

    @Singleton
    @Provides
    public TaxiRanks provideTaxiRanks(Config config) {
        FTMConfigGroup ftmConfigGroup = (FTMConfigGroup) config.getModules().get("ftm_simulation");

        TaxiRanks taxiRanks = new TaxiRanks();
        new TaxiRanksXMLReader(taxiRanks).readFile(ftmConfigGroup.getTaxiRanksFile());
        return taxiRanks;
    }

    @Singleton
    @Provides
    public UniformGridWithBorderPredictions provideUniformGridWithBorderPredictions(Config config){
        FTMConfigGroup ftmConfigGroup = (FTMConfigGroup) config.getModules().get("ftm_simulation");

        UniformGridWithBorderPredictions uniformGridWithBorderPredictions =  UniformGridWithBorderPredictions.fromXML(ftmConfigGroup.getRebalancingDemandFile());
        //TODO: @michaelwittmann add assertions, that demand file matches simulation timeframe

        return uniformGridWithBorderPredictions;
    }


    @Singleton
    @Provides
    public DispatchingZones provideZones(Config config) {
        FTMConfigGroup ftmConfigGroup = (FTMConfigGroup) config.getModules().get("ftm_simulation");

        DispatchingZones dispatchingZones = new DispatchingZones();
        new DispatchingZonesXMLReader(dispatchingZones).readFile(ftmConfigGroup.getDispatchingZonesFile());
        return dispatchingZones;
    }


    @Provides
    @Singleton
    public ZonalUtils provideZonalUtils(DispatchingZones zones) {
        Map<Id<Link>, Zone> linkToZone = NetworkWithZonesUtils.createLinkToZoneMap(network,
                new DispatchingZoneFinderImpl(zones, 0));

        return new ZonalUtils(network, zones, linkToZone);
    }

    @Provides
    @Singleton
    public ScenarioOptions provideScenarioOptions() throws IOException {
        return new ScenarioOptions(MultiFileTools.getDefaultWorkingDirectory(), ScenarioOptionsBase.getDefault());
    }

    @Provides
    @Singleton
    public DynamicFleetSize provideDynamicFleetSize(Config config) {
        FTMConfigGroup ftmConfigGroup = (FTMConfigGroup) config.getModules().get("ftm_simulation");
        DynamicFleetSize dynamicFleetSize = new DynamicFleetSize();
        new DynamicFleetSizeXMLReader(dynamicFleetSize).readFile(ftmConfigGroup.getDynamicFleetSizeFile());
        return dynamicFleetSize;
    }

    @Provides
    @Singleton
    @Named("freeSpeedTravelTimes")
    public double[][] provideFreespeedTravelTime(Config config) {
        FTMConfigGroup ftmConfigGroup = (FTMConfigGroup) config.getModules().get("ftm_simulation");

        GridInformationXMLReader gridInformationXMLReader = new GridInformationXMLReader();
        gridInformationXMLReader.readFile(ftmConfigGroup.getGridInformationFile());

        return gridInformationXMLReader.getFreespeedTravelTimes();
    }

    @Provides
    @Singleton
    @Named("adaptedTravelTimes")
    public double[][] provideAdaptedTravelTime(@Named("freeSpeedTravelTimes") double[][] freeSpeedTravelTimes, Config config) {
        AmodeusConfigGroup amodeusConfigGroup = (AmodeusConfigGroup) config.getModules().get("amodeus");
        LinkSpeedsConfig linkSpeedsConfig = amodeusConfigGroup.getLinkSpeedsConfig();

        File workingDirectory = MultiFileTools.getDefaultWorkingDirectory();
        File linkSpeedData = new File(workingDirectory, linkSpeedsConfig.getLinkSpeedFile());
        double overallRatio = 1;

        LinkSpeedDataContainer lsData;
        try{
            lsData = LinkSpeedUtils.loadLinkSpeedData(linkSpeedData);
            logger.info("Using link speed data for adapted travel times");
            System.out.println("Using link speed data for adapted travel times");

            Map<Id<Link>, ? extends Link> linkMap = network.getLinks();
            if (lsData != null) {
                overallRatio = lsData.calculateAverageLinkSpeedRatio(linkMap);
            }
            double[][] adaptedTravelTimes = new double[freeSpeedTravelTimes.length][freeSpeedTravelTimes[0].length];
            for (int row = 0; row < freeSpeedTravelTimes.length; row++) {
                for (int col = 0; col < freeSpeedTravelTimes[row].length; col++) {
                    adaptedTravelTimes[row][col] = freeSpeedTravelTimes[row][col] / overallRatio;
                }
            }
            return adaptedTravelTimes;
        }
        catch (Exception e){
            logger.warn("Could not load link speed data from file, using FreeSpeedTravelTimes as Backup");
            logger.error(e);
            return provideFreespeedTravelTime(config);
        }
    }

    @Provides
    @Singleton
    @Named("cellDistances")
    public double[][] provideCellDistances(Config config) {
        FTMConfigGroup ftmConfigGroup = (FTMConfigGroup) config.getModules().get("ftm_simulation");

        GridInformationXMLReader gridInformationXMLReader = new GridInformationXMLReader();
        gridInformationXMLReader.readFile(ftmConfigGroup.getGridInformationFile());

        return gridInformationXMLReader.getDistances();
    }

    @Provides
    @Singleton
    @Named("targetProbabilities")
    public double[][] provideTargetProbabilities(Config config) {
        FTMConfigGroup ftmConfigGroup = (FTMConfigGroup) config.getModules().get("ftm_simulation");

        TargetProbabilityXMLReader targetProbabilityXMLReader = new TargetProbabilityXMLReader();
        targetProbabilityXMLReader.readFile(ftmConfigGroup.getTargetProbabilitiesFile());

        return targetProbabilityXMLReader.getTargetProbabilities();
    }

    @Override
    public void install() {
        addControlerListenerBinding().to(PythonAnalysisOutputListener.class);
    }
}
