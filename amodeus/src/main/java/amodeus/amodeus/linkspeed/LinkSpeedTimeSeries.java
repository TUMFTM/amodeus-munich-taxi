/* amodeus - Copyright (c) 2018, ETH Zurich, Institute for Dynamic Systems and Control */
package amodeus.amodeus.linkspeed;

import java.io.Serializable;
import java.util.Map;
import java.util.NavigableMap;
import java.util.Set;
import java.util.TreeMap;

import amodeus.amodeus.util.math.GlobalAssert;

/** DO NOT MODIFY CLASS
 * 
 * INSTANCES OF LinkSpeedTimeSeries
 * ARE USED IN MANY SCENARIOS */
public class LinkSpeedTimeSeries implements Serializable {
    private static final long serialVersionUID = 6529685098267757691L;
    /**
     * keyMap contains times and Tensor a list of recorded speeds at the time
     */
    private /* non-final */ NavigableMap<Integer, Double> data = new TreeMap<>();

    /* package */ LinkSpeedTimeSeries() {
        // ---
    }

    /** @return link speed at time @param time or null if no recording,
     *         use this function if exactly this time value is required. */
    public Double getSpeedsAt(Integer time) {
        return data.get(time);
    }

    /**
     * @return link speed at the maximum recorded time smaller or equal than @param time
     * null if lowestKey < time, use this function if an approximate speed should be
     * returned in any case, returns null, if there is no observation before @param time
     */
    public Double getSpeedsFloor(Integer time) {
        Map.Entry<Integer, Double> entry = data.floorEntry(time);
        if (entry == null) return null;
        else return entry.getValue();
    }

    /**
     * @return link speed at the maximum recorded time smaller or equal than @param time - @param dt
     * null if lowestKey + dt < time , use this function if exactly this time interval is required.
     */
    public Double getSpeedsInInterval(Integer time, Integer dt) {
        if (dt == null) dt = 0;
        Map.Entry<Integer, Double> entry = data.floorEntry(time);
        if (entry == null || entry.getKey() + dt < time) return null;
        else return entry.getValue();

    }

    public Integer getTimeFloor(Integer time) {
        return data.floorEntry(time).getKey();
    }

    public Set<Integer> getRecordedTimes() {
        return data.keySet();
    }

    /* package */ boolean containsTime(Integer time) {
        return data.containsKey(time);
    }

    public void setSpeed(Integer time, double speed) {
        GlobalAssert.that(speed >= 0);
        data.put(time, speed);
    }

    public NavigableMap<Integer, Double> getData() {
        return data;
    }
}
