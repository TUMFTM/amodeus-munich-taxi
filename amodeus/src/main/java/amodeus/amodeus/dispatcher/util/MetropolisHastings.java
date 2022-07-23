/* amodeus - Copyright (c) 2018, ETH Zurich, Institute for Dynamic Systems and Control */
package amodeus.amodeus.dispatcher.util;

import org.matsim.api.core.v01.network.Link;

import amodeus.amodeus.virtualnetwork.Neighboring;
import amodeus.amodeus.virtualnetwork.core.VirtualNetwork;
import amodeus.amodeus.virtualnetwork.core.VirtualNode;
import ch.ethz.idsc.tensor.RealScalar;
import ch.ethz.idsc.tensor.Scalar;
import ch.ethz.idsc.tensor.Tensor;
import ch.ethz.idsc.tensor.alg.Array;

/** w_{ij} = 1/(1+ max(N_i, N_j)) where N_k is the number nodes for which a link
 * (k,l) exists in the network *
 *
 * @author clruch */
public class MetropolisHastings {
    private final Tensor weights;

    public MetropolisHastings(VirtualNetwork<Link> virtualNetwork, Neighboring neighboring) {
        weights = Array.zeros(virtualNetwork.getvNodesCount(), virtualNetwork.getvNodesCount());
        for (VirtualNode<Link> from : virtualNetwork.getVirtualNodes())
            for (VirtualNode<Link> to : virtualNetwork.getVirtualNodes()) {
                int fromInd = from.getIndex();
                int toInd = to.getIndex();
                int fromOut = getNumNeighbors(from, neighboring);
                int toOut = getNumNeighbors(to, neighboring);
                int max = fromOut >= toOut ? fromOut : toOut;
                weights.set(RealScalar.of(max), fromInd, toInd);
            }
    }

    private static int getNumNeighbors(VirtualNode<Link> vNode, Neighboring neighboring) {
        return neighboring.geNumNeighbors(vNode);
    }

    public Scalar get(VirtualNode<Link> from, VirtualNode<Link> to) {
        return weights.Get(from.getIndex(), to.getIndex());
    }

    public Tensor getAll() {
        return weights;
    }
}