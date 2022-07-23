/* amodeus - Copyright (c) 2018, ETH Zurich, Institute for Dynamic Systems and Control */
package amodeus.amodeus.analysis.element;

import java.util.HashMap;
import java.util.Map;

import amodeus.amodeus.analysis.AnalysisSummary;
import amodeus.amodeus.analysis.DriveTimeImages;
import amodeus.amodeus.analysis.report.HtmlBodyElement;
import amodeus.amodeus.analysis.report.HtmlReportElement;

public enum DriveTimeHtml implements HtmlReportElement {
    INSTANCE;

    private static final String IMAGE_FOLDER = "../data"; // relative to report folder

    @Override
    public Map<String, HtmlBodyElement> process(AnalysisSummary analysisSummary) {
        TravelTimeAnalysis travelTimeAnalysis = analysisSummary.getTravelTimeAnalysis();
        HtmlBodyElement aRElement = new HtmlBodyElement();
        aRElement.getHTMLGenerator().insertTextLeft(Quantiles.lblString("Drive Times"));
        aRElement.getHTMLGenerator().insertTextLeft(Quantiles.formatAggregates(travelTimeAnalysis.getDrveAggrgte()));
        aRElement.getHTMLGenerator().newLine();
        aRElement.getHTMLGenerator().insertImg(IMAGE_FOLDER + "/" + DriveTimeImages.FILENAME + ".png", 800, 600);

        // TODO @ChengQi , also distribution over time bins?
        // aRElement.getHTMLGenerator().insertImg(IMAGE_FOLDER + "/" + RequestsPerWaitingTimeImage.FILENAME + ".png", 800, 600);

        Map<String, HtmlBodyElement> bodyElements = new HashMap<>();
        bodyElements.put("", aRElement);
        return bodyElements;
    }
}