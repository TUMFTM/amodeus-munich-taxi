package de.tum.mw.ftm.amod.taxi.preprocessing.grid;

import amodeus.amodeus.util.matsim.NetworkLoader;
import de.tum.mw.ftm.amod.taxi.preprocessing.demandprediction.GridDemandPrediction;
import de.tum.mw.ftm.amod.taxi.preprocessing.demandprediction.UniformGridWithBorderPredictions;
import de.tum.mw.ftm.amod.taxi.util.AmodeusUtil;
import org.matsim.amodeus.config.FTMConfigGroup;
import org.matsim.api.core.v01.network.Network;
import org.matsim.core.config.Config;
import org.matsim.core.config.groups.NetworkConfigGroup;

import java.io.File;
import java.io.IOException;
import java.util.concurrent.ExecutionException;

public class GridInformationGenerator {

    public static void main(String[] args) throws IOException, ExecutionException, InterruptedException {
        Config config = AmodeusUtil.loadMatSimConfig();
        FTMConfigGroup ftmConfigGroup = (FTMConfigGroup) config.getModules().get("ftm_simulation");
        NetworkConfigGroup networkConfigGroup = config.network();
        final Network network = NetworkLoader.fromNetworkFile(new File(networkConfigGroup.getInputFile()));

        GridInformationGenerator.createGridInformationFile(ftmConfigGroup, network);
    }

    public static void createGridInformationFile(FTMConfigGroup ftmConfigGroup, Network network)
            throws InterruptedException, ExecutionException, IOException {

        GridDemandPrediction predictions =  UniformGridWithBorderPredictions.fromXML(ftmConfigGroup.getRebalancingDemandFile());
        GridInformationCalculator gridInformationCalculator = new GridInformationCalculator(predictions.getGridCells(),
                predictions.getNumberOfRows(),
                predictions.getNumberOfColumns(),
                network);

        gridInformationCalculator.calculateGridInformation();

        GridInformationXMLWriter gridInformationXMLWriter = new GridInformationXMLWriter(gridInformationCalculator);
        gridInformationXMLWriter.write(ftmConfigGroup.getGridInformationFile());
    }
}
