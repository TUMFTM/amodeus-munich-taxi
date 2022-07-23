package de.tum.mw.ftm.amod.taxi.util;

import org.locationtech.jts.geom.Coordinate;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.network.Node;
import org.matsim.core.config.Config;
import org.matsim.core.config.groups.NetworkConfigGroup;
import org.matsim.core.network.NetworkUtils;
import org.matsim.core.utils.geometry.geotools.MGC;
import org.matsim.core.utils.gis.PointFeatureFactory;
import org.matsim.core.utils.gis.PolylineFeatureFactory;
import org.matsim.core.utils.gis.ShapeFileWriter;
import org.opengis.feature.simple.SimpleFeature;
import org.opengis.referencing.crs.CoordinateReferenceSystem;

import java.util.ArrayList;
import java.util.Collection;

public class NetworkToSHP {


    public static void main(String[] args) {
        final String NETWORK_SHAPE = args[0];
        Config config = AmodeusUtil.loadMatSimConfig();
        NetworkConfigGroup networkConfigGroup = config.network();
        Network network = NetworkUtils.readNetwork(networkConfigGroup.getInputFile());
        CoordinateReferenceSystem crs = MGC.getCRS("EPSG:32632");


        Collection<SimpleFeature> features = new ArrayList<>();
        PolylineFeatureFactory linkFactory = new PolylineFeatureFactory.Builder().
                setCrs(crs).
                setName("link").
                addAttribute("ID", String.class).
                addAttribute("fromID", String.class).
                addAttribute("toID", String.class).
                addAttribute("length", Double.class).
                addAttribute("type", String.class).
                addAttribute("capacity", Double.class).
                addAttribute("freespeed", Double.class).
                create();


        for (Link link : network.getLinks().values()) {
            Coordinate fromNodeCoordinate = new Coordinate(link.getFromNode().getCoord().getX(), link.getFromNode().getCoord().getY());
            Coordinate toNodeCoordinate = new Coordinate(link.getToNode().getCoord().getX(), link.getToNode().getCoord().getY());
            Coordinate linkCoordinate = new Coordinate(link.getCoord().getX(), link.getCoord().getY());
            SimpleFeature ft = linkFactory.createPolyline(new Coordinate[]{fromNodeCoordinate, linkCoordinate, toNodeCoordinate},
                    new Object[]{link.getId().toString(), link.getFromNode().getId().toString(), link.getToNode().getId().toString(), link.getLength(), NetworkUtils.getType(link), link.getCapacity(), link.getFreespeed()}, null);
            features.add(ft);
        }
        ShapeFileWriter.writeGeometries(features, NETWORK_SHAPE + "_links.shp");


        features = new ArrayList<>();
        PointFeatureFactory nodeFactory = new PointFeatureFactory.Builder().
                setCrs(crs).
                setName("nodes").
                addAttribute("ID", String.class).
                create();


        for (Node node : network.getNodes().values()) {
            SimpleFeature ft = nodeFactory.createPoint(node.getCoord(), new Object[]{node.getId().toString()}, null);
            features.add(ft);
        }
        ShapeFileWriter.writeGeometries(features, NETWORK_SHAPE + "_nodes.shp");
    }
}
