/* amodeus - Copyright (c) 2018, ETH Zurich, Institute for Dynamic Systems and Control */
package amodeus.amodeus.linkspeed;

import java.io.Serializable;
import java.util.*;
import java.util.stream.Collectors;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;

public class LinkSpeedDataContainer implements Serializable {
    private static final long serialVersionUID = 6529685098267757690L;
    // linkMap saves for every link(Integer as IDs) a list with time(key)/speed(value)
    private final SortedMap<Integer, LinkSpeedTimeSeries> linkMap = new TreeMap<>();
    private final Integer dt;

    public LinkSpeedDataContainer() {
        this.dt = 1;
    }

    public LinkSpeedDataContainer(int dt) {
        this.dt = dt;
    }

    public void addData(Link link, int time, double speed) {
        addData(link.getId(), time, speed);
    }

    public void addData(Id<Link> linkId, int time, double speed) {
        addData(linkId.index(), time, speed);
    }

    /** add a speed recording for link with
     * 
     * @param linkIndex at
     * @param time with a speed value
     * @param speed [m/s] */
    private void addData(Integer linkIndex, int time, double speed) {
        linkMap.computeIfAbsent(linkIndex, idx -> new LinkSpeedTimeSeries()). //
        /* linkMap.get(linkIndex) */ setSpeed(time, speed);
    }

    public SortedMap<Integer, LinkSpeedTimeSeries> getLinkMap() {
        return Collections.unmodifiableSortedMap(linkMap);
    }

    public LinkSpeedTimeSeries get(Link link) {
        return get(link.getId());
    }

    public LinkSpeedTimeSeries get(Id<Link> linkId) {
        return linkMap.get(linkId.index());
    }

    /**
     * @return {@link Set} with all time steps for which a link speed was recorded on some {@link Link}
     */
    public Set<Integer> getRecordedTimes() {
        return linkMap.values().stream().map(LinkSpeedTimeSeries::getRecordedTimes) //
                .flatMap(Collection::stream).collect(Collectors.toSet());
    }

    /**
     * Retrieves the time series sampling interval, specified when this times series was created.
     * Attention: This value does not ensure, t
     *
     * @return The time series frequency in seconds, null if no frequecy was specified at the beginning.
     */
    public Integer getDt() {
        return dt;
    }

    public double calculateAverageLinkSpeedRatio(Map<Id<Link>, ? extends Link> links) {
        List<Double> ratios = new ArrayList<>();
        for (Map.Entry<Id<Link>, ? extends Link> linkEntry : links.entrySet()) {
            if (linkMap.containsKey(linkEntry.getKey().index())) {
                LinkSpeedTimeSeries timeSeries = linkMap.get(linkEntry.getKey().index());
                Map<Integer, Double> linkSpeeds = timeSeries.getData();
                for (Double linkSpeed : linkSpeeds.values()) {
                    ratios.add(linkSpeed / linkEntry.getValue().getFreespeed());
                }
            }
        }
        OptionalDouble overallRatio = ratios.stream().mapToDouble(e -> e).average();
        if (overallRatio.isPresent()) {
            return overallRatio.getAsDouble();
        } else {
            return 1;
        }
    }
}