package de.tum.mw.ftm.amod.taxi.preprocessing.ranks;

import org.apache.log4j.Logger;
import org.matsim.core.api.internal.MatsimWriter;
import org.matsim.core.utils.collections.Tuple;
import org.matsim.core.utils.io.MatsimXmlWriter;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

public class TaxiRanksXMLWriter extends MatsimXmlWriter implements MatsimWriter {
    private static final String DTD = "https://raw.githubusercontent.com/michaelwittmann/amodeus-taxi-munich-dtd/master/taxi_ranks.dtd";
    private static final Logger log = Logger.getLogger(TaxiRanksXMLWriter.class);


    private final TaxiRanks taxiRanks;

    /**
     * Creates a new TaxiRanksWriter to write the specified taxi ranks to the file.
     *
     * @param taxiRanks the taxi ranks to write
     */
    public TaxiRanksXMLWriter(
            final TaxiRanks taxiRanks) {
        this.taxiRanks = taxiRanks;
    }

    @Override
    public void write(String filename) {
        openFile(filename);
        this.writeInit(Paths.get(filename));

        for (TaxiRank rank : taxiRanks) {
            this.writeTaxiRank(rank);
        }
        this.writeFinish();
    }

    private void writeInit(Path filepath) {
        this.writeXmlHead();
        this.writeDoctype("ranks", DTD);
        this.startTaxiRanks();
    }

    private void startTaxiRanks() {
        writeStartTag("ranks", null);
    }

    private void writeTaxiRank(final TaxiRank taxiRank) {
        List<Tuple<String, String>> attributes = new ArrayList<>();
        attributes.add(new Tuple<>("id", taxiRank.getId() + ""));
        attributes.add(new Tuple<>("capacity", taxiRank.getCapacity() + ""));
        attributes.add(new Tuple<>("description", taxiRank.getDescription() + ""));
        attributes.add(new Tuple<>("x", taxiRank.getCoordinate().getX() + ""));
        attributes.add(new Tuple<>("y", taxiRank.getCoordinate().getY() + ""));
        attributes.add(new Tuple<>("popularity", taxiRank.getPopularity() + ""));
        writeStartTag("rank", attributes, true);
        try {
            this.writer.flush();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void writeFinish() {
        try {
            this.endTaxiRanks();
            this.writer.flush();
            this.writer.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void endTaxiRanks() {
        writeEndTag("ranks");
    }

}

