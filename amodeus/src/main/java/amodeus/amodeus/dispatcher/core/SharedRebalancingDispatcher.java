/* amodeus - Copyright (c) 2018, ETH Zurich, Institute for Dynamic Systems and Control */
package amodeus.amodeus.dispatcher.core;

import java.util.List;
import java.util.stream.Collectors;

import org.matsim.amodeus.config.AmodeusModeConfig;
import org.matsim.amodeus.plpc.ParallelLeastCostPathCalculator;
import org.matsim.api.core.v01.network.Link;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.config.Config;
import org.matsim.core.router.util.TravelTime;

import amodeus.amodeus.dispatcher.shared.SharedCourse;
import amodeus.amodeus.dispatcher.shared.SharedCourseAccess;
import amodeus.amodeus.dispatcher.shared.SharedMealType;
import amodeus.amodeus.dispatcher.shared.SharedMenu;
import amodeus.amodeus.net.MatsimAmodeusDatabase;
import amodeus.amodeus.util.math.GlobalAssert;

/** abstract base class for dispatchers that perform relocation of empty
 * vehicles (rebalancing) or redirect vehicles during customer journeys
 * to links which are not a dropoff or pickup location */
public abstract class SharedRebalancingDispatcher extends SharedUniversalDispatcher {

    protected SharedRebalancingDispatcher(Config config, AmodeusModeConfig operatorConfig, TravelTime travelTime, ParallelLeastCostPathCalculator parallelLeastCostPathCalculator,
            EventsManager eventsManager, //
            MatsimAmodeusDatabase db) {
        super(config, operatorConfig, travelTime, parallelLeastCostPathCalculator, eventsManager, db);
    }

    /** @param roboTaxi is rebalanced to
     * @param destination and all the oustanding pickup and dropoff tasks are deleted */
    protected final void setRoboTaxiRebalance(RoboTaxi roboTaxi, final Link destination) {
        GlobalAssert.that(roboTaxi.isWithoutCustomer());
        /** clear menu and put requests back to pending requests */
        cleanAndAbondon(roboTaxi);
        GlobalAssert.that(!SharedCourseAccess.hasStarter(roboTaxi));
        SharedCourse redirectCourse = SharedCourse.redirectCourse(destination, Double.toString(getTimeNow()) + roboTaxi.getId().toString());
        addSharedRoboTaxiRedirect(roboTaxi, redirectCourse);
    }

    /** {@link RoboTaxi} @param roboTaxi is redirected to the {@link Link} of the {@link SharedCourse}
     * the course can be moved to another position in the {@link SharedMenu} of the {@link} RoboTaxi */
    protected static void addSharedRoboTaxiRedirect(RoboTaxi roboTaxi, SharedCourse redirectCourse) {
        GlobalAssert.that(redirectCourse.getMealType().equals(SharedMealType.REDIRECT));
        roboTaxi.addRedirectCourseToMenu(redirectCourse);
    }

    /** @return {@link List } of all {@link RoboTaxi} which are currently rebalancing. */
    protected final List<RoboTaxi> getRebalancingRoboTaxis() {
        return getRoboTaxis().stream() //
                .filter(rt -> rt.getStatus().equals(RoboTaxiStatus.REBALANCEDRIVE)) //
                .collect(Collectors.toList());
    }

    /** @return {@link List} of all {@link RoboTaxi} which are divertable and not in a rebalacing task. */
    protected final List<RoboTaxi> getDivertableNotRebalancingRoboTaxis() {
        return getDivertableRoboTaxis().stream() //
                .filter(rt -> !rt.getStatus().equals(RoboTaxiStatus.REBALANCEDRIVE)) //
                .collect(Collectors.toList());
    }
}
