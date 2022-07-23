package de.tum.mw.ftm.amod.taxi.dispatcher;

import amodeus.amodeus.dispatcher.core.RoboTaxi;
import amodeus.amodeus.dispatcher.core.UniversalDispatcher;
import amodeus.amodeus.dispatcher.util.TreeMaintainer;
import amodeus.amodeus.net.TensorCoords;
import ch.ethz.idsc.tensor.Tensor;
import ch.ethz.idsc.tensor.Tensors;
import org.apache.log4j.Logger;
import org.matsim.amodeus.config.FTMConfigGroup;
import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.network.Network;
import org.matsim.contrib.dvrp.passenger.PassengerRequest;
import org.matsim.core.config.Config;
import org.matsim.core.network.NetworkUtils;

import java.util.Collection;
import java.util.Objects;

public class FTM_NTNR_Dispatcher implements IFTMDispatcher {
    private final static Logger logger = Logger.getLogger(FTM_NTNR_Dispatcher.class);
    private final UniversalDispatcher universalDispatcher;
    private final TreeMaintainer<PassengerRequest> requestMaintainer;
    private final TreeMaintainer<RoboTaxi> roboTaxiMaintainer;
    private final long maxCustomerWaitingTime;

    public FTM_NTNR_Dispatcher(UniversalDispatcher universalDispatcher, Network network, Config config) {
        this.universalDispatcher = universalDispatcher;
        double[] networkBounds = NetworkUtils.getBoundingBox(network.getNodes().values());
        this.requestMaintainer = new TreeMaintainer<>(networkBounds, this::getLocation);
        this.roboTaxiMaintainer = new TreeMaintainer<>(networkBounds, this::getRoboTaxiLoc);
        FTMConfigGroup ftmConfigGroup = (FTMConfigGroup) config.getModules().get("ftm_simulation");
        this.maxCustomerWaitingTime = ftmConfigGroup.getMaxCustomerWaitingTime();
    }

    @Override
    public void dispatch(Collection<RoboTaxi> availableTaxis, Collection<PassengerRequest> requests, double timeNow) {
        if (availableTaxis.size() > 0 && requests.size() > 0) {
            availableTaxis.forEach(roboTaxiMaintainer::add);
            requests.forEach(requestMaintainer::add);

            boolean oversupply = false;
            if (availableTaxis.size() >= requests.size())
                oversupply = true;

            if (oversupply) {
                for (PassengerRequest request : requests) {
                    RoboTaxi closestTaxi = roboTaxiMaintainer.getClosest(getLocation(request));
                    if (closestTaxi != null) {
                        assignRequest(closestTaxi, request, timeNow);
                    }
                }
            } else {
                for (RoboTaxi taxi : availableTaxis) {
                    Coord coord = taxi.getDivertableLocation().getFromNode().getCoord();
                    Tensor tCoord = Tensors.vector(coord.getX(), coord.getY());
                    PassengerRequest closestRequest = requestMaintainer.getClosest(tCoord);
                    if (Objects.nonNull(closestRequest)) {
                        assignRequest(taxi, closestRequest, timeNow);
                    }
                }
            }

            roboTaxiMaintainer.clear();
            requestMaintainer.clear();
        }
    }

    private boolean assignRequest(RoboTaxi roboTaxi, PassengerRequest request, double timeNow){
        double arrivalTime = universalDispatcher.getEstimatedArrivalTime(roboTaxi.getLastKnownLocation(), request.getFromLink(), timeNow);
        if (arrivalTime-request.getSubmissionTime()<=maxCustomerWaitingTime*0.9) {
            universalDispatcher.setRoboTaxiPickup(roboTaxi, request);
            roboTaxiMaintainer.remove(roboTaxi);
            requestMaintainer.remove(request);
            return true;
        }
        else {
            logger.warn(String.format("Request %s has not been assigned, cause timeToArrival exceeds maxCustomerWaitingTime", request.getPassengerId()));
            logger.debug(String.format("Estimated arrival time for Passenger Request %s, at %f is %f",
                    request.getPassengerId(),
                    timeNow,
                    arrivalTime));
            return false;
        }
    }

    Tensor getLocation(PassengerRequest request) {
        return TensorCoords.toTensor(request.getFromLink().getFromNode().getCoord());
    }

    Tensor getRoboTaxiLoc(RoboTaxi roboTaxi) {
        return TensorCoords.toTensor(roboTaxi.getDivertableLocation().getCoord());
    }
}
