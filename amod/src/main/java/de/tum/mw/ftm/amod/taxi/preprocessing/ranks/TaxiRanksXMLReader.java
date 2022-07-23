package de.tum.mw.ftm.amod.taxi.preprocessing.ranks;

import org.matsim.core.utils.io.MatsimXmlParser;
import org.xml.sax.Attributes;

import java.util.Stack;

public class TaxiRanksXMLReader extends MatsimXmlParser {
    private static final String RANK = "rank";

    private static final String ID = "id";
    private static final String CAPACITY = "capacity";
    private static final String DESCRIPTION = "description";
    private static final String X = "x";
    private static final String Y = "y";
    private static final String POPULARITY = "popularity";

    private TaxiRanks taxiRanks;

    public TaxiRanksXMLReader(TaxiRanks taxiRanks) {
        this.taxiRanks = taxiRanks;
    }


    @Override
    public void startTag(String s, Attributes attributes, Stack<String> stack) {
        if (s.equals(RANK)) {
            try {
                taxiRanks.add(new TaxiRank(
                        Integer.parseInt(attributes.getValue(ID)),
                        Integer.parseInt(attributes.getValue(CAPACITY)),
                        attributes.getValue(DESCRIPTION),
                        Double.parseDouble(attributes.getValue(X)),
                        Double.parseDouble(attributes.getValue(Y)),
                        Float.parseFloat(attributes.getValue(POPULARITY))
                ));
            } catch (NumberFormatException e) {
                throw new NumberFormatException("Error while parsing TaxiRanks. Please verify that all entries are valid. " + e.getMessage());
            }
        }
    }

    @Override
    public void endTag(String s, String s1, Stack<String> stack) {

    }
}



