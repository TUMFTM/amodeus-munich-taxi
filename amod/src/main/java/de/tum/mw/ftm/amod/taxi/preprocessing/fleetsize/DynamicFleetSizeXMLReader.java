package de.tum.mw.ftm.amod.taxi.preprocessing.fleetsize;

import amodeus.amodeus.fleetsize.DynamicFleetSize;
import org.matsim.core.utils.io.MatsimXmlParser;
import org.xml.sax.Attributes;

import java.util.Stack;

public class DynamicFleetSizeXMLReader extends MatsimXmlParser {
    private static final String FLEET_SIZE = "fleetsize";
    private static final String TIMESTAMP = "timestamp";
    private static final String COUNT = "count";

    private DynamicFleetSize dynamicFleetSize;

    public DynamicFleetSizeXMLReader(DynamicFleetSize dynamicFleetSize) {
        this.dynamicFleetSize = dynamicFleetSize;
    }

    @Override
    public void startTag(String s, Attributes attributes, Stack<String> stack) {
        if(s.equals(FLEET_SIZE)) {
            try {
                dynamicFleetSize.put(
                        Long.parseLong(attributes.getValue(TIMESTAMP)),
                        Long.parseLong(attributes.getValue(COUNT)));
            } catch (NumberFormatException e) {
                throw new NumberFormatException("Error while parsing DynamicFleetSize input xml. Check if attributes contain valid entries. " + e.getMessage());
            }
        }
    }

    @Override
    public void endTag(String s, String s1, Stack<String> stack) {

    }
}
