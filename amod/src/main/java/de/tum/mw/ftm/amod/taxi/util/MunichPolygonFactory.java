package de.tum.mw.ftm.amod.taxi.util;

import de.tum.mw.ftm.amod.taxi.preprocessing.demand.TaxiRide;
import org.apache.log4j.Logger;
import org.locationtech.jts.geom.*;
import org.matsim.api.core.v01.Coord;
import org.matsim.core.utils.geometry.CoordinateTransformation;
import org.matsim.core.utils.geometry.transformations.TransformationFactory;

import java.util.ArrayList;
import java.util.List;

public class MunichPolygonFactory {

    private final static GeometryFactory geometryFactory = new GeometryFactory();
    private final static Logger logger = Logger.getLogger(MunichPolygonFactory.class);



    public static Polygon getMunichAirportPolygon(String CRS) {
        CoordinateTransformation targetCrs_WGS84 = TransformationFactory.getCoordinateTransformation(CRS, "EPSG:4326");
        CoordinateTransformation WGS84_targetCRS = TransformationFactory.getCoordinateTransformation("EPSG:4326", CRS);

        GeometryFactory geometryFactory = new GeometryFactory();

        ArrayList<Coord> originalPoints = new ArrayList<>();
        originalPoints.add(WGS84_targetCRS.transform(new Coord(11.8375824008011, 48.3707330053954)));
        originalPoints.add(WGS84_targetCRS.transform(new Coord(11.8482412369276, 48.3333382553183)));
        originalPoints.add(WGS84_targetCRS.transform(new Coord(11.8350065154039, 48.3317838417165)));
        originalPoints.add(WGS84_targetCRS.transform(new Coord(11.831808864566, 48.3431532669181)));
        originalPoints.add(WGS84_targetCRS.transform(new Coord(11.7389881666311, 48.3368467888766)));
        originalPoints.add(WGS84_targetCRS.transform(new Coord(11.7445840555975, 48.3641600564507)));
        originalPoints.add(WGS84_targetCRS.transform(new Coord(11.8375824008011, 48.3707330053954)));
        originalPoints.add(WGS84_targetCRS.transform(new Coord(11.8065567819475, 48.3618580439262)));
        ArrayList<Coordinate> transformedPoints = new ArrayList<>();

        for (Coord point : originalPoints) {
            transformedPoints.add(new Coordinate(point.getX(), point.getY()));
        }

        return geometryFactory.createPolygon(transformedPoints.toArray(new Coordinate[]{}));
    }

    private static boolean isRideFromAirport(TaxiRide ride) {
        // check if the x and y coordinates of the start point of that ride are within the airport area
        double x = ride.getLocationStart().getX();
        double y = ride.getLocationStart().getY();
        Point point = geometryFactory.createPoint(new Coordinate(x, y));
        return getMunichAirportPolygon("EPSG:4326").contains(point);
    }

    public static List<TaxiRide> eliminateAirportTracks(List<TaxiRide> rides) {
        int ridesBeforeFilter = rides.size();
        rides.removeIf(MunichPolygonFactory::isRideFromAirport);
        logger.info("Removed " + (ridesBeforeFilter - rides.size()) + " rides because they start at the airport.");
        return rides;
    }

}
