/* amodeus - Copyright (c) 2019, ETH Zurich, Institute for Dynamic Systems and Control */
package amodeus.amodeus.parking;

import java.util.Random;

import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.population.Population;
import org.matsim.core.controler.AbstractModule;

import com.google.inject.Provides;
import com.google.inject.Singleton;

import amodeus.amodeus.options.ScenarioOptions;
import amodeus.amodeus.parking.capacities.ParkingCapacity;
import amodeus.amodeus.parking.strategies.ParkingStrategy;

/** This MATSim {@link AbstractModule} is required for all dispatchers which take parking into
 * consideration, i.e., the ones with an additional {@link ParkingStrategy}.
 * It provides the parking capacities of all the {@link Link}s and provides
 * as well the strategy to avoid overfilling. */
public class AmodeusParkingModule extends AbstractModule {
    private final ScenarioOptions scenarioOptions;
    private ParkingCapacity parkingCapacity;
    private final Random random;

    public AmodeusParkingModule(ScenarioOptions scenarioOptions, Random random) {
        this.scenarioOptions = scenarioOptions;
        this.random = random;
    }

    @Override
    public void install() {
        // ---
    }

    @Provides
    @Singleton
    public ParkingStrategy provideParkingStrategy() {
        return scenarioOptions.getParkingStrategy(random);
    }

    @Provides
    @Singleton
    public ParkingCapacity provideAVSpatialCapacity(Network network, Population population) {
        try {
            ParkingCapacityGenerator generator = scenarioOptions.getParkingCapacityGenerator();
            parkingCapacity = generator.generate(network, population, scenarioOptions);
            return parkingCapacity;
        } catch (Exception exception) {
            System.err.println("Unable to load parking capacity for all links, returning null.");
            exception.printStackTrace();
        }
        return null;
    }

    public ParkingCapacity getParkingCapacity() {
        return parkingCapacity;
    }
}
