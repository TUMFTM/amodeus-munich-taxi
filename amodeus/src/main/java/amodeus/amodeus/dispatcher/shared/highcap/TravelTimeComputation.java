/* amodeus - Copyright (c) 2019, ETH Zurich, Institute for Dynamic Systems and Control */
package amodeus.amodeus.dispatcher.shared.highcap;

import java.util.HashMap;
import java.util.Map;

import org.matsim.api.core.v01.network.Link;
import org.matsim.core.router.util.LeastCostPathCalculator;
import org.matsim.core.router.util.LeastCostPathCalculator.Path;

import amodeus.amodeus.util.math.LruCache;

public class TravelTimeComputation {

    private final LeastCostPathCalculator leastCostPathCalculator;
    private final Map<Link, Map<Link, Double>> travelTimeDataMap;

    public TravelTimeComputation(LeastCostPathCalculator leastCostPathCalculator, int sizeLimit) {
        this.leastCostPathCalculator = leastCostPathCalculator;
        travelTimeDataMap = LruCache.create(sizeLimit);
    }

    // map data structure
    public double of(Link fromLink, Link toLink, double now, boolean storeInCache) {
        travelTimeDataMap.computeIfAbsent(fromLink, l -> new HashMap<>());

        if (travelTimeDataMap.get(fromLink).containsKey(toLink))
            return travelTimeDataMap.get(fromLink).get(toLink);

        // if it reaches here, we need to calculate the travel time
        Path shortest = leastCostPathCalculator.calcLeastCostPath(fromLink.getFromNode(), toLink.getToNode(), now, null, null);
        if (storeInCache)
            travelTimeDataMap.get(fromLink).put(toLink, shortest.travelTime);

        return shortest.travelTime;
    }

    void clearDataMap() {
        travelTimeDataMap.clear();
    }

    void storeInCache(Link fromLink, Link toLink, double travelTime) {
        travelTimeDataMap.get(fromLink).put(toLink, travelTime);
    }

    public void removeEntry(Link fromLink) {
        travelTimeDataMap.remove(fromLink);
    }

    int getMapSize() {
        return travelTimeDataMap.size();
    }
}
