package de.tum.mw.ftm.amod.taxi.dispatcher;

import amodeus.amodeus.dispatcher.core.DynamicFleetSizeDispatcher;
import amodeus.amodeus.dispatcher.core.RoboTaxi;
import amodeus.amodeus.dispatcher.core.RoboTaxiStatus;
import amodeus.amodeus.fleetsize.DynamicFleetSize;
import amodeus.amodeus.net.MatsimAmodeusDatabase;
import amodeus.amodeus.options.ScenarioOptions;
import amodeus.amodeus.util.math.GlobalAssert;
import amodeus.amodeus.util.matsim.SafeConfig;
import de.tum.mw.ftm.amod.taxi.preprocessing.ranks.TaxiRank;
import de.tum.mw.ftm.amod.taxi.preprocessing.ranks.TaxiRanks;
import de.tum.mw.ftm.amod.taxi.preprocessing.zones.DispatchingZone;
import de.tum.mw.ftm.amod.taxi.util.AmodeusUtil;
import de.tum.mw.ftm.amod.taxi.util.MatsimNetworkUtil;
import de.tum.mw.ftm.amod.taxi.util.ZonalUtils;
import org.apache.log4j.Logger;
import org.locationtech.jts.geom.*;
import org.locationtech.jts.util.GeometricShapeFactory;
import org.matsim.amodeus.components.AmodeusDispatcher;
import org.matsim.amodeus.components.AmodeusRouter;
import org.matsim.amodeus.config.AmodeusModeConfig;
import org.matsim.amodeus.config.FTMConfigGroup;
import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.contrib.dvrp.passenger.PassengerRequest;
import org.matsim.contrib.dvrp.run.ModalProviders;
import org.matsim.contrib.zone.Zone;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.config.Config;
import org.matsim.core.config.ReflectiveConfigGroup;
import org.matsim.core.network.NetworkUtils;
import org.matsim.core.router.util.TravelTime;

import java.util.*;
import java.util.stream.Collectors;

public class ReferenceDispatcher extends DynamicFleetSizeDispatcher {
    private final static Logger logger = Logger.getLogger(ReferenceDispatcher.class);

    private final int rebalancingPeriod;
    private final int dispatchPeriod;
    private final ZonalUtils zonalUtils;
    private final Map<Id<Zone>, Set<TaxiRank>> zoneTaxiRankMap;
    private final TaxiRanks taxiRanks;
    private final Network network;
    private final Coord networkCenter;
    private static final Random random = new Random(42);
    private final double radiusAroundCenter = 3000;
    private final float inCenterThreshold = 0.5f; // Warning: The value of 0.5f has been validated against real observations for the year 2019 for a circe size of 3000 m. Changes may dramatically change rebalancing behavior
    private final Polygon circleAroundCenter;
    private final GeometryFactory geometryFactory;
    private final int fleetSizePeriod;
    private boolean started;
    private long rebalanceTaxisToCenter = 0;
    protected final long maxCustomerWaitingTime;
    protected final long maxCustomerAssignmentTime;
    private final TaxiRanks centerRanks;
    private final TaxiRanks outsideRanks;


    protected ReferenceDispatcher(Config config, AmodeusModeConfig operatorConfig, TravelTime travelTime,
                                  AmodeusRouter router, EventsManager eventsManager, Network network,
                                  MatsimAmodeusDatabase db, ZonalUtils zonalUtils,
                                  TaxiRanks taxiRanks, ScenarioOptions scenarioOptions,
                                  DynamicFleetSize dynamicFleetSize) {
        super(config, operatorConfig, travelTime, router, eventsManager, db, dynamicFleetSize);

        this.zonalUtils = zonalUtils;
        this.network = network;

        FTMConfigGroup ftmConfigGroup = (FTMConfigGroup) config.getModules().get("ftm_simulation");
        this.dispatchPeriod = ftmConfigGroup.getDispatchingPeriodSeconds();
        this.rebalancingPeriod = ftmConfigGroup.getRebalancingPeriodSeconds();

        if(this.rebalancingPeriod != 1200){
            logger.warn("The scenario was validated based on 1200 s rebalancing interval. Change this only for further validation or debugging");
        }

        this.maxCustomerWaitingTime = ftmConfigGroup.getMaxCustomerWaitingTime();
        this.maxCustomerAssignmentTime = ftmConfigGroup.getMaxCustomerAssignmentTime();

        this.networkCenter = scenarioOptions.getLocationSpec().center();


        GeometricShapeFactory geometricShapeFactory = new GeometricShapeFactory();
        geometricShapeFactory.setCentre(new Coordinate(networkCenter.getX(), networkCenter.getY()));
        geometricShapeFactory.setSize(radiusAroundCenter);
        this.circleAroundCenter = geometricShapeFactory.createCircle();
        this.geometryFactory = new GeometryFactory();


        this.taxiRanks = taxiRanks;
        initializeTaxiRanks(taxiRanks);
        this.zoneTaxiRankMap = getZoneTaxiRankMap();

        this.centerRanks = new TaxiRanks(taxiRanks.stream().filter(r -> this.circleAroundCenter.contains(r.getGeom())).collect(Collectors.toList()));
        this.outsideRanks = new TaxiRanks(taxiRanks.stream().filter(r -> !this.circleAroundCenter.contains(r.getGeom())).collect(Collectors.toList()));
        SafeConfig saveFTMConfig = SafeConfig.wrap((ReflectiveConfigGroup) config.getModules().get("ftm_simulation"));
        this.fleetSizePeriod = saveFTMConfig.getInteger("fleetsizePeriod", 60);
        this.started = false;


    }

    /**
     * This function initializes the  TaxiRanks. This means adding a TaxiRank to the containing zone
     * and creating a TaxiRank inside a zone if there does not already exist one.
     * Additionally the TaxiRank capacity is limited to 10 taxis for big stands and 2 for smaller stands
     *
     * @param taxiRanks: List of TaxiRanks which should be used for initialization.
     * @return Map of zone ids and TaxiRanks which are located in these zones.
     */
    private void initializeTaxiRanks(Set<TaxiRank> taxiRanks) {
        // TODO: Maybe add ranks to Zones without rank again. But most probabaly we won't need that
        GeometricShapeFactory geometricShapeFactory = new GeometricShapeFactory();
        geometricShapeFactory.setCentre(new Coordinate(networkCenter.getX(), networkCenter.getY()));
        geometricShapeFactory.setSize(6000);
        Polygon rankBuffer = geometricShapeFactory.createCircle();
        // adapt rank capacities
        for (TaxiRank rank : taxiRanks) {
            rank.setNearestLink(network);
//            if(rankBuffer.contains(rank.getGeom())){
//                if (rank.getCapacity() > 10) {
//                    rank.setCapacity(10);
//                }
//                if (rank.getCapacity() > 5) {
//                    rank.setCapacity(5);
//                }
//            }
//            else {
//                rank.setCapacity(2);
//            }
        }
    }
    private Map<Id<Zone>, Set<TaxiRank>> getZoneTaxiRankMap(){
        Map<Id<Zone>, Set<TaxiRank>> resultMap = new TreeMap<>(ZonalUtils.zoneComparator());
        Collection<DispatchingZone> dispatchingZones = zonalUtils.getZones().values();
        for(Zone zone : dispatchingZones) {
            Set<TaxiRank> zoneRanks = new HashSet<>();
            for(TaxiRank taxiRank: taxiRanks) {
                if (zone.getMultiPolygon().contains(taxiRank.getGeom())) {
                    zoneRanks.add(taxiRank);
                }
            }
            resultMap.put(zone.getId(), zoneRanks);
        }
        return  resultMap;
    }


    //TODO: Maybe kick this out again. And Implement periodic rebalancing, if the interval is small this has the same effect.
    // One problem here is that wave effects occur
    // Maybe make a first decision after dropoff, and rebalance at lower frequency, every 5 Minutes?
    @Override
    protected void scheduleAfterDropoffAction(RoboTaxi taxi) {
        super.scheduleAfterDropoffAction(taxi);
        tryToRebalanceTaxiToRank(taxi);
//        rebalanceTaxi(taxi);
    }

    @Override
    protected void redispatch(double now) {
        long round_now = Math.round(now);
        if (!started) {
            if (getRoboTaxis().size() == 0) /* return if the roboTaxis are not ready yet */
                return;
            /* as soon as the roboTaxis are ready, make sure to execute rebalancing and dispatching for now=0 */
            round_now = 0;
            started = true;
        }

        // Every time that some rebalancing or dispatching should take place the state of the Taxis
        // (if they are at a rank or not) should be updated
        if (round_now % dispatchPeriod == 0 || round_now % rebalancingPeriod == 0) {
            updateTaxiRanks();
        }

        if (round_now % fleetSizePeriod == 0) {
            adjustFleetSize(round_now);
        }

        if (round_now % dispatchPeriod == 0) {
            dispatch();
        }

        if (round_now % rebalancingPeriod == 0) {

            rebalance();
        }
    }

    /**
     * Updates the "atTaxiRank" flag of all taxis. If number of taxis at rank capacity exceeds the ranks maximum capacity,
     * rebalance those taxis the next freeRank. This Function must be called only once per timestep and before dispatching.
     */
    private void updateTaxiRanks() {
        // Reset all countings, and taxi states.
        taxiRanks.forEach(rank -> rank.setNumberOfTaxis(0));
        getRoboTaxis().forEach(rt -> rt.setAtTaxiRank(false));
        List<RoboTaxi> stayingTaxis = (List<RoboTaxi>) getStayingTaxis();
        // Sort in FIFO Order. Last taxi arrives needs to be rebalanced if stand capacity exceeds.
        stayingTaxis.sort(Comparator.comparingDouble(o -> o.getSchedule().getCurrentTask().getBeginTime()));

        for (RoboTaxi taxi : stayingTaxis) {
            for (TaxiRank taxiRank : taxiRanks) {
                if (taxi.getCurrentDriveDestination() == taxiRank.getNearestLink()) {
                    // If rank has space, assign taxi to that rank
                    if (taxiRank.getCapacity() >= taxiRank.getNumberOfTaxis()) {
                        taxiRank.setNumberOfTaxis(taxiRank.getNumberOfTaxis() + 1);
                        taxi.setAtTaxiRank(true);
                    } else
                        tryToRebalanceTaxiToRank(taxi);
                }
            }
        }
    }

    /**
     * Do dispatching for all open requests.
     */
    protected void dispatch() {
        // Cancel all requests not assigned served in maxCustomerWaitingTime
        getPendingRequests().stream().filter(r -> (getTimeNow() - r.getSubmissionTime()) > maxCustomerWaitingTime).forEach(this::cancelRequest);

        // Cancel all pending requests which not served within in maxCustomerWaitingTime
        getUnassignedPassengerRequests().stream().filter(request -> (getTimeNow()- request.getSubmissionTime()) > maxCustomerAssignmentTime).forEach(this::cancelRequest);

        List<PassengerRequest> unassignedRequests = getUnassignedPassengerRequests();
        // Sort Requests in FIFO order.
        unassignedRequests.sort(Comparator.comparingDouble(PassengerRequest::getSubmissionTime));

        for (PassengerRequest request : unassignedRequests) {
            tryToAssignRequest(request);
        }
    }

    /**
     * Tries to assign a request according to the Munich Taxi policy. This means the following steps are performed:
     * 1. Get the zone which contains the origin of the request
     * 2. Try to assign the request to a taxi at rank in the origin zone
     * 3. Try to assign the request to a cruising taxi in the origin zone
     * 4. Repeat steps 3 and 4 for adjacent zones of the origin zone
     * 5. Assign to the closest remaining taxi
     *
     * @param request: request which should be dispatched
     */
    private boolean tryToAssignRequest(PassengerRequest request) {

        List<RoboTaxi> availableTaxis = (List<RoboTaxi>) getDivertableUnassignedRoboTaxis();
        // Retrieve start Zone, if request is inside dispatching area
        Optional<DispatchingZone> optionalStartZone = zonalUtils.getContainingZone(request.getFromLink());
        DispatchingZone startZone;
        if (optionalStartZone.isPresent()) {
            startZone = optionalStartZone.get();

            // Try to assign request inside the origin dispatching zone
            if (tryToAssignRequestInZone(request, availableTaxis, startZone.getId())) {
                return true;
            }

            // Else try to dispatch request to taxis in adjacent dispatching zones
            List<Id<Zone>> adjacentZoneIds = startZone.getAdjacentZones();
            // Look for empty taxis at ranks in adjacent zones first
            for (Id<Zone> zoneId : adjacentZoneIds) {
                if (tryToAssignRequestToTaxiAtRankInZone(request, zoneId, availableTaxis)) {
                    return true;
                }
            }
            // Second look for empty taxis cruising in adjacent zone
            for (Id<Zone> zoneId : adjacentZoneIds) {
                if (tryToAssignRequestToCruisingTaxiInZone(request, zonalUtils.getZones().get(zoneId), availableTaxis)) {
                    return true;
                }
            }
        }

        // else try to assign request to the nearest (beeline) taxi available
        return tryToAssignToNearestTaxi(request, availableTaxis);

    }

    /**
     * Try to assign a request to emtpy taxis in a disaptching zone
     * 1. Look for empty taxis at rank in zone
     * 2. Look for empty taxis cruising in zone
     *
     * @param request        passenger request to be assigned
     * @param availableTaxis list of all available taxis taken into account for dispatching
     * @param zoneId         origin zone of the related customer request
     * @return true if the request was assigned, false otherwise
     */
    private boolean tryToAssignRequestInZone(PassengerRequest request, List<RoboTaxi> availableTaxis, Id<Zone> zoneId) {
        // First look for empty taxis at rank
        if (tryToAssignRequestToTaxiAtRankInZone(request, zoneId, availableTaxis)) {
            return true;
        }
        // Second look for empty cruising Taxis
        return tryToAssignRequestToCruisingTaxiInZone(request, zonalUtils.getZones().get(zoneId), availableTaxis);
    }

    /**
     * Try to assign a request to available taxis at rank in a given zone
     * If there are more than one rank in the given dispatching zone, ranks are ordered by distance to the request
     *
     * @param request        Passenger request to be assigned
     * @param startZoneId    origin zone of the related customer request
     * @param availableTaxis list of available taxis taken into account for dispatching
     * @return true if the request was assigned, false otherwise
     */
    private boolean tryToAssignRequestToTaxiAtRankInZone(PassengerRequest request, Id<Zone> startZoneId, List<RoboTaxi> availableTaxis) {
        List<TaxiRank> ranksInZone = zoneTaxiRankMap.get(startZoneId).stream().filter(r -> r.getNumberOfTaxis() > 0).collect(Collectors.toList());
        List<Link> taxiRankLinks = ranksInZone.stream().map(TaxiRank::getNearestLink).collect(Collectors.toList());
        List<Link> sortedTaxiRankLinks = MatsimNetworkUtil.orderListByDistanceToLink(request.getFromLink(), taxiRankLinks);
        for (Link taxiRankLink : sortedTaxiRankLinks) {
            Optional<RoboTaxi> optionalRoboTaxi = getNextTaxiAtTaxiRank(availableTaxis, taxiRankLink);
            if (optionalRoboTaxi.isPresent()) {
                return assignRequest(optionalRoboTaxi.get(), request);
            }
        }
        return false;
    }

    /**
     * Try to assign request to the closest (beeline) cruising taxi in zone if present
     *
     * @param request        the request to be assigned
     * @param availableTaxis lsit of available taxis for dispatching
     * @return true if the request was assigned, false otherwise.
     */
    private boolean tryToAssignRequestToCruisingTaxiInZone(PassengerRequest request, Zone startZone, List<RoboTaxi> availableTaxis) {
        Optional<RoboTaxi> closestTaxiInZone = getClosestTaxiInZone(availableTaxis, startZone, request.getFromLink());
        return closestTaxiInZone.filter(taxi -> assignRequest(taxi, request)).isPresent();
    }

    /**
     * Try to assign request to the closest taxi (beeline) if present
     *
     * @param request        the request to be assigned
     * @param availableTaxis lsit of available taxis for dispatching
     * @return true if the request was assigned, false otherwise.
     */
    private boolean tryToAssignToNearestTaxi(PassengerRequest request, List<RoboTaxi> availableTaxis) {
        Optional<RoboTaxi> optionalClosestTaxi = AmodeusUtil.getClosestTaxiToLink(request.getFromLink(), availableTaxis);
        if (optionalClosestTaxi.isPresent()) {
            RoboTaxi closestTaxi = optionalClosestTaxi.get();
            return assignRequest(closestTaxi, request);
        }
        return false;

    }

    /**
     * Assign customer request to a taxi, if ETA is smaller than 95% of the maxCustomerWaitingTime
     *
     * @param taxi    the taxi the request should be assigned to
     * @param request the request to assign
     * @return true if the request was assigned, false otherwise.
     */
    private boolean assignRequest(RoboTaxi taxi, PassengerRequest request) {
        double arrivalTime = getEstimatedArrivalTime(taxi.getLastKnownLocation(), request.getFromLink(), getTimeNow());
        // Assing request only if estimated arrival time is within maxCustomerWaitingTime
        if (arrivalTime - request.getSubmissionTime() <= maxCustomerWaitingTime * 0.90) {
            setRoboTaxiPickup(taxi, request);
            return true;
        } else {
//            logger.debug("Request {} has not been assigned, cause timeToArrival exceeds maxCustomerWaitingTime", request.getPassengerId()));
//            logger.debug(String.format("Estimated arrival time for Passenger Request %s, at %f is %f",
//                    request.getPassengerId(),
//                    getTimeNow(),
//                    arrivalTime));
            return false;
        }
    }


    /**
     * Do empiric rebalancing for all staying taxis.
     */

    protected void rebalance() {

        rebalanceTaxisToCenter = missingTaxisInCenter();
        //TODO: Check what happesn if we use divertableunassigenedtaxis instead.
        List<RoboTaxi> stayingTaxis = (List<RoboTaxi>) getStayingTaxis();
        stayingTaxis.sort((o1, o2) -> Double.compare(o1.getSchedule().getCurrentTask().getBeginTime(), o2.getSchedule().getCurrentTask().getBeginTime()));
        for (RoboTaxi roboTaxi : stayingTaxis) {
            GlobalAssert.that(roboTaxi.getStatus() != RoboTaxiStatus.DRIVETOCUSTOMER
                    && roboTaxi.getStatus() != RoboTaxiStatus.DRIVEWITHCUSTOMER);
            rebalanceTaxi(roboTaxi);
        }
    }


    /**
     * There are 3 possible cases for a taxi which decide what happens during the rebalancing process:
     * 1. If over X% of available taxis are in a XXm circle around the center the taxi will be send to
     * the closest free rank (or remain at the present rank).
     * //TODO: Maybe rebalance taxis, which are waiting at a rank longer than XX Minutes...
     * 2. The Taxi will be sent to a random free rank inside within XX m circle around the center.
     *
     * @param taxi: Taxi which should be rebalanced.
     */
    private void rebalanceTaxi(RoboTaxi taxi) {
        // If there are less taxis in center than defined and this taxi is outside -> rebalance this taxis to center
        if (rebalanceTaxisToCenter > 0 && getEuclideanDistanceToCenter(taxi) > radiusAroundCenter) {
            // Try to rebalance the taxi to a random center rank
            if (rebalanceTaxiToRandomRank(taxi, centerRanks)) {
                rebalanceTaxisToCenter--;
                return;
            // If no rank is free inside the center, rebalance to a random link
            } else {
                rebalanceTaxiToCenter(taxi);
                rebalanceTaxisToCenter--;
                return;
            }
        // If there are more taxis in center than defined and this taxi is inside -> rebalance out of center
        // TODO: This case never occurs, cause missingTaxisInCenter is always positive -_-
        } else if (rebalanceTaxisToCenter < 0 && getEuclideanDistanceToCenter(taxi) < radiusAroundCenter) {
            // Try to rebalance to a random Rank outside
            if (rebalanceTaxiToRandomRank(taxi, outsideRanks)){
                rebalanceTaxisToCenter++;
            }
            else{
                //TODO: Check if this is a problem
                logger.warn("Can't find free rank outside center, even though there is oversupply inside center");
            }
        // If ratio of taxis in and out of center is balanced, rebalance all remaining taxis, which are not at a rank.
        } else if (!taxi.isAtTaxiRank()) {
            tryToRebalanceTaxiToRank(taxi);
        }
        else if (taxi.isAtTaxiRank()){
            // Do nothing with taxis at rank, maybe rebalance Taxis which are stayin at rank for a long time
        }
        // TODO: Verify that this case never happens ;-)
        else {
            logger.warn("Don't know where to rebalance this taxi to... ");
        }
    }



    /**
     * Rebalances a taxi to a random place around the center.
     */
    private void rebalanceTaxiToCenter(RoboTaxi taxi) {
        double distanceToCenter = NetworkUtils.getEuclideanDistance(networkCenter,
                taxi.getCurrentDriveDestination().getCoord());

        double deltaX = taxi.getLastKnownLocation().getCoord().getX() - networkCenter.getX();
        double deltaY = taxi.getLastKnownLocation().getCoord().getY() - networkCenter.getY();

        double randomDistanceFromCenter = random.nextDouble() * radiusAroundCenter;

        double rebalanceX = networkCenter.getX() + randomDistanceFromCenter * (deltaX / distanceToCenter);
        double rebalanceY = networkCenter.getY() + randomDistanceFromCenter * (deltaY / distanceToCenter);

        // This only works when networkCenter is given in a metric coordinate system
        Coord randomRebalanceCoord = new Coord(rebalanceX, rebalanceY);
        Link nearestLink = NetworkUtils.getNearestLink(network, randomRebalanceCoord);
        setRoboTaxiRebalance(taxi, nearestLink);
    }


    private double fractionOfDivertableUnassignedTaxisAroundCenter() {
        return (double) getDivertableUnassignedRoboTaxis().stream().filter(rt -> {
            Link lastLocation = rt.getLastKnownLocation();
            Point lastLocationPoint = geometryFactory.createPoint(new Coordinate(lastLocation.getCoord().getX(),
                    lastLocation.getCoord().getY()));
            return circleAroundCenter.contains(lastLocationPoint);
        }).count() / getDivertableUnassignedRoboTaxis().size();
    }
    private long missingTaxisInCenter(){
        long taxisInCenter =  getStayingTaxis().stream().filter(rt -> {
            Link lastLocation = rt.getLastKnownLocation();
            Point lastLocationPoint = geometryFactory.createPoint(new Coordinate(lastLocation.getCoord().getX(),
                    lastLocation.getCoord().getY()));
            return circleAroundCenter.contains(lastLocationPoint);
        }).count();
        long stayingTaxis = getStayingTaxis().size();

        return (int) (this.inCenterThreshold*stayingTaxis) - taxisInCenter;
    }

    private void tryToRebalanceTaxiToRank(RoboTaxi taxi) {
        if (taxi.isAtTaxiRank()){
            logger.warn("Tyring to rebalance a taxi which is already at a rank");
            return;
        }


        DispatchingZone nearestZone = zonalUtils.getNearestZoneToLink(taxi.getLastKnownLocation());
        if (tryToRebalanceTaxiToRankInZone(taxi, nearestZone)) {
            return;
        }

        List<Id<Zone>> adjacentZoneIds = nearestZone.getAdjacentZones();
        for (Id<Zone> zoneId : adjacentZoneIds) {
            if (tryToRebalanceTaxiToRankInZone(taxi, zonalUtils.getZones().get(zoneId))) {
                return;
            }
        }

        rebalanceTaxiToRandomRank(taxi, centerRanks);

        //rebalanceTaxiToCenter(taxi);
    }

    private boolean tryToRebalanceTaxiToRankInZone(RoboTaxi taxi, Zone zone) {
        Set<TaxiRank> ranksInZone = zoneTaxiRankMap.get(zone.getId());
        if (ranksInZone != null) {
            List<Link> taxiRankLinks = ranksInZone.stream()
                    .filter(rank -> rank.getNumberOfTaxis() < rank.getCapacity())
                    .map(TaxiRank::getNearestLink)
                    .collect(Collectors.toList());

            if (!taxiRankLinks.isEmpty()) {
                Link linkOfNearestRank = MatsimNetworkUtil.getNearestLink(taxi.getDivertableLocation(), taxiRankLinks);
                GlobalAssert.that(network.getLinks().containsValue(linkOfNearestRank));
                setRoboTaxiRebalance(taxi, linkOfNearestRank);
                return true;
            }
        }
        return false;
    }

    /**
     * Rebalance taxi to a random rank, which has free slots at the moment
     * Random choice is done according to precalculated rank probabilities.
     * @param taxi taxi to be rebalanced to a random free rank
     */
    private boolean rebalanceTaxiToRandomRank(RoboTaxi taxi){
        return rebalanceTaxiToRandomRank(taxi, taxiRanks);
    }

    /**
     * Rebalance taxi to a random rank, which has free slots at the moment
     * Random choice is done according to precalculated rank probabilities.
     * @param taxi taxi to be rebalanced to a random free rank
     * @param taxiRanks subset of ranks to be used
     */
    private boolean rebalanceTaxiToRandomRank(RoboTaxi taxi, Collection<TaxiRank> taxiRanks){
        TaxiRank randomFreeRank = AmodeusUtil.getRandomFreeTaxiRank(taxiRanks, random);
        if(randomFreeRank != null) {
            setRoboTaxiRebalance(taxi, randomFreeRank.getNearestLink());
            return true;
        }
        else return false;
    }



    // Rebalance active Taxis to the next Taxi Rank
    @Override
    protected void setRoboTaxiActive(RoboTaxi roboTaxi) {
        super.setRoboTaxiActive(roboTaxi);
        //TODO: Check this, not possible with new Task Schedule
        //tryToRebalanceTaxiToRank(roboTaxi);
    }


    /**
     * Get the next waiting taxi in queue at the given rank, if present.
     * @param availableTaxis List of all available Taxis taken into account for dispatching
     * @param rankLink Link of the related taxi rank
     * @return The longest waiting taxi at the given rank, if present. Empty optional otherwise.
     */
    private Optional<RoboTaxi> getNextTaxiAtTaxiRank(Collection<RoboTaxi> availableTaxis, Link rankLink) {
        List<RoboTaxi> taxisAtRank = availableTaxis.stream().filter(RoboTaxi::isAtTaxiRank).filter(rt -> rt.getCurrentDriveDestination() == rankLink).collect(Collectors.toList());
        taxisAtRank.forEach(t-> GlobalAssert.that(t.getStatus()!=RoboTaxiStatus.OFFSERVICE));
        if (taxisAtRank.isEmpty()) {
            return Optional.empty();
        } else {
            // Return taxi, with the longest waiting time.
            return Optional.of(Collections.min(taxisAtRank, Comparator.comparing(t -> t.getSchedule().getCurrentTask().getBeginTime())));
        }
    }

    private Optional<RoboTaxi> getClosestTaxiInZone(List<RoboTaxi> availableTaxis, Zone zone, Link link) {
        List<RoboTaxi> taxisInZone = AmodeusUtil.getTaxisInZone(availableTaxis, zone);
        if (taxisInZone != null && !taxisInZone.isEmpty()) {
            return Optional.of(MatsimNetworkUtil.getClosestRoboTaxi(link, availableTaxis));
        }
        return Optional.empty();
    }

    private double getEuclideanDistanceToCenter(RoboTaxi taxi) {
        return NetworkUtils.getEuclideanDistance(networkCenter,
                new Coord(taxi.getLastKnownLocation().getCoord().getX(), taxi.getLastKnownLocation().getCoord().getY()));
    }


    public static class Factory implements AVDispatcherFactory {
        @Override
        public AmodeusDispatcher createDispatcher(ModalProviders.InstanceGetter inject) {
            Config config = inject.get(Config.class);
            AmodeusModeConfig operatorConfig = inject.getModal(AmodeusModeConfig.class);
            AmodeusRouter router = inject.getModal(AmodeusRouter.class);
            MatsimAmodeusDatabase db = inject.get(MatsimAmodeusDatabase.class);
            TravelTime travelTime = inject.getModal(TravelTime.class);
            Network network = inject.getModal(Network.class);
            EventsManager eventsManager = inject.get(EventsManager.class);
            ZonalUtils zonalUtils = inject.get(ZonalUtils.class);
            TaxiRanks taxiRanks = inject.get(TaxiRanks.class);
            ScenarioOptions scenarioOptions = inject.get(ScenarioOptions.class);
            DynamicFleetSize dynamicFleetSize = inject.get(DynamicFleetSize.class);

            return new ReferenceDispatcher(config, operatorConfig, travelTime, router,
                    eventsManager, network, db, zonalUtils, taxiRanks, scenarioOptions, dynamicFleetSize);
        }
    }
}
