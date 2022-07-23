/* amod - Copyright (c) 2018, ETH Zurich, Institute for Dynamic Systems and Control */
package amodeus.socket;

import amodeus.amodeus.util.io.Unzip;
import amodeus.socket.core.SocketScenarioDownload;

import java.io.*;
import java.util.List;

public enum SocketScenarioResource {
    ;

    private static final String SCENARIO_ZIP = "scenario.zip";

    /** @param key for instance "SanFrancisco.20080518"
     * @throws IOException */
    public static List<File> extract(final String key, File workingDirectory) throws IOException {
        /** file name is arbitrary, file will be deleted after un-zipping */
        final File file = new File(workingDirectory, SCENARIO_ZIP);
        String resource = "/scenario/" + key.replace('.', '/') + "/" + SCENARIO_ZIP;
        try (InputStream inputStream = SocketScenarioResource.class.getResourceAsStream(resource)) {
            System.out.println("obtain as resource: [" + resource + "]");
            try (OutputStream outputStream = new FileOutputStream(file)) {
                byte[] buffer = new byte[1024];
                int length;
                while (0 < (length = inputStream.read(buffer)))
                    outputStream.write(buffer, 0, length);
            }

        } catch (Exception exception) {
            System.out.println("scenario not fount as resource: [" + resource + "]");
            SocketScenarioDownload.of(key, file);
        }
        List<File> list = Unzip.of(file, workingDirectory, true);
        file.delete();
        return list;
    }

}
