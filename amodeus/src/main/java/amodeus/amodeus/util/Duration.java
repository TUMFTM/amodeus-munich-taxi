/* amodeus - Copyright (c) 2019, ETH Zurich, Institute for Dynamic Systems and Control */
package amodeus.amodeus.util;

import java.time.LocalDateTime;
import java.time.ZoneOffset;

import amodeus.amodeus.util.math.SI;
import ch.ethz.idsc.tensor.Scalar;
import ch.ethz.idsc.tensor.qty.Quantity;

public enum Duration {
    ;

    /** @return {@link Scalar} duration of interval [@param ldt1, @param ldt2] in [s]
     * @throws Exception */
    public static Scalar between(LocalDateTime ldt1, LocalDateTime ldt2) throws Exception {
        if (ldt1.isAfter(ldt2))
            throw new Exception(ldt1.toString() + " is after " + ldt2.toString() //
                    + ": cannot compute duration.");
        long sec1 = ldt1.toEpochSecond(ZoneOffset.UTC);
        long sec2 = ldt2.toEpochSecond(ZoneOffset.UTC);
        return Quantity.of(sec2 - sec1, SI.SECOND);
    }

    /** @return {@link Scalar} duration of interval [@param ldt1, @param ldt2] in [s]
     * @throws RuntimeException */
    public static Scalar abs(LocalDateTime ldt1, LocalDateTime ldt2) throws RuntimeException {
        if (ldt1.isAfter(ldt2)) {
            try {
                return between(ldt2, ldt1);
            } catch (Exception ex) {
                // ---
            }

        } else {
            try {
                return between(ldt1, ldt2);
            } catch (Exception ex) {
                // ---
            }
        }

        // should never get here...
        throw new RuntimeException();
    }
}
