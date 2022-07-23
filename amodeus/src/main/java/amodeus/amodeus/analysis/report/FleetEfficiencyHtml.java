/* amodeus - Copyright (c) 2018, ETH Zurich, Institute for Dynamic Systems and Control */
package amodeus.amodeus.analysis.report;

import java.util.HashMap;
import java.util.Map;

import amodeus.amodeus.analysis.AnalysisSummary;
import amodeus.amodeus.analysis.element.DistanceDistributionOverDayImage;
import amodeus.amodeus.analysis.element.OccupancyDistanceRatiosImage;
import amodeus.amodeus.analysis.shared.NumberPassengerStatusDistribution;
import amodeus.amodeus.analysis.shared.RideSharingDistributionCompositionStack;

public enum FleetEfficiencyHtml implements HtmlReportElement {
    INSTANCE;

    private static final String IMAGE_FOLDER = "../data"; // relative to report folder

    @Override
    public Map<String, HtmlBodyElement> process(AnalysisSummary analysisSummary) {
        Map<String, HtmlBodyElement> bodyElements = new HashMap<>();

        // Fleet Efficency
        HtmlBodyElement fEElement = new HtmlBodyElement();
        fEElement.getHTMLGenerator().insertImg(IMAGE_FOLDER + "/" + DistanceDistributionOverDayImage.FILE_PNG, 800, 600);
        fEElement.getHTMLGenerator().insertImg(IMAGE_FOLDER + "/" + OccupancyDistanceRatiosImage.FILE_PNG, 800, 600);
        fEElement.getHTMLGenerator().insertImg(IMAGE_FOLDER + "/" + NumberPassengerStatusDistribution.IMAGE_NAME, 800, 600);
        fEElement.getHTMLGenerator().insertImg(IMAGE_FOLDER + "/" + RideSharingDistributionCompositionStack.FILE_NAME, RideSharingDistributionCompositionStack.WIDTH,
                RideSharingDistributionCompositionStack.HEIGHT);
        bodyElements.put(BodyElementKeys.FLEETEFFICIENCY, fEElement);
        return bodyElements;
    }

}
