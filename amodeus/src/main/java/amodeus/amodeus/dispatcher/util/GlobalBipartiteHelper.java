/* amodeus - Copyright (c) 2019, ETH Zurich, Institute for Dynamic Systems and Control */
package amodeus.amodeus.dispatcher.util;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.matsim.api.core.v01.network.Link;

import amodeus.amodeus.dispatcher.core.RoboTaxi;
import amodeus.amodeus.util.hungarian.HungarianAlgorithmWrap;
import amodeus.amodeus.util.math.GlobalAssert;

/* package */ enum GlobalBipartiteHelper {
    ;

    public static <T> Map<RoboTaxi, T> genericMatch(Collection<RoboTaxi> roboTaxis, Collection<T> linkObjects, //
            Function<T, Link> linkOfT, GlobalBipartiteCost globalBipartiteCost) {
        /** storage in {@link List} as {@link Collection} does not guarantee order */
        final List<RoboTaxi> orderedRoboTaxis = new ArrayList<>(roboTaxis);
        final List<T> ordered_linkObjects = new ArrayList<>(linkObjects);

        /** setup cost matrix */
        final int n = orderedRoboTaxis.size(); // workers
        final int m = ordered_linkObjects.size(); // jobs
        final double[][] costMatrix = new double[n][m];

        /** cost of assigning vehicle i to dest j, i.e. distance from vehicle i to destination j */
        int i = 0;
        for (RoboTaxi roboTaxi : orderedRoboTaxis) {
            int j = 0;
            for (T t : ordered_linkObjects)
                costMatrix[i][j++] = globalBipartiteCost.between(roboTaxi, linkOfT.apply(t));
            ++i;
        }

        /** vehicle at position i is assigned to destination matchinghungarianAlgorithm[j],
         * int[] matchinghungarianAlgorithm = new HungarianAlgorithm(distancematrix).execute(); O(n^3) */
        int[] matchinghungarianAlgorithm = HungarianAlgorithmWrap.matching(costMatrix);

        /** do the assignment according to the Hungarian algorithm (only for the matched elements) */
        AtomicInteger ai = new AtomicInteger();
        final Map<RoboTaxi, T> map = orderedRoboTaxis.stream().filter(rt -> 0 <= matchinghungarianAlgorithm[ai.getAndIncrement()]) //
                .collect(Collectors.toMap(rt -> rt, rt -> ordered_linkObjects.get(matchinghungarianAlgorithm[ai.get() - 1])));
        GlobalAssert.that(map.size() == Math.min(n, m));
        return map;
    }

}
