package de.tum.mw.ftm.amod.taxi.preprocessing;

import amodeus.amodeus.util.matsim.NetworkLoader;
import de.tum.mw.ftm.amod.taxi.preprocessing.network.NetworkCutter;
import org.junit.Before;
import org.junit.Test;
import org.matsim.amodeus.config.FTMConfigGroup;
import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.network.Network;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.config.groups.NetworkConfigGroup;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;

import static org.junit.Assert.assertTrue;

public class NetworkCutterTest {
    private static Network network;
    private int nodesBefore;

    @Before
    public void setup() {
        Config config = ConfigUtils.loadConfig("src/test/resources/config_full.xml", new FTMConfigGroup());
        NetworkConfigGroup networkConfigGroup = config.network();
        network = NetworkLoader.fromNetworkFile(new File("src/test/resources", networkConfigGroup.getInputFile()));
        nodesBefore = network.getNodes().size();
    }

    @Test
    public void cutCircle() {
        NetworkCutter networkCutter = new NetworkCutter(new Coord(690982.68, 5335584.15), 2500);
        networkCutter.run(network);
        assertTrue(nodesBefore > network.getNodes().size());

    }

    @Test
    public void cutRectangle() {
        NetworkCutter networkCutter = new NetworkCutter(new Coord(690951.06, 5335095.62), new Coord(692108.45, 5334509.83));
        networkCutter.run(network);
        assertTrue(nodesBefore > network.getNodes().size());
    }

    @Test
    public void cutPolygon() throws IOException {
        Path path = Paths.get("src", "test", "resources", "district_university.geojson");
        NetworkCutter networkCutter = NetworkCutter.fomGeoJSON(path.toString());
        networkCutter.run(network);
        final File fileExport = new File("network_polygoncut" + ".xml");
        assertTrue(nodesBefore > network.getNodes().size());
    }

}