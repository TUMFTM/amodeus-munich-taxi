/* amodeus - Copyright (c) 2018, ETH Zurich, Institute for Dynamic Systems and Control */
package amodeus.amodeus.traveldata;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

import java.io.File;
import java.io.IOException;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.matsim.amodeus.config.AmodeusConfigGroup;
import org.matsim.amodeus.config.modal.GeneratorConfig;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.population.Population;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.scenario.ScenarioUtils;

import amodeus.amodeus.options.ScenarioOptions;
import amodeus.amodeus.options.ScenarioOptionsBase;
import amodeus.amodeus.prep.PopulationArrivalRate;
import amodeus.amodeus.prep.Request;
import amodeus.amodeus.prep.VirtualNetworkCreator;
import amodeus.amodeus.test.TestFileHandling;
import amodeus.amodeus.util.io.Locate;
import amodeus.amodeus.util.io.MultiFileTools;
import amodeus.amodeus.util.math.GlobalAssert;
import amodeus.amodeus.virtualnetwork.core.VirtualNetwork;
import ch.ethz.idsc.tensor.Tensor;
import ch.ethz.idsc.tensor.Tensors;
import ch.ethz.idsc.tensor.alg.Array;

public class PopulationToolsTestVN2 {
    private static VirtualNetwork<Link> virtualNetwork2;
    private static ScenarioOptions scenarioOptions;
    private static Population population;
    private static Network network;
    private static Set<Request> requestsSingle3 = new HashSet<>();
    private static Set<Request> requestsEmpty = Collections.emptySet();
    private static Set<Request> requests3 = new HashSet<>();
    private static int endTime;

    @BeforeClass
    public static void setup() throws IOException {
        // copy scenario data into main directory
        File scenarioDirectory = new File(Locate.repoFolder(PopulationToolsTestVN2.class, "amodeus"), "resources/testScenario");
        File workingDirectory = MultiFileTools.getDefaultWorkingDirectory();
        GlobalAssert.that(workingDirectory.exists());
        TestFileHandling.copyScnearioToMainDirectory(scenarioDirectory.getAbsolutePath(), workingDirectory.getAbsolutePath());

        /* input data */
        scenarioOptions = new ScenarioOptions(scenarioDirectory, ScenarioOptionsBase.getDefault());
        File configFile = new File(scenarioOptions.getPreparerConfigName());
        AmodeusConfigGroup avCg = new AmodeusConfigGroup();
        Config config = ConfigUtils.loadConfig(configFile.getAbsolutePath(), avCg);
        GeneratorConfig genConfig = avCg.getModes().values().iterator().next().getGeneratorConfig();
        int numRt = genConfig.getNumberOfVehicles();
        endTime = (int) config.qsim().getEndTime().seconds();
        Scenario scenario = ScenarioUtils.loadScenario(config);
        network = scenario.getNetwork();
        population = scenario.getPopulation();

        // create 2 node virtual network
        scenarioOptions.setProperty(ScenarioOptionsBase.NUMVNODESIDENTIFIER, "2");
        VirtualNetworkCreator virtualNetworkCreator = scenarioOptions.getVirtualNetworkCreator();
        virtualNetwork2 = virtualNetworkCreator.create(network, population, scenarioOptions, numRt, endTime);

        Link node0 = (Link) virtualNetwork2.getVirtualNode(0).getLinks().toArray()[0];
        Link node1 = (Link) virtualNetwork2.getVirtualNode(1).getLinks().toArray()[0];

        requestsSingle3.add(new Request(10, node0, node1));
        requests3.add(new Request(0, node0, node1));
        requests3.add(new Request(3600, node1, node0));
        requests3.add(new Request(30 * 3600 - 1, node1, node0));
        requests3.add(new Request(3600, node0, node0));
    }

    @Test
    public void testInvalid() {
        try {
            PopulationArrivalRate.getVNodeAndInterval(requestsSingle3, virtualNetwork2, 3601, endTime);
            fail();
        } catch (Exception exception) {
            // ---
        }

        try {
            PopulationArrivalRate.getVNodeAndInterval(requestsSingle3, virtualNetwork2, -1, endTime);
            fail();
        } catch (Exception exception) {
            // ---
        }
    }

    @Test
    public void testEmpty() {
        Tensor lambda = PopulationArrivalRate.getVNodeAndInterval(requestsEmpty, virtualNetwork2, 3600, endTime);
        assertEquals(lambda, Array.zeros(30, 2, 2));
    }

    @Test
    public void testVirtualNetwork2() {
        Tensor lambda = PopulationArrivalRate.getVNodeAndInterval(requestsSingle3, virtualNetwork2, 15 * 3600, endTime);
        assertEquals(lambda, Tensors.of(Tensors.of(Tensors.vector(0, 1), Tensors.vector(0, 0)), Tensors.of(Tensors.vector(0, 0), Tensors.vector(0, 0))));

        lambda = PopulationArrivalRate.getVNodeAndInterval(requests3, virtualNetwork2, 15 * 3600, endTime);
        assertEquals(lambda, Tensors.of(Tensors.of(Tensors.vector(1, 1), Tensors.vector(1, 0)), Tensors.of(Tensors.vector(0, 0), Tensors.vector(1, 0))));
    }

    @AfterClass
    public static void tearDownOnce() throws IOException {
        TestFileHandling.removeGeneratedFiles();
    }
}
