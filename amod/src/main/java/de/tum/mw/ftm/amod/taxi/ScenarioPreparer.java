/* amod - Copyright (c) 2018, ETH Zurich, Institute for Dynamic Systems and Control */
package de.tum.mw.ftm.amod.taxi;

import amodeus.amod.ext.Static;
import amodeus.amodeus.fleetsize.DynamicFleetSize;
import amodeus.amodeus.options.ScenarioOptions;
import amodeus.amodeus.options.ScenarioOptionsBase;
import amodeus.amodeus.prep.ConfigCreator;
import amodeus.amodeus.prep.NetworkPreparer;
import amodeus.amodeus.prep.PopulationPreparer;
import amodeus.amodeus.prep.VirtualNetworkPreparer;
import amodeus.amodeus.util.io.MultiFileTools;
import de.tum.mw.ftm.amod.taxi.preprocessing.demand.TaxiDemandGenerator;
import de.tum.mw.ftm.amod.taxi.preprocessing.fleetsize.DynamicFleetSizeXMLReader;
import de.tum.mw.ftm.amod.taxi.preprocessing.grid.GridInformationGenerator;
import de.tum.mw.ftm.amod.taxi.preprocessing.grid.TargetProbabilityGenerator;
import de.tum.mw.ftm.amod.taxi.util.AmodeusUtil;
import org.apache.log4j.Logger;
import org.matsim.amodeus.config.AmodeusConfigGroup;
import org.matsim.amodeus.config.FTMConfigGroup;
import org.matsim.amodeus.config.modal.DispatcherConfig;
import org.matsim.amodeus.config.modal.GeneratorConfig;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.population.Population;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.network.NetworkUtils;
import org.matsim.core.scenario.ScenarioUtils;

import java.io.File;
import java.net.MalformedURLException;

import static java.lang.Math.toIntExact;

/**
 * Class to prepare a given scenario for MATSim, includes preparation of
 * network, population, creation of virtualNetwork and travelData objects. As an example
 * a user may want to restrict the population size to few 100s of agents to run simulations
 * quickly during testing, or the network should be reduced to a certain area.
 */
public enum ScenarioPreparer {
    ;

    public static void main(String[] args) throws Exception {
        File workingDirectory = MultiFileTools.getDefaultWorkingDirectory();
        run(workingDirectory);
    }

    /**
     * loads scenario preparer in the {@link File} workingDirectory @param workingDirectory
     *
     * @throws MalformedURLException
     * @throws Exception
     */
    public static void run(File workingDirectory) throws MalformedURLException, Exception {
        Logger logger = Logger.getLogger("ScenarioPreparer");
        Static.setup();
        Static.checkGLPKLib();

        /** The {@link ScenarioOptions} contain amodeus specific options. Currently there are 3
         * options files:
         * - MATSim configurations (config.xml)
         * - AV package configurations (av.xml)
         * - AMoDeus configurations (AmodeusOptions.properties).
         *
         * The number of configs is planned to be reduced in subsequent refactoring steps. */
        ScenarioOptions scenarioOptions = new ScenarioOptions(workingDirectory, ScenarioOptionsBase.getDefault());

        /** MATSim config */
        AmodeusConfigGroup avConfigGroup = new AmodeusConfigGroup();
        Config config = ConfigUtils.loadConfig(scenarioOptions.getPreparerConfigName(), avConfigGroup, new FTMConfigGroup());
        AmodeusUtil.checkConfigConsistency(config);
        FTMConfigGroup ftmConfigGroup = AmodeusUtil.loadFTMConfigGroup();

        logger.info("Create raw population for simulation");
        File populationFile = new File(config.plans().getInputFile());
        if (!populationFile.isFile()) {
            TaxiDemandGenerator.createRawPopulationFileFromCSV(config, ftmConfigGroup,
                    NetworkUtils.readNetwork(config.network().getInputFile()),
                    new File("data_input/example_trips_munich_csv"));
        }
        Scenario scenario = ScenarioUtils.loadScenario(config);
        /** adaption of MATSim network, e.g., radius cutting */
        Network network = scenario.getNetwork();
        network = NetworkPreparer.run(network, scenarioOptions);

        /* Add your custom Generators here, to prepare the needed input files.
        * The simulation was connected to a local database, must be implemented individually,
        * if you want to generate the input data in this step. (Suggested) */


        File dispatchingZonesFile = new File(ftmConfigGroup.getDispatchingZonesFile());
        if (!dispatchingZonesFile.isFile()) {
            logger.warn(ftmConfigGroup.getDispatchingZonesFile() + " does not exist.");
            // ADD YOUR GENERATOR HERE
        }

        File taxiRanksFile = new File(ftmConfigGroup.getTaxiRanksFile());
        if (!taxiRanksFile.isFile()) {
            logger.warn(ftmConfigGroup.getTaxiRanksFile() + " does not exist.");
            // ADD YOUR GENERATOR HERE
        }


        DynamicFleetSize dynamicFleetSize = new DynamicFleetSize();

        // This is bypassed, cause we have no connection to a datasource in this example,
        // // ADD YOUR GENERATOR HERE
        new DynamicFleetSizeXMLReader(dynamicFleetSize).readFile(ftmConfigGroup.getDynamicFleetSizeFile()); // Replace this with your generator
        int numberOfVehicles = toIntExact(dynamicFleetSize.values().stream().max(Long::compare).get()); // Replace this with your generator

        GeneratorConfig genConfig = avConfigGroup.getModes().values().iterator().next().getGeneratorConfig();
        genConfig.setNumberOfVehicles(numberOfVehicles);
        System.out.println("NumberOfVehicles=" + numberOfVehicles);


        DispatcherConfig dispatcherConfig = avConfigGroup.getModes().values().iterator().next().getDispatcherConfig();
        if (!dispatcherConfig.getType().equals("DynamicFleetSizeFeedforwardFluidicRebalancingPolicy") &&
                !dispatcherConfig.getType().equals("DynamicFleetSizeFeedforwardFluidicTimeVaryingRebalancingPolicy")) {
            Static.setLPtoNone(workingDirectory);
        }

        if(dispatcherConfig.getType().equals("RebalancingLPDispatcher")){
            logger.warn(ftmConfigGroup.getGridInformationFile() + " Generate Grid InformationFile");
            GridInformationGenerator.createGridInformationFile(ftmConfigGroup, network);


            logger.info("Calculate target probabilities for simulation");
            File targetProbabilitiesFile = new File(ftmConfigGroup.getTargetProbabilitiesFile());
            if (!targetProbabilitiesFile.isFile()) {
                logger.warn(ftmConfigGroup.getTaxiRanksFile() + " does not exist.");
                TargetProbabilityGenerator.createProbabilitiesFile(ftmConfigGroup);
            }
        }

        /** adaption of MATSim population, e.g., radius cutting */
        Population population = scenario.getPopulation();
        long apoSeed = 1234;
        PopulationPreparer.run(network, population, scenarioOptions, config, apoSeed);


        /** creating a virtual network, e.g., for operational policies requiring a graph structure on the city */
        int endTime = (int) config.qsim().getEndTime().seconds();
        VirtualNetworkPreparer.INSTANCE.create(network, population, scenarioOptions, numberOfVehicles, endTime); //

        /** create a simulation MATSim config file linking the created input data */
        ConfigCreator.createSimulationConfigFile(config, scenarioOptions);
    }

}