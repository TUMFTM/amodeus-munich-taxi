package de.tum.mw.ftm.amod.taxi.dispatcher;

import amodeus.amodeus.dispatcher.core.RoboTaxi;
import amodeus.amodeus.dispatcher.util.*;
import amodeus.amodeus.util.matsim.SafeConfig;
import org.matsim.api.core.v01.network.Network;
import org.matsim.contrib.dvrp.passenger.PassengerRequest;


import java.util.Collection;
import java.util.Map;

public class FTMBipartiteMatcher implements TaxiRequestMatcher {
    private final AbstractRoboTaxiDestMatcher globalBipartiteMatcher;
    /** Allows to instantiate a configurable bipartite matching algorithm via the av.xml file, there are two options:
     * - 1 (default option): the Hungarian method is used, this is chosen if no specification is given in av.xml or the specification
     *
     * <dispatcher strategy="GlobalBipartiteMatchingDispatcher">
     * <param name="matchingAlgorithm" value="HUNGARIAN" />
     *
     * -2: solution of the assignment problem via Integer Linear Program, for this option the av.xml file should look as follows
     * <dispatcher strategy="GlobalBipartiteMatchingDispatcher">
     * <param name="matchingAlgorithm" value="ILP" />
     * <param name="matchingWeight" value="[1.0,1.0,1.0]" />
     *
     * The values are retrieved via @param safeConfig, other parameters necessary for instantiation are
     * the network @param network, and the distance function @param distanceFunction */
    public FTMBipartiteMatcher(GlobalBipartiteCost cost, SafeConfig safeConfig) {

        String matchingAlg = safeConfig.getString("matchingAlgorithm", "HUNGARIAN");
        switch (matchingAlg) {
            case "HUNGARIAN":
                globalBipartiteMatcher = new GlobalBipartiteMatching(cost);
                break;
            case "ILP":
                globalBipartiteMatcher = new GlobalBipartiteMatchingILP(cost, safeConfig);
                break;
            default:
                // hungarian = null;
                // globalBipartiteMatcher = null;
                throw new RuntimeException("An invalid option for the matching algorithm was chosen. " + matchingAlg);
        }
    }


    public Map<RoboTaxi, PassengerRequest> getGBPMatch(Collection<RoboTaxi> roboTaxis, /** <- typically universalDispatcher.getDivertableRoboTaxis() */
                                                       Collection<PassengerRequest> requests){ /** <- typically universalDispatcher.getPassengerRequests() */
        return globalBipartiteMatcher.match(roboTaxis, requests);
    }


    @Override
    public Map<RoboTaxi, PassengerRequest> matchRequests(Collection<RoboTaxi> roboTaxis, Collection<PassengerRequest> requests) {
        return getGBPMatch(roboTaxis, requests);

    }

}
