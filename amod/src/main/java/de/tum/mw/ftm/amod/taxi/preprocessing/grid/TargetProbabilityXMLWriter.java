package de.tum.mw.ftm.amod.taxi.preprocessing.grid;

import org.matsim.core.api.internal.MatsimWriter;
import org.matsim.core.utils.collections.Tuple;
import org.matsim.core.utils.io.MatsimXmlWriter;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class TargetProbabilityXMLWriter extends MatsimXmlWriter implements MatsimWriter {
    public static final String probabilitiesIdentifier = "probabilities";
    public static final String startCellIdentifier = "startCell";
    public static final String endCellIdentifier = "endCell";
    public static final String idIdentifier = "id";
    public static final String valueIdentifier = "value";


    private static final String DOCTYPE = "<!DOCTYPE " + probabilitiesIdentifier + " [\n" +
            "        <!ELEMENT " + probabilitiesIdentifier + " (" + startCellIdentifier + ")*>\n" +
            "        <!ELEMENT " + startCellIdentifier + " (" + endCellIdentifier + ")*>\n" +
            "        <!ATTLIST " + startCellIdentifier + "\n" +
            "                " + idIdentifier + "    CDATA #REQUIRED>\n" +
            "        <!ELEMENT " + endCellIdentifier + " (#PCDATA)>\n" +
            "        <!ATTLIST " + endCellIdentifier + "\n" +
            "                " + idIdentifier + "  CDATA #REQUIRED\n" +
            "                " + valueIdentifier + " CDATA #REQUIRED>\n" +
            "        ]>";

    private final double[][] targetProbabilities;

    public TargetProbabilityXMLWriter(double[][] targetProbabilities) {
        this.targetProbabilities = targetProbabilities;
    }


    @Override
    public void write(String filename) {
        openFile(filename);
        this.writeInit();

        writeGridInfoArray(targetProbabilities);

        this.writeFinish();
    }

    private void writeInit() {
        this.writeXmlHead();
        try {
            this.writer.write(DOCTYPE);
        } catch (IOException e) {
            e.printStackTrace();
        }
        this.startProbabilities();
    }

    private void writeGridInfoArray(double[][] array) {
        for (int i = 0; i < array.length; i++) {
            List<Tuple<String, String>> attributes = new ArrayList<>();
            attributes.add(new Tuple<>(idIdentifier, Integer.toString(i)));
            writeStartTag(startCellIdentifier, attributes, false);

            for (int j = 0; j < array[i].length; j++) {
                List<Tuple<String, String>> innerAttributes = new ArrayList<>();
                innerAttributes.add(new Tuple<>(idIdentifier, Integer.toString(j)));
                innerAttributes.add(new Tuple<>(valueIdentifier, Double.toString(array[i][j])));
                writeStartTag(endCellIdentifier, innerAttributes, true);
            }

            writeEndTag(startCellIdentifier);
        }
    }

    private void startProbabilities() {
        writeStartTag(probabilitiesIdentifier, null);
    }

    private void writeFinish() {
        try {
            this.endProbabilities();
            this.writer.flush();
            this.writer.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void endProbabilities() {
        writeEndTag(probabilitiesIdentifier);
    }
}
