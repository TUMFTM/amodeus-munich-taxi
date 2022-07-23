package de.tum.mw.ftm.amod.taxi.analysis;

import java.io.*;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;

import amodeus.amodeus.util.math.GlobalAssert;

import org.matsim.amodeus.config.AmodeusConfigGroup;
import org.matsim.amodeus.config.FTMConfigGroup;
import org.matsim.core.config.Config;
import org.matsim.core.config.groups.ControlerConfigGroup;
import org.matsim.core.controler.events.IterationEndsEvent;
import org.matsim.core.controler.events.IterationStartsEvent;
import org.matsim.core.controler.events.ShutdownEvent;
import org.matsim.core.controler.listener.IterationEndsListener;
import org.matsim.core.controler.listener.IterationStartsListener;
import org.matsim.core.controler.listener.ShutdownListener;
import com.google.inject.Singleton;

import com.google.inject.Inject;

@Singleton
public class PythonAnalysisOutputListener implements IterationStartsListener, IterationEndsListener, ShutdownListener {
    private final DateTimeFormatter noWhitespaceDateTimeFormatter;
    private final DateTimeFormatter dateTimeFormatter;
    private AmodeusConfigGroup amodeusConfigGroup;
    private FTMConfigGroup ftmConfigGroup;



    private final PassengerRequestAnalysisEventListener passengerRequestEventListener;
    private final FinalRoboTaxiSchedulesEventListener finalRoboTaxiSchedulesEventListener;
    private final FleetStatusLogEventListener fleetStatusLogEventListener;
    private final String dispatcher;
    private ZonedDateTime simulationRunStarted;
    private ZonedDateTime simulationRunFinished;
    private final LocalDateTime simStartDateTime;
    private final LocalDateTime simEndtDateTime;
    private final File outputDir;

    @Inject
    public PythonAnalysisOutputListener(ControlerConfigGroup controllerConfig,
                                        AmodeusConfigGroup amodeusConfigGroup,
                                        FTMConfigGroup ftmConfigGroup,
                                        Config matsimConfig) {


        this.amodeusConfigGroup = amodeusConfigGroup;
        this.ftmConfigGroup = ftmConfigGroup;


        this.passengerRequestEventListener = new PassengerRequestAnalysisEventListener(amodeusConfigGroup);
        this.finalRoboTaxiSchedulesEventListener = new FinalRoboTaxiSchedulesEventListener(ftmConfigGroup);
        this.fleetStatusLogEventListener = new FleetStatusLogEventListener();
        this.noWhitespaceDateTimeFormatter = DateTimeFormatter.ofPattern("yyyyMMdd-HHmm");
        this.dateTimeFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
        this.dispatcher = amodeusConfigGroup.getMode("av").getDispatcherConfig().getType();
        this.simStartDateTime = ftmConfigGroup.getSimStartDateTime();
        this.simEndtDateTime = ftmConfigGroup.getSimEndDateTime();
        outputDir = new File(Paths.get(matsimConfig.controler().getOutputDirectory(), "data", "PythonExport").toString());

    }


    @Override
    public void notifyIterationStarts(IterationStartsEvent event) {
        simulationRunStarted = ZonedDateTime.now();

        createPythonExportDir(outputDir);
        event.getServices().getEvents().addHandler(passengerRequestEventListener);
        event.getServices().getEvents().addHandler(finalRoboTaxiSchedulesEventListener);
        event.getServices().getEvents().addHandler(fleetStatusLogEventListener);

    }

    @Override
    public void notifyIterationEnds(IterationEndsEvent event) {
        simulationRunFinished = ZonedDateTime.now();

    }

    @Override
    public void notifyShutdown(ShutdownEvent event) {
        event.getServices().getEvents().removeHandler(passengerRequestEventListener);
        event.getServices().getEvents().removeHandler(finalRoboTaxiSchedulesEventListener);
        event.getServices().getEvents().removeHandler(fleetStatusLogEventListener);

        // DistanceGroupedByStatus

        FinalRoboTaxiScheduleCSVWriter finalRoboTaxiScheduleCSVWriter = new FinalRoboTaxiScheduleCSVWriter(finalRoboTaxiSchedulesEventListener);

        FleetStatusCSVWriter fleetStatusCSVWriter = new FleetStatusCSVWriter(fleetStatusLogEventListener, ftmConfigGroup);

        PassengerRequestInformationCSVWriter passengerRequestInformationCSVWriter = new PassengerRequestInformationCSVWriter(passengerRequestEventListener, ftmConfigGroup);

        // Write Outputs:

        try {
            finalRoboTaxiScheduleCSVWriter.wirteTripsCSV(generateFilePath("trips"));
            finalRoboTaxiScheduleCSVWriter.writeDistanceByStatus(generateFilePath("distanceGroupedByStatus"));
            fleetStatusCSVWriter.writeCSV(generateFilePath("fleetStatus"));
            passengerRequestInformationCSVWriter.writeCSV(generateFilePath("requests"));
            writeSimulataionMetadataCSV(generateFilePath("simulationMetadata"));
        } catch (IOException e) {
            e.printStackTrace();
        }

    }
    private void writeSimulataionMetadataCSV(File path) throws IOException{

        BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(path)));

        writer.write(String.join(",", new String[] { //
                "simulation_run_started",
                "simulation_run_finished",
                "simulation_start_time",
                "simulation_end_time",
                "dispatcher",
                "dispatching_period",
                "rebalancing_period",
                "number_of_requests",
                "number_of_vehicles"  }) + "\n");
        writer.write(String.join(",", new String[] { //
                dateTimeFormatter.format(simulationRunStarted),
                dateTimeFormatter.format(simulationRunFinished),
                dateTimeFormatter.format(simStartDateTime),
                dateTimeFormatter.format(simEndtDateTime),
                dispatcher,
                String.valueOf(ftmConfigGroup.getDispatchingPeriodSeconds()),
                String.valueOf(ftmConfigGroup.getRebalancingPeriodSeconds()),
                String.valueOf(passengerRequestEventListener.getPassengerRequests().size()),
                String.valueOf(fleetStatusLogEventListener.getFleetSize())

        }) + "\n");
        writer.close();
    }

    private File generateFilePath(String fileBaseName) throws IOException {
        GlobalAssert.that(outputDir.isDirectory());

        String filename = fileBaseName + "_"
                + this.dispatcher + "_"
                + this.simStartDateTime.format(this.noWhitespaceDateTimeFormatter) + "_"
                + this.simEndtDateTime.format(this.noWhitespaceDateTimeFormatter) + ".csv";

        return new File(outputDir, filename);

    }

    private File createPythonExportDir(File folder) {
        if (folder.isDirectory()) {
            return folder;
        }
        folder.mkdirs();
        return folder;
    }
}