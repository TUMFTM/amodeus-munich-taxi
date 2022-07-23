/* amodeus - Copyright (c) 2018, ETH Zurich, Institute for Dynamic Systems and Control */
package amodeus.amodeus.analysis.plot;

import org.jfree.chart.ChartUtils;
import org.jfree.chart.JFreeChart;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;

public class AmodeusChartUtils {
    static public void saveAsPNG(JFreeChart chart, String filename, int width, int height) {
        try {
            FileOutputStream out = new FileOutputStream(filename);
            ChartUtils.writeChartAsPNG(out, chart, width, height);
            out.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    static public void saveChartAsPNG(File fileChart, JFreeChart chart, int width, int height) {
        saveAsPNG(chart, fileChart.toString(), width, height);
    }
}
