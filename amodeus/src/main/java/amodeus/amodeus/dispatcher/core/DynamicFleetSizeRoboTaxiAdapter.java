package amodeus.amodeus.dispatcher.core;

import org.matsim.amodeus.dvrp.schedule.*;
import org.matsim.contrib.dvrp.schedule.Task;

public class DynamicFleetSizeRoboTaxiAdapter implements FleetSizingRoboTaxiTaskListener{
    public DynamicFleetSizeRoboTaxiAdapter(Task task) {

        Task.TaskType taskType = task.getTaskType();
        if(taskType instanceof FleetSizingTask){
            switch ((FleetSizingTask) taskType){
                case LOGIN:
                    handle((AmodeusLoginTask) task);
                    break;
                case LOGOFF:
                    handle((OffServiceStayTask) task);
                    break;

            }
        }
        else if (taskType instanceof AmodeusTaskType) {
            switch ((AmodeusTaskType) taskType) {
                case PICKUP:
                    handle((AmodeusPickupTask) task);
                    break;
                case DROPOFF:
                    handle((AmodeusDropoffTask) task);
                    break;
                case DRIVE:
                    handle((AmodeusDriveTask) task);
                    break;
                case STAY:
                    handle((AmodeusStayTask) task);
                    break;
            }
        }
    }

    @Override
    public void handle(AmodeusLoginTask avStayTask) {

    }

    @Override
    public void handle(OffServiceStayTask avStayTask) {

    }

    @Override
    public void handle(AmodeusPickupTask avPickupTask) {

    }

    @Override
    public void handle(AmodeusDropoffTask avDropoffTask) {

    }

    @Override
    public void handle(AmodeusDriveTask avDriveTask) {

    }

    @Override
    public void handle(AmodeusStayTask avStayTask) {

    }


    public enum FleetSizingTask implements Task.TaskType {
        LOGIN, LOGOFF
    }
}
