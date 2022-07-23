package de.tum.mw.ftm.amod.taxi.preprocessing.demandprediction;

import de.tum.mw.ftm.amod.geom.GridCell;

import java.time.LocalDateTime;

public class DemandPredictions {
    private LocalDateTime start_date;
    private LocalDateTime stop_date;
    private int n_grid_rows;
    private int n_grid_columns;
    private float borderWidth;
    private long x_min;
    private long x_max;
    private long y_min;
    private long y_max;
    private int prediction_windows;
    private long prediction_horizon;

    private GridCell[] gridCells;



}
