package de.tum.mw.ftm.amod.taxi.util;

import org.locationtech.jts.geom.Point;
import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.contrib.zone.Zone;
import org.matsim.core.gbl.MatsimRandom;
import org.matsim.core.network.NetworkUtils;
import org.matsim.core.utils.geometry.transformations.TransformationFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Random;

public class LinkRandomizer {
    private Network network;
    private static Random random;
    private Map<Id<Link>, Zone> linkToZone;

    public LinkRandomizer(Network network, Map<Id<Link>, Zone> linkToZone){
        this.network = network;
        this.linkToZone = linkToZone;
        random = MatsimRandom.getLocalInstance();
    }

    public void setDefinedSeed(){
        MatsimRandom.reset(network.getLinks().size());
    }

    public Link getRandomLink(){
        Link link;
        do {
            Id<Link> linkId= Id.createLinkId(String.valueOf(random.nextInt(network.getLinks().size())));
            link =  network.getLinks().get(linkId);
        }
        while (link==null || !link.getAllowedModes().contains(TransportMode.car));
        return link;
    }

    public Link getRandomLinkWithinNetworkPolygon(){
        Link link;
        do {
            Id<Link> linkId= Id.createLinkId(String.valueOf(random.nextInt(network.getLinks().size())));
            link =  network.getLinks().get(linkId);
        }
        while (link==null || !link.getAllowedModes().contains(TransportMode.car));
        return link;
    }

    public Link getRandomLinkWithinZone(Zone zone){
        if(!linkToZone.containsValue(zone)){
            throw new NullPointerException("The zone with id " + zone.getId() + " does not have a link! You should remove it.");
        }
        List<Id<Link>> linksWithinZone = new ArrayList<>();
        for(Id<Link> linkId : linkToZone.keySet()){
            Zone zoneToLink = linkToZone.get(linkId);
            if(zoneToLink != null && zoneToLink.equals(zone)){
                linksWithinZone.add(linkId);
            }
        }
        Id<Link> randomLinkId = linksWithinZone.get(random.nextInt(linksWithinZone.size()));
        return network.getLinks().get(randomLinkId);
    }

    public List<Link> mapToLinksInsideNetwork(List<Point> points, String geoJsonPath){
        List<Link> linkList = new ArrayList<>();
        for(Point point : points){
                linkList.add(getRandomLinkWithinNetworkPolygon());
        }
        return linkList;
    }
}
