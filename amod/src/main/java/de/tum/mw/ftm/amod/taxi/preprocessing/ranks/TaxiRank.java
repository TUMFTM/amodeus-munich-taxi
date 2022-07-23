package de.tum.mw.ftm.amod.taxi.preprocessing.ranks;

import amodeus.amod.ext.UserReferenceFrames;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.Point;
import org.locationtech.jts.geom.impl.CoordinateArraySequence;
import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.core.network.NetworkUtils;

import java.util.Collection;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;


public class TaxiRank {
    GeometryFactory geometryFactory = new GeometryFactory();
    private int id;
    private int capacity;
    private String description;
    private Coord coordinate;
    private Link nearestLink;
    private int numberOfTaxis;
    private float popularity;


    public TaxiRank() {
    }

    public TaxiRank(int capacity, Coord coordinate) {
        this(0, capacity, "GenericTaxiRank", coordinate, 0.50f);
    }

    public TaxiRank(int capacity, Point point) {
        this(capacity, new Coord(point.getX(), point.getY()));
    }

    public TaxiRank(int id, int capacity, String description, Coord coordinate, float popularity) {
        this.id = id;
        this.capacity = capacity;
        this.description = description;
        this.coordinate = coordinate;
        this.popularity = popularity;
    }

    public TaxiRank(int id, int capacity, String description, double x, double y, float popularity) {
        this(id, capacity, description, new Coord(x, y), popularity);
    }


    public int getCapacity() {
        return capacity;
    }

    public void setCapacity(int capacity) {
        this.capacity = capacity;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public Coord getCoordinate() {
        return coordinate;
    }

    public void setCoordinate(Coord coordinate) {
        this.coordinate = coordinate;
    }

    public Point getGeom() {
        CoordinateArraySequence coordinateArraySequence = new CoordinateArraySequence(new Coordinate[]{new Coordinate(coordinate.getX(), coordinate.getY())});
        return new Point(coordinateArraySequence, geometryFactory);
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public Link getNearestLink() {
        return nearestLink;
    }

    public void setNearestLink(Network network) {
        this.nearestLink = NetworkUtils.getNearestLink(network, new Coord(this.coordinate.getX(), this.coordinate.getY()));
    }

    public int getNumberOfTaxis() {
        return numberOfTaxis;
    }

    public void setNumberOfTaxis(int numberOfTaxis) {
        this.numberOfTaxis = numberOfTaxis;
    }

    public float getPopularity() {
        return popularity;
    }

    public void setPopularity(float popularity) {
        this.popularity = popularity;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        TaxiRank taxiRank = (TaxiRank) o;
        return Objects.equals(coordinate, taxiRank.coordinate);
    }

    @Override
    public int hashCode() {
        return Objects.hash(coordinate);
    }

    @Override
    public String toString() {
        return "TaxiRank{" +
                "id=" + id +
                ", capacity=" + capacity +
                ", description='" + description + '\'' +
                ", popularity=" + popularity +
                '}';
    }
}


