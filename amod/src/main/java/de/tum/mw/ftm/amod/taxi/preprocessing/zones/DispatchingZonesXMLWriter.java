package de.tum.mw.ftm.amod.taxi.preprocessing.zones;

import org.apache.log4j.Logger;
import org.locationtech.jts.io.WKTWriter;
import org.matsim.api.core.v01.Id;
import org.matsim.contrib.zone.Zone;
import org.matsim.core.api.internal.MatsimWriter;
import org.matsim.core.utils.collections.Tuple;
import org.matsim.core.utils.io.MatsimXmlWriter;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class DispatchingZonesXMLWriter extends MatsimXmlWriter implements MatsimWriter {
    private static final String DTD = "https://raw.githubusercontent.com/michaelwittmann/amodeus-taxi-munich-dtd/master/taxi_dispatching_zones.dtd";
    private static final Logger log = Logger.getLogger(DispatchingZonesXMLWriter.class);
    private final DispatchingZones dispatchingZones;
    private WKTWriter wktWriter = new WKTWriter();

    public DispatchingZonesXMLWriter(final DispatchingZones dispatchingZones) {
        this.dispatchingZones = dispatchingZones;
    }


    @Override
    public void write(String filename) {
        openFile(filename);
        this.writeInit(Paths.get(filename));

        for (Map.Entry<Id<Zone>, DispatchingZone> zone : dispatchingZones.entrySet()) {
            this.writeZone(zone);
        }
        this.writeFinish();
    }

    private void writeZone(Map.Entry<Id<Zone>, DispatchingZone> zone) {
        List<Tuple<String, String>> attributes = new ArrayList<>();
        attributes.add(new Tuple<>(DispatchingZonesXMLReader.ID, zone.getKey().toString() + ""));
        attributes.add(new Tuple<>(DispatchingZonesXMLReader.WKT, wktWriter.write(zone.getValue().getMultiPolygon()) + ""));
        writeStartTag(DispatchingZonesXMLReader.ZONE, attributes, false);
        if(!zone.getValue().getAdjacentZones().isEmpty()) {
            writeStartTag(DispatchingZonesXMLReader.ALTERNATIVE_ZONES, null);
            for (Id<Zone> id : zone.getValue().getAdjacentZones()) {
                List<Tuple<String, String>> attributes2 = new ArrayList<>();
                attributes2.add(new Tuple<>(DispatchingZonesXMLReader.ID, id.toString() + ""));
                writeStartTag(DispatchingZonesXMLReader.ALTERNATIVE_ZONE, attributes2, true);
            }
            writeEndTag(DispatchingZonesXMLReader.ALTERNATIVE_ZONES);
        }
        writeEndTag(DispatchingZonesXMLReader.ZONE);
        try {
            this.writer.flush();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void writeInit(Path filepath) {
        this.writeXmlHead();
        this.writeDoctype("zones", DTD);
        this.startDispatchingZones();
    }

    private void startDispatchingZones() {
        writeStartTag("zones", null);
    }

    private void writeFinish() {
        try {
            this.endDispatchingZones();
            this.writer.flush();
            this.writer.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void endDispatchingZones() {
        writeEndTag("zones");
    }
}
