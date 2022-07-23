package de.tum.mw.ftm.amod.taxi.preprocessing.demandprediction;

import de.tum.mw.ftm.amod.geom.GridCell;

public interface GridDemandPrediction {

    int getNumberOfRows();
    int getNumberOfColumns();
    int getPredictionWindows();
    double getPredictionHorizon();
    GridCell[] getGridCells();

    double[] getDemandPredictionForCell(double time, GridCell cell);
    GridCell getCellWithHighestDemand(double time);

}
