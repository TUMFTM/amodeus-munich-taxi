package de.tum.mw.ftm.amod.taxi.preprocessing.ranks;

import de.tum.mw.ftm.amod.taxi.dispatcher.ReferenceDispatcher;
import de.tum.mw.ftm.amod.taxi.util.AmodeusUtil;
import junit.framework.TestCase;
import org.junit.Assert;

import java.util.HashMap;
import java.util.Map;
import java.util.Random;


public class TaxiRanksTest extends TestCase {
    private TaxiRanks taxiRanks;
    private Random random = new Random(42);
    public void setUp() throws Exception {
        super.setUp();
        taxiRanks = new TaxiRanks();
        new TaxiRanksXMLReader(taxiRanks).readFile("taxiRanks.xml");
        assertTrue(taxiRanks.size() > 0);
    }

    public void testGetRandomTaxiRank() {
        Map<TaxiRank, Integer> randomRanksCount = new HashMap<>();
        int numberOfSamples = 50000;
        for (int i = 0; i <= numberOfSamples; i++) {
            TaxiRank rank = AmodeusUtil.getRandomTaxiRank(taxiRanks, random);
            if (randomRanksCount.containsKey(rank)) {
                randomRanksCount.put(rank, randomRanksCount.get(rank) + 1);
            } else randomRanksCount.put(rank, 1);
        }


        for(Map.Entry<TaxiRank, Integer> entry : randomRanksCount.entrySet()){
            float error = Math.abs(((float) entry.getValue()/(float)numberOfSamples) - entry.getKey().getPopularity());
                    Assert.assertTrue(error <= 0.01 );
            System.out.printf("Rank: %d Probability: %f, RandomSample %f\n",
                    entry.getKey().getId(),
                    entry.getKey().getPopularity(),
                    (float) entry.getValue()/ (float)numberOfSamples);
        }
    }
}