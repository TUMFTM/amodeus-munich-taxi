package de.tum.mw.ftm.amod.taxi.preprocessing.network;


import org.apache.log4j.Logger;
import org.geotools.geojson.geom.GeometryJSON;
import org.locationtech.jts.geom.*;
import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.network.Node;
import org.matsim.core.api.internal.NetworkRunnable;
import org.matsim.core.network.algorithms.NetworkScenarioCut;

import java.io.*;
import java.nio.charset.StandardCharsets;

import java.util.Set;
import java.util.stream.Collectors;

public class NetworkCutter implements NetworkRunnable {

    private final static Logger log = Logger.getLogger(NetworkScenarioCut.class);
    private final static Logger logger = Logger.getLogger(NetworkCutter.class);
    private final MultiPolygon polygon;
    private GeometryFactory geometryFactory = new GeometryFactory();


    public NetworkCutter(final Coord coord1, final Coord coord2) {
        super();
        Polygon[] singlePolygon = new Polygon[1];
        singlePolygon[0] = (Polygon) geometryFactory.toGeometry(new Envelope(coord1.getX(), coord2.getX(), coord1.getY(), coord2.getY()));

        this.polygon = geometryFactory.createMultiPolygon(singlePolygon);
    }

    public NetworkCutter(final Coord center, final double radius) {
        super();
        Polygon[] singlePolygon = new Polygon[1];
        singlePolygon[0] = (Polygon) geometryFactory.createPoint(new Coordinate(center.getX(), center.getY())).buffer(radius);
        this.polygon = geometryFactory.createMultiPolygon(singlePolygon);
    }

    public NetworkCutter(final MultiPolygon polygon) {
        this.polygon = polygon;
    }

    public static NetworkCutter fomGeoJSON(String geoJSONFile) throws IOException {
        logger.warn("NettworkCutter assumes that all given Coordinates match to the same CRS");
        logger.info("Creating network polygon from " + geoJSONFile + "...");
        GeometryJSON g = new GeometryJSON();
        // read in geojson file with utf-8 encoding
        File fileDir = new File(geoJSONFile);
        BufferedReader in = new BufferedReader(
                new InputStreamReader(
                        new FileInputStream(fileDir), StandardCharsets.UTF_8));
        return new NetworkCutter(g.readMultiPolygon(in));
    }

    @Override
    public void run(final Network network) {

        Set<Node> nodesToRemove = network.getNodes().values().stream().filter(
                n -> !polygon.contains(geometryFactory.createPoint(coordsToCoordinate(n.getCoord()))))
                .collect(Collectors.toSet());

        int nofLinksRemoved = 0;
        for (Node n : nodesToRemove) {
            nofLinksRemoved += n.getInLinks().size() + n.getOutLinks().size();
            network.removeNode(n.getId());
        }
        log.info("number of nodes removed: " + nodesToRemove.size());
        log.info("number of links removed: " + nofLinksRemoved);
        log.info("number of nodes remaining: " + network.getNodes().size());
        log.info("number of links remaining: " + network.getLinks().size());
    }

    private Coordinate coordsToCoordinate(Coord coord) {
        return new Coordinate(coord.getX(), coord.getY());
    }


}
