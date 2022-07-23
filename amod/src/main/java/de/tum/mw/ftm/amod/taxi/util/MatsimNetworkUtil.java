package de.tum.mw.ftm.amod.taxi.util;

import amodeus.amodeus.dispatcher.core.RoboTaxi;
import amodeus.amodeus.util.math.GlobalAssert;
import org.locationtech.jts.algorithm.ConvexHull;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.Polygon;
import org.matsim.amodeus.plpc.ParallelLeastCostPathCalculator;
import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.network.Node;
import org.matsim.contrib.dvrp.path.VrpPath;
import org.matsim.contrib.dvrp.path.VrpPathWithTravelData;
import org.matsim.contrib.dvrp.path.VrpPaths;
import org.matsim.core.network.NetworkUtils;
import org.matsim.core.router.util.LeastCostPathCalculator;
import org.matsim.core.router.util.TravelTime;
import org.matsim.core.utils.geometry.geotools.MGC;
import org.matsim.core.utils.gis.PolylineFeatureFactory;
import org.opengis.feature.simple.SimpleFeature;
import org.opengis.referencing.crs.CoordinateReferenceSystem;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.stream.Collectors;

public class MatsimNetworkUtil {
    public static Link getNearestLink(Link origin, List<Link> possibleLinks) {
        GlobalAssert.that(!possibleLinks.isEmpty());

        double distanceToNearestLink = Double.POSITIVE_INFINITY;
        Link nearestLink = null;
        for (Link link : possibleLinks) {
            double distanceToLink = airlineDistanceBetweenLinks(origin, link);
            if (distanceToLink < distanceToNearestLink) {
                distanceToNearestLink = distanceToLink;
                nearestLink = link;
            }
        }
        return nearestLink;
    }

    public static RoboTaxi getClosestRoboTaxi(Link origin, List<RoboTaxi> taxis) {
        GlobalAssert.that(!taxis.isEmpty());

        double distanceToNearestTaxi = Double.POSITIVE_INFINITY;
        RoboTaxi nearestTaxi = null;
        for (RoboTaxi taxi : taxis) {
            double distanceToTaxi = airlineDistanceBetweenLinks(origin, taxi.getLastKnownLocation());
            if (distanceToTaxi < distanceToNearestTaxi) {
                distanceToNearestTaxi = distanceToTaxi;
                nearestTaxi = taxi;
            }
        }
        return nearestTaxi;
    }

    public static List<Link> orderListByDistanceToLink(Link origin, List<Link> links) {
        links.sort((o1, o2) -> (int) (airlineDistanceBetweenLinks(o1, origin) - airlineDistanceBetweenLinks(o2, origin)));
        return links;
    }

    private static double airlineDistanceBetweenLinks(Link l1, Link l2) {
        double xDelta = l1.getCoord().getX() - l2.getCoord().getX();
        double yDelta = l1.getCoord().getY() - l2.getCoord().getY();

        return Math.sqrt(Math.pow(xDelta, 2) + Math.pow(yDelta, 2));
    }

    public static double roadDistanceBetweenLinks(Link l1, Link l2, double startTime,
                                                  ParallelLeastCostPathCalculator calculator, TravelTime travelTime) {
        Future<LeastCostPathCalculator.Path> pathFuture = calculator.calcLeastCostPath(l1.getToNode(), l2.getFromNode(), startTime, null, null);

        LeastCostPathCalculator.Path path;
        try {
            path = pathFuture.get();
        } catch (InterruptedException | ExecutionException e) {
            throw new RuntimeException(e);
        }

        VrpPathWithTravelData vrpPathWithTravelData = VrpPaths.createPath(l1, l2, startTime, path, travelTime);
        GlobalAssert.that(vrpPathIsConsistent(vrpPathWithTravelData));
        return VrpPaths.calcDistance(vrpPathWithTravelData);
    }

    public static boolean vrpPathIsConsistent(VrpPath vrpPath) {
        boolean status = true;
        Iterator<Link> iterator = vrpPath.iterator();
        Node node = iterator.next().getToNode();
        while (iterator.hasNext()) {
            Link link = iterator.next();
            status &= node.getId().equals(link.getFromNode().getId());
            node = link.getToNode();
        }
        return status;
    }

    public static Polygon getNetworkBoundaries(Network network){

        List<Coordinate> nodes = network.getNodes().values().stream().map(n->new Coordinate(n.getCoord().getX(), n.getCoord().getY())).collect(Collectors.toList());
        ConvexHull ch = new ConvexHull((Coordinate[]) nodes.toArray(), new GeometryFactory());
        return (Polygon) ch.getConvexHull();
    }
}
