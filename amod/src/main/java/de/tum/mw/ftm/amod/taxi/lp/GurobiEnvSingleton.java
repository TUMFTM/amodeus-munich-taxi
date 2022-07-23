package de.tum.mw.ftm.amod.taxi.lp;

import gurobi.GRB;
import gurobi.GRBEnv;
import gurobi.GRBException;

/**
 * Singleton class for a Gurobi Environment.
 */
public class GurobiEnvSingleton {
    private static GurobiEnvSingleton instance;
    private static GRBEnv grbEnv;

    private GurobiEnvSingleton() throws GRBException {
        grbEnv = new GRBEnv(true);
        // Set higher tolerances so that solving the model is more probable
        grbEnv.set(GRB.DoubleParam.MIPGap, 1e-2);
        grbEnv.set(GRB.DoubleParam.IntFeasTol, 1e-1);
        grbEnv.set(GRB.DoubleParam.FeasibilityTol, 1e-3);
        grbEnv.set(GRB.IntParam.DualReductions, 0);
        grbEnv.set(GRB.DoubleParam.TimeLimit, 20*60);
        grbEnv.set(GRB.IntParam.Threads, 3);
        grbEnv.start();
    }

    public static GurobiEnvSingleton getInstance() throws GRBException {
        if (GurobiEnvSingleton.instance == null) {
            GurobiEnvSingleton.instance = new GurobiEnvSingleton();
        }
        return GurobiEnvSingleton.instance;
    }

    public void dispose() throws GRBException {
        grbEnv.dispose();
        grbEnv = null;
    }

    public GRBEnv getGrbEnv() {
        return grbEnv;
    }
}
