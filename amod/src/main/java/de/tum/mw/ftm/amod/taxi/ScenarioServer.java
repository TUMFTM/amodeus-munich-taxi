/* amod - Copyright (c) 2018, ETH Zurich, Institute for Dynamic Systems and Control */
package de.tum.mw.ftm.amod.taxi;


import amodeus.amod.dispatcher.DemoDispatcher;
import amodeus.amod.ext.Static;
import amodeus.amod.generator.DemoGenerator;

import amodeus.amodeus.data.LocationSpec;
import amodeus.amodeus.data.ReferenceFrame;
import amodeus.amodeus.linkspeed.*;
import amodeus.amodeus.net.MatsimAmodeusDatabase;
import amodeus.amodeus.net.SimulationServer;
import amodeus.amodeus.options.ScenarioOptions;
import amodeus.amodeus.options.ScenarioOptionsBase;
import amodeus.amodeus.util.io.MultiFileTools;
import amodeus.amodeus.util.math.GlobalAssert;
import amodeus.amodeus.util.matsim.AddCoordinatesToActivities;
import com.google.common.base.Stopwatch;
import de.tum.mw.ftm.amod.taxi.dispatcher.*;
import de.tum.mw.ftm.amod.taxi.dispatcher.RebalancingLPDispatcher;
import de.tum.mw.ftm.amod.taxi.util.AmodeusUtil;
import org.apache.log4j.Logger;
import org.matsim.amodeus.config.FTMConfigGroup;
import de.tum.mw.ftm.amod.taxi.di.SimulationModule;
import org.matsim.amodeus.AmodeusConfigurator;
import org.matsim.amodeus.config.AmodeusConfigGroup;
import org.matsim.amodeus.framework.AmodeusUtils;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.population.Population;
import org.matsim.contrib.dvrp.run.DvrpConfigGroup;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.config.groups.PlanCalcScoreConfigGroup.ActivityParams;
import org.matsim.core.config.groups.QSimConfigGroup;
import org.matsim.core.controler.AbstractModule;
import org.matsim.core.controler.Controler;
import org.matsim.core.scenario.ScenarioUtils;

import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.time.ZonedDateTime;
import java.util.Objects;
import java.util.concurrent.TimeUnit;

/**
 * This class runs an AMoDeus simulation based on MATSim. The results can be
 * viewed if the {@link ScenarioViewer} is executed in the same working
 * directory and the button "Connect" is pressed.
 */
public enum ScenarioServer {
    ;
    private final static Logger logger = Logger.getLogger(ScenarioServer.class);

    public static void main(String[] args) throws MalformedURLException, Exception {
        simulate(MultiFileTools.getDefaultWorkingDirectory());
    }

    /**
     * runs a simulation run using input data from Amodeus.properties, av.xml and
     * MATSim config.xml
     *
     * @throws MalformedURLException
     * @throws Exception
     */
    public static void simulate(File workingDirectory) throws MalformedURLException, Exception {
        Stopwatch stopwatch = Stopwatch.createStarted();
        ZonedDateTime simStarted = ZonedDateTime.now();
        Static.setup();
        System.out.println("\n\n\n" + Static.glpInfo() + "\n\n\n");

        /** working directory and options */
        ScenarioOptions scenarioOptions = new ScenarioOptions(workingDirectory, ScenarioOptionsBase.getDefault());

        /** set to true in order to make server wait for at least 1 client, for
         * instance viewer client, for fals the ScenarioServer starts the simulation
         * immediately */
        boolean waitForClients = scenarioOptions.getBoolean("waitForClients");
        File configFile = new File(scenarioOptions.getSimulationConfigName());

        /** geographic information */
        LocationSpec locationSpec = scenarioOptions.getLocationSpec();
        ReferenceFrame referenceFrame = locationSpec.referenceFrame();

        /** open server port for clients to connect to */
        SimulationServer.INSTANCE.startAcceptingNonBlocking();
        SimulationServer.INSTANCE.setWaitForClients(waitForClients);

        /** load MATSim configs - including av.xml configurations, load routing packages */
        GlobalAssert.that(configFile.exists());
        DvrpConfigGroup dvrpConfigGroup = new DvrpConfigGroup();
        dvrpConfigGroup.setTravelTimeEstimationAlpha(0.05);
        Config config = ConfigUtils.loadConfig(configFile.toString(), new AmodeusConfigGroup(), dvrpConfigGroup, new FTMConfigGroup());
        AmodeusUtil.checkConfigConsistency(config);
        config.planCalcScore().addActivityParams(new ActivityParams("activity"));

        config.qsim().setStartTime(0.0);
        config.qsim().setSimStarttimeInterpretation(QSimConfigGroup.StarttimeInterpretation.onlyUseStarttime);

        /** MATSim does not allow the typical duration not to be set, therefore for scenarios
         * generated from taxi data such as the "SanFrancisco" scenario, it is set to 1 hour. */
        for (ActivityParams activityParams : config.planCalcScore().getActivityParams()) {
            // TODO set typical duration in scenario generation and remove
            activityParams.setTypicalDuration(3600.0);
        }

        /** output directory for saving results */
        String outputdirectory = config.controler().getOutputDirectory();

        /** load MATSim scenario for simulation */
        Scenario scenario = ScenarioUtils.loadScenario(config);
        AddCoordinatesToActivities.run(scenario);
        Network network = scenario.getNetwork();
        Population population = scenario.getPopulation();
        GlobalAssert.that(Objects.nonNull(network));
        GlobalAssert.that(Objects.nonNull(population));

        MatsimAmodeusDatabase db = MatsimAmodeusDatabase.initialize(network, referenceFrame);
        Controler controller = new Controler(scenario);
        controller.addOverridingModule(new SimulationModule(network));


        AmodeusConfigurator.configureController(controller, db, scenarioOptions);

        /** try to load link speed data and use for speed adaption in network */
        try {
            File linkSpeedDataFile = new File(scenarioOptions.getLinkSpeedDataName());
            System.out.println(linkSpeedDataFile.toString());
            LinkSpeedDataContainer lsData = LinkSpeedUtils.loadLinkSpeedData(linkSpeedDataFile);
            controller.addOverridingModule(new LinkSpeedsModule(lsData));
            controller.addOverridingQSimModule(new TrafficDataModule(lsData));
        } catch (IOException exception) {
            logger.error("Unable to load linkspeed data.");
            logger.error(exception);
        }


        /** With the subsequent lines an additional user-defined dispatcher is added, functionality
         * in class
         * DemoDispatcher, as long as the dispatcher was not selected in the file av.xml, it is not
         * used in the simulation. */
        controller.addOverridingModule(new AbstractModule() {
            @Override
            public void install() {
                AmodeusUtils.registerDispatcherFactory(binder(), //
                        DemoDispatcher.class.getSimpleName(), DemoDispatcher.Factory.class);
            }
        });

        controller.addOverridingModule(new AbstractModule() {
            @Override
            public void install() {
                AmodeusUtils.registerDispatcherFactory(binder(), //
                        ReferenceDispatcher.class.getSimpleName(), ReferenceDispatcher.Factory.class);
            }
        });
        controller.addOverridingModule(new AbstractModule() {
            @Override
            public void install() {
                AmodeusUtils.registerDispatcherFactory(binder(), //
                        ReferenceRebalancer.class.getSimpleName(), ReferenceRebalancer.Factory.class);
            }
        });
        controller.addOverridingModule(new AbstractModule() {
            @Override
            public void install() {
                AmodeusUtils.registerDispatcherFactory(binder(), //
                        RebalancingLPDispatcher.class.getSimpleName(), RebalancingLPDispatcher.Factory.class);
            }
        });
        controller.addOverridingModule(new AbstractModule() {
            @Override
            public void install() {
                AmodeusUtils.registerDispatcherFactory(binder(), //
                        FleetSizeDemoDispatcher.class.getSimpleName(), FleetSizeDemoDispatcher.Factory.class);
            }
        });
        controller.addOverridingModule(new AbstractModule() {
            @Override
            public void install() {
                AmodeusUtils.registerDispatcherFactory(binder(), //
                        DynamicFleetSizeFeedforwardFluidicRebalancingPolicy.class.getSimpleName(), DynamicFleetSizeFeedforwardFluidicRebalancingPolicy.Factory.class);
            }
        });
        controller.addOverridingModule(new AbstractModule() {
            @Override
            public void install() {
                AmodeusUtils.registerDispatcherFactory(binder(), //
                        DynamicFleetSizeFeedforwardFluidicTimeVaryingRebalancingPolicy.class.getSimpleName(), DynamicFleetSizeFeedforwardFluidicTimeVaryingRebalancingPolicy.Factory.class);
            }
        });

        /** With the subsequent lines, additional user-defined initial placement logic called
         * generator is added,
         * functionality in class DemoGenerator. As long as the generator is not selected in the
         * file av.xml,
         * it is not used in the simulation. */
        controller.addOverridingModule(new AbstractModule() {
            @Override
            public void install() {
                AmodeusUtils.registerGeneratorFactory(binder(), "DemoGenerator", DemoGenerator.Factory.class);
            }
        });

        /** With the subsequent lines, another custom router is added apart from the
         * {@link DefaultAStarLMRouter},
         * it has to be selected in the av.xml file with the lines as follows:
         * <operator id="op1">
         * <param name="routerName" value="DefaultAStarLMRouter" />
         * <generator strategy="PopulationDensity">
         * ...
         *
         * otherwise the normal {@link DefaultAStarLMRouter} will be used. */
        /** Custom router that ensures same network speeds as taxis in original data set. */
        controller.addOverridingModule(new AbstractModule() {
            @Override
            public void install() {
                AmodeusUtils.bindRouterFactory(binder(), DynamicTravelTimeFastAStarLandmarkRouter.class.getSimpleName()).to(DynamicTravelTimeFastAStarLandmarkRouter.Factory.class);
            }
        });
        controller.addOverridingModule(new AbstractModule() {
            @Override
            public void install() {
                AmodeusUtils.bindRouterFactory(binder(), DynamicTravelTimeShortestFastAStarLandmarkRouter.class.getSimpleName()).to(DynamicTravelTimeShortestFastAStarLandmarkRouter.Factory.class);
            }
        });



        /** run simulation */
        controller.run();
        ZonedDateTime simFinished = ZonedDateTime.now();
        logger.info("Simulation finished elapsed: " + stopwatch.elapsed(TimeUnit.MINUTES) + " minutes");

        /** close port for visualizaiton */
        SimulationServer.INSTANCE.stopAccepting();

        /** perform analysis of simulation, a demo of how to add custom analysis methods
         * is provided in the package amod.demo.analysis */
//        Analysis analysis = Analysis.setup(scenarioOptions, new File(outputdirectory), db);
//
//
//        PythonAnalysis.addTo(analysis, new File(outputdirectory), db, simStarted, simFinished);
//        analysis.run();

    }
}