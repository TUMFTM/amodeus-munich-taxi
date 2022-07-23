package de.tum.mw.ftm.amod.taxi.preprocessing.demandprediction;

import de.tum.mw.ftm.amod.geom.GridCell;
import junit.framework.TestCase;

public class UniformGridWithBorderPredictionsTest extends TestCase {

    public void testFromXML() {
        UniformGridWithBorderPredictions uniformGridWithBorderPredictions =  UniformGridWithBorderPredictions.fromXML("prediction.xml");
        GridCell gridCell = uniformGridWithBorderPredictions.getGridCells()[5];
        double[] predictions = uniformGridWithBorderPredictions.getDemandPredictionForCell(6000.0, gridCell);
        System.out.println("Test");

    }
}