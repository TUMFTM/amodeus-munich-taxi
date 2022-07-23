package de.tum.mw.ftm.amod.taxi.analysis;

import de.tum.mw.ftm.amod.analysis.events.passengerrequest.PassengerRequestInformation;
import org.matsim.amodeus.config.AmodeusConfigGroup;
import org.matsim.amodeus.config.FTMConfigGroup;
import org.matsim.core.utils.misc.OptionalTime;

import java.io.*;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public class PassengerRequestInformationCSVWriter {
    private final List<PassengerRequestInformation>  passengerRequests;
    private final DateTimeFormatter dtf = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    private final LocalDateTime simStartTime;
    public PassengerRequestInformationCSVWriter(PassengerRequestAnalysisEventListener passengerRequestAnalysisEventListener, FTMConfigGroup ftmConfigGroup) {
       this.passengerRequests = new ArrayList<>(passengerRequestAnalysisEventListener.getPassengerRequests());
        simStartTime = ftmConfigGroup.getSimStartDateTime();
       passengerRequests.sort(new Comparator<PassengerRequestInformation>() {
           @Override
           public int compare(PassengerRequestInformation o1, PassengerRequestInformation o2) {
             return   Double.compare(o1.getSubmissionTime().orElse(0), o2.getSubmissionTime().orElse(0));
           }
       });
    }

    public void writeCSV(File path) throws IOException {
        BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(path)));

        writer.write(String.join(",", new String[] { //
                "request_id",
                "timestamp_submitted",
                "timestamp_dispatched",
                "timestamp_arrived",
                "timestamp_started",
                "location_start",
                "location_stop",
                "denied"
        }) + "\n");

        for (PassengerRequestInformation passengerRequest : passengerRequests) {
            writer.write(String.join(",", new String[] { //
                    String.valueOf(passengerRequest.getRequestId()),
                    getDateTimeStringForOptionalTime(passengerRequest.getSubmissionTime()),
                    getDateTimeStringForOptionalTime(passengerRequest.getAssignTime()),
                    getDateTimeStringForOptionalTime(passengerRequest.getPickupTime()),
                    getDateTimeStringForOptionalTime(passengerRequest.getStartTime()),
                    passengerRequest.getStartLocation().toText(),
                    passengerRequest.getStopLocation().toText(),
                    String.valueOf(passengerRequest.isCanceled())
            }) + "\n");
        }

        writer.close();
    }

    private String getDateTimeStringForOptionalTime(OptionalTime time) {
        if (time.isUndefined()) {
            return "";
        } else {
            return simStartTime.plusSeconds((long) time.seconds()).format(dtf);
        }
    }
}
