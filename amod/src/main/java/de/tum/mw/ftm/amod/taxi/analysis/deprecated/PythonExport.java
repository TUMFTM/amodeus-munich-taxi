package de.tum.mw.ftm.amod.taxi.analysis.deprecated;

import amodeus.amodeus.analysis.AnalysisSummary;
import amodeus.amodeus.analysis.RemoveUnit;
import amodeus.amodeus.analysis.ScenarioParameters;
import amodeus.amodeus.analysis.element.AnalysisExport;
import amodeus.amodeus.analysis.element.DistanceElement;
import amodeus.amodeus.util.io.SaveFormats;
import amodeus.amodeus.util.math.GlobalAssert;
import ch.ethz.idsc.tensor.RealScalar;
import ch.ethz.idsc.tensor.Tensor;
import ch.ethz.idsc.tensor.Tensors;
import ch.ethz.idsc.tensor.img.ColorDataIndexed;
import ch.ethz.idsc.tensor.io.TableBuilder;
import de.tum.mw.ftm.amod.taxi.preprocessing.demand.TaxiRide;
import org.matsim.core.utils.misc.OptionalTime;

import java.io.File;
import java.io.IOException;
import java.time.LocalDateTime;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Collection;
import java.util.Map;

public class PythonExport implements AnalysisExport {
    private final SimulationObjectRecorder simulationObjectRecorder;
    private final LocalDateTime simStartDateTime;
    private final LocalDateTime simEndtDateTime;
    private final ZonedDateTime simRunStartDateTime;
    private final ZonedDateTime simRunEndDateTime;
    private final DateTimeFormatter dateTimeFormatter;
    private final DateTimeFormatter noWhitespaceDateTimeFormatter;
    private String dispatcher;

    public PythonExport(SimulationObjectRecorder simulationObjectRecorder, LocalDateTime simStartDateTime,
                        LocalDateTime simEndDateTime, ZonedDateTime simRunStartDateTime,
                        ZonedDateTime simRunEndDateTime) {
        this.simulationObjectRecorder = simulationObjectRecorder;
        this.simStartDateTime = simStartDateTime;
        this.simEndtDateTime = simEndDateTime;
        this.simRunEndDateTime = simRunEndDateTime;
        this.simRunStartDateTime = simRunStartDateTime;
        this.dateTimeFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
        this.noWhitespaceDateTimeFormatter = DateTimeFormatter.ofPattern("yyyyMMdd-HHmm");
        this.dispatcher = "";
    }

    @Override
    public void summaryTarget(AnalysisSummary analysisSummary, File relativeDirectory, ColorDataIndexed colorDataIndexed) {
        this.dispatcher = analysisSummary.getScenarioParameters().dispatcher;

        exportSimulationInformation(analysisSummary, relativeDirectory);

        exportFleetStatus(analysisSummary, relativeDirectory);
        exportDistanceGroupedByStatus(analysisSummary, relativeDirectory);

        Map<Integer, VehicleTripAnalyzer> tripAnalyzerMap = simulationObjectRecorder.getTripAnalyzerMap();
        exportTrips(tripAnalyzerMap.values(), relativeDirectory);

        Map<Integer, PassengerRequestInformation> requestTimeInformationMap = simulationObjectRecorder.getRequestInformationMap();
        exportRequests(requestTimeInformationMap, relativeDirectory);

    }

    private void exportSimulationInformation(AnalysisSummary analysisSummary, File relativeDirectory) {
        TableBuilder tableBuilder = new TableBuilder();
        Tensor columnsTensor = Tensors.of(
                Tensors.fromString("simulation_run_started"),
                Tensors.fromString("simulation_run_finished"),
                Tensors.fromString("simulation_start_time"),
                Tensors.fromString("simulation_end_time"),
                Tensors.fromString("dispatcher"),
                Tensors.fromString("rebalancing_period"),
                Tensors.fromString("dispatching_period"),
                Tensors.fromString("number_of_requests"),
                Tensors.fromString("number_of_vehicles")
        );

        tableBuilder.appendRow(columnsTensor);

        ScenarioParameters scenarioParameters = analysisSummary.getScenarioParameters();
        Tensor valueTensor = Tensors.of(
                Tensors.fromString(simRunStartDateTime.format(dateTimeFormatter)),
                Tensors.fromString(simRunEndDateTime.format(dateTimeFormatter)),
                Tensors.fromString(simStartDateTime.format(dateTimeFormatter)),
                Tensors.fromString(simEndtDateTime.format(dateTimeFormatter)),
                Tensors.fromString(this.dispatcher),
                RealScalar.of(scenarioParameters.rebalancingPeriod),
                RealScalar.of(scenarioParameters.redispatchPeriod),
                RealScalar.of(analysisSummary.getSimulationInformationElement().reqsize()),
                RealScalar.of(analysisSummary.getSimulationInformationElement().vehicleSize())
        );
        tableBuilder.appendRow(valueTensor);

        try {
            savePythonExportCSV(tableBuilder.getTable(), relativeDirectory, "simulationMetadata");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void exportFleetStatus(AnalysisSummary analysisSummary, File relativeDirectory) {
        TableBuilder tableBuilder = new TableBuilder();
        Tensor columnsTensor = Tensors.of(
                Tensors.fromString("time"),
                Tensors.fromString("count_occupied"),
                Tensors.fromString("count_approach"),
                Tensors.fromString("count_rebalancing"),
                Tensors.fromString("count_idle"),
                Tensors.fromString("count_inactive")
        );
        tableBuilder.appendRow(columnsTensor);
        Tensor timeTensor = Tensors.fromString(simStartDateTime.format(dateTimeFormatter));
        Tensor valueTensor = Tensors.of(
                RealScalar.of(0),
                RealScalar.of(0),
                RealScalar.of(0),
                RealScalar.of(0),
                analysisSummary.getStatusDistribution().statusTensor.get(0).stream().reduce(Tensor::add).orElse(RealScalar.of(0))
        );
        tableBuilder.appendRow(Tensors.of(timeTensor, valueTensor));

        for (int index = 0; index < analysisSummary.getStatusDistribution().time.length(); ++index) {
            timeTensor = Tensors.fromString(simStartDateTime.plusSeconds(
                    analysisSummary.getStatusDistribution().time.Get(index).number().longValue())
                    .format(dateTimeFormatter));
            valueTensor = analysisSummary.getStatusDistribution().statusTensor.get(index);
            tableBuilder.appendRow(Tensors.of(timeTensor, valueTensor));
        }

        try {
            savePythonExportCSV(tableBuilder.getTable(), relativeDirectory, "fleetStatus");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void exportDistanceGroupedByStatus(AnalysisSummary analysisSummary, File relativeDirectory) {
        DistanceElement distanceElement = analysisSummary.getDistanceElement();

        TableBuilder tableBuilder = new TableBuilder();
        Tensor columnsTensor = Tensors.of(
                Tensors.fromString("with_customer"),
                Tensors.fromString("to_customer"),
                Tensors.fromString("rebalance")
        );
        tableBuilder.appendRow(columnsTensor);
        tableBuilder.appendRow(Tensors.of(distanceElement.totalDistanceWtCst, distanceElement.totalDistancePicku, distanceElement.totalDistanceRebal));

        try {
            savePythonExportCSV(tableBuilder.getTable(), relativeDirectory, "distanceGroupedByStatus");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void exportTrips(Collection<VehicleTripAnalyzer> vehicleTripAnalyzers, File relativeDirectory) {
        TableBuilder tableBuilder = new TableBuilder();
        Tensor columnsTensor = Tensors.of(
                Tensors.fromString("trip_id"),
                Tensors.fromString("request_id"),
                Tensors.fromString("vehicle_id"),
                Tensors.fromString("start_date_time"),
                Tensors.fromString("end_date_time"),
                Tensors.fromString("taxi_status"),
                Tensors.fromString("start_location"),
                Tensors.fromString("end_location"),
                Tensors.fromString("distance"),
                Tensors.fromString("costs"),
                Tensors.fromString("revenue")
        );
        tableBuilder.appendRow(columnsTensor);
        for (VehicleTripAnalyzer vehicleTripAnalyzer : vehicleTripAnalyzers) {
            for (TaxiRide taxiRide : vehicleTripAnalyzer.getTaxiRides()) {
                tableBuilder.appendRow(Tensors.of(
                        RealScalar.of(taxiRide.getTripId()),
                        Tensors.fromString(taxiRide.getRequestId()),
                        RealScalar.of(taxiRide.getVehicleId()),
                        Tensors.fromString(taxiRide.getTimestampStart().format(dateTimeFormatter)),
                        Tensors.fromString(taxiRide.getTimestampStop().format(dateTimeFormatter)),
                        Tensors.fromString(taxiRide.getType()),
                        Tensors.fromString(taxiRide.getLocationStart().toText()),
                        Tensors.fromString(taxiRide.getLocationStop().toText()),
                        RealScalar.of(taxiRide.getDistance()),
                        RealScalar.of(taxiRide.getOverallCosts()),
                        RealScalar.of(taxiRide.getOverallFare())));
            }
        }

        try {
            savePythonExportCSV(tableBuilder.getTable(), relativeDirectory, "trips");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void exportRequests(Map<Integer, PassengerRequestInformation> requestTimeInformationMap, File relativeDirectory) {
        TableBuilder tableBuilder = new TableBuilder();
        Tensor columnsTensor = Tensors.of(
                Tensors.fromString("request_id"),
                Tensors.fromString("timestamp_submitted"),
                Tensors.fromString("timestamp_dispatched"),
                Tensors.fromString("timestamp_arrived"),
                Tensors.fromString("timestamp_started"),
                Tensors.fromString("location_start"),
                Tensors.fromString("location_stop"),
                Tensors.fromString("denied")
        );
        tableBuilder.appendRow(columnsTensor);

        for (Map.Entry<Integer, PassengerRequestInformation> entry : requestTimeInformationMap.entrySet()) {
            if (!entry.getValue().isTripConsistent())
                System.err.println(String.format("Trip %d is not consistent. Please check it.", entry.getKey()));
            tableBuilder.appendRow(Tensors.of(
                    RealScalar.of(entry.getKey())),
                    Tensors.fromString(getDateTimeStringForOptionalTime(entry.getValue().getSubmissionTime())),
                    Tensors.fromString(getDateTimeStringForOptionalTime(entry.getValue().getAssignTime())),
                    Tensors.fromString(getDateTimeStringForOptionalTime(entry.getValue().getPickupTime())),
                    Tensors.fromString(getDateTimeStringForOptionalTime(entry.getValue().getStartTime())),
                    Tensors.fromString(entry.getValue().getStartLocation().toText()),
                    Tensors.fromString(entry.getValue().getStopLocation().toText()),
                    Tensors.fromString(String.valueOf(entry.getValue().isCanceled()))
            );
        }

        try {
            savePythonExportCSV(tableBuilder.getTable(), relativeDirectory, "requests");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private String getDateTimeStringForOptionalTime(OptionalTime time) {
        if (time.isUndefined()) {
            return "";
        } else {
            return simStartDateTime.plusSeconds((long) time.seconds()).format(dateTimeFormatter);
        }
    }

    private void savePythonExportCSV(Tensor quantityMatrix, File saveToFolder, String name) throws IOException {
        GlobalAssert.that(saveToFolder.isDirectory());
        File folder = createPythonExportDir(saveToFolder);

        Tensor bareMatrix = quantityMatrix.map(RemoveUnit.FUNCTION);
        SaveFormats.CSV.save(bareMatrix, folder, name + "_"
                + this.dispatcher + "_"
                + this.simStartDateTime.format(this.noWhitespaceDateTimeFormatter) + "_"
                + this.simEndtDateTime.format(this.noWhitespaceDateTimeFormatter));
    }

    private File createPythonExportDir(File saveToFolder) {
        File folder = new File(saveToFolder, "PythonExport");
        if (folder.isDirectory()) {
            return folder;
        }
        folder.mkdir();
        return folder;
    }
}
