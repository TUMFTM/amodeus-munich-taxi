package de.tum.mw.ftm.amod.taxi.util;


import amodeus.amodeus.dispatcher.core.RoboTaxi;
import amodeus.amodeus.options.ScenarioOptions;
import amodeus.amodeus.options.ScenarioOptionsBase;
import amodeus.amodeus.util.io.MultiFileTools;
import amodeus.amodeus.util.math.GlobalAssert;
import com.google.common.annotations.VisibleForTesting;
import de.tum.mw.ftm.amod.geom.GridCell;
import de.tum.mw.ftm.amod.taxi.preprocessing.ranks.TaxiRank;
import org.apache.commons.lang3.ArrayUtils;
import org.locationtech.jts.geom.*;
import org.locationtech.jts.geom.impl.CoordinateArraySequence;
import org.locationtech.jts.operation.distance.DistanceOp;
import org.locationtech.jts.operation.union.CascadedPolygonUnion;
import org.matsim.amodeus.config.AmodeusConfigGroup;
import org.matsim.amodeus.config.FTMConfigGroup;
import org.matsim.amodeus.dvrp.schedule.AmodeusDriveTask;
import org.matsim.amodeus.dvrp.schedule.AmodeusDropoffTask;
import org.matsim.amodeus.dvrp.schedule.AmodeusPickupTask;
import org.matsim.amodeus.dvrp.schedule.AmodeusStayTask;
import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.network.Link;
import org.matsim.contrib.dvrp.schedule.Schedule;
import org.matsim.contrib.dvrp.schedule.Task;
import org.matsim.contrib.zone.Zone;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;

import javax.swing.border.Border;
import java.io.File;
import java.io.IOException;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.stream.Collectors;

public class AmodeusUtil {
    public enum BorderOrientation {
        NORTH,
        EAST,
        SOUTH,
        WEST
    }

    public static RoboTaxi getLongestWaitingTaxi(List<RoboTaxi> taxis) {
        GlobalAssert.that(!taxis.isEmpty());
        GlobalAssert.that(taxis.stream().allMatch(RoboTaxi::isInStayTask));

        RoboTaxi longestWaitingTaxi = null;
        double longestWaitTime = Double.NEGATIVE_INFINITY;
        for (RoboTaxi taxi : taxis) {
            double waitTime = taxi.getSchedule().getCurrentTask().getBeginTime();
            if (waitTime > longestWaitTime) {
                longestWaitingTaxi = taxi;
                longestWaitTime = waitTime;
            }
        }
        return longestWaitingTaxi;
    }

    public static List<RoboTaxi> getTaxisInZone(Collection<RoboTaxi> taxis, Zone zone) {
        return taxis.stream().filter(rt -> !rt.isAtTaxiRank())
                .filter(rt -> {
                    Coord lastKnownCoordinate = rt.getLastKnownLocation().getCoord();
                    MultiPolygon zoneMultiPolygon = zone.getMultiPolygon();
                    Point point = matsimCoordToPoint(lastKnownCoordinate);
                    return zoneMultiPolygon.contains(point);
                }).collect(Collectors.toList());
    }

    public static int[] getCoordsPerCell(List<Coord> coords, GridCell[] cells) {
        List<Coord> copiedCoords = new ArrayList<>(coords);
        
        int[] coordsPerCell = new int[cells.length];
        Arrays.fill(coordsPerCell, 0);

        List<Coord> handledCoords = new ArrayList<>();
        for (int i = 0; i < cells.length; i++) {
            for (Coord coord : copiedCoords) {
                if (cells[i].contains(coord.getX(), coord.getY())) {
                    coordsPerCell[i] += 1;
                    handledCoords.add(coord);
                }
            }
            copiedCoords.removeAll(handledCoords);

            if (copiedCoords.isEmpty()) {
                break;
            }

            handledCoords.clear();
        }

        return coordsPerCell;
    }

    public static int[] getNumberOfTaxisInsideCells(Collection<RoboTaxi> taxis, GridCell[] cells){
        List<List<RoboTaxi>> taxisPerCell = AmodeusUtil.getTaxisInsideCells(taxis, cells);
        return ArrayUtils.toPrimitive(taxisPerCell.stream().map(List::size).toArray(Integer[]::new));
    }

    public static List<List<RoboTaxi>> getTaxisInsideCells(Collection<RoboTaxi> taxis, GridCell[] cells) {
        List<RoboTaxi> copiedTaxis = new ArrayList<>(taxis);
        List<List<RoboTaxi>> taxisInsideCells = new ArrayList<>();

        for (GridCell cell : cells) {
            List<RoboTaxi> taxisInCurrentCell = new ArrayList<>();
            for (RoboTaxi taxi : copiedTaxis) {
                Coord coord = taxi.getDivertableLocation().getCoord();
                if (cell.contains(coord.getX(), coord.getY())) {
                    taxisInCurrentCell.add(taxi);
                }
            }
            copiedTaxis.removeAll(taxisInCurrentCell);
            taxisInsideCells.add(taxisInCurrentCell);
        }
        GlobalAssert.that(taxisInsideCells.size() == cells.length);
        return taxisInsideCells;
    }
    public static List<List<RoboTaxi>> getTaxisByTargetCells(Collection<RoboTaxi> taxis, GridCell[] cells) {
        List<RoboTaxi> copiedTaxis = new ArrayList<>(taxis);
        List<List<RoboTaxi>> taxisWithCellAsTarget  = new ArrayList<>();

        for (GridCell cell : cells) {
            List<RoboTaxi> taxisInCurrentCell = new ArrayList<>();
            for (RoboTaxi taxi : copiedTaxis) {
                Coord coord = taxi.getCurrentDriveDestination().getCoord();
                if (cell.contains(coord.getX(), coord.getY())) {
                    taxisInCurrentCell.add(taxi);
                }
            }
            copiedTaxis.removeAll(taxisInCurrentCell);
            taxisWithCellAsTarget .add(taxisInCurrentCell);
        }
        GlobalAssert.that(taxisWithCellAsTarget .size() == cells.length);
        return taxisWithCellAsTarget ;
    }

    public static GridCell[] createEvenSpacedGrid(double minX, double maxX, double minY, double maxY,
                                                  int numberOfColumns, int numberOfRows) {
        GlobalAssert.that(maxX > minX);
        GlobalAssert.that(maxY > minY);

        GridCell[] grid = new GridCell[numberOfRows * numberOfColumns];
        double cellWidth = (maxX - minX) / numberOfColumns;
        double cellHeight = (maxY - minY) / numberOfRows;

        for (int row = 0; row < numberOfRows; row++) {
            for (int column = 0; column < numberOfColumns; column++) {
                double cellMinX = minX + cellWidth * column;
                double cellMaxX = minX + cellWidth * (column + 1);
                double cellMaxY = maxY - cellHeight * row;
                double cellMinY = maxY - cellHeight * (row + 1);
                String id = String.format("(%d, %d)", row, column);
                GridCell gridCell = new GridCell(cellMinX, cellMaxX, cellMinY, cellMaxY,id);
                grid[row * numberOfColumns + column] = gridCell;
            }
        }

        return grid;
    }

    public static int[] getNumberOfFutureTaxisArrivingInGridCell(Collection<RoboTaxi> taxis, GridCell[] cells,
                                                                 long lowerTimeLimit, long upperTimeLimit) {
        List<Coord> destinationCoordsOfTaxisArrivingBetweenLimits = taxis.stream().filter(roboTaxi -> roboTaxi.getSchedule().getCurrentTask().getEndTime() >= lowerTimeLimit
                && roboTaxi.getSchedule().getCurrentTask().getEndTime() < upperTimeLimit && !(roboTaxi.getSchedule().getCurrentTask() instanceof AmodeusPickupTask)).map(roboTaxi -> roboTaxi.getCurrentDriveDestination().getCoord()).collect(Collectors.toList());

        return getCoordsPerCell(destinationCoordsOfTaxisArrivingBetweenLimits, cells);
    }
    public static long getNumberOfFutureTaxisArrivingInPolygon(Collection<RoboTaxi> taxis, Polygon polygon,
                                                                 long lowerTimeLimit, long upperTimeLimit) {
        List<Coord> destinationCoordsOfTaxisArrivingBetweenLimits = taxis.stream().filter(roboTaxi -> roboTaxi.getSchedule().getCurrentTask().getEndTime() >= lowerTimeLimit
                && roboTaxi.getSchedule().getCurrentTask().getEndTime() < upperTimeLimit && !(roboTaxi.getSchedule().getCurrentTask() instanceof AmodeusPickupTask)).map(roboTaxi -> roboTaxi.getCurrentDriveDestination().getCoord()).collect(Collectors.toList());

        return destinationCoordsOfTaxisArrivingBetweenLimits.stream().filter(c-> polygon.contains(matsimCoordToPoint(c))).count();
    }



    public static Point matsimCoordToPoint(Coord coord) {
        GeometryFactory geometryFactory = new GeometryFactory();
        return new Point(new CoordinateArraySequence(new Coordinate[]{
                new Coordinate(coord.getX(), coord.getY())}), geometryFactory);
    }

    public static Config loadMatSimConfig() {
        try {
            ScenarioOptions scenarioOptions = new ScenarioOptions(MultiFileTools.getDefaultWorkingDirectory(),
                    ScenarioOptionsBase.getDefault());
            File configFile = new File(scenarioOptions.getPreparerConfigName());
            GlobalAssert.that(configFile.exists());
            Config config = ConfigUtils.loadConfig(configFile.toString(), new FTMConfigGroup(), new AmodeusConfigGroup());
            checkConfigConsistency(config);
            return config;
        } catch (IOException e) {
            System.err.println("Cannot load scenarioOptions. Please check that you've set the correct working directory!\n" +
                    "Will return empty config!");
            e.printStackTrace();
            return new Config();
        }
    }

    public static FTMConfigGroup loadFTMConfigGroup() {
        Config config = loadMatSimConfig();
        return (FTMConfigGroup) config.getModules().get("ftm_simulation");
    }

    public static void checkConfigConsistency(Config config) {
        FTMConfigGroup ftmConfigGroup = (FTMConfigGroup) config.getModules().get("ftm_simulation");
        GlobalAssert.that(ftmConfigGroup.getSimEndDateTime().isAfter(ftmConfigGroup.getSimStartDateTime()));
        long ftmConfigTime = ChronoUnit.SECONDS.between(ftmConfigGroup.getSimStartDateTime(),
                ftmConfigGroup.getSimEndDateTime()) + ftmConfigGroup.getSimEndTimeBufferSeconds();
        long qsimConfigTime = (long) (config.qsim().getEndTime().seconds() - config.qsim().getStartTime().seconds());
        GlobalAssert.that(ftmConfigTime == qsimConfigTime);
    }


    public static List<RoboTaxi> getNumberOfTaxisInBorderZone(BorderOrientation orientation, double borderWidth, double gridMinX,
                                             double gridMaxX, double gridMinY, double gridMaxY, Collection<RoboTaxi> taxis) {
        GeometryFactory geometryFactory = new GeometryFactory();
        Polygon polygon = getBorderPolygon(orientation, borderWidth, gridMinX, gridMaxX, gridMinY, gridMaxY, geometryFactory);

        return taxis.stream().filter(rt -> polygon.contains(matsimCoordToPoint(rt.getLastKnownLocation().getCoord()))).collect(Collectors.toList());
    }

    public static List<RoboTaxi> getTaxisInPolygon(Polygon polygon, Collection<RoboTaxi> taxis) {
        return taxis.stream().filter(rt -> polygon.contains(matsimCoordToPoint(rt.getLastKnownLocation().getCoord()))).collect(Collectors.toList());
    }

    public static long getNumberOfTaxisInPolygon(Polygon polygon, Collection<RoboTaxi>taxis){
        return taxis.stream().filter(rt -> polygon.contains(matsimCoordToPoint(rt.getLastKnownLocation().getCoord()))).count();
    }
    public static Map<BorderOrientation, Collection<RoboTaxi>> getTaxisByBorderZone(Map<BorderOrientation, Polygon> borders, Collection<RoboTaxi> roboTaxis){
        Map<BorderOrientation, Collection<RoboTaxi>> resultMap = new HashMap<>();
        for(Map.Entry<BorderOrientation, Polygon> border : borders.entrySet()){
            List<RoboTaxi> roboTaxisInBorder = roboTaxis.stream().filter(t -> border.getValue().contains(matsimCoordToPoint(t.getDivertableLocation().getCoord()))).collect(Collectors.toList());
            resultMap.put(border.getKey(), roboTaxisInBorder);
        }
        return resultMap;
    }


    @VisibleForTesting
    public static Polygon getBorderPolygon(BorderOrientation orientation, double borderWidth, double gridMinX,
                                     double gridMaxX, double gridMinY, double gridMaxY, GeometryFactory geometryFactory) {
        Coordinate[] coordinates = new Coordinate[5];

        if (orientation == BorderOrientation.NORTH) {
            coordinates[0] = new Coordinate(gridMinX, gridMaxY);
            coordinates[1] = new Coordinate(gridMaxX, gridMaxY);
            coordinates[2] = new Coordinate(gridMaxX + borderWidth, gridMaxY + borderWidth);
            coordinates[3] = new Coordinate(gridMinX - borderWidth, gridMaxY + borderWidth);
        } else if (orientation == BorderOrientation.EAST) {
            coordinates[0] = new Coordinate(gridMaxX, gridMinY);
            coordinates[1] = new Coordinate(gridMaxX + borderWidth, gridMinY - borderWidth);
            coordinates[2] = new Coordinate(gridMaxX + borderWidth, gridMaxY + borderWidth);
            coordinates[3] = new Coordinate(gridMaxX, gridMaxY);
        } else if (orientation == BorderOrientation.SOUTH) {
            coordinates[0] = new Coordinate(gridMinX - borderWidth, gridMinY - borderWidth);
            coordinates[1] = new Coordinate(gridMaxX + borderWidth, gridMinY - borderWidth);
            coordinates[2] = new Coordinate(gridMaxX, gridMinY);
            coordinates[3] = new Coordinate(gridMinX, gridMinY);
        } else if (orientation == BorderOrientation.WEST) {
            coordinates[0] = new Coordinate(gridMinX - borderWidth, gridMinY - borderWidth);
            coordinates[1] = new Coordinate(gridMinX, gridMinY);
            coordinates[2] = new Coordinate(gridMinX, gridMaxY);
            coordinates[3] = new Coordinate(gridMinX - borderWidth, gridMaxY + borderWidth);
        } else {
            throw new IllegalArgumentException();
        }
        coordinates[4] = coordinates[0];

        return geometryFactory.createPolygon(coordinates);
    }

    public static GridCell getClosestGridCell(GridCell[] gridCells, Coord location) {
        double minDistance = Double.POSITIVE_INFINITY;
        GridCell closestCell = null;
        Coordinate coordinate = new Coordinate(location.getX(), location.getY());
        for (GridCell gridCell : gridCells) {
            double distance = gridCell.distance(new Envelope(coordinate));
            if (distance < minDistance) {
                minDistance = distance;
                closestCell = gridCell;
            }
        }
        return closestCell;
    }

    public static Map.Entry<BorderOrientation, Map.Entry<Polygon, Coordinate>> getClosestBorderPolygon(Map<BorderOrientation, Polygon> polygons, Coord location) {
        double minDistance = Double.POSITIVE_INFINITY;
        Map.Entry<BorderOrientation, Map.Entry<Polygon, Coordinate>> closestPolygonPair = null;
        for (Map.Entry<BorderOrientation, Polygon> polygonEntry : polygons.entrySet()) {
            Point point = matsimCoordToPoint(location);
            Coordinate[] points = DistanceOp.nearestPoints(polygonEntry.getValue(), point);
            double distance = polygonEntry.getValue().distance(point);
            if (distance < minDistance) {
                minDistance = distance;
                closestPolygonPair = new AbstractMap.SimpleEntry<>(polygonEntry.getKey(),
                        new AbstractMap.SimpleEntry<>(polygonEntry.getValue(), points[0]));
            }
        }

        return closestPolygonPair;
    }

    public static List<Map.Entry<RoboTaxi, Coordinate>> getMaxNClosestTaxis(Polygon polygon, List<RoboTaxi> taxis, int n) {
        Map<RoboTaxi, Map.Entry<Coordinate, Double>> distanceMap = new HashMap<>();
        for (RoboTaxi taxi : taxis) {
            Point point = matsimCoordToPoint(taxi.getDivertableLocation().getCoord());
            Coordinate[] points = DistanceOp.nearestPoints(polygon, point);
            double distance = polygon.distance(point);
            GlobalAssert.that(distance >= 0);
            distanceMap.put(taxi, new AbstractMap.SimpleEntry<>(points[0], distance));
        }

        return distanceMap.entrySet().stream()
                .sorted((e1, e2) -> e2.getValue().getValue().compareTo(e1.getValue().getValue()))
                .map(entry -> new AbstractMap.SimpleEntry<>(entry.getKey(), entry.getValue().getKey()))
                .limit(n)
                .collect(Collectors.toList());
    }

    public static Optional<RoboTaxi> getClosestTaxiToLink(Link link, List<RoboTaxi> taxis) {
        double minDistance = Double.POSITIVE_INFINITY;
        Optional<RoboTaxi> closestTaxi = Optional.empty();
        Point linkPoint = matsimCoordToPoint(link.getCoord());
        for (RoboTaxi taxi : taxis) {
            Point taxiPoint = matsimCoordToPoint(taxi.getDivertableLocation().getCoord());
            double distance = linkPoint.distance(taxiPoint);
            GlobalAssert.that(distance >= 0);
            if (distance < minDistance) {
                minDistance = distance;
                closestTaxi = Optional.of(taxi);
            }
        }

        return closestTaxi;
    }

    public static List<RoboTaxi> getTaxisOutsideOfGridAndBorders(GridCell[] gridCells, List<Polygon> borderPolygons,
                                                                 Collection<RoboTaxi> taxis) {
        GeometryFactory geometryFactory = new GeometryFactory();

        List<Polygon> combinedPolygons = new ArrayList<>();
        for (GridCell gridCell : gridCells) {
            combinedPolygons.add(
                    geometryFactory.createPolygon(geometryFactory.toGeometry(gridCell).getCoordinates())
            );
        }
        combinedPolygons.addAll(borderPolygons);

        Geometry unionedGeometry = CascadedPolygonUnion.union(combinedPolygons);

        return taxis.stream()
                .filter(taxi -> !unionedGeometry.contains(matsimCoordToPoint(taxi.getDivertableLocation().getCoord())))
                .collect(Collectors.toList());
    }

    public static TaxiRank getRandomTaxiRank(Collection<TaxiRank> ranks, Random random){
        float totalWeight = 0.f;
        NavigableMap<Float, TaxiRank> probabilityMap = new TreeMap<>();
        for (TaxiRank rank : ranks){
            totalWeight += rank.getPopularity();
            probabilityMap.put(totalWeight, rank);
        }

        float randomChoice = random.nextFloat() * totalWeight;
        return probabilityMap.higherEntry(randomChoice).getValue();
    }

    public static TaxiRank getRandomFreeTaxiRank(Collection<TaxiRank> ranks, Random random){
        float totalWeight = 0.f;
        NavigableMap<Float, TaxiRank> probabilityMap = new TreeMap<>();
        for (TaxiRank rank : ranks.stream().filter(r -> (r.getCapacity()-r.getNumberOfTaxis() >0)).collect(Collectors.toList())){
            totalWeight += rank.getPopularity();
            probabilityMap.put(totalWeight, rank);
        }

        float randomChoice = random.nextFloat() * totalWeight;
        return probabilityMap.higherEntry(randomChoice).getValue();
    }
}
