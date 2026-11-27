package cemestre.sorting;

import cemestre.model.Combination;

import java.util.Comparator;

public interface SortCriterion {

    String getName();

    Comparator<Combination> comparator();
}
