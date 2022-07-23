package de.tum.mw.ftm.amod.taxi.preprocessing.ranks;

import org.matsim.api.core.v01.network.Network;

import java.util.*;
import java.util.stream.Collectors;

public class TaxiRanks extends HashSet<TaxiRank> {

    public TaxiRanks(Collection<TaxiRank> taxiRanks) {
        super.addAll(taxiRanks);
    }

    public TaxiRanks() {
        super();
    }

    public void addLinksToTaxiRanks(Network network) {
        Iterator<TaxiRank> taxiRankIterator = this.iterator();
        while (taxiRankIterator.hasNext()) {
            taxiRankIterator.next().setNearestLink(network);
        }
    }

    @Override
    public boolean add(TaxiRank taxiRank) {
        return super.add(taxiRank);
    }

}
