package de.tum.mw.ftm.amod.taxi.preprocessing.network;


import amodeus.amod.ext.UserReferenceFrames;
import de.tum.mw.ftm.amod.taxi.util.AmodeusUtil;
import org.matsim.amodeus.config.FTMConfigGroup;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.core.config.Config;
import org.matsim.core.config.groups.NetworkConfigGroup;
import org.matsim.core.network.NetworkUtils;
import org.matsim.core.network.algorithms.NetworkCleaner;
import org.matsim.core.network.io.NetworkWriter;
import org.matsim.core.utils.geometry.CoordinateTransformation;
import org.matsim.core.utils.io.OsmNetworkReader;

import java.io.File;
import java.util.Map;


public class ConvertOSMNetwork {
    public static void main(String[] args) {
        CoordinateTransformation coordinateTransformation = UserReferenceFrames.MUNICH.coords_fromWGS84();

        String osmFile = args[0];
        Config config = AmodeusUtil.loadMatSimConfig();
        NetworkConfigGroup networkConfigGroup = config.network();

        Network network = NetworkUtils.createNetwork();
        OsmNetworkReader reader = new OsmNetworkReader(network, coordinateTransformation, false, false);

        double freespeedFactor = 0.7;

        reader.setHighwayDefaults(1, "motorway", 2.0D, 33.333333333333336D, freespeedFactor, 2000.0D, true);
        reader.setHighwayDefaults(1, "motorway_link", 1.0D, 22.22222222222222D, freespeedFactor, 1500.0D, true);
        reader.setHighwayDefaults(2, "trunk", 1.0D, 22.22222222222222D, freespeedFactor, 2000.0D);
        reader.setHighwayDefaults(2, "trunk_link", 1.0D, 13.88888888888889D, freespeedFactor, 1500.0D);
        reader.setHighwayDefaults(3, "primary", 1.0D, 22.22222222222222D, freespeedFactor, 1500.0D);
        reader.setHighwayDefaults(3, "primary_link", 1.0D, 16.666666666666668D, freespeedFactor, 1500.0D);
        reader.setHighwayDefaults(4, "secondary", 1.0D, 8.333333333333334D, freespeedFactor, 1000.0D);
        reader.setHighwayDefaults(4, "secondary_link", 1.0D, 8.333333333333334D, freespeedFactor, 1000.0D);
        reader.setHighwayDefaults(5, "tertiary", 1.0D, 6.944444444444445D, freespeedFactor, 600.0D);
        reader.setHighwayDefaults(5, "tertiary_link", 1.0D, 6.944444444444445D, freespeedFactor, 600.0D);
        reader.setHighwayDefaults(6, "unclassified", 1.0D, 4.166666666666667D, freespeedFactor, 600.0D);
        reader.setHighwayDefaults(7, "residential", 1.0D, 4.166666666666667D, freespeedFactor, 600.0D);
        reader.setHighwayDefaults(8, "living_street", 1.0D, 2.7777777777777777D, freespeedFactor, 300.0D);

        reader.setScaleMaxSpeed(true);
        reader.setKeepPaths(true);
        reader.setMemoryOptimization(true);
        reader.parse(osmFile);
        Map<Id<Link>, ? extends Link> map = network.getLinks();
        for (Link link : map.values()) {
            double oldLanes = link.getNumberOfLanes();
            if (oldLanes < 1) {
                link.setNumberOfLanes(1);
                link.setCapacity(link.getCapacity() / oldLanes);
            }
        }

        // new NetworkSimplifier().run(network);
        new NetworkCleaner().run(network);
        new NetworkWriter(network).write(networkConfigGroup.getInputFile());

    }
}
