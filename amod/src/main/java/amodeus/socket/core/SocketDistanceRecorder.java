/* amodeus - Copyright (c) 2018, ETH Zurich, Institute for Dynamic Systems and Control */
package amodeus.socket.core;

import amodeus.amodeus.net.MatsimAmodeusDatabase;
import amodeus.amodeus.net.SimulationObject;
import ch.ethz.idsc.tensor.Tensor;

import java.util.HashMap;
import java.util.Map;

/* package */ class SocketDistanceRecorder {
    private final MatsimAmodeusDatabase db;
    private final Map<Integer, SocketVehicleStatistic> map = new HashMap<>();

    public SocketDistanceRecorder(MatsimAmodeusDatabase db) {
        this.db = db;
    }

    Tensor distance(SimulationObject simulationObject) {
        return simulationObject.vehicles.stream() //
                .map(vehicleContainer -> map.computeIfAbsent(vehicleContainer.vehicleIndex, //
                        i -> new SocketVehicleStatistic(db)).distance(vehicleContainer)) //
                .reduce(Tensor::add) //
                .orElse(StaticHelper.ZEROS.copy());
    }
}
