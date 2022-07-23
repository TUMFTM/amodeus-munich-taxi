package de.tum.mw.ftm.amod.taxi.preprocessing.grid;

import org.matsim.core.api.internal.MatsimWriter;
import org.matsim.core.utils.collections.Tuple;
import org.matsim.core.utils.io.MatsimXmlWriter;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class GridInformationXMLWriter extends MatsimXmlWriter implements MatsimWriter {
    public static final String gridInformationIdentifier = "gridInformation";
    public static final String distancesIdentifier = "distances";
    public static final String freespeedTravelTimesIdentifier = "freespeedTravelTimes";
    public static final String startCellIdentifier = "startCell";
    public static final String endCellIdentifier = "endCell";
    public static final String idIdentifier = "id";
    public static final String valueIdentifier = "value";


    private static final String DOCTYPE = "<!DOCTYPE " + gridInformationIdentifier + " [\n" +
            "        <!ELEMENT " + gridInformationIdentifier + " (" + distancesIdentifier + ", " + freespeedTravelTimesIdentifier + ")>\n" +
            "        <!ELEMENT " + distancesIdentifier + " (" + startCellIdentifier + ")*>\n" +
            "        <!ELEMENT " + freespeedTravelTimesIdentifier + " (" + startCellIdentifier + ")*>\n" +
            "        <!ELEMENT " + startCellIdentifier + " (" + endCellIdentifier + ")*>\n" +
            "        <!ATTLIST " + startCellIdentifier + "\n" +
            "                " + idIdentifier + "    CDATA #REQUIRED>\n" +
            "        <!ELEMENT " + endCellIdentifier + " (#PCDATA)>\n" +
            "        <!ATTLIST " + endCellIdentifier + "\n" +
            "                " + idIdentifier + "  CDATA #REQUIRED\n" +
            "                " + valueIdentifier + " CDATA #REQUIRED>\n" +
            "        ]>";
    private final GridInformationCalculator gridInformationCalculator;

    public GridInformationXMLWriter(GridInformationCalculator gridInformationCalculator) {
        this.gridInformationCalculator = gridInformationCalculator;
    }

    @Override
    public void write(String filename) {
        openFile(filename);
        this.writeInit();

        writeStartTag(distancesIdentifier, null);
        double[][] gridDistances = gridInformationCalculator.getDistancesBetweenCells();
        writeGridInfoArray(gridDistances);
        writeEndTag(distancesIdentifier);

        writeStartTag(freespeedTravelTimesIdentifier, null);
        double[][] travelTimes = gridInformationCalculator.getFreeSpeedTravelTimes();
        writeGridInfoArray(travelTimes);
        writeEndTag(freespeedTravelTimesIdentifier);


        this.writeFinish();
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

    private void writeInit() {
        this.writeXmlHead();
        try {
            this.writer.write(DOCTYPE);
        } catch (IOException e) {
            e.printStackTrace();
        }
        this.startGridInformation();
    }

    private void startGridInformation() {
        writeStartTag(gridInformationIdentifier, null);
    }

    private void writeFinish() {
        try {
            this.endGridInformation();
            this.writer.flush();
            this.writer.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void endGridInformation() {
        writeEndTag(gridInformationIdentifier);
    }
}
