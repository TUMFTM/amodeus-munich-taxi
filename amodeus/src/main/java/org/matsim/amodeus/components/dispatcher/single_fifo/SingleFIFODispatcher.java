package org.matsim.amodeus.components.dispatcher.single_fifo;

import java.util.LinkedList;
import java.util.Queue;

import org.matsim.amodeus.components.AmodeusDispatcher;
import org.matsim.amodeus.components.AmodeusRouter;
import org.matsim.amodeus.components.dispatcher.AVVehicleAssignmentEvent;
import org.matsim.amodeus.components.dispatcher.utils.SingleRideAppender;
import org.matsim.amodeus.config.AmodeusModeConfig;
import org.matsim.amodeus.dvrp.schedule.AmodeusTaskType;
import org.matsim.contrib.dvrp.fleet.DvrpVehicle;
import org.matsim.contrib.dvrp.passenger.PassengerRequest;
import org.matsim.contrib.dvrp.run.ModalProviders.InstanceGetter;
import org.matsim.contrib.dvrp.schedule.Task;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.router.util.TravelTime;

public class SingleFIFODispatcher implements AmodeusDispatcher {
    static public final String TYPE = "SingleFIFO";

    final private SingleRideAppender appender;
    final private Queue<DvrpVehicle> availableVehicles = new LinkedList<>();
    final private Queue<PassengerRequest> pendingRequests = new LinkedList<>();

    final private EventsManager eventsManager;

    private final String mode;

    private boolean reoptimize = false;

    public SingleFIFODispatcher(String mode, EventsManager eventsManager, SingleRideAppender appender) {
        this.appender = appender;
        this.eventsManager = eventsManager;
        this.mode = mode;
    }

    @Override
    public void onRequestSubmitted(PassengerRequest request) {
        pendingRequests.add(request);
        reoptimize = true;
    }

    @Override
    public void onNextTaskStarted(DvrpVehicle vehicle) {
        Task task = vehicle.getSchedule().getCurrentTask();
        if (task.getTaskType() == AmodeusTaskType.STAY) {
            availableVehicles.add(vehicle);
        }
    }

    @Override
    public void addVehicle(DvrpVehicle vehicle) {
        availableVehicles.add(vehicle);
        eventsManager.processEvent(new AVVehicleAssignmentEvent(mode, vehicle.getId(), 0));
    }

    private void reoptimize(double now) {
        while (availableVehicles.size() > 0 && pendingRequests.size() > 0) {
            DvrpVehicle vehicle = availableVehicles.poll();
            PassengerRequest request = pendingRequests.poll();
            appender.schedule(request, vehicle, now);
        }

        reoptimize = false;
    }

    @Override
    public void onNextTimestep(double now) {
        appender.update();
        if (reoptimize)
            reoptimize(now);
    }

    static public class Factory implements AVDispatcherFactory {
        @Override
        public AmodeusDispatcher createDispatcher(InstanceGetter inject) {
            EventsManager eventsManager = inject.get(EventsManager.class);
            TravelTime travelTime = inject.getModal(TravelTime.class);
            AmodeusModeConfig operatorConfig = inject.getModal(AmodeusModeConfig.class);
            AmodeusRouter router = inject.getModal(AmodeusRouter.class);

            return new SingleFIFODispatcher(operatorConfig.getMode(), eventsManager, new SingleRideAppender(operatorConfig.getTimingConfig(), router, travelTime));
        }
    }
}
