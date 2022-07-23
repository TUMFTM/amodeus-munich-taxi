/* amodeus - Copyright (c) 2018, ETH Zurich, Institute for Dynamic Systems and Control */
package amodeus.amodeus.dispatcher.shared;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import amodeus.amodeus.dispatcher.core.RoboTaxi;
import amodeus.amodeus.util.math.GlobalAssert;

/** Top level class in SharedRoboTaxi functionality, a {@link SharedMenu} is
 * composed of {@link SharedCourse}s which internally have a {@link SharedMealType}s
 * A Menu contains a list of shared Courses (pickup, dropoff, rebalance) planned
 * for an RoboTaxi. The List of shared Couses can not be null. It is empty instead.
 * 
 * Important: the List of Shared Courses is final and not modifiable.
 * Thus only a View on the current menu can be received and changes are not permitted */
public class SharedMenu {
    private static final SharedMenu EMPTY = new SharedMenu(new ArrayList<>());

    /** Creates a Shared Menu which is consistent in itself (e.g. no coureses appear twice, for each request it is secured that the dropoff happens after the
     * pickup
     * 
     * @param list of {@link SharedCourse}
     * @return */
    public static SharedMenu of(List<SharedCourse> list) {
        GlobalAssert.that(SharedMenuCheck.coursesAppearOnce(list));
        GlobalAssert.that(SharedMenuCheck.eachPickupAfterDropoff(list));
        return new SharedMenu(new ArrayList<>(list));
    }

    /** Creates an empty Menu. It has no next course. It can be used for example for idling {@link RoboTaxi}
     * 
     * @return {@link SharedMenu} with no courses planed. */
    public static SharedMenu empty() {
        return EMPTY;
    }

    // ---
    /** Unmodifiable List of Shared Courses */
    private final List<SharedCourse> roboTaxiMenu;
    private final long menuOnBoardCustomers;

    private SharedMenu(List<SharedCourse> list) {
        roboTaxiMenu = Collections.unmodifiableList(list);
        menuOnBoardCustomers = OnMenuRequests.getOnBoardCustomers(list);
    }

    /** @return an unmodifiable view of the menu */
    public List<SharedCourse> getCourseList() {
        return roboTaxiMenu;
    }

    public long getMenuOnBoardCustomers() {
        return menuOnBoardCustomers;
    }

    @Override
    public boolean equals(Object object) {
        return object instanceof SharedMenu && ((SharedMenu) object).roboTaxiMenu.equals(roboTaxiMenu);
    }

    @Override
    public int hashCode() {
        return roboTaxiMenu.hashCode();
    }

}
