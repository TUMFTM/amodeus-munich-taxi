package amodeus.amodeus.linkspeed;


import ch.ethz.idsc.tensor.io.Import;
import org.apache.log4j.Logger;

import java.io.*;
import java.util.zip.DataFormatException;
import java.util.zip.Inflater;
import java.util.zip.InflaterInputStream;

public class LinkSpeedReader {
    private final static Logger logger = Logger.getLogger(LinkSpeedReader.class);

    /**
     * @return {@link LinkSpeedDataContainer} in {@link File} @param inputFile
     * @throws ClassNotFoundException
     * @throws IOException
     * @throws DataFormatException
     */
    public static LinkSpeedDataContainer loadLinkSpeedData(File inputFile) {
        try {
            return Import.object(inputFile);
        } catch (Exception ex) {
            ex.printStackTrace();
        }
        return null;
    }

    public static void main(String[] args) throws DataFormatException, IOException, ClassNotFoundException, InterruptedException {
        File file = new File("linkSpeedData");
        long tic = System.currentTimeMillis();
        logger.info("Reading File");
//        LinkSpeedDataContainer lsData = object(file);
        LinkSpeedDataContainer lsData = Import.object(file);
        logger.info(String.format("Finished: %f", (System.currentTimeMillis() - tic) / 1000.0));
        System.gc();
        while (true) {

        }
    }

    public static boolean deflateFile(File input, File output) {
        return true;
    }

    public static <T> T object(File file) throws IOException, ClassNotFoundException, DataFormatException {
        T result = null;
        FileInputStream fis;
        InflaterInputStream iis;
        ObjectInputStream objectInputStream;
        Inflater inflater = new Inflater();

        try {
            fis = new FileInputStream(file);
            iis = new InflaterInputStream(fis, inflater);
            objectInputStream = new ObjectInputStream(iis);
            result = (T) objectInputStream.readObject();
            objectInputStream.close();
            iis.close();
            fis.close();

        } catch (IOException e) {
            logger.error(e);
            return null;
        } catch (OutOfMemoryError e) {
            logger.error(e);
            return null;
        }
        return result;
    }

}
