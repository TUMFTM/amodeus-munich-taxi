package de.tum.mw.ftm.amod.taxi.preprocessing.demand;

import javax.persistence.Basic;
import javax.persistence.Column;
import javax.persistence.Embeddable;
import java.io.Serializable;
import java.util.Objects;

@Embeddable
public class TaxiRideID implements Serializable {
    private Long trackId;
    private Long tripId;

    public TaxiRideID() {
    }

    public TaxiRideID(Long trackId, Long tripId) {
        this.trackId = trackId;
        this.tripId = tripId;
    }

    @Basic
    @Column(name = "track_id")
    public Long getTrackId() {
        return trackId;
    }

    public void setTrackId(Long trackId) {
        this.trackId = trackId;
    }

    @Basic
    @Column(name = "trip_id")
    public Long getTripId() {
        return tripId;
    }

    public void setTripId(Long tripId) {
        this.tripId = tripId;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        TaxiRideID that = (TaxiRideID) o;
        return Objects.equals(trackId, that.trackId) &&
                Objects.equals(tripId, that.tripId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(trackId, tripId);
    }
}
