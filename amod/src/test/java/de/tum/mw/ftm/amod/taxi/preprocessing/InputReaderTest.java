package de.tum.mw.ftm.amod.taxi.preprocessing;

import amodeus.amodeus.fleetsize.DynamicFleetSize;
import de.tum.mw.ftm.amod.taxi.preprocessing.fleetsize.DynamicFleetSizeXMLReader;
import de.tum.mw.ftm.amod.taxi.preprocessing.fleetsize.DynamicFleetSizeXMLWriter;
import de.tum.mw.ftm.amod.taxi.preprocessing.ranks.TaxiRanks;
import de.tum.mw.ftm.amod.taxi.preprocessing.ranks.TaxiRanksXMLReader;
import de.tum.mw.ftm.amod.taxi.preprocessing.ranks.TaxiRanksXMLWriter;
import de.tum.mw.ftm.amod.taxi.preprocessing.zones.DispatchingZones;
import de.tum.mw.ftm.amod.taxi.preprocessing.zones.DispatchingZonesXMLReader;
import de.tum.mw.ftm.amod.taxi.preprocessing.zones.DispatchingZonesXMLWriter;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import java.io.IOException;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;


public class InputReaderTest {

    @Rule
    public TemporaryFolder folder = new TemporaryFolder();


    @Test
    public void testRankReadWrite() throws IOException {
        //Read from valid XML
        TaxiRanks taxiRanks = new TaxiRanks();
        new TaxiRanksXMLReader(taxiRanks).readFile("taxiRanks.xml");
        assertTrue(taxiRanks.size() > 0);

        //Write to XML
        String tmp_file = folder.newFile("_taxiRanks.xml").getAbsolutePath();
        new TaxiRanksXMLWriter(taxiRanks).write(tmp_file);

        //Read from written XML again
        TaxiRanks _taxiRanks = new TaxiRanks();
        new TaxiRanksXMLReader(_taxiRanks).readFile(tmp_file);

        assertEquals(taxiRanks.size(), _taxiRanks.size());

    }

    @Test
    public void testDynamicFleetSizeReadWrite() throws IOException {
        //Read from valid XML
        DynamicFleetSize dynamicFleetSize = new DynamicFleetSize();
        new DynamicFleetSizeXMLReader(dynamicFleetSize).readFile("dynamicFleetSize.xml");
        assertTrue(dynamicFleetSize.size() > 0);

        //Write to XML
        String tmp_file = folder.newFile("_dynamicFleetSize.xml").getAbsolutePath();
        new DynamicFleetSizeXMLWriter(dynamicFleetSize).write(tmp_file);

        //Read from written XML again
        DynamicFleetSize _dynamicFleetSize = new DynamicFleetSize();
        new DynamicFleetSizeXMLReader(_dynamicFleetSize).readFile(tmp_file);
    }

    @Test
    public void testDispatchingZonesReadWrite() throws IOException {
        //Read from valid XML
        DispatchingZones dispatchingZones = new DispatchingZones();
        new DispatchingZonesXMLReader(dispatchingZones).readFile("dispatchingZones.xml");
        assertTrue(dispatchingZones.size() > 0);

        //Write to XML
        String tmp_file = folder.newFile("_dispatchingZones.xml").getAbsolutePath();
        new DispatchingZonesXMLWriter(dispatchingZones).write(tmp_file);

        //Read from written XML again
        DispatchingZones _dispatchingZones = new DispatchingZones();
        new DispatchingZonesXMLReader(_dispatchingZones).readFile(tmp_file);

        assertEquals(dispatchingZones.size(), _dispatchingZones.size());
    }

}
