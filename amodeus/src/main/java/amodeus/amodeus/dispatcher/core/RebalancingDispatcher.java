/* amodeus - Copyright (c) 2018, ETH Zurich, Institute for Dynamic Systems and Control */
package amodeus.amodeus.dispatcher.core;

import java.util.List;
import java.util.stream.Collectors;

import org.matsim.amodeus.config.AmodeusModeConfig;
import org.matsim.amodeus.plpc.ParallelLeastCostPathCalculator;
import org.matsim.api.core.v01.network.Link;
import org.matsim.contrib.dvrp.passenger.PassengerRequest;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.config.Config;
import org.matsim.core.router.util.TravelTime;

import amodeus.amodeus.net.MatsimAmodeusDatabase;
import amodeus.amodeus.util.math.GlobalAssert;

/** class for wich all Dispatchers performing rebalancing, i.e., replacement of empty vehicles should be derived */
public abstract class RebalancingDispatcher extends UniversalDispatcher {

    protected RebalancingDispatcher(Config config, AmodeusModeConfig operatorConfig, TravelTime travelTime, //
            ParallelLeastCostPathCalculator parallelLeastCostPathCalculator, EventsManager eventsManager, //
            MatsimAmodeusDatabase db) {
        super(config, operatorConfig, travelTime, parallelLeastCostPathCalculator, eventsManager, db);
    }

    /** Command to rebalance {@link RoboTaxi} to a certain {@link Link} destination. The {@link RoboTaxi} will appear as
     * Rebalancing in the visualizer. Can only be used for {@link RoboTaxi} which are without a customer and divertible.
     * Function can only be invoked one time in each iteration of {@link RoboTaxiMaintainer#redispatch}
     * 
     * @param roboTaxi
     * @param destination */
    public final void setRoboTaxiRebalance(final RoboTaxi roboTaxi, final Link destination) {
        GlobalAssert.that(roboTaxi.isWithoutCustomer());
        /** if {@link RoboTaxi} is during pickup, remove from pickup register */
        if (isInPickupRegister(roboTaxi)) {
            PassengerRequest toRemove = getPickupRoboTaxis().get(roboTaxi);
            removeFromPickupRegisters(toRemove);
        }
        setRoboTaxiDiversion(roboTaxi, destination, RoboTaxiStatus.REBALANCEDRIVE);
        eventsManager.processEvent(RebalanceVehicleEvent.create(getTimeNow(), roboTaxi, destination));
    }

    /** @return {@link java.util.List } of all {@link RoboTaxi} which are currently rebalancing. */
    protected final List<RoboTaxi> getRebalancingRoboTaxis() {
        return getRoboTaxis().stream() //
                .filter(rt -> rt.getStatus().equals(RoboTaxiStatus.REBALANCEDRIVE)) //
                .collect(Collectors.toList());
    }

    /** @return {@link java.util.List} of all {@link RoboTaxi} which are divertable and not in a rebalacing
     *         task. */
    protected final List<RoboTaxi> getDivertableNotRebalancingRoboTaxis() {
        return getDivertableRoboTaxis().stream() //
                .filter(rt -> !rt.getStatus().equals(RoboTaxiStatus.REBALANCEDRIVE)) //
                .collect(Collectors.toList());
    }

}
