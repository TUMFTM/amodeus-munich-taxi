/* amodeus - Copyright (c) 2018, ETH Zurich, Institute for Dynamic Systems and Control */
package amodeus.amodeus.testutils;

import java.io.File;
import java.io.IOException;

import org.matsim.api.core.v01.network.Network;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;

import amodeus.amodeus.data.LocationSpec;
import amodeus.amodeus.data.ReferenceFrame;
import amodeus.amodeus.gfx.AmodeusComponent;
import amodeus.amodeus.gfx.AmodeusViewerFrame;
import amodeus.amodeus.gfx.ViewerConfig;
import amodeus.amodeus.net.MatsimAmodeusDatabase;
import amodeus.amodeus.options.ScenarioOptions;
import amodeus.amodeus.options.ScenarioOptionsBase;
import amodeus.amodeus.util.matsim.NetworkLoader;
import amodeus.amodeus.virtualnetwork.core.VirtualNetworkGet;

public class TestViewer {
    public static TestViewer run(File workingDirectory) throws IOException {
        return new TestViewer(workingDirectory);
    }

    private final AmodeusComponent amodeusComponent;
    private final ViewerConfig viewerConfig;

    private TestViewer(File workingDirectory) throws IOException {
        // Static.setup();
        ScenarioOptions scenarioOptions = new ScenarioOptions(workingDirectory, ScenarioOptionsBase.getDefault());

        /** load options */
        Config config = ConfigUtils.loadConfig(scenarioOptions.getSimulationConfigName());
        System.out.println("MATSim config file: " + scenarioOptions.getSimulationConfigName());
        final File outputSubDirectory = new File(config.controler().getOutputDirectory()).getAbsoluteFile();
        if (!outputSubDirectory.isDirectory())
            throw new RuntimeException("output directory: " + outputSubDirectory.getAbsolutePath() + " not found.");
        System.out.println("outputSubDirectory=" + outputSubDirectory.getAbsolutePath());
        File outputDirectory = outputSubDirectory.getParentFile();
        System.out.println("showing simulation results from outputDirectory=" + outputDirectory);

        /** geographic information, .e.g., coordinate system */
        LocationSpec locationSpec = scenarioOptions.getLocationSpec();
        ReferenceFrame referenceFrame = locationSpec.referenceFrame();

        /** MATSim simulation network */
        Network network = NetworkLoader.fromConfigFile(new File(workingDirectory, scenarioOptions.getString("simuConfig")));
        System.out.println("INFO network loaded");
        System.out.println("INFO total links " + network.getLinks().size());
        System.out.println("INFO total nodes " + network.getNodes().size());

        /** initializing the viewer */
        MatsimAmodeusDatabase db = MatsimAmodeusDatabase.initialize(network, referenceFrame);
        amodeusComponent = AmodeusComponent.createDefault(db, workingDirectory);

        /** virtual network layer, should not cause problems if layer does not exist */
        amodeusComponent.virtualNetworkLayer.setVirtualNetwork(VirtualNetworkGet.readDefault(network, scenarioOptions));

        /** starting the viewer */
        viewerConfig = ViewerConfig.from(db, workingDirectory);
        System.out.println("Used viewer config: " + viewerConfig);
        AmodeusViewerFrame amodeusViewerFrame = new AmodeusViewerFrame(amodeusComponent, outputDirectory, network, scenarioOptions);
        amodeusViewerFrame.setDisplayPosition(viewerConfig.settings.coord, viewerConfig.settings.zoom);
        amodeusViewerFrame.jFrame.setSize(viewerConfig.settings.dimensions);
        amodeusViewerFrame.jFrame.setVisible(true);
    }

    public AmodeusComponent getAmodeusComponent() {
        return amodeusComponent;
    }

    public ViewerConfig getViewerConfig() {
        return viewerConfig;
    }
}
