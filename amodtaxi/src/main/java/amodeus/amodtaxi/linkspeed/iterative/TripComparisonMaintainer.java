/* amodeus - Copyright (c) 2019, ETH Zurich, Institute for Dynamic Systems and Control */
package amodeus.amodtaxi.linkspeed.iterative;

import amodeus.amodeus.linkspeed.LinkSpeedDataContainer;
import amodeus.amodeus.net.FastLinkLookup;
import amodeus.amodeus.net.MatsimAmodeusDatabase;
import amodeus.amodeus.taxitrip.ShortestDurationCalculator;
import amodeus.amodeus.taxitrip.TaxiTrip;
import amodeus.amodeus.util.math.GlobalAssert;
import ch.ethz.idsc.tensor.RealScalar;
import ch.ethz.idsc.tensor.Scalar;
import org.matsim.api.core.v01.network.Network;
import org.matsim.core.router.util.LeastCostPathCalculator;

import java.util.*;
import java.util.stream.Collectors;

/* package */ class TripComparisonMaintainer {
    // TODO can maintain same size more pretty?
    private final Map<TaxiTrip, Scalar> ratioLookupMap = new HashMap<>();
    private final NavigableMap<Scalar, TaxiTrip> ratioSortedMap = new TreeMap<>();

    public TripComparisonMaintainer(RandomTripMaintainer randomTrips, Network network, MatsimAmodeusDatabase db) {
        // initial fill
        ShortestDurationCalculator calc = new ShortestDurationCalculator(network, db);
        for (int i = 0; i < randomTrips.numTrips(); ++i) {
            if (i % 100 == 0)
                System.out.println("Freespeed length calculation: " + i);
            TaxiTrip trip = randomTrips.nextRandom();
            DurationCompare compare = DurationCompare.getInstance(trip, calc);
            if (compare == null) {
                continue;
            }
            Scalar pathDurationratio = compare.nwPathDurationRatio;
            randomTrips.addRecordedRatio(pathDurationratio);
            ratioLookupMap.put(trip, pathDurationratio);
            Scalar cost = pathDurationratio.subtract(RealScalar.ONE).abs();
            ratioSortedMap.put(cost, trip);
        }
        GlobalAssert.that(ratioSortedMap.size() <= ratioLookupMap.size());
    }

    public TripComparisonMaintainer(RandomTripMaintainer randomTrips, Network network, LinkSpeedDataContainer lsData,
                                    FastLinkLookup fastLinkLookup) {
        // initial fill
        LeastCostPathCalculator lcpc = LinkSpeedLeastPathCalculator.from(network, lsData);
        ShortestDurationCalculator calc = new ShortestDurationCalculator(lcpc, fastLinkLookup);
        for (int i = 0; i < randomTrips.numTrips(); ++i) {
            if (i % 100 == 0)
                System.out.println("Initial length calculation: " + i);
            TaxiTrip trip = randomTrips.nextRandom();
            DurationCompare compare = DurationCompare.getInstance(trip, calc);
            if (compare == null) {
                continue;
            }
            Scalar pathDurationratio = compare.nwPathDurationRatio;
            randomTrips.addRecordedRatio(pathDurationratio);
            ratioLookupMap.put(trip, pathDurationratio);
            Scalar cost = pathDurationratio.subtract(RealScalar.ONE).abs();
            ratioSortedMap.put(cost, trip);
        }
        GlobalAssert.that(ratioSortedMap.size() <= ratioLookupMap.size());
    }

    public void update(TaxiTrip trip, Scalar pathDurationratio) {
        // remove using old value
        Scalar ratioBefore = ratioLookupMap.get(trip);
        Scalar costBefore = ratioBefore.subtract(RealScalar.ONE).abs();
        // update worst trip
        if (!ratioSortedMap.remove(costBefore, trip)) {
            // GlobalAssert.that(false); // TODO To check why should this happen?
            System.out.println("Cleansing sorted map...");
            // if not removed successfully, remove all values associated to this trip.
            ratioSortedMap.entrySet().removeIf(e -> e.getValue().equals(trip));
        }
        ratioSortedMap.put(pathDurationratio.subtract(RealScalar.ONE).abs(), trip);
        // update lookupMap
        ratioLookupMap.put(trip, pathDurationratio);
        GlobalAssert.that(ratioSortedMap.size() <= ratioLookupMap.size());
    }

    public TaxiTrip getWorst() {
        return ratioSortedMap.lastEntry().getValue();
    }

    public List<TaxiTrip> getNWorst(int n) {
        return ratioSortedMap.descendingMap().entrySet().stream().limit(n).map(Map.Entry::getValue).collect(Collectors.toList());
    }

    public Scalar getWorstCost() {
        return ratioSortedMap.lastEntry().getKey();
    }

    public Map<TaxiTrip, Scalar> getLookupMap() {
        return Collections.unmodifiableMap(ratioLookupMap);
    }
}
