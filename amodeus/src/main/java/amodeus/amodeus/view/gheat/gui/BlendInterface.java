/* amodeus - Copyright (c) 2018, ETH Zurich, Institute for Dynamic Systems and Control */
package amodeus.amodeus.view.gheat.gui;

@FunctionalInterface
interface BlendInterface {
    void blend(int[] src, int[] dst, int[] result);
}
