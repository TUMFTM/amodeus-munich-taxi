package de.tum.mw.ftm.amod.taxi.preprocessing.demand;

import amodeus.amod.ext.UserReferenceFrames;
import de.tum.mw.ftm.amod.taxi.util.AmodeusUtil;
import org.apache.log4j.Logger;
import org.matsim.amodeus.config.FTMConfigGroup;
import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.population.*;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.config.groups.NetworkConfigGroup;
import org.matsim.core.network.NetworkUtils;
import org.matsim.core.population.PopulationUtils;
import org.matsim.core.utils.geometry.CoordinateTransformation;

import java.io.File;
import java.io.FileNotFoundException;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

public class
TaxiDemandGenerator {

    private final static Logger logger = Logger.getLogger(TaxiDemandGenerator.class);

    private final static CoordinateTransformation WGS84_UTM32N = UserReferenceFrames.MUNICH.coords_fromWGS84();

    public static void main(String[] args) throws FileNotFoundException {
        logger.info(" --------- GENERATING TAXI DEMAND ----------");
        Config config = AmodeusUtil.loadMatSimConfig();
        FTMConfigGroup ftmConfigGroup = (FTMConfigGroup) config.getModules().get("ftm_simulation");
        NetworkConfigGroup networkConfigGroup = config.network();
        Network network = NetworkUtils.readNetwork(networkConfigGroup.getInputFile());
        TaxiDemandGenerator.createRawPopulationFileFromCSV(config, ftmConfigGroup, network, new File(args[0]));
        logger.info(" --------- FINISHED GENERATING TAXI DEMAND ----------\n\n");
    }

    public static void createRawPopulationFileFromCSV(Config config,
                                                      FTMConfigGroup ftmConfigGroup,
                                                      Network network,
                                                      File taxiRidesCSV) throws FileNotFoundException {

        logger.info("Loading trips from CSV-File...");
        List<CSVTaxiRide> rides = CSVTaxiRide.parseTaxiTripsFromCSV(taxiRidesCSV);

        // create a population
        Population population = PopulationUtils.createPopulation(ConfigUtils.createConfig());
        PopulationFactory populationFactory = population.getFactory();

        // fill the population
        logger.info("Creating population...");
        int ignoredPersons = 0;
        for (CSVTaxiRide ride : rides) {
            Person person = createPerson(ride, ftmConfigGroup.getSimStartDateTime(), network, populationFactory);
            if (person == null) {
                ignoredPersons++;
            } else {
                population.addPerson(person);
            }
        }
        // write to file
        new PopulationWriter(population, network).write(config.plans().getInputFile());
        logger.info("Finished creating population for " + rides.size() + " persons. Ignored " + ignoredPersons + " person(s).");
    }

    private static Person createPerson(CSVTaxiRide ride, LocalDateTime simStartTime, Network network,
                                       PopulationFactory populationFactory) {
        // fill the person's plan
        Person person = populationFactory.createPerson(Id.createPersonId(ride.getId()));
        Plan plan = populationFactory.createPlan();
        Coord pickupLocation = WGS84_UTM32N.transform(new Coord(ride.getStartLng(), ride.getStartLat()));
        Link pickupLink = NetworkUtils.getNearestLink(network, pickupLocation);

        Coord dropoffLocation = WGS84_UTM32N.transform(new Coord(ride.getStopLng(), ride.getStopLat()));
        Link dropoffLink = NetworkUtils.getNearestLink(network, dropoffLocation);


        if (pickupLink == null || dropoffLink == null) {
            return null;
        }

        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
        LocalDateTime tripStartTime = LocalDateTime.parse(ride.getTimestampStart(), formatter);
        LocalDateTime tripStopTime = LocalDateTime.parse(ride.getTimestampStop(), formatter);

        // pickup activity
        Activity pickup = populationFactory.createActivityFromLinkId("activity", pickupLink.getId());
        pickup.setEndTime(Duration.between(simStartTime, tripStartTime).getSeconds());
        plan.addActivity(pickup);

        // traveling
        Leg taxiRide = populationFactory.createLeg("av");
        taxiRide.setDepartureTime(Duration.between(simStartTime, tripStartTime).getSeconds());
        plan.addLeg(taxiRide);

        // dropoff activity
        Activity dropoff = populationFactory.createActivityFromLinkId("activity", dropoffLink.getId());
        dropoff.setStartTime(Duration.between(simStartTime, tripStopTime).getSeconds());
        plan.addActivity(dropoff);

        person.addPlan(plan);

        return person;
    }

}
