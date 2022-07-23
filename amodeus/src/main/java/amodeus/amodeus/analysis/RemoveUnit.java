/* amodeus - Copyright (c) 2018, ETH Zurich, Institute for Dynamic Systems and Control */
package amodeus.amodeus.analysis;

import ch.ethz.idsc.tensor.Scalar;
import ch.ethz.idsc.tensor.qty.Quantity;
import ch.ethz.idsc.tensor.sca.ScalarUnaryOperator;

public enum RemoveUnit implements ScalarUnaryOperator {
    FUNCTION;

    @Override
    public Scalar apply(Scalar scalar) {
        return scalar instanceof Quantity //
                ? ((Quantity) scalar).value()
                : scalar;
    }

}
