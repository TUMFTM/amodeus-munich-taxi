package de.tum.mw.ftm.amod.taxi.util;

import org.apache.commons.math3.exception.DimensionMismatchException;

import static org.apache.commons.math3.util.MathArrays.checkEqualLength;

public class MathUtils {

    public static int[] ebeAdd(int[] a, int[] b)
            throws DimensionMismatchException {
        checkEqualLength(a, b);

        final int[] result = a.clone();
        for (int i = 0; i < a.length; i++) {
            result[i] += b[i];
        }
        return result;
    }

    public static int[] ebeSub(int[] a, int[] b)
            throws DimensionMismatchException {
        checkEqualLength(a, b);

        final int[] result = a.clone();
        for (int i = 0; i < a.length; i++) {
            result[i] -= b[i];
        }
        return result;
    }


    public static double[] ebeDivide(int[] a, double[] b)
            throws DimensionMismatchException {
        if (a.length != b.length) {
                throw new DimensionMismatchException(a.length, b.length);
            }
        double[] result = new double[a.length];
        for(int i =0 ; i<a.length; i++){
            result[i] = ((double) a[i])/b[i];
        }
        return result;
    }
}
