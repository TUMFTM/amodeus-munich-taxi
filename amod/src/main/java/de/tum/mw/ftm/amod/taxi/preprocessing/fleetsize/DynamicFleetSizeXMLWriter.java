package de.tum.mw.ftm.amod.taxi.preprocessing.fleetsize;

import amodeus.amodeus.fleetsize.DynamicFleetSize;
import org.apache.log4j.Logger;
import org.matsim.core.api.internal.MatsimWriter;
import org.matsim.core.utils.collections.Tuple;
import org.matsim.core.utils.io.MatsimXmlWriter;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class DynamicFleetSizeXMLWriter extends MatsimXmlWriter implements MatsimWriter {
    private static final String DTD = "https://raw.githubusercontent.com/michaelwittmann/amodeus-taxi-munich-dtd/master/dynamic_fleet_size.dtd";
    private static final Logger log = Logger.getLogger(DynamicFleetSizeXMLWriter.class);


    private final DynamicFleetSize dynamicFleetSize;

    /**
     * Creates a new TaxiRanksWriter to write the specified taxi ranks to the file.
     *
     * @param fleetSize the taxi ranks to write
     */
    public DynamicFleetSizeXMLWriter(
            final DynamicFleetSize fleetSize) {
        this.dynamicFleetSize = fleetSize;
    }

    @Override
    public void write(String filename) {
        openFile(filename);
        this.writeInit(Paths.get(filename));

        for (Map.Entry<Long, Long> entry : dynamicFleetSize.entrySet()) {
            this.writeDynamicFleetSize(entry);
        }
        this.writeFinish();
    }

    private void writeInit(Path filepath) {
        this.writeXmlHead();
        this.writeDoctype("dynamicfleetsize", DTD);
        this.startDynamicFleetSize();
    }

    private void startDynamicFleetSize() {
        writeStartTag("dynamicfleetsize", null);
    }

    private void writeDynamicFleetSize(final Map.Entry<Long, Long> entry) {
        List<Tuple<String, String>> attributes = new ArrayList<>();
        attributes.add(new Tuple<>("timestamp", entry.getKey() + ""));
        attributes.add(new Tuple<>("count", entry.getValue() + ""));
        writeStartTag("fleetsize", attributes, true);
        try {
            this.writer.flush();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void writeFinish() {
        try {
            this.endDynamicFleetSize();
            this.writer.flush();
            this.writer.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void endDynamicFleetSize() {
        writeEndTag("dynamicfleetsize");
    }

}

