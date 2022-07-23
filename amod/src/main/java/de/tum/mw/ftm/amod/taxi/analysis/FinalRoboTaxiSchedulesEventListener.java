package de.tum.mw.ftm.amod.taxi.analysis;

import amodeus.amodeus.dispatcher.core.RoboTaxi;
import amodeus.amodeus.util.math.GlobalAssert;
import de.tum.mw.ftm.amod.taxi.preprocessing.demand.TaxiRide;
import org.apache.log4j.Logger;
import de.tum.mw.ftm.amod.analysis.events.trips.FinalRoboTaxiSchedulesEvent;
import de.tum.mw.ftm.amod.analysis.events.trips.FinalRoboTaxiSchedulesEventHandler;
import org.matsim.amodeus.config.FTMConfigGroup;
import org.matsim.amodeus.config.FTMRevenueConfig;
import org.matsim.amodeus.dvrp.schedule.AmodeusDriveTask;
import org.matsim.amodeus.dvrp.schedule.AmodeusDropoffTask;
import org.matsim.amodeus.dvrp.schedule.AmodeusPickupTask;
import org.matsim.contrib.dvrp.passenger.PassengerRequest;
import org.matsim.contrib.dvrp.schedule.Schedule;
import org.matsim.contrib.dvrp.schedule.Task;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

import static de.tum.mw.ftm.amod.analysis.AnalysisUtils.*;

public class FinalRoboTaxiSchedulesEventListener implements FinalRoboTaxiSchedulesEventHandler {
    private final static Logger logger = Logger.getLogger(FinalRoboTaxiSchedulesEventListener.class);
    private List<TaxiRide> taxiRides = new ArrayList<>();
    private final FTMConfigGroup ftmConfigGroup;
    private double simulationEndTime;
    public FinalRoboTaxiSchedulesEventListener(FTMConfigGroup ftmConfigGroup) {
        this.ftmConfigGroup = ftmConfigGroup;
    }

    @Override
    public void handleEvent(FinalRoboTaxiSchedulesEvent event) {
        simulationEndTime = event.getTime();
        Collection<RoboTaxi> roboTaxis = event.getRoboTaxis();
        for (RoboTaxi roboTaxi : roboTaxis){
            //TODO: @michaelwittmann Maybe replace by just toString()
            int id = Integer.parseInt(roboTaxi.getId().toString().split("\\:")[2]);

            taxiRides.addAll(extractTripsFromDVRPSchedule(roboTaxi.getSchedule(), id));

        }
    }


    public List<TaxiRide> getTaxiRides() {
        return taxiRides;
    }

    List<TaxiRide> extractTripsFromDVRPSchedule(Schedule schedule, int vehicleId) {
        List<TaxiRide> taxiRides = new ArrayList<>();
        //Filter only performed tasks, usually the last task has endtime infinity, makes problem in analysis
        for (Task task : schedule.getTasks().stream().filter(t->t.getStatus()!= Task.TaskStatus.PLANNED).collect(Collectors.toSet())) {
            TaxiTripType taxiTripType = getTaxiTripTypeFromTask(task);
            double distance = getDistanceFromTask(task);
            String requestId = getPassengerRequestIdFromTask(task);

            TaxiRide taxiRide = new TaxiRide(requestId,
                    vehicleId,
                    ftmConfigGroup.getSimStartDateTime().plusSeconds((long) task.getBeginTime()),
                    ftmConfigGroup.getSimStartDateTime().plusSeconds((long) getTaskEndTime(task)),
                    taxiTripType,
                    getStatLinkFromTask(task),
                    getEndLinkFromTask(task),
                    distance,
                    calculateCosts(distance,task.getBeginTime(), getTaskEndTime(task), taxiTripType),
                    calculateRevenue(distance, taxiTripType)
                    );
        taxiRides.add(taxiRide);
        }
        return taxiRides;
    }

    private double getTaskEndTime(Task task) {
        switch (task.getStatus()){
            case PLANNED:
                return Double.MAX_VALUE;
            case STARTED:
                return simulationEndTime;
            case PERFORMED:
                return task.getEndTime();
            default:
                return Double.MAX_VALUE;
        }
    }


    private double calculateCosts(double distance, double tripStartTime, double tripEndTime, TaxiTripType tripType) {
        GlobalAssert.that(tripEndTime >= tripStartTime);
        if (tripType == TaxiTripType.OFFSERVICE) {
            return 0;
        } else {
            return ftmConfigGroup.getFtmCostsConfig().getCostsPerKm() * distance / 1000
                    + ftmConfigGroup.getFtmCostsConfig().getCostsPerHour() * (tripEndTime - tripStartTime) / 3600;
        }
    }

    private double calculateRevenue(double distance, TaxiTripType tripType) {
        if (tripType == TaxiTripType.OCCUPIED) {
            FTMRevenueConfig ftmRevenueConfig = ftmConfigGroup.getFtmRevenueConfig();
            if (distance <= 5000) {
                double metersPer20Cent = 1000 * 0.2 / ftmRevenueConfig.getRevenuePerKmBelow5Km();
                return Math.floor(distance/ metersPer20Cent) * 0.2 + ftmRevenueConfig.getRevenuePerTrip();
            } else if (distance <= 10000) {
                double metersPer20Cent = 1000 * 0.2 / ftmRevenueConfig.getRevenuePerKmBelow10Km();
                return 10 + Math.floor((distance - 5000) / metersPer20Cent) * 0.2 + ftmRevenueConfig.getRevenuePerTrip();
            } else {
                double metersPer20Cent = 1000 * 0.2 / ftmRevenueConfig.getRevenuePerKmAbove10Km();
                return 19 + Math.floor((distance - 10000) / metersPer20Cent) * 0.2 + ftmRevenueConfig.getRevenuePerTrip();
            }
        } else {
            return 0;
        }
    }


    private String getPassengerRequestIdFromTask(Task task){
        String passegerId = "null";
        if (task instanceof AmodeusPickupTask) {
            passegerId = getPassengerRequestIdFromRequests(((AmodeusPickupTask)task).getRequests().values());
        }
        if(task instanceof AmodeusDropoffTask){
            passegerId = getPassengerRequestIdFromRequests(((AmodeusDropoffTask)task).getRequests().values());
        }
        if(task instanceof AmodeusDriveTask){
            passegerId = getPassengerRequestIdFromRequests(((AmodeusDriveTask)task).getRequests());
        }
        return passegerId;
    }
    private String getPassengerRequestIdFromRequests(Collection<PassengerRequest> requests){
        List<PassengerRequest> requestsList = new ArrayList<>(requests);
        if(requestsList.size()>1){
            throw new IllegalStateException("This analysis is not compatible with multiple requests.");
        }
        if (requests.size()==0){
            return "null";
        }
        return requestsList.get(0).getPassengerId().toString();
    }

}
