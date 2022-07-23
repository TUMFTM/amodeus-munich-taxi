/* amodeus - Copyright (c) 2018, ETH Zurich, Institute for Dynamic Systems and Control */
package amodeus.amodeus.testutils;

import amodeus.amodeus.data.LocationSpec;
import amodeus.amodeus.data.LocationSpecDatabase;

/* package */ enum StaticHelper {
    ;

    public static void setup() {
        for (LocationSpec locationSpec : TestLocationSpecs.values())
            LocationSpecDatabase.INSTANCE.put(locationSpec);
    }

}
