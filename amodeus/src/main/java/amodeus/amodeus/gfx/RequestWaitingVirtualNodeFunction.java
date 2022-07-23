/* amodeus - Copyright (c) 2018, ETH Zurich, Institute for Dynamic Systems and Control */
package amodeus.amodeus.gfx;

import java.util.function.Function;

import org.matsim.api.core.v01.network.Link;

import amodeus.amodeus.net.MatsimAmodeusDatabase;
import amodeus.amodeus.net.RequestContainer;
import amodeus.amodeus.net.SimulationObject;
import amodeus.amodeus.virtualnetwork.core.VirtualNetwork;
import amodeus.amodeus.virtualnetwork.core.VirtualNode;
import ch.ethz.idsc.tensor.DoubleScalar;
import ch.ethz.idsc.tensor.Scalar;
import ch.ethz.idsc.tensor.Tensor;
import ch.ethz.idsc.tensor.Tensors;

/** mean request waiting time */
/* package */ class RequestWaitingVirtualNodeFunction extends AbstractVirtualNodeFunction {

    private final Function<Tensor, Scalar> function;

    public RequestWaitingVirtualNodeFunction( //
            MatsimAmodeusDatabase db, VirtualNetwork<Link> virtualNetwork, Function<Tensor, Scalar> function) {
        super(db, virtualNetwork);
        this.function = function;
    }

    @Override
    public Tensor evaluate(SimulationObject ref) {
        Tensor collect = Tensors.vector(i -> Tensors.empty(), virtualNetwork.getvNodesCount());
        for (RequestContainer rc : ref.requests) {
            double duration = ref.now - rc.submissionTime;
            Link linkAnte = db.getOsmLink(rc.fromLinkIndex).link;
            // Link linkPost = db.getOsmLink(rc.toLinkIndex).link;
            VirtualNode<Link> vn = virtualNetwork.getVirtualNode(linkAnte);
            collect.set(s -> s.append(DoubleScalar.of(duration)), vn.getIndex());
        }
        return Tensors.vector(i -> function.apply(collect.get(i)), virtualNetwork.getvNodesCount());
    }
}
