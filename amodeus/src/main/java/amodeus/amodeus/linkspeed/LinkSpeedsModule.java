package amodeus.amodeus.linkspeed;


import amodeus.amodeus.util.io.MultiFileTools;
import com.google.inject.Provides;
import com.google.inject.Singleton;
import org.matsim.core.config.Config;
import org.matsim.core.controler.AbstractModule;
import org.matsim.core.router.util.TravelTime;


public class LinkSpeedsModule extends AbstractModule {
    private final LinkSpeedDataContainer lsData;

    public LinkSpeedsModule(LinkSpeedDataContainer lsData) {
        this.lsData = lsData;
    }

    public LinkSpeedsModule(Config config, LinkSpeedDataContainer lsData) {
        super(config);
        this.lsData = lsData;
    }

    @Override
    public void install() {

    }

    @Singleton
    @Provides
    public LinkSpeedDataContainer provideLinkSpeedDataContainer(Config config) {
        return lsData;
    }

    @Singleton
    @Provides
    @com.google.inject.name.Named("car")
    public TravelTime provideTravelTime(Config config) {
        return new LSDataTravelTime(lsData);
    }
}
