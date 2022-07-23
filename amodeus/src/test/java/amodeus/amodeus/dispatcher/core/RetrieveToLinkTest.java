/* amodeus - Copyright (c) 2018, ETH Zurich, Institute for Dynamic Systems and Control */
package amodeus.amodeus.dispatcher.core;

import java.util.Collections;
import java.util.Optional;
import java.util.stream.DoubleStream;

import org.matsim.api.core.v01.network.Link;
import org.matsim.contrib.dvrp.path.VrpPathWithTravelData;
import org.matsim.contrib.dvrp.path.VrpPathWithTravelDataImpl;
import org.matsim.contrib.dvrp.util.LinkTimePair;

import amodeus.amodeus.dispatcher.shared.SharedCourse;
import junit.framework.TestCase;

//TODO @clruch shorten this monster @marcalbert, e.g., distribute on several files
public class RetrieveToLinkTest extends TestCase {
    public void testStayTask() {
        ArtificialSharedScenarioCreator s = new ArtificialSharedScenarioCreator();

        Link divertableLink = s.linkDepotOut;
        RoboTaxi roboTaxi = StaticRoboTaxiCreator.createStayingRoboTaxi(divertableLink, null);

        // *****************************
        // Case 1 Staying vehicle where stayTask Link equals Divertable Link;

        // Casse 1a) No Course present
        assertFalse(RetrieveToLink.forShared(roboTaxi, 0.0).isPresent()); // case empty menu

        // Case 1b) Next course is on same link
        roboTaxi.addPassengerRequestToMenu(s.avRequestDepotOut);
        assertTrue(RetrieveToLink.forShared(roboTaxi, 0.0).isPresent()); // case pickup course
        assertEquals(RetrieveToLink.forShared(roboTaxi, 0.0).get(), s.avRequestDepotOut.getFromLink()); // case pickup course

        roboTaxi.pickupNewCustomerOnBoard();
        assertTrue(RetrieveToLink.forShared(roboTaxi, 0.0).isPresent()); // case dropoff course
        assertEquals(RetrieveToLink.forShared(roboTaxi, 0.0).get(), s.avRequestDepotOut.getToLink()); // case pickup course

        StaticRoboTaxiCreator.updateRoboTaxiMenuTo(roboTaxi, Collections.singletonList(SharedCourse.redirectCourse(s.linkDepotOut, "depotRed")));
        try {
            RetrieveToLink.forShared(roboTaxi, 0.0);
            fail();
        } catch (Exception exception) {
            // ---
        }
        StaticRoboTaxiCreator.cleanRTMenu(roboTaxi);

        // Case 1c) Next course is on different link
        assertFalse(RetrieveToLink.forShared(roboTaxi, 0.0).isPresent()); // case empty menu
        roboTaxi.addPassengerRequestToMenu(s.avRequest1);
        assertTrue(RetrieveToLink.forShared(roboTaxi, 0.0).isPresent()); // case pickup course
        assertEquals(RetrieveToLink.forShared(roboTaxi, 0.0).get(), s.avRequest1.getFromLink()); // case pickup course

        StaticRoboTaxiCreator.updateRoboTaxiMenuTo(roboTaxi, Collections.singletonList(SharedCourse.dropoffCourse(s.avRequest1)));
        assertTrue(RetrieveToLink.forShared(roboTaxi, 0.0).isPresent()); // case dropoff course
        assertEquals(RetrieveToLink.forShared(roboTaxi, 0.0).get(), s.avRequest1.getToLink()); // case pickup course

        StaticRoboTaxiCreator.updateRoboTaxiMenuTo(roboTaxi, Collections.singletonList(SharedCourse.redirectCourse(s.linkUp, "linkUpRedirection")));
        assertTrue(RetrieveToLink.forShared(roboTaxi, 0.0).isPresent()); // case redirect course
        assertEquals(RetrieveToLink.forShared(roboTaxi, 0.0).get(), s.linkUp); // case pickup course
        StaticRoboTaxiCreator.cleanRTMenu(roboTaxi);

        // *****************************
        // Case 2 Staying vehicle where stayTask Link NOT (!!!) equals Divertable Link;
        roboTaxi = StaticRoboTaxiCreator.createStayingRoboTaxi(divertableLink, s.linkDepotIn);
        // Case 2a) No Course present
        assertFalse(RetrieveToLink.forShared(roboTaxi, 0.0).isPresent()); // case empty menu

        // Case 2b) Next course is on same link as Divertable Location
        roboTaxi.addPassengerRequestToMenu(s.avRequestDepotOut);
        assertTrue(RetrieveToLink.forShared(roboTaxi, 0.0).isPresent()); // case pickup course
        assertEquals(RetrieveToLink.forShared(roboTaxi, 0.0).get(), s.linkDepotOut); // case pickup course

        roboTaxi.pickupNewCustomerOnBoard();
        assertTrue(RetrieveToLink.forShared(roboTaxi, 0.0).isPresent()); // case dropoff course
        assertEquals(RetrieveToLink.forShared(roboTaxi, 0.0).get(), s.linkDepotOut); // case pickup course

        StaticRoboTaxiCreator.updateRoboTaxiMenuTo(roboTaxi, Collections.singletonList(SharedCourse.redirectCourse(s.linkDepotOut, "depotRed")));
        // FIXME @ChengQi the next test should be possible This is a diversion from the stay task to the divertable location
        // assertTrue(SharedRoboTaxiDiversionHelper.getToLink(roboTaxi, 0.0).get().equals(s.linkDepotOut)); // case pickup course
        try {
            RetrieveToLink.forShared(roboTaxi, 0.0);
            fail();
        } catch (Exception exception) {
            // ---
        }
        StaticRoboTaxiCreator.cleanRTMenu(roboTaxi);

        // Case 2c) Next course is on same link as Stay Task Location
        roboTaxi.addPassengerRequestToMenu(s.avRequestDepotIn);
        assertTrue(RetrieveToLink.forShared(roboTaxi, 0.0).isPresent()); // case pickup course
        assertEquals(RetrieveToLink.forShared(roboTaxi, 0.0).get(), s.linkDepotIn); // case pickup course

        roboTaxi.setDivertableLinkTime(new LinkTimePair(s.linkDepotIn, 0.0));
        roboTaxi.pickupNewCustomerOnBoard();
        roboTaxi.setDivertableLinkTime(new LinkTimePair(s.linkDepotOut, 0.0));

        assertTrue(RetrieveToLink.forShared(roboTaxi, 0.0).isPresent()); // case dropoff course
        assertEquals(RetrieveToLink.forShared(roboTaxi, 0.0).get(), s.linkDepotIn); // case pickup course

        StaticRoboTaxiCreator.updateRoboTaxiMenuTo(roboTaxi, Collections.singletonList(SharedCourse.redirectCourse(s.linkDepotIn, "depotRed")));
        // FIXME @ChengQi This should be empty as it is a redirection to the current location so no diversion is required.
        assertTrue(RetrieveToLink.forShared(roboTaxi, 0.0).isPresent()); // case dropoff course
        assertEquals(RetrieveToLink.forShared(roboTaxi, 0.0).get(), s.linkDepotIn); // case pickup course
        StaticRoboTaxiCreator.cleanRTMenu(roboTaxi);

        // Case 2d) Next course is on different link
        assertFalse(RetrieveToLink.forShared(roboTaxi, 0.0).isPresent()); // case empty menu
        roboTaxi.addPassengerRequestToMenu(s.avRequest1);
        assertTrue(RetrieveToLink.forShared(roboTaxi, 0.0).isPresent()); // case pickup course
        assertEquals(RetrieveToLink.forShared(roboTaxi, 0.0).get(), s.avRequest1.getFromLink()); // case pickup course

        StaticRoboTaxiCreator.updateRoboTaxiMenuTo(roboTaxi, Collections.singletonList(SharedCourse.dropoffCourse(s.avRequest1)));
        assertTrue(RetrieveToLink.forShared(roboTaxi, 0.0).isPresent()); // case dropoff course
        assertEquals(RetrieveToLink.forShared(roboTaxi, 0.0).get(), s.avRequest1.getToLink()); // case pickup course

        StaticRoboTaxiCreator.updateRoboTaxiMenuTo(roboTaxi, Collections.singletonList(SharedCourse.redirectCourse(s.linkUp, "linkUpRedirection")));
        assertTrue(RetrieveToLink.forShared(roboTaxi, 0.0).isPresent()); // case redirect course
        assertEquals(RetrieveToLink.forShared(roboTaxi, 0.0).get(), s.linkUp); // case pickup course
        StaticRoboTaxiCreator.cleanRTMenu(roboTaxi);
    }

    public void testPickupTask() {
        ArtificialSharedScenarioCreator artificialScenarioCreator = new ArtificialSharedScenarioCreator();

        Link divertableLink = artificialScenarioCreator.linkDepotOut;
        RoboTaxi roboTaxi = StaticRoboTaxiCreator.createPickUpRoboTaxi(divertableLink);

        // ***************************************************
        // Case 1 Task ends in the future. Now is 5.0
        double now = 5.0;

        // Case 1a) No course present
        try { // Impossible because if a pickup task is going on there has always to be a dropoff course
            RetrieveToLink.forShared(roboTaxi, now);
            fail();
        } catch (Exception exception) {
            // ---
        }

        // Case 1b) Next course is on same link
        roboTaxi.addPassengerRequestToMenu(artificialScenarioCreator.avRequestDepotOut);
        assertFalse(RetrieveToLink.forShared(roboTaxi, now).isPresent()); // case pickup course

        roboTaxi.pickupNewCustomerOnBoard();
        assertFalse(RetrieveToLink.forShared(roboTaxi, now).isPresent()); // case dropoff course

        StaticRoboTaxiCreator.updateRoboTaxiMenuTo(roboTaxi, Collections.singletonList(SharedCourse.redirectCourse(artificialScenarioCreator.linkDepotOut, "depotRed")));
        try {
            RetrieveToLink.forShared(roboTaxi, now);
            fail();
        } catch (Exception exception) {
            // ---
        }
        StaticRoboTaxiCreator.cleanRTMenu(roboTaxi);

        // Case 1c) Next course is on other link
        roboTaxi.addPassengerRequestToMenu(artificialScenarioCreator.avRequest1);
        assertFalse(RetrieveToLink.forShared(roboTaxi, now).isPresent()); // case pickup course

        StaticRoboTaxiCreator.updateRoboTaxiMenuTo(roboTaxi, Collections.singletonList(SharedCourse.dropoffCourse(artificialScenarioCreator.avRequest1)));
        assertFalse(RetrieveToLink.forShared(roboTaxi, now).isPresent()); // case dropoff course

        StaticRoboTaxiCreator.updateRoboTaxiMenuTo(roboTaxi, Collections.singletonList(SharedCourse.redirectCourse(artificialScenarioCreator.linkUp, "RedirectionUp")));
        assertFalse(RetrieveToLink.forShared(roboTaxi, now).isPresent()); // case dropoff course
        StaticRoboTaxiCreator.cleanRTMenu(roboTaxi);

        // ***************************************************
        // Case 2 Task ends now. Now is equal to task end
        now = StaticRoboTaxiCreator.TASK_END;

        // Case 2a) No course present
        try { // Impossible because if a pickup task is going on there has always to be a dropoff course
            RetrieveToLink.forShared(roboTaxi, now);
            fail();
        } catch (Exception exception) {
            // ---
        }

        // Case 2b) Next course is on same link
        roboTaxi.addPassengerRequestToMenu(artificialScenarioCreator.avRequestDepotOut);
        assertTrue(RetrieveToLink.forShared(roboTaxi, now).isPresent()); // case pickup course
        assertEquals(RetrieveToLink.forShared(roboTaxi, now).get(), artificialScenarioCreator.linkDepotOut); // case pickup course

        roboTaxi.pickupNewCustomerOnBoard();
        assertTrue(RetrieveToLink.forShared(roboTaxi, now).isPresent()); // case dropoff course
        assertEquals(RetrieveToLink.forShared(roboTaxi, now).get(), artificialScenarioCreator.linkDepotOut); // case dropoff course

        StaticRoboTaxiCreator.updateRoboTaxiMenuTo(roboTaxi, Collections.singletonList(SharedCourse.redirectCourse(artificialScenarioCreator.linkDepotOut, "depotRed")));
        try {
            RetrieveToLink.forShared(roboTaxi, now);
            fail();
        } catch (Exception exception) {
            // ---
        }
        StaticRoboTaxiCreator.cleanRTMenu(roboTaxi);

        // Case 2c) Next course is on other link
        roboTaxi.addPassengerRequestToMenu(artificialScenarioCreator.avRequest1);
        assertTrue(RetrieveToLink.forShared(roboTaxi, now).isPresent()); // case pickup course
        assertEquals(RetrieveToLink.forShared(roboTaxi, now).get(), artificialScenarioCreator.linkUp); // case pickup course

        StaticRoboTaxiCreator.updateRoboTaxiMenuTo(roboTaxi, Collections.singletonList(SharedCourse.dropoffCourse(artificialScenarioCreator.avRequest1)));
        assertTrue(RetrieveToLink.forShared(roboTaxi, now).isPresent()); // case dropoff course
        assertEquals(RetrieveToLink.forShared(roboTaxi, now).get(), artificialScenarioCreator.linkDown); // case pickup course

        StaticRoboTaxiCreator.updateRoboTaxiMenuTo(roboTaxi, Collections.singletonList(SharedCourse.redirectCourse(artificialScenarioCreator.linkUp, "RedirectionUp")));
        assertTrue(RetrieveToLink.forShared(roboTaxi, now).isPresent()); // case dropoff course
        assertEquals(RetrieveToLink.forShared(roboTaxi, now).get(), artificialScenarioCreator.linkUp); // case pickup course
        StaticRoboTaxiCreator.cleanRTMenu(roboTaxi);
    }

    public void testDropoffTask() {
        ArtificialSharedScenarioCreator s = new ArtificialSharedScenarioCreator();

        Link divertableLink = s.linkDepotOut;
        RoboTaxi roboTaxi = StaticRoboTaxiCreator.createDropoffRoboTaxi(divertableLink);

        // ***************************************************
        // Case 1 Task ends in the future. Now is 5.0
        double now = 5.0;

        // Case 1a) No course present
        try { // Impossible because if a pickup task is going on there has always to be a dropoff course
            RetrieveToLink.forShared(roboTaxi, now);
            fail();
        } catch (Exception exception) {
            // ---
        }

        // Case 1b) Next course is on same link
        roboTaxi.addPassengerRequestToMenu(s.avRequestDepotOut);
        assertFalse(RetrieveToLink.forShared(roboTaxi, now).isPresent()); // case pickup course

        roboTaxi.pickupNewCustomerOnBoard();
        assertFalse(RetrieveToLink.forShared(roboTaxi, now).isPresent()); // case dropoff course

        StaticRoboTaxiCreator.updateRoboTaxiMenuTo(roboTaxi, Collections.singletonList(SharedCourse.redirectCourse(s.linkDepotOut, "depotRed")));
        try {
            RetrieveToLink.forShared(roboTaxi, now);
            fail();
        } catch (Exception exception) {
            // ---
        }
        StaticRoboTaxiCreator.cleanRTMenu(roboTaxi);

        // Case 1c) Next course is on other link
        roboTaxi.addPassengerRequestToMenu(s.avRequest1);
        assertFalse(RetrieveToLink.forShared(roboTaxi, now).isPresent()); // case pickup course

        StaticRoboTaxiCreator.updateRoboTaxiMenuTo(roboTaxi, Collections.singletonList(SharedCourse.dropoffCourse(s.avRequest1)));
        assertFalse(RetrieveToLink.forShared(roboTaxi, now).isPresent()); // case dropoff course

        StaticRoboTaxiCreator.updateRoboTaxiMenuTo(roboTaxi, Collections.singletonList(SharedCourse.redirectCourse(s.linkUp, "RedirectionUp")));
        assertFalse(RetrieveToLink.forShared(roboTaxi, now).isPresent()); // case dropoff course
        StaticRoboTaxiCreator.cleanRTMenu(roboTaxi);

        // ***************************************************
        // Case 2 Task ends now. Now is equal to task end
        now = StaticRoboTaxiCreator.TASK_END;

        // Case 2a) No course present
        assertFalse(RetrieveToLink.forShared(roboTaxi, now).isPresent());

        // Case 2b) Next course is on same link
        roboTaxi.addPassengerRequestToMenu(s.avRequestDepotOut);
        assertTrue(RetrieveToLink.forShared(roboTaxi, now).isPresent()); // case pickup course
        assertEquals(RetrieveToLink.forShared(roboTaxi, now).get(), s.linkDepotOut); // case pickup course

        roboTaxi.pickupNewCustomerOnBoard();
        assertTrue(RetrieveToLink.forShared(roboTaxi, now).isPresent()); // case dropoff course
        assertEquals(RetrieveToLink.forShared(roboTaxi, now).get(), s.linkDepotOut); // case dropoff course

        StaticRoboTaxiCreator.updateRoboTaxiMenuTo(roboTaxi, Collections.singletonList(SharedCourse.redirectCourse(s.linkDepotOut, "depotRed")));
        try {
            RetrieveToLink.forShared(roboTaxi, now);
            fail();
        } catch (Exception exception) {
            // ---
        }
        StaticRoboTaxiCreator.cleanRTMenu(roboTaxi);

        // Case 2c) Next course is on other link
        roboTaxi.addPassengerRequestToMenu(s.avRequest1);
        assertTrue(RetrieveToLink.forShared(roboTaxi, now).isPresent()); // case pickup course
        assertEquals(RetrieveToLink.forShared(roboTaxi, now).get(), s.linkUp); // case pickup course

        StaticRoboTaxiCreator.updateRoboTaxiMenuTo(roboTaxi, Collections.singletonList(SharedCourse.dropoffCourse(s.avRequest1)));
        assertTrue(RetrieveToLink.forShared(roboTaxi, now).isPresent()); // case dropoff course
        assertEquals(RetrieveToLink.forShared(roboTaxi, now).get(), s.linkDown); // case pickup course

        StaticRoboTaxiCreator.updateRoboTaxiMenuTo(roboTaxi, Collections.singletonList(SharedCourse.redirectCourse(s.linkUp, "RedirectionUp")));
        assertTrue(RetrieveToLink.forShared(roboTaxi, now).isPresent()); // case dropoff course
        assertEquals(RetrieveToLink.forShared(roboTaxi, now).get(), s.linkUp); // case pickup course
        StaticRoboTaxiCreator.cleanRTMenu(roboTaxi);
    }

    public void testDriveTask() {
        ArtificialSharedScenarioCreator s = new ArtificialSharedScenarioCreator();

        Link[] links = { s.linkDepotOut, s.linkLeft, s.linkUp };
        double[] linkTTs = { 5.0, 5.0, 5.0 };
        double travelTIme = DoubleStream.of(linkTTs).sum();

        VrpPathWithTravelData vrpPathWithTravelData = new VrpPathWithTravelDataImpl(0.0, travelTIme, links, linkTTs);
        RoboTaxi roboTaxi = StaticRoboTaxiCreator.createDriveRoboTaxi(vrpPathWithTravelData);

        // ***************************************************
        // Case 1 Task ends in the future. Now is 5.0
        double now = 5.0;

        // Case 1a) No course present
        {
            Optional<Link> link = RetrieveToLink.forShared(roboTaxi, now);
            assertTrue(link.isPresent());
            assertEquals(link.get(), s.linkDepotOut);
        }

        // Case 1b) Next course is on same link as current position
        roboTaxi.addPassengerRequestToMenu(s.avRequestDepotOut);
        assertTrue(RetrieveToLink.forShared(roboTaxi, now).isPresent());
        assertEquals(RetrieveToLink.forShared(roboTaxi, now).get(), s.linkDepotOut);

        roboTaxi.pickupNewCustomerOnBoard();
        assertTrue(RetrieveToLink.forShared(roboTaxi, now).isPresent());
        assertEquals(RetrieveToLink.forShared(roboTaxi, now).get(), s.linkDepotOut);

        StaticRoboTaxiCreator.updateRoboTaxiMenuTo(roboTaxi, Collections.singletonList(SharedCourse.redirectCourse(s.linkDepotOut, "depotRed")));
        try {
            RetrieveToLink.forShared(roboTaxi, now);
            fail();
        } catch (Exception exception) {
            // ---
        }
        StaticRoboTaxiCreator.cleanRTMenu(roboTaxi);

        // Case 1c) Next course is on same course as destination link
        roboTaxi.addPassengerRequestToMenu(s.avRequest1);
        assertFalse(RetrieveToLink.forShared(roboTaxi, now).isPresent()); // case pickup course

        StaticRoboTaxiCreator.updateRoboTaxiMenuTo(roboTaxi, Collections.singletonList(SharedCourse.dropoffCourse(s.avRequest3)));
        assertFalse(RetrieveToLink.forShared(roboTaxi, now).isPresent()); // case dropoff course

        StaticRoboTaxiCreator.updateRoboTaxiMenuTo(roboTaxi, Collections.singletonList(SharedCourse.redirectCourse(s.linkUp, "RedirectionUp")));
        assertFalse(RetrieveToLink.forShared(roboTaxi, now).isPresent()); // case dropoff course
        StaticRoboTaxiCreator.cleanRTMenu(roboTaxi);

        // Case 1d) Next course is on different link than divertable and destination link
        roboTaxi.addPassengerRequestToMenu(s.avRequest4);
        assertTrue(RetrieveToLink.forShared(roboTaxi, now).isPresent());
        assertEquals(RetrieveToLink.forShared(roboTaxi, now).get(), s.linkRight);

        StaticRoboTaxiCreator.updateRoboTaxiMenuTo(roboTaxi, Collections.singletonList(SharedCourse.dropoffCourse(s.avRequest4)));
        assertTrue(RetrieveToLink.forShared(roboTaxi, now).isPresent());
        assertEquals(RetrieveToLink.forShared(roboTaxi, now).get(), s.linkDown);

        StaticRoboTaxiCreator.updateRoboTaxiMenuTo(roboTaxi, Collections.singletonList(SharedCourse.redirectCourse(s.linkDepotIn, "depotRedIn")));
        assertTrue(RetrieveToLink.forShared(roboTaxi, now).isPresent());
        assertEquals(RetrieveToLink.forShared(roboTaxi, now).get(), s.linkDepotIn);
        StaticRoboTaxiCreator.cleanRTMenu(roboTaxi);

        // ***************************************************
        // Case 2 Task ends now. Now is equal to task end but we are not yet on the last link of the path
        now = travelTIme;

        // Case 2a) No course present
        { // Divertable Location is not on last link of path
            Optional<Link> link = RetrieveToLink.forShared(roboTaxi, now);
            assertTrue(link.isPresent());
            assertEquals(link.get(), s.linkDepotOut);
        }

        { // Divertable Location is on last link of path
            roboTaxi.setDivertableLinkTime(new LinkTimePair(s.linkUp, now));
            Optional<Link> link = RetrieveToLink.forShared(roboTaxi, now);
            assertTrue(link.isPresent());
            assertEquals(link.get(), s.linkDepotOut);
            roboTaxi.setDivertableLinkTime(new LinkTimePair(s.linkDepotOut, now));
        }

        // Case 2b) Next course is on same link as current position
        roboTaxi.addPassengerRequestToMenu(s.avRequestDepotOut);
        assertTrue(RetrieveToLink.forShared(roboTaxi, now).isPresent());
        assertEquals(RetrieveToLink.forShared(roboTaxi, now).get(), s.linkDepotOut);

        roboTaxi.pickupNewCustomerOnBoard();
        assertTrue(RetrieveToLink.forShared(roboTaxi, now).isPresent());
        assertEquals(RetrieveToLink.forShared(roboTaxi, now).get(), s.linkDepotOut);

        StaticRoboTaxiCreator.updateRoboTaxiMenuTo(roboTaxi, Collections.singletonList(SharedCourse.redirectCourse(s.linkDepotOut, "depotRed")));
        try {
            RetrieveToLink.forShared(roboTaxi, now);
            fail();
        } catch (Exception exception) {
            // ---
        }
        StaticRoboTaxiCreator.cleanRTMenu(roboTaxi);

        // Case 2c) Next course is on same course as destination link
        roboTaxi.addPassengerRequestToMenu(s.avRequest1);
        assertTrue(RetrieveToLink.forShared(roboTaxi, now).isPresent()); // case pickup course
        assertEquals(RetrieveToLink.forShared(roboTaxi, now).get(), s.linkUp);

        StaticRoboTaxiCreator.updateRoboTaxiMenuTo(roboTaxi, Collections.singletonList(SharedCourse.dropoffCourse(s.avRequest3)));
        assertTrue(RetrieveToLink.forShared(roboTaxi, now).isPresent()); // case dropoff course
        assertEquals(RetrieveToLink.forShared(roboTaxi, now).get(), s.linkUp);

        StaticRoboTaxiCreator.updateRoboTaxiMenuTo(roboTaxi, Collections.singletonList(SharedCourse.redirectCourse(s.linkUp, "RedirectionUp")));
        assertTrue(RetrieveToLink.forShared(roboTaxi, now).isPresent()); // case dropoff course
        assertEquals(RetrieveToLink.forShared(roboTaxi, now).get(), s.linkUp);
        StaticRoboTaxiCreator.cleanRTMenu(roboTaxi);

        // Case 2c) Next course is on different link than divertable and end link
        roboTaxi.addPassengerRequestToMenu(s.avRequest4);
        assertTrue(RetrieveToLink.forShared(roboTaxi, now).isPresent());
        assertEquals(RetrieveToLink.forShared(roboTaxi, now).get(), s.linkRight);

        StaticRoboTaxiCreator.updateRoboTaxiMenuTo(roboTaxi, Collections.singletonList(SharedCourse.dropoffCourse(s.avRequest4)));
        assertTrue(RetrieveToLink.forShared(roboTaxi, now).isPresent());
        assertEquals(RetrieveToLink.forShared(roboTaxi, now).get(), s.linkDown);

        StaticRoboTaxiCreator.updateRoboTaxiMenuTo(roboTaxi, Collections.singletonList(SharedCourse.redirectCourse(s.linkDepotIn, "depotRedIn")));
        assertTrue(RetrieveToLink.forShared(roboTaxi, now).isPresent());
        assertEquals(RetrieveToLink.forShared(roboTaxi, now).get(), s.linkDepotIn);
        StaticRoboTaxiCreator.cleanRTMenu(roboTaxi);
    }
}
