package de.tum.mw.ftm.amod.taxi.preprocessing.grid;

import amodeus.amodeus.util.matsim.NetworkLoader;
import de.tum.mw.ftm.amod.taxi.preprocessing.demandprediction.UniformGridWithBorderPredictions;
import de.tum.mw.ftm.amod.taxi.util.AmodeusUtil;
import org.apache.log4j.Logger;
import org.matsim.amodeus.config.FTMConfigGroup;
import org.matsim.api.core.v01.network.Network;
import org.matsim.core.config.Config;
import org.matsim.core.config.groups.NetworkConfigGroup;

import java.io.File;
import java.io.FileNotFoundException;

public class TargetProbabilityGenerator {
    private final static Logger logger = Logger.getLogger(TargetProbabilityGenerator.class);

    public static void main(String[] args) throws FileNotFoundException {
        Config config = AmodeusUtil.loadMatSimConfig();
        FTMConfigGroup ftmConfigGroup = (FTMConfigGroup) config.getModules().get("ftm_simulation");
        NetworkConfigGroup networkConfigGroup = config.network();
        final Network network = NetworkLoader.fromNetworkFile(new File(networkConfigGroup.getInputFile()));

        TargetProbabilityGenerator.createProbabilitiesFile(ftmConfigGroup);
    }

    public static void createProbabilitiesFile(FTMConfigGroup ftmConfigGroup) throws FileNotFoundException {

        UniformGridWithBorderPredictions predictions =  UniformGridWithBorderPredictions.fromXML(ftmConfigGroup.getRebalancingDemandFile());
        TargetProbabilityCalculator targetProbabilityCalculator = new TargetProbabilityCalculator(predictions.getGridCells(), ftmConfigGroup);
        double[][] targetProbabilities = targetProbabilityCalculator.calculateTargetProbability();

        TargetProbabilityXMLWriter targetProbabilityXMLWriter = new TargetProbabilityXMLWriter(targetProbabilities);
        targetProbabilityXMLWriter.write(ftmConfigGroup.getTargetProbabilitiesFile());
        logger.info("Finished calculating target probabilities!");
    }
}
