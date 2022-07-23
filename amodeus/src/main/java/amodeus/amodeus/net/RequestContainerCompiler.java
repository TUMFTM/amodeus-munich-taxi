/* amodeus - Copyright (c) 2018, ETH Zurich, Institute for Dynamic Systems and Control */
package amodeus.amodeus.net;

import amodeus.amodeus.dispatcher.core.RequestStatus;
import amodeus.amodeus.util.math.GlobalAssert;
import org.matsim.contrib.dvrp.passenger.PassengerRequest;

import java.util.EnumSet;
import java.util.Objects;

enum RequestContainerCompiler {
    ;

    /**
     * @param avRequest     {@link PassengerRequest}
     * @param requestStatus {@link RequestStatus}
     * @return {@link RequestContainer} with information for storage and later viewing in
     * {@link SimulationObject}
     */
    public static RequestContainer compile( //
                                            PassengerRequest avRequest, //
                                            RequestStatus requestStatus) {
        GlobalAssert.that(Objects.nonNull(avRequest));

        // In future versions this can be removed, because it will be checked in the AV package already
        GlobalAssert.that(Objects.nonNull(avRequest.getFromLink()));
        GlobalAssert.that(Objects.nonNull(avRequest.getToLink()));

        RequestContainer requestContainer = new RequestContainer();
        requestContainer.requestIndex = avRequest.getId().index();
        requestContainer.fromLinkIndex = avRequest.getFromLink().getId().index();
        requestContainer.submissionTime = avRequest.getSubmissionTime();
        requestContainer.toLinkIndex = avRequest.getToLink().getId().index();
        requestContainer.requestStatus = EnumSet.of(requestStatus);
        requestContainer.passengerId = Integer.parseInt(avRequest.getPassengerId().toString());
        return requestContainer;
    }
}
