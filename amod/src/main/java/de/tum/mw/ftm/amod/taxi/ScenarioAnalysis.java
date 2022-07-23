package de.tum.mw.ftm.amod.taxi;

import amodeus.amod.ext.Static;
import amodeus.amodeus.data.LocationSpec;
import amodeus.amodeus.data.ReferenceFrame;
import amodeus.amodeus.net.MatsimAmodeusDatabase;
import amodeus.amodeus.options.ScenarioOptions;
import amodeus.amodeus.options.ScenarioOptionsBase;
import amodeus.amodeus.util.io.MultiFileTools;
import amodeus.amodeus.util.math.GlobalAssert;
import de.tum.mw.ftm.amod.taxi.analysis.deprecated.Analysis;
import de.tum.mw.ftm.amod.taxi.analysis.PythonAnalysis;
import org.matsim.amodeus.config.AmodeusConfigGroup;
import org.matsim.amodeus.config.FTMConfigGroup;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.Network;
import org.matsim.contrib.dvrp.run.DvrpConfigGroup;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.scenario.ScenarioUtils;

import java.io.File;
import java.time.ZonedDateTime;

public class ScenarioAnalysis {
    public static void main(String[] args) throws Exception {
        File workingDirectory = MultiFileTools.getDefaultWorkingDirectory();
        Static.setup();
        /** working directory and options */
        ScenarioOptions scenarioOptions = new ScenarioOptions(workingDirectory, ScenarioOptionsBase.getDefault());

        File configFile = new File(scenarioOptions.getSimulationConfigName());

        /** geographic information */
        LocationSpec locationSpec = scenarioOptions.getLocationSpec();
        ReferenceFrame referenceFrame = locationSpec.referenceFrame();

        /** load MATSim configs - including av.xml configurations, load routing packages */
        GlobalAssert.that(configFile.exists());
        DvrpConfigGroup dvrpConfigGroup = new DvrpConfigGroup();
        dvrpConfigGroup.setTravelTimeEstimationAlpha(0.05);
        Config config = ConfigUtils.loadConfig(configFile.toString(), new AmodeusConfigGroup(), dvrpConfigGroup, new FTMConfigGroup());

        /** output directory for saving results */
        String outputdirectory = config.controler().getOutputDirectory();

        Scenario scenario = ScenarioUtils.loadScenario(config);
        Network network = scenario.getNetwork();
        MatsimAmodeusDatabase db = MatsimAmodeusDatabase.initialize(network, referenceFrame);

        /** perform analysis of simulation, a demo of how to add custom analysis methods
         * is provided in the package amod.demo.analysis */
        Analysis analysis = Analysis.setup(scenarioOptions, new File(outputdirectory), db);
//        CustomAnalysis.addTo(analysis);

        PythonAnalysis.addTo(analysis, new File(outputdirectory), db, ZonedDateTime.now(), ZonedDateTime.now());
        analysis.run();

    }
}
