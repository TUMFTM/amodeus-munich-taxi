package de.tum.mw.ftm.amod.taxi.preprocessing.zones;

import org.locationtech.jts.geom.MultiPolygon;
import org.locationtech.jts.io.ParseException;
import org.locationtech.jts.io.WKTReader;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.population.Person;
import org.matsim.contrib.zone.Zone;
import org.matsim.core.utils.io.MatsimXmlParser;
import org.xml.sax.Attributes;

import java.util.Stack;

public class DispatchingZonesXMLReader extends MatsimXmlParser {
    public static final String ZONE = "zone";
    public static final String ID = "id";
    public static final String WKT = "wkt";
    public static final String ALTERNATIVE_ZONES = "alternative_zones";
    public static final String ALTERNATIVE_ZONE = "alternative_zone";

    private DispatchingZone currZone = null;
    private WKTReader wktReader = new WKTReader();

    private DispatchingZones dispatchingZones;

    public DispatchingZonesXMLReader(DispatchingZones dispatchingZones) {
        this.dispatchingZones = dispatchingZones;
    }


    @Override
    public void startTag(String s, Attributes attributes, Stack<String> stack) {
        if (s.equals(ZONE)) {
            try {
                long id = Long.parseLong(attributes.getValue(ID));
                MultiPolygon multiPolygon = (MultiPolygon) wktReader.read(attributes.getValue(WKT));
                this.currZone = new DispatchingZone(Id.create(id, Zone.class), "multipolygon", multiPolygon);

            } catch (NumberFormatException e) {
                throw new NumberFormatException("Error while parsing TaxiRanks. Please verify that all entries are valid. " + e.getMessage());
            } catch (ParseException e) {
                //TODO: DO something here
                e.printStackTrace();
            }
        }
        if(s.equals(ALTERNATIVE_ZONE)){
            long id = Long.parseLong(attributes.getValue(ID));
            this.currZone.addAdjacentZones(Id.create(id, Zone.class));
        }
    }

    @Override
    public void endTag(String s, String s1, Stack<String> stack) {
        if(s.equals(ZONE)){
            dispatchingZones.put(currZone.getId(), currZone);
        }
    }
}
