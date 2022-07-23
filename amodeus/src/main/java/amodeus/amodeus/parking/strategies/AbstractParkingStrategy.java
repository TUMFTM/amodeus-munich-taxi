/* amodeus - Copyright (c) 2019, ETH Zurich, Institute for Dynamic Systems and Control */
package amodeus.amodeus.parking.strategies;

import org.matsim.api.core.v01.network.Network;

import amodeus.amodeus.parking.capacities.ParkingCapacity;
import amodeus.amodeus.routing.DistanceFunction;

/* package */ abstract class AbstractParkingStrategy implements ParkingStrategy {

    protected ParkingCapacity parkingCapacity;
    protected Network network;
    protected DistanceFunction distanceFunction;

    /** this function gives the implementation the possibility to use the network and
     * distance function which will only be available after the construction.
     * Normally this function should be called in the constructor of the dispatcher.
     * 
     * @param network
     * @param distanceFunction */
    @Override
    public void setRuntimeParameters(ParkingCapacity parkingCapacity, Network network, //
            DistanceFunction distanceFunction) {
        this.parkingCapacity = parkingCapacity;
        this.network = network;
        this.distanceFunction = distanceFunction;
    }
}
