package de.tum.mw.ftm.amod.taxi.preprocessing.grid;

import amodeus.amod.ext.UserReferenceFrames;
import de.tum.mw.ftm.amod.geom.GridCell;
import de.tum.mw.ftm.amod.taxi.preprocessing.demand.CSVTaxiRide;
import de.tum.mw.ftm.amod.taxi.preprocessing.demand.TaxiRide;
import de.tum.mw.ftm.amod.taxi.util.AmodeusUtil;
import org.apache.log4j.Logger;
import org.matsim.amodeus.config.FTMConfigGroup;
import org.matsim.api.core.v01.Coord;
import org.matsim.core.utils.geometry.CoordinateTransformation;

import java.io.File;
import java.io.FileNotFoundException;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.DoubleStream;
import java.util.stream.IntStream;

public class TargetProbabilityCalculator {
    private final static Logger logger = Logger.getLogger(TargetProbabilityCalculator.class);

    private final GridCell[] gridCells;
    private final FTMConfigGroup ftmConfigGroup;
    private final CoordinateTransformation coordinateTransformation;

    public TargetProbabilityCalculator(GridCell[] gridCells, FTMConfigGroup ftmConfigGroup) {
        this.gridCells = gridCells;
        this.ftmConfigGroup = ftmConfigGroup;
        this.coordinateTransformation = UserReferenceFrames.MUNICH.coords_fromWGS84();
    }

    public double[][] calculateTargetProbability() throws FileNotFoundException {
        double[][] probabilities = new double[gridCells.length][gridCells.length];



        logger.info("Receiving population from database...");
        FTMConfigGroup ftmConfigGroupForDB = new FTMConfigGroup();
        // Set sim time to time before simulation start to calculate the overall probability during the last 2 months
        ftmConfigGroupForDB.setSimStartDateTime(ftmConfigGroup.getSimStartDateTime().minusMonths(2).toString());
        ftmConfigGroupForDB.setSimEndDateTime(ftmConfigGroup.getSimStartDateTime().minusSeconds(1).toString());
        List<TaxiRide> ridesList = CSVTaxiRide.parseTaxiTripsFromCSV(new File("historic_rides_data")).stream().map(l->l.toTaxiRide()).collect(Collectors.toList());
        Set<TaxiRide> rides = new HashSet<>(ridesList);

        for (int startCellIndex = 0; startCellIndex < gridCells.length; startCellIndex++) {
            int currentStartCellIndex = startCellIndex;
            Set<TaxiRide> ridesStartingInCell = rides.stream().filter(ride -> {
                Coord pickupLocation = coordinateTransformation.transform(new Coord(ride.getLocationStart().getX(), ride.getLocationStart().getY()));
                return gridCells[currentStartCellIndex].contains(pickupLocation.getX(), pickupLocation.getY());
            }).collect(Collectors.toSet());

            rides.removeAll(ridesStartingInCell);

            List<Coord> destinationCoords = ridesStartingInCell.stream()
                    .map(ride -> coordinateTransformation.transform(new Coord(ride.getLocationStop().getX(), ride.getLocationStop().getY())))
                    .collect(Collectors.toList());


            int[] numberOfTripsEndingInCells = AmodeusUtil.getCoordsPerCell(destinationCoords, gridCells);
            long sumOfTrips = IntStream.of(numberOfTripsEndingInCells).sum();
            double[] probabilityForCurrentCell = new double[gridCells.length];
            for (int endCellIndex = 0; endCellIndex < gridCells.length; endCellIndex++) {
                probabilityForCurrentCell[endCellIndex] = (double) numberOfTripsEndingInCells[endCellIndex] / sumOfTrips;
            }
            double sumOfProbabilities = DoubleStream.of(probabilityForCurrentCell).sum();
            if (Math.abs(1.0 - sumOfProbabilities) > 0.01) {
                throw new RuntimeException("Probability sum diverts more than 1% from 100%. Aborting!");
            }

            probabilities[startCellIndex] = probabilityForCurrentCell;
        }

        return probabilities;
    }
}
