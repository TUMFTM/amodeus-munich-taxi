/* amod - Copyright (c) 2018, ETH Zurich, Institute for Dynamic Systems and Control */
package de.tum.mw.ftm.amod.taxi;


import com.google.common.base.Stopwatch;
import org.apache.log4j.Logger;

import java.net.MalformedURLException;
import java.util.concurrent.TimeUnit;

/**
 * Helper class to run a default preparer and server, the typical
 * sequence of execution.
 */
public class ScenarioExecutionSequence {
    private final static Logger logger = Logger.getLogger(ScenarioExecutionSequence.class);

    public static void main(String[] args) throws MalformedURLException, Exception {
        Stopwatch stopwatch = Stopwatch.createStarted();
        ScenarioPreparer.main(args);
        ScenarioServer.main(args);
        stopwatch.stop();
        logger.info("Time elapsed: " + stopwatch.elapsed(TimeUnit.MINUTES) + " minutes");
    }

}