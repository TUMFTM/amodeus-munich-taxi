/* amodeus - Copyright (c) 2018, ETH Zurich, Institute for Dynamic Systems and Control */
package amodeus.amodeus.dispatcher.shared.fifs;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;

import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.contrib.dvrp.passenger.PassengerRequest;

import amodeus.amodeus.dispatcher.core.RoboTaxi;
import amodeus.amodeus.dispatcher.util.TreeMultipleItems;
import amodeus.amodeus.routing.CachedNetworkTimeDistance;

/* package */ class BlockRebalancing {

    /** General Settings */
    private final int minNumberForRebalance;
    private final CachedNetworkTimeDistance timeDb;

    /** in this Set All the operations are made. */
    private final Map<Integer, Block> blocks;
    /** this tree is only used as an lookup to quickly find the corresponding block */
    private final Map<Link, Block> linkBlockLookup = new HashMap<>();

    /** The {@link BlockRebalancing} enables calculations of a Grid based Rebalancing strategy. It generates a grid of Blocks over the network and then
     * calculates
     * at each call of {@link #getRebalancingDirectives} for each of this cells a block balance which is a measure for the need or surplus of robotaxis. Based
     * on
     * that measure Rebalancing directives are returned. That is a List of directives for Robotaxis to drive to certain links.
     * 
     * Implementation based on: Fagnant, D. J., Kockelman, K. M., & Bansal, P. (2015). Operations of shared autonomous vehicle fleet for austin, texas, market.
     * Transportation Research
     * Record: Journal of the Transportation Research Board, (2536), 98-106.
     * 
     * @param network over which the Grid should be laid
     * @param timeDb travel Time Calculator to efficiently calculate the travel time between links
     * @param minNumberRobotaxisForRebalance The minimal Threshold from which on robotaxis are rebalanced
     * @param historicalDataTime duration in seconds over which past requests are collected to predict future requests
     * @param predictedTime duration in seconds for which the future request should be predicted
     * @param gridDistance distance in meter which corresponds to the length of a block */
    public BlockRebalancing(Network network, CachedNetworkTimeDistance timeDb, int minNumberRobotaxisForRebalance, double historicalDataTime, double predictedTime,
            double gridDistance) {
        this.minNumberForRebalance = minNumberRobotaxisForRebalance;
        this.timeDb = timeDb;

        /** generate the Blocks based on the network and the grid distance. */
        blocks = BlocksGenerator.of(network, historicalDataTime, predictedTime, gridDistance);

        /** Fill the Lookup Map for the Link to Block */
        network.getLinks().values().forEach(l -> linkBlockLookup.put(l, getCorespondingBlock(l.getCoord())));
    }

    /** @param coord
     * @return the corresponding block based oon the given coordinate */
    private Block getCorespondingBlock(Coord coord) {
        return blocks.values().stream().filter(block -> block.contains(coord)).findFirst() // replace by findAny if order does not matter or is unique
                .orElseThrow(RuntimeException::new); // every link has to be part of a block otherwise the generation was not concise
    }

    /** Calculates rebalancing directives based on the current state of the robotaxis and requests
     * 
     * @param now the current time
     * @param historicalRequestLinks historical request link data. should correspond to the historicalDataTime entered in the constructor. Can come from a
     *            collection of the data in the simulation or from historical data like a taxi company could have it.
     * @param allUnassignedPassengerRequests all currently unassigned {@link PassengerRequest}s
     * @param allAvailableRobotaxisforRebalance all {@link RoboTaxi}s which should be considered for Rebalancing
     * @return rebalancing directives */
    public RebalancingDirectives getRebalancingDirectives( //
            double now, Set<Link> historicalRequestLinks, Set<PassengerRequest> allUnassignedPassengerRequests, //
            Set<RoboTaxi> allAvailableRobotaxisforRebalance) {

        /** First we have to update all the blocks with the new values of requests and RoboTaxis */
        blocks.values().forEach(Block::clear);

        allAvailableRobotaxisforRebalance.forEach(rt -> blocks.get(linkBlockLookup.get(rt.getDivertableLocation()).getId()).addRoboTaxi(rt));
        allUnassignedPassengerRequests.forEach(req -> blocks.get(linkBlockLookup.get(req.getFromLink()).getId()).addUnassignedRequest());
        historicalRequestLinks.forEach(l -> blocks.get(linkBlockLookup.get(l).getId()).addRequestLastHour(l));

        /** Calculate the initial Block Balances for each block */
        blocks.values().forEach(v -> v.calculateInitialBlockBalance(allAvailableRobotaxisforRebalance.size(), allUnassignedPassengerRequests.size()));

        /** By using push and pull between the Blocks Lets determine which block sends how many robotaxis to which other block */
        calculateRebalancing();

        /** Calculate for each block which vehicles will move to which link based on the results of the
         * calculated rebalancing numbers above */
        RebalancingDirectives directives = new RebalancingDirectives(new HashMap<>());
        blocks.values().forEach(b -> directives.addOtherDirectives(b.executeRebalance(timeDb, now)));
        return directives;
    }

    /** Plans pushing and pulling of Robotaxis between the blocks. */
    private void calculateRebalancing() {

        /** Store the Blocks in the Order of their Block Balance */
        TreeMultipleItems<Block> blockBalances = new TreeMultipleItems<>(this::getAbsOfBlockBalance);
        blocks.values().forEach(blockBalances::add);
        Set<Block> calculatedBlocks = new HashSet<>();

        /** Get the block with the largest absolute value of the block Balance */
        Block block = blockBalances.getLast().iterator().next();

        while (getAbsOfBlockBalance(block) > minNumberForRebalance) {
            /** remove the block and its adjacent blocks from the tree, will be added with the updated balance afterwards */
            block.getAdjacentBlocks().forEach(blockBalances::remove);
            blockBalances.remove(block);

            /** If the Block has enough free Robotaxis it pushes to other blocks (block balance > minNumberForRebalancing) */
            if (block.getBlockBalance() > minNumberForRebalance) {
                while (block.getBlockBalance() > minNumberForRebalance && BlockUtils.lowerBalancesPresentInNeighbourhood(block) && block.hasAvailableRobotaxisToRebalance())
                    block.pushRobotaxiTo(BlockUtils.getBlockwithLowestBalance(block.getAdjacentBlocks()));
                /** If the Block has not enough free Robotaxis it pulls from other blocks (block balance < -minNumberForRebalancing) */
            } else if (block.getBlockBalance() < 0 - minNumberForRebalance) {
                Optional<Block> blockWithHighestBalance = BlockUtils.getBlockwithHighestBalanceAndAvailableRobotaxi(block.getAdjacentBlocks());
                while (block.getBlockBalance() < 0 - minNumberForRebalance && blockWithHighestBalance.isPresent()
                        && blockWithHighestBalance.get().hasAvailableRobotaxisToRebalance() && BlockUtils.balance1HigherThanBalance2(blockWithHighestBalance.get(), block)) {
                    blockWithHighestBalance.get().pushRobotaxiTo(block);
                    blockWithHighestBalance = BlockUtils.getBlockwithHighestBalanceAndAvailableRobotaxi(block.getAdjacentBlocks());
                }
            } else
                throw new RuntimeException();

            /** add the adjacent blocks back to the block balance tree with the updated balance. Btw The current block is not added Anymore as all possible
             * rebalancings have
             * been carried out. It could well be that this block still has the highest balance but we have to move on to the next block. */
            calculatedBlocks.add(block);
            block.getAdjacentBlocks().stream().filter(b -> !calculatedBlocks.contains(b)).forEach(blockBalances::add);
            /** update the current block */
            Set<Block> set = blockBalances.getLast();
            if (Objects.isNull(set))
                break;
            block = set.iterator().next();
        }
    }

    /** @param block
     * @return {@link Coord} with {@link RoboTaxi} location */
    private double getAbsOfBlockBalance(Block block) {
        return Math.abs(block.getBlockBalance());
    }
}
