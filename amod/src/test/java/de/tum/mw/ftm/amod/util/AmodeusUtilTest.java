package de.tum.mw.ftm.amod.util;

import amodeus.amodeus.dispatcher.core.RoboTaxi;
import de.tum.mw.ftm.amod.geom.GridCell;
import de.tum.mw.ftm.amod.taxi.util.AmodeusUtil;
import org.junit.BeforeClass;
import org.junit.Test;
import org.locationtech.jts.geom.Envelope;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.Polygon;
import org.matsim.api.core.v01.BasicLocation;
import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.network.Link;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Random;
import java.util.stream.Collectors;

import static org.junit.Assert.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class AmodeusUtilTest {
    private static double minX;
    private static double maxX;
    private static double minY;
    private static double maxY;
    private static double allowedDoubleDeviation;
    private static Random random;

    @BeforeClass
    public static void setupOnce() {
        minX = 687412;
        maxX = 695412;
        minY = 5331314;
        maxY = 5339314;
        allowedDoubleDeviation = 0.001;
        random = new Random(42);
    }

    @Test
    public void createEvenSpacedGridSameNumberOfRowsAndColumns() {
        int numberOfColumns = 27;
        int numberOfRows = 22;

        double cellWidth = (maxX - minX) / numberOfColumns;
        double cellHeight = (maxY - minY) / numberOfRows;


        Envelope outerEnvelope = new Envelope(minX, maxX, minY, maxY);

        Envelope[] grid = AmodeusUtil.createEvenSpacedGrid(minX, maxX, minY, maxY, numberOfColumns, numberOfRows);

        assertEquals(numberOfColumns * numberOfRows, grid.length);

        for (int row = 0; row < numberOfRows; row++) {
            double currentMaxY = maxY - row * cellHeight;
            double currentMinY = maxY - (row + 1) * cellHeight;
            for (int column = 0; column < numberOfColumns; column++) {
                double currentMinX = minX + column * cellWidth;
                double currentMaxX = minX + (column + 1) * cellWidth;

                Envelope currentCell = grid[row * numberOfColumns + column];

                assertEquals(cellWidth * cellHeight, currentCell.getArea(), allowedDoubleDeviation);
                assertTrue(outerEnvelope.contains(currentCell));
                assertEquals(currentCell.getMinX(), currentMinX, allowedDoubleDeviation);
                assertEquals(currentCell.getMaxX(), currentMaxX, allowedDoubleDeviation);
                assertEquals(currentCell.getMinY(), currentMinY, allowedDoubleDeviation);
                assertEquals(currentCell.getMaxY(), currentMaxY, allowedDoubleDeviation);
            }
        }
    }

    @Test
    public void createEvenSpacedGridMoreRowsThanColumns() {
        int numberOfColumns = 17;
        int numberOfRows = 33;

        double cellWidth = (maxX - minX) / numberOfColumns;
        double cellHeight = (maxY - minY) / numberOfRows;

        Envelope outerEnvelope = new Envelope(minX, maxX, minY, maxY);

        Envelope[] grid = AmodeusUtil.createEvenSpacedGrid(minX, maxX, minY, maxY, numberOfColumns, numberOfRows);

        assertEquals(numberOfColumns * numberOfRows, grid.length);

        for (int row = 0; row < numberOfRows; row++) {
            double currentMaxY = maxY - row * cellHeight;
            double currentMinY = maxY - (row + 1) * cellHeight;
            for (int column = 0; column < numberOfColumns; column++) {
                double currentMinX = minX + column * cellWidth;
                double currentMaxX = minX + (column + 1) * cellWidth;

                Envelope currentCell = grid[row * numberOfColumns + column];

                assertEquals(cellWidth * cellHeight, currentCell.getArea(), allowedDoubleDeviation);
                assertTrue(outerEnvelope.contains(currentCell));
                assertEquals(currentCell.getMinX(), currentMinX, allowedDoubleDeviation);
                assertEquals(currentCell.getMaxX(), currentMaxX, allowedDoubleDeviation);
                assertEquals(currentCell.getMinY(), currentMinY, allowedDoubleDeviation);
                assertEquals(currentCell.getMaxY(), currentMaxY, allowedDoubleDeviation);
            }
        }
    }

    @Test
    public void createEvenSpacedGridMoreColumnsThanRows() {
        int numberOfColumns = 50;
        int numberOfRows = 20;

        double cellWidth = (maxX - minX) / numberOfColumns;
        double cellHeight = (maxY - minY) / numberOfRows;

        Envelope outerEnvelope = new Envelope(minX, maxX, minY, maxY);

        Envelope[] grid = AmodeusUtil.createEvenSpacedGrid(minX, maxX, minY, maxY, numberOfColumns, numberOfRows);

        assertEquals(numberOfColumns * numberOfRows, grid.length);

        for (int row = 0; row < numberOfRows; row++) {
            double currentMaxY = maxY - row * cellHeight;
            double currentMinY = maxY - (row + 1) * cellHeight;
            for (int column = 0; column < numberOfColumns; column++) {
                double currentMinX = minX + column * cellWidth;
                double currentMaxX = minX + (column + 1) * cellWidth;

                Envelope currentCell = grid[row * numberOfColumns + column];

                assertEquals(cellWidth * cellHeight, currentCell.getArea(), allowedDoubleDeviation);
                assertTrue(outerEnvelope.contains(currentCell));
                assertEquals(currentCell.getMinX(), currentMinX, allowedDoubleDeviation);
                assertEquals(currentCell.getMaxX(), currentMaxX, allowedDoubleDeviation);
                assertEquals(currentCell.getMinY(), currentMinY, allowedDoubleDeviation);
                assertEquals(currentCell.getMaxY(), currentMaxY, allowedDoubleDeviation);
            }
        }
    }


    @Test
    public void allLinksOutsideOfGrid() {
        int numberOfLinks = 1000;
        int spreadAroundGrid = 5000;
        int numberOfColumns = 27;
        int numberOfRows = 27;

        int[] expectedArray = new int[numberOfColumns * numberOfRows];
        Arrays.fill(expectedArray, 0);

        List<Link> mockedLinks = new ArrayList<>();

        for (int i = 0; i < numberOfLinks; i++) {
            double x, y;

            switch (i % 4) {
                case 0:
                    x = minX - random.nextDouble() * spreadAroundGrid;
                    y = minY - random.nextDouble() * spreadAroundGrid;
                    break;
                case 1:
                    x = maxX + random.nextDouble() * spreadAroundGrid;
                    y = minY - random.nextDouble() * spreadAroundGrid;
                    break;
                case 2:
                    x = minX - random.nextDouble() * spreadAroundGrid;
                    y = maxY + random.nextDouble() * spreadAroundGrid;
                    break;
                default:
                    x = maxX + random.nextDouble() * spreadAroundGrid;
                    y = maxY + random.nextDouble() * spreadAroundGrid;
                    break;
            }

            Link mockedLink = mock(Link.class);
            when(mockedLink.getCoord()).thenReturn(new Coord(x, y));

            mockedLinks.add(mockedLink);
        }

        GridCell[] grid = AmodeusUtil.createEvenSpacedGrid(minX, maxX, minY, maxY, numberOfColumns, numberOfRows);
        List<Coord> coordList = mockedLinks.stream().map(BasicLocation::getCoord).collect(Collectors.toList());
        int[] linksPerCell = AmodeusUtil.getCoordsPerCell(coordList, grid);

        assertArrayEquals(expectedArray, linksPerCell);
    }

    @Test
    public void allLinksInFirstGridCell() {
        int numberOfLinks = 1000;
        int numberOfColumns = 27;
        int numberOfRows = 22;

        double cellWidth = (maxX - minX) / numberOfColumns;
        double cellHeight = (maxY - minY) / numberOfRows;

        int[] expectedArray = new int[numberOfColumns * numberOfRows];
        Arrays.fill(expectedArray, 0);
        expectedArray[0] = numberOfLinks;

        List<Link> mockedLinks = new ArrayList<>();

        for (int i = 0; i < numberOfLinks; i++) {
            double x = minX + random.nextDouble() * cellWidth;
            double y = maxY - random.nextDouble() * cellHeight;

            Link mockedLink = mock(Link.class);
            when(mockedLink.getCoord()).thenReturn(new Coord(x, y));

            mockedLinks.add(mockedLink);
        }

        GridCell[] grid = AmodeusUtil.createEvenSpacedGrid(minX, maxX, minY, maxY, numberOfColumns, numberOfRows);

        List<Coord> coordList = mockedLinks.stream().map(BasicLocation::getCoord).collect(Collectors.toList());
        int[] linksPerCell = AmodeusUtil.getCoordsPerCell(coordList, grid);

        assertArrayEquals(expectedArray, linksPerCell);
    }

    @Test
    public void linksEvenlyDistributedBetweenGridCells() {
        int numberOfLinksPerCell = 5;
        int numberOfColumns = 27;
        int numberOfRows = 22;

        double cellWidth = (maxX - minX) / numberOfColumns;
        double cellHeight = (maxY - minY) / numberOfRows;

        int[] expectedArray = new int[numberOfColumns * numberOfRows];
        Arrays.fill(expectedArray, numberOfLinksPerCell);

        List<Link> mockedLinks = new ArrayList<>();

        for (int row = 0; row < numberOfRows; row++) {
            double currentMaxY = maxY - row * cellHeight;
            for (int column = 0; column < numberOfColumns; column++) {
                double currentMinX = minX + column * cellWidth;
                for (int i = 0; i < numberOfLinksPerCell; i++) {
                    double x = currentMinX + random.nextDouble() * cellWidth;
                    double y = currentMaxY - random.nextDouble() * cellHeight;

                    Link mockedLink = mock(Link.class);
                    when(mockedLink.getCoord()).thenReturn(new Coord(x, y));

                    mockedLinks.add(mockedLink);
                }
            }
        }

        GridCell[] grid = AmodeusUtil.createEvenSpacedGrid(minX, maxX, minY, maxY, numberOfColumns, numberOfRows);

        List<Coord> coordList = mockedLinks.stream().map(BasicLocation::getCoord).collect(Collectors.toList());
        int[] linksPerCell = AmodeusUtil.getCoordsPerCell(coordList, grid);

        assertArrayEquals(expectedArray, linksPerCell);
    }

    @Test
    public void createNorthBorderPolygon() {
        AmodeusUtil.BorderOrientation orientation = AmodeusUtil.BorderOrientation.NORTH;
        float borderWidth = 5000;
        GeometryFactory geometryFactory = new GeometryFactory();
        Polygon polygon = AmodeusUtil.getBorderPolygon(orientation, borderWidth, minX, maxX,
                minY, maxY, geometryFactory);

        double expectedArea = (2 * (maxX - minX) + 2 * borderWidth) * borderWidth * 0.5;
        assertEquals(expectedArea,  polygon.getArea(), allowedDoubleDeviation);

    }

    @Test
    public void createEastBorderPolygon() {
        AmodeusUtil.BorderOrientation orientation = AmodeusUtil.BorderOrientation.EAST;
        float borderWidth = 5000;
        GeometryFactory geometryFactory = new GeometryFactory();
        Polygon polygon = AmodeusUtil.getBorderPolygon(orientation, borderWidth, minX, maxX,
                minY, maxY, geometryFactory);

        double expectedArea = (2 * (maxY - minY) + 2 * borderWidth) * borderWidth * 0.5;
        assertEquals(expectedArea,  polygon.getArea(), allowedDoubleDeviation);

    }

    @Test
    public void createSouthBorderPolygon() {
        AmodeusUtil.BorderOrientation orientation = AmodeusUtil.BorderOrientation.SOUTH;
        float borderWidth = 5000;
        GeometryFactory geometryFactory = new GeometryFactory();
        Polygon polygon = AmodeusUtil.getBorderPolygon(orientation, borderWidth, minX, maxX,
                minY, maxY, geometryFactory);

        double expectedArea = (2 * (maxX - minX) + 2 * borderWidth) * borderWidth * 0.5;
        assertEquals(expectedArea,  polygon.getArea(), allowedDoubleDeviation);

    }

    @Test
    public void createWestBorderPolygon() {
        AmodeusUtil.BorderOrientation orientation = AmodeusUtil.BorderOrientation.WEST;
        float borderWidth = 5000;
        GeometryFactory geometryFactory = new GeometryFactory();
        Polygon polygon = AmodeusUtil.getBorderPolygon(orientation, borderWidth, minX, maxX,
                minY, maxY, geometryFactory);

        double expectedArea = (2 * (maxY - minY) + 2 * borderWidth) * borderWidth * 0.5;
        assertEquals(expectedArea,  polygon.getArea(), allowedDoubleDeviation);

    }

    @Test
    public void noTaxisInBorder() {
        double borderWidth = 5000;
        int numberOfTaxis = 500;
        int spreadAroundCenter = 1000;
        List<RoboTaxi> mockedTaxis = new ArrayList<>();
        for (int i=0; i < numberOfTaxis; i++) {
            double x, y;
            x = 0.5 * (maxX + minX) - random.nextDouble() * spreadAroundCenter;
            y = 0.5 * (maxY + minY) - random.nextDouble() * spreadAroundCenter;

            RoboTaxi mockedTaxi = mock(RoboTaxi.class);
            Link mockedLink = mock(Link.class);
            when(mockedLink.getCoord()).thenReturn(new Coord(x, y));
            when(mockedTaxi.getLastKnownLocation()).thenReturn(mockedLink);

            mockedTaxis.add(mockedTaxi);
        }


        long actual = AmodeusUtil.getNumberOfTaxisInBorderZone(AmodeusUtil.BorderOrientation.NORTH, borderWidth,
                minX, maxX, minY, maxY, mockedTaxis).size();
        assertEquals(0, actual);
    }

    @Test
    public void allTaxisInBorder() {
        double borderWidth = 5000;
        int numberOfTaxis = 500;
        int spreadAroundCenter = 1000;
        List<RoboTaxi> mockedTaxis = new ArrayList<>();
        for (int i=0; i < numberOfTaxis; i++) {
            double x, y;
            x = 0.5 * (maxX + minX) - random.nextDouble() * spreadAroundCenter;
            y = maxY + borderWidth / 2;

            RoboTaxi mockedTaxi = mock(RoboTaxi.class);
            Link mockedLink = mock(Link.class);
            when(mockedLink.getCoord()).thenReturn(new Coord(x, y));
            when(mockedTaxi.getLastKnownLocation()).thenReturn(mockedLink);

            mockedTaxis.add(mockedTaxi);
        }


        long actual = AmodeusUtil.getNumberOfTaxisInBorderZone(AmodeusUtil.BorderOrientation.NORTH, borderWidth,
                minX, maxX, minY, maxY, mockedTaxis).size();
        assertEquals(numberOfTaxis, actual);
    }

    @Test
    public void someTaxisInBorder() {
        double borderWidth = 5000;
        int numberOfTaxisInPolygon = 250;
        int numberOfTaxisOutsideOfPolygon = 250;
        int spreadAroundCenter = 1000;
        List<RoboTaxi> mockedTaxis = new ArrayList<>();
        for (int i=0; i < numberOfTaxisInPolygon; i++) {
            double x, y;
            x = 0.5 * (maxX + minX) - random.nextDouble() * spreadAroundCenter;
            y = maxY + borderWidth / 2;

            RoboTaxi mockedTaxi = mock(RoboTaxi.class);
            Link mockedLink = mock(Link.class);
            when(mockedLink.getCoord()).thenReturn(new Coord(x, y));
            when(mockedTaxi.getLastKnownLocation()).thenReturn(mockedLink);

            mockedTaxis.add(mockedTaxi);
        }
        for (int i=0; i < numberOfTaxisOutsideOfPolygon; i++) {
            double x, y;
            x = 0.5 * (maxX + minX) - random.nextDouble() * spreadAroundCenter;
            y = 0.5 * (maxY + minY) - random.nextDouble() * spreadAroundCenter;

            RoboTaxi mockedTaxi = mock(RoboTaxi.class);
            Link mockedLink = mock(Link.class);
            when(mockedLink.getCoord()).thenReturn(new Coord(x, y));
            when(mockedTaxi.getLastKnownLocation()).thenReturn(mockedLink);

            mockedTaxis.add(mockedTaxi);
        }


        long actual = AmodeusUtil.getNumberOfTaxisInBorderZone(AmodeusUtil.BorderOrientation.NORTH, borderWidth,
                minX, maxX, minY, maxY, mockedTaxis).size();
        assertEquals(numberOfTaxisInPolygon, actual);
    }
}
