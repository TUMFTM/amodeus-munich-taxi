package de.tum.mw.ftm.amod.geom;

import org.locationtech.jts.geom.Envelope;

import java.util.Objects;

public class GridCell extends Envelope {
    private String id;
    private double centerX;
    private double centerY;


    public GridCell(double x1, double x2, double y1, double y2) {
        super(x1, x2, y1, y2);
        centerX = 0.5 * (getMaxX() + getMinX());
        centerY = 0.5 * (getMaxY() + getMinY());
    }

    public GridCell(double x1, double x2, double y1, double y2, String id) {
        this(x1, x2, y1, y2);
        this.id = id;
    }

    public String getId() {
        return id;
    }

    public double getCenterX() {
        return centerX;
    }

    public double getCenterY() {
        return centerY;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        if (!super.equals(o)) return false;
        GridCell gridCell = (GridCell) o;
        return Double.compare(gridCell.centerX, centerX) == 0 && Double.compare(gridCell.centerY, centerY) == 0 && Objects.equals(id, gridCell.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(super.hashCode(), id, centerX, centerY);
    }

    @Override
    public String toString() {
        return "GridCell{" +
                "id='" + id + '\'' +
                '}';
    }
}
