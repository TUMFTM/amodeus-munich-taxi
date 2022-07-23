/* amodeus - Copyright (c) 2018, ETH Zurich, Institute for Dynamic Systems and Control */
package amodeus.amodeus.linkspeed;


import ch.ethz.idsc.tensor.io.Export;

import ch.ethz.idsc.tensor.io.Import;
import org.matsim.api.core.v01.network.Link;

import java.io.File;
import java.io.IOException;
import java.util.Objects;
import java.util.zip.DataFormatException;

/**
 * Helper class to save/load {@link LinkSpeedDataContainer}s to the file system.
 * {@link LinkSpeedDataContainer} are used to save a certain traffic status, e.g., as found
 * in a dataset of taxi traces.
 */
public enum LinkSpeedUtils {
    ;

    /**
     * @return {@link LinkSpeedDataContainer} in {@link File} @param inputFile
     * @throws ClassNotFoundException
     * @throws IOException
     * @throws DataFormatException
     */
    public static LinkSpeedDataContainer loadLinkSpeedData(File inputFile) throws DataFormatException, IOException, ClassNotFoundException {
            return Import.object(inputFile);

    }

    /**
     * Writes the {@link LinkSpeedDataContainer} @param lsData to the location @param file
     *
     * @throws IOException if the operation fails
     */
    public static void writeLinkSpeedData(File file, LinkSpeedDataContainer lsData) throws IOException {
        Export.object(file, lsData);
        System.out.println("LinkSpeedData exported to: " + file.getAbsolutePath());
    }


    public static double getLinkSpeedForTime(LinkSpeedDataContainer lsData, Link link, double time) {
        double speed = link.getFreespeed();
        LinkSpeedTimeSeries timeSeries = lsData.get(link);
        if (Objects.nonNull(timeSeries)) {
            Double newSpeed = timeSeries.getSpeedsInInterval((int) time, lsData.getDt());
            if (newSpeed != null) speed = newSpeed;
        }
        double minSpeed = 10 / 3.6;
        if (speed < minSpeed)
            speed = minSpeed;
        return speed;
    }
}
