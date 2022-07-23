/* amod - Copyright (c) 2018, ETH Zurich, Institute for Dynamic Systems and Control */
package amodeus.amodeus.linkspeed;

import amodeus.amodeus.dispatcher.core.RoboTaxi;
import amodeus.amodeus.options.ScenarioOptions;
import amodeus.amodeus.options.ScenarioOptionsBase;
import amodeus.amodeus.util.io.MultiFileTools;
import amodeus.amodeus.util.math.GlobalAssert;
import org.matsim.amodeus.components.AmodeusRouter;
import org.matsim.amodeus.config.AmodeusConfigGroup;
import org.matsim.amodeus.config.FTMConfigGroup;
import org.matsim.amodeus.plpc.DefaultParallelLeastCostPathCalculator;
import org.matsim.amodeus.plpc.ParallelLeastCostPathCalculator;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.network.Node;
import org.matsim.api.core.v01.population.Person;
import org.matsim.contrib.dvrp.run.ModalProviders.InstanceGetter;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.config.groups.GlobalConfigGroup;
import org.matsim.core.router.DijkstraFactory;
import org.matsim.core.router.costcalculators.OnlyTimeDependentTravelDisutility;
import org.matsim.core.router.util.LeastCostPathCalculator.Path;
import org.matsim.core.router.util.TravelTime;
import org.matsim.vehicles.Vehicle;

import java.io.File;
import java.io.IOException;
import java.util.concurrent.Future;

/**
 * This is a nonfunctional sample demonstrating of how to include a custom
 * router to AMoDeus which is not the standard choice of the Paralllel Djikstra
 * router used normally to calculate the path for {@link RoboTaxi}
 */
public class TaxiTravelTimeRouter implements AmodeusRouter {
    private final ParallelLeastCostPathCalculator delegate;

    public TaxiTravelTimeRouter(ParallelLeastCostPathCalculator delegate) {
        this.delegate = delegate;
    }

    @Override
    public Future<Path> calcLeastCostPath(Node fromNode, Node toNode, double startTime, Person person, Vehicle vehicle) {
        return delegate.calcLeastCostPath(fromNode, toNode, startTime, person, vehicle);
    }

    @Override
    public void close() throws IOException {
        delegate.close();
    }

    public static class Factory implements AmodeusRouter.Factory {


        @Override
        public AmodeusRouter createRouter(InstanceGetter inject) {
            AmodeusConfigGroup config = inject.get(AmodeusConfigGroup.class);
            Network network = inject.getModal(Network.class);
            LinkSpeedDataContainer lsData = inject.get(LinkSpeedDataContainer.class);

            TravelTime travelTime = new LSDataTravelTime(lsData);

            return new TaxiTravelTimeRouter(DefaultParallelLeastCostPathCalculator.create((int) config.getNumberOfParallelRouters(), //
                    new DijkstraFactory(), network, new OnlyTimeDependentTravelDisutility(travelTime), travelTime));
        }

    }
}
