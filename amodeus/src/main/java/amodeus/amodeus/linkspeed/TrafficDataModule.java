/* amodeus - Copyright (c) 2018, ETH Zurich, Institute for Dynamic Systems and Control */
package amodeus.amodeus.linkspeed;

import java.io.File;
import java.util.Objects;

import amodeus.amodeus.util.io.MultiFileTools;
import com.google.inject.Key;
import com.google.inject.name.Names;
import org.matsim.api.core.v01.Scenario;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.config.Config;
import org.matsim.core.mobsim.qsim.AbstractQSimModule;
import org.matsim.core.mobsim.qsim.qnetsimengine.ConfigurableQNetworkFactory;
import org.matsim.core.mobsim.qsim.qnetsimengine.QNetworkFactory;
import org.matsim.core.mobsim.qsim.qnetsimengine.linkspeedcalculator.LinkSpeedCalculator;

import com.google.inject.Provides;
import com.google.inject.Singleton;
import org.matsim.core.router.util.TravelTime;

public class TrafficDataModule extends AbstractQSimModule {
    private final LinkSpeedDataContainer lsData;

    public TrafficDataModule(LinkSpeedDataContainer lsData) {
        this.lsData = Objects.isNull(lsData) ? new LinkSpeedDataContainer() : lsData;
    }

    @Provides
    @Singleton
    public QNetworkFactory provideCustomConfigurableQNetworkFactory(EventsManager events, Scenario scenario, //
            TaxiTrafficData trafficData) {
        ConfigurableQNetworkFactory factory = new ConfigurableQNetworkFactory(events, scenario);
        LinkSpeedCalculator linkSpeedCalculator = new AmodeusLinkSpeedCalculator(trafficData);
        factory.setLinkSpeedCalculator(linkSpeedCalculator);
        return factory;
    }

    @Provides
    @Singleton
    public DefaultTaxiTrafficData provideTaxiTrafficData() {
        return new DefaultTaxiTrafficData(lsData);
    }


    @Override
    protected void configureQSim() {
        bind(TaxiTrafficData.class).to(DefaultTaxiTrafficData.class).asEagerSingleton();
    }
}