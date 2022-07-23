package de.tum.mw.ftm.amod.taxi.preprocessing.grid;

import amodeus.amodeus.util.math.GlobalAssert;
import org.matsim.core.utils.io.MatsimXmlParser;
import org.xml.sax.Attributes;

import java.util.*;

public class GridInformationXMLReader extends MatsimXmlParser {
    private double[][] distances;
    private double[][] freespeedTravelTimes;

    private String currentInfo;
    private int currentStartCell;

    private final Map<String, Map<Integer, List<Double>>> gridInformationMap = new HashMap<>();

    public static void main(String[] args) {
        GridInformationXMLReader gridInformationXMLReader = new GridInformationXMLReader();
        gridInformationXMLReader.readFile("grid_info.xml");
        System.out.println("Successfully read grid info file");
    }

    @Override
    public void startTag(String s, Attributes attributes, Stack<String> stack) {
        switch (s) {
            case GridInformationXMLWriter.distancesIdentifier:
            case GridInformationXMLWriter.freespeedTravelTimesIdentifier:
                currentInfo = s;
                break;

            case GridInformationXMLWriter.startCellIdentifier:
                currentStartCell = Integer.parseInt(attributes.getValue(GridInformationXMLWriter.idIdentifier));
                break;

            case GridInformationXMLWriter.endCellIdentifier:
                double value = Double.parseDouble(attributes.getValue(GridInformationXMLWriter.valueIdentifier));
                gridInformationMap.computeIfAbsent(currentInfo, info -> new HashMap<>()).computeIfAbsent(currentStartCell, startCell -> new ArrayList<>()).add(value);
                break;
        }
    }

    @Override
    public void endTag(String s, String s1, Stack<String> stack) {
        if (s.equals(GridInformationXMLWriter.gridInformationIdentifier)) {
            consolidate();
        }

    }

    private void consolidate() {
        Map<Integer, List<Double>> distanceMap = gridInformationMap.get(GridInformationXMLWriter.distancesIdentifier);
        Map<Integer, List<Double>> travelTimesMap = gridInformationMap.get(GridInformationXMLWriter.freespeedTravelTimesIdentifier);

        GlobalAssert.that(distanceMap.size() == travelTimesMap.size());
        int numberOfCells = distanceMap.size();
        distances = new double[numberOfCells][numberOfCells];
        freespeedTravelTimes = new double[numberOfCells][numberOfCells];

        for (int startCell = 0; startCell < numberOfCells; startCell++) {
            for (int endCell = 0; endCell < numberOfCells; endCell++) {
                distances[startCell][endCell] = distanceMap.get(startCell).get(endCell);
                freespeedTravelTimes[startCell][endCell] = travelTimesMap.get(startCell).get(endCell);
            }
        }
    }

    public double[][] getDistances() {
        return distances;
    }

    public double[][] getFreespeedTravelTimes() {
        return freespeedTravelTimes;
    }
}
