/* amodeus - Copyright (c) 2018, ETH Zurich, Institute for Dynamic Systems and Control */
package amodeus.amodtaxi.linkspeed.iterative;

import amodeus.amodeus.linkspeed.LSDataTravelTime;
import amodeus.amodeus.linkspeed.LinkSpeedDataContainer;
import org.matsim.api.core.v01.network.Network;
import org.matsim.core.router.DijkstraFactory;
import org.matsim.core.router.FastAStarEuclidean;
import org.matsim.core.router.FastAStarLandmarksFactory;
import org.matsim.core.router.costcalculators.OnlyTimeDependentTravelDisutility;
import org.matsim.core.router.util.LeastCostPathCalculator;
import org.matsim.core.router.util.TravelTime;

import static java.lang.Runtime.*;

public class LinkSpeedLeastPathCalculatorFastAStarLandmark {

    public static LeastCostPathCalculator from(Network network, LinkSpeedDataContainer lsData, int dt) {
        TravelTime travelTime = new LSDataTravelTime(lsData);
        return new FastAStarLandmarksFactory(getRuntime().availableProcessors()).createPathCalculator(network, new OnlyTimeDependentTravelDisutility(travelTime), travelTime);
//
//                new DijkstraFactory().createPathCalculator(network, //
//                new OnlyTimeDependentTravelDisutility(travelTime), travelTime);
    }
}
