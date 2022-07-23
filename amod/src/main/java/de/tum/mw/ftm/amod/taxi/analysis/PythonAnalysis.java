package de.tum.mw.ftm.amod.taxi.analysis;


import amodeus.amodeus.net.MatsimAmodeusDatabase;
import amodeus.amodeus.net.StorageSupplier;
import amodeus.amodeus.net.StorageUtils;
import de.tum.mw.ftm.amod.taxi.analysis.deprecated.Analysis;
import de.tum.mw.ftm.amod.taxi.analysis.deprecated.PythonExport;
import de.tum.mw.ftm.amod.taxi.analysis.deprecated.SimulationObjectRecorder;
import de.tum.mw.ftm.amod.taxi.util.AmodeusUtil;
import org.matsim.amodeus.config.FTMConfigGroup;
import org.matsim.core.config.Config;

import java.io.File;
import java.time.ZonedDateTime;
import java.util.Set;
import java.util.stream.Collectors;

public class PythonAnalysis {
    public static void addTo(Analysis analysis, File outputDirectory, MatsimAmodeusDatabase amodeusDatabase,
                             ZonedDateTime simRunStarted, ZonedDateTime simRunFinished) throws Exception {
        /** first an element to gather the necessary data is defined, it is an implementation of the
         * interface AnalysisElement */
        StorageUtils storageUtils = new StorageUtils(outputDirectory);
        StorageSupplier storageSupplier = new StorageSupplier(storageUtils.getFirstAvailableIteration());
        Set<Integer> vehicleIndices = storageSupplier.getSimulationObject(1).vehicles.stream().map(vc -> vc.vehicleIndex).collect(Collectors.toSet());

        Config config = AmodeusUtil.loadMatSimConfig();

        SimulationObjectRecorder simulationObjectRecorder = new SimulationObjectRecorder(vehicleIndices, amodeusDatabase, config, storageSupplier.size());

        /** next an element to export the processed data to an image or other element is defined, it
         * is an implementation of the interface AnalysisExport */
        FTMConfigGroup ftmConfigGroup = (FTMConfigGroup) config.getModules().get("ftm_simulation");
        PythonExport pythonExport = new PythonExport(simulationObjectRecorder, ftmConfigGroup.getSimStartDateTime(),
                ftmConfigGroup.getSimEndDateTime(), simRunStarted, simRunFinished);

        /** all are added to the Analysis before running */
        analysis.addAnalysisElement(simulationObjectRecorder);
        analysis.addAnalysisExport(pythonExport);
    }
}
