/* amodeus - Copyright (c) 2019, ETH Zurich, Institute for Dynamic Systems and Control */
package amodeus.amodeus.util.math;

import java.io.Serializable;
import java.util.Objects;

public class IntPoint implements Serializable {
    public final int x;
    public final int y;

    public IntPoint(int x, int y) {
        this.x = x;
        this.y = y;
    }

    @Override
    public int hashCode() {
        return Objects.hash(x, y);
    }

    @Override
    public boolean equals(Object object) {
        if (object instanceof IntPoint) {
            IntPoint intPoint = (IntPoint) object;
            return x == intPoint.x && y == intPoint.y;
        }
        return false;
    }

    @Override
    public String toString() {
        return getClass().getSimpleName() + "(" + x + ", " + y + ")";
    }

}
