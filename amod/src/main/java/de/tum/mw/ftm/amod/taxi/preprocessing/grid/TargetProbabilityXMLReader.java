package de.tum.mw.ftm.amod.taxi.preprocessing.grid;

import org.matsim.core.utils.io.MatsimXmlParser;
import org.xml.sax.Attributes;

import java.util.*;

public class TargetProbabilityXMLReader extends MatsimXmlParser {
    private double[][] targetProbabilities;

    private final Map<Integer, List<Double>> probabilitiesMap = new HashMap<>();
    private int currentStartCell;

    public static void main(String[] args) {
        TargetProbabilityXMLReader targetProbabilityXMLReader = new TargetProbabilityXMLReader();
        targetProbabilityXMLReader.readFile("target_probabilities.xml");
        System.out.println("Successfully read grid info file");
    }


    @Override
    public void startTag(String s, Attributes attributes, Stack<String> stack) {
        switch (s) {
            case TargetProbabilityXMLWriter.startCellIdentifier:
                currentStartCell = Integer.parseInt(attributes.getValue(GridInformationXMLWriter.idIdentifier));
                break;

            case TargetProbabilityXMLWriter.endCellIdentifier:
                double value = Double.parseDouble(attributes.getValue(GridInformationXMLWriter.valueIdentifier));
                probabilitiesMap.computeIfAbsent(currentStartCell, startCell -> new ArrayList<>()).add(value);
                break;
        }

    }

    @Override
    public void endTag(String s, String s1, Stack<String> stack) {
        if (s.equals(TargetProbabilityXMLWriter.probabilitiesIdentifier)) {
            consolidate();
        }
    }

    private void consolidate() {
        int numberOfCells = probabilitiesMap.size();
        targetProbabilities = new double[numberOfCells][numberOfCells];

        for (int startCell = 0; startCell < numberOfCells; startCell++) {
            for (int endCell = 0; endCell < numberOfCells; endCell++) {
                targetProbabilities[startCell][endCell] = probabilitiesMap.get(startCell).get(endCell);
            }
        }
    }

    public double[][] getTargetProbabilities() {
        return targetProbabilities;
    }
}
