package org.matsim.amodeus.dynamics;

import java.io.File;
import java.io.IOException;
import java.util.LinkedList;
import java.util.List;

import org.apache.commons.io.FileUtils;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.Test;
import org.matsim.amodeus.config.AmodeusConfigGroup;
import org.matsim.amodeus.config.AmodeusModeConfig;
import org.matsim.amodeus.config.modal.AmodeusScoringConfig;
import org.matsim.amodeus.framework.AmodeusModule;
import org.matsim.amodeus.framework.AmodeusQSimModule;
import org.matsim.amodeus.routing.AmodeusRoute;
import org.matsim.amodeus.scenario.TestScenarioGenerator;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.api.core.v01.population.PlanElement;
import org.matsim.api.core.v01.population.Population;
import org.matsim.contrib.dvrp.run.DvrpConfigGroup;
import org.matsim.contrib.dvrp.run.DvrpModule;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.config.groups.PlanCalcScoreConfigGroup;
import org.matsim.core.config.groups.StrategyConfigGroup.StrategySettings;
import org.matsim.core.controler.Controler;
import org.matsim.core.controler.events.IterationEndsEvent;
import org.matsim.core.controler.listener.IterationEndsListener;

public class WaitingTimeTest {
    static AmodeusConfigGroup createConfig() {
        AmodeusConfigGroup avConfigGroup = new AmodeusConfigGroup();

        AmodeusModeConfig operatorConfig = new AmodeusModeConfig(AmodeusModeConfig.DEFAULT_MODE);
        operatorConfig.getGeneratorConfig().setNumberOfVehicles(100);
        operatorConfig.getPricingConfig().setPricePerKm(0.48);
        operatorConfig.getPricingConfig().setSpatialBillingInterval(1000.0);
        avConfigGroup.addMode(operatorConfig);

        AmodeusScoringConfig scoringParams = operatorConfig.getScoringParameters(null);
        scoringParams.setMarginalUtilityOfWaitingTime(-0.84);

        return avConfigGroup;
    }

    static Controler createController(AmodeusConfigGroup avConfigGroup) {
        Config config = ConfigUtils.createConfig(avConfigGroup, new DvrpConfigGroup());
        Scenario scenario = TestScenarioGenerator.generateWithAVLegs(config);

        PlanCalcScoreConfigGroup.ModeParams modeParams = config.planCalcScore().getOrCreateModeParams(AmodeusModeConfig.DEFAULT_MODE);
        modeParams.setMonetaryDistanceRate(0.0);
        modeParams.setMarginalUtilityOfTraveling(8.86);
        modeParams.setConstant(0.0);

        Controler controler = new Controler(scenario);
        controler.addOverridingModule(new DvrpModule());
        controler.addOverridingModule(new AmodeusModule());
        controler.addOverridingQSimModule(new AmodeusQSimModule());

        controler.configureQSimComponents(AmodeusQSimModule.activateModes(avConfigGroup));

        return controler;
    }

    @Test
    public void testConstantWaitingTime() {
        AmodeusConfigGroup config = createConfig();
        AmodeusModeConfig operatorConfig = config.getModes().get(AmodeusModeConfig.DEFAULT_MODE);

        operatorConfig.getWaitingTimeEstimationConfig().setDefaultWaitingTime(123.0);

        Controler controller = createController(config);
        controller.run();

        Population population = controller.getScenario().getPopulation();

        int numberOfRoutes = 0;

        for (Person person : population.getPersons().values()) {
            Plan plan = person.getSelectedPlan();

            for (PlanElement element : plan.getPlanElements()) {
                if (element instanceof Leg) {
                    Leg leg = (Leg) element;
                    AmodeusRoute route = (AmodeusRoute) leg.getRoute();

                    Assert.assertEquals(route.getWaitingTime().seconds(), 123.0, 1e-2);
                    numberOfRoutes++;
                }
            }
        }

        Assert.assertEquals(100, numberOfRoutes);
    }

    @Test
    public void testAttributeWaitingTime() {
        AmodeusConfigGroup config = createConfig();
        AmodeusModeConfig operatorConfig = config.getModes().get(AmodeusModeConfig.DEFAULT_MODE);

        operatorConfig.getWaitingTimeEstimationConfig().setDefaultWaitingTime(123.0);
        operatorConfig.getWaitingTimeEstimationConfig().setConstantWaitingTimeLinkAttribute("avWaitingTime");

        Controler controller = createController(config);

        Link link = controller.getScenario().getNetwork().getLinks().get(Id.createLinkId("8:9_9:9"));
        link.getAttributes().putAttribute("avWaitingTime", 456.0);

        controller.run();

        Population population = controller.getScenario().getPopulation();

        int numberOfRoutes = 0;
        int numberOfSpecialRoutes = 0;

        for (Person person : population.getPersons().values()) {
            Plan plan = person.getSelectedPlan();

            for (PlanElement element : plan.getPlanElements()) {
                if (element instanceof Leg) {
                    Leg leg = (Leg) element;
                    AmodeusRoute route = (AmodeusRoute) leg.getRoute();

                    if (Id.createLinkId("8:9_9:9").equals(route.getStartLinkId())) {
                        Assert.assertEquals(route.getWaitingTime().seconds(), 456.0, 1e-2);
                        numberOfSpecialRoutes++;
                    } else {
                        Assert.assertEquals(route.getWaitingTime().seconds(), 123.0, 1e-2);
                    }

                    numberOfRoutes++;
                }
            }
        }

        Assert.assertEquals(100, numberOfRoutes);
        Assert.assertEquals(2, numberOfSpecialRoutes);
    }

    @Test
    public void testDynamicWaitingTime() {
        AmodeusConfigGroup config = createConfig();
        AmodeusModeConfig operatorConfig = config.getModes().get(AmodeusModeConfig.DEFAULT_MODE);

        operatorConfig.getWaitingTimeEstimationConfig().setDefaultWaitingTime(123.0);
        operatorConfig.getWaitingTimeEstimationConfig().setConstantWaitingTimeLinkAttribute("avWaitingTime");
        operatorConfig.getWaitingTimeEstimationConfig().setEstimationLinkAttribute("avGroup");
        operatorConfig.getWaitingTimeEstimationConfig().setEstimationAlpha(0.7);

        Controler controller = createController(config);

        Link link = controller.getScenario().getNetwork().getLinks().get(Id.createLinkId("8:9_9:9"));
        link.getAttributes().putAttribute("avWaitingTime", 456.0);

        int index = 0;
        for (Link _link : controller.getScenario().getNetwork().getLinks().values()) {
            _link.getAttributes().putAttribute("avGroup", index++);
        }

        controller.getConfig().controler().setLastIteration(2);

        StrategySettings strategy = new StrategySettings();
        strategy.setStrategyName("ReRoute");
        strategy.setWeight(1.0);
        controller.getConfig().strategy().addStrategySettings(strategy);

        List<Double> waitingTimes = new LinkedList<>();

        controller.addControlerListener(new IterationEndsListener() {
            @Override
            public void notifyIterationEnds(IterationEndsEvent event) {
                Population population = event.getServices().getScenario().getPopulation();
                Person person = population.getPersons().get(Id.createPersonId(17));
                Plan plan = person.getSelectedPlan();

                for (PlanElement element : plan.getPlanElements()) {
                    if (element instanceof Leg) {
                        Leg leg = (Leg) element;
                        AmodeusRoute route = (AmodeusRoute) leg.getRoute();

                        if (Id.createLinkId("8:9_9:9").equals(route.getStartLinkId())) {
                            waitingTimes.add(route.getWaitingTime().seconds());
                        }
                    }
                }
            }
        });

        controller.run();

        Assert.assertEquals(456.0, waitingTimes.get(0), 1e-3);
        Assert.assertEquals(144.5, waitingTimes.get(1), 1e-3);
        Assert.assertEquals(51.05, waitingTimes.get(2), 1e-3);
    }

    @Test
    public void testDynamicWaitingTimeWithoutConstantAttribute() {
        AmodeusConfigGroup config = createConfig();
        AmodeusModeConfig operatorConfig = config.getModes().get(AmodeusModeConfig.DEFAULT_MODE);

        operatorConfig.getWaitingTimeEstimationConfig().setDefaultWaitingTime(123.0);
        operatorConfig.getWaitingTimeEstimationConfig().setEstimationLinkAttribute("avGroup");
        operatorConfig.getWaitingTimeEstimationConfig().setEstimationAlpha(0.7);

        Controler controller = createController(config);

        Link link = controller.getScenario().getNetwork().getLinks().get(Id.createLinkId("8:9_9:9"));
        link.getAttributes().putAttribute("avWaitingTime", 456.0);

        int index = 0;
        for (Link _link : controller.getScenario().getNetwork().getLinks().values()) {
            _link.getAttributes().putAttribute("avGroup", index++);
        }

        controller.getConfig().controler().setLastIteration(2);

        StrategySettings strategy = new StrategySettings();
        strategy.setStrategyName("ReRoute");
        strategy.setWeight(1.0);
        controller.getConfig().strategy().addStrategySettings(strategy);

        List<Double> waitingTimes = new LinkedList<>();

        controller.addControlerListener(new IterationEndsListener() {
            @Override
            public void notifyIterationEnds(IterationEndsEvent event) {
                Population population = event.getServices().getScenario().getPopulation();
                Person person = population.getPersons().get(Id.createPersonId(17));
                Plan plan = person.getSelectedPlan();

                for (PlanElement element : plan.getPlanElements()) {
                    if (element instanceof Leg) {
                        Leg leg = (Leg) element;
                        AmodeusRoute route = (AmodeusRoute) leg.getRoute();

                        if (Id.createLinkId("8:9_9:9").equals(route.getStartLinkId())) {
                            waitingTimes.add(route.getWaitingTime().seconds());
                        }
                    }
                }
            }
        });

        controller.run();

        Assert.assertEquals(123.0, waitingTimes.get(0), 1e-3);
        Assert.assertEquals(44.6, waitingTimes.get(1), 1e-3);
        Assert.assertEquals(21.08, waitingTimes.get(2), 1e-3);
    }

    @AfterClass
    public static void doYourOneTimeTeardown() throws IOException {
        FileUtils.deleteDirectory(new File(TestScenarioGenerator.outputDir));
    }
}
