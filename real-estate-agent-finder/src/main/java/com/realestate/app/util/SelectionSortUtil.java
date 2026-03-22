package com.realestate.app.util;

import com.realestate.app.model.Agent;

import java.util.List;

public class SelectionSortUtil {

    /**
     * Sorts agents in descending order by average rating using Selection Sort.
     */
    public static void sortByRatingDesc(List<Agent> agents) {
        int n = agents.size();
        for (int i = 0; i < n - 1; i++) {
            int maxIdx = i;
            for (int j = i + 1; j < n; j++) {
                if (agents.get(j).getAverageRating() > agents.get(maxIdx).getAverageRating()) {
                    maxIdx = j;
                }
            }
            Agent temp = agents.get(maxIdx);
            agents.set(maxIdx, agents.get(i));
            agents.set(i, temp);
        }
    }
}
