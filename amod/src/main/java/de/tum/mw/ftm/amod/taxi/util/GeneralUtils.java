package de.tum.mw.ftm.amod.taxi.util;


import java.util.ArrayList;
import java.util.List;

public class GeneralUtils {

    public static List<List<Integer>> uniquePermutations(int firstLowerBound, int firstUpperBound,
                                                         int secondLowerBound, int secondUpperBound) {
        int estimatedSize = (int) ((firstUpperBound - firstLowerBound) * (secondUpperBound - secondLowerBound) * 0.4);
        List<List<Integer>> permutations = new ArrayList<>(estimatedSize);
        List<Integer> jiPair = new ArrayList<>();
        for (int i = firstLowerBound; i <= firstUpperBound; i++) {
            for (int j = secondLowerBound; j <= secondUpperBound; j++) {
                if (i != j) {
                    jiPair.add(j);
                    jiPair.add(i);
                    if (!permutations.contains(jiPair)) {
                        List<Integer> ijPair = new ArrayList<>();
                        ijPair.add(i);
                        ijPair.add(j);
                        permutations.add(ijPair);
                    }
                    jiPair.clear();
                }
            }
        }

        return permutations;
    }
}
