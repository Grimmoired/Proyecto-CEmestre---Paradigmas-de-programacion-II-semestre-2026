package cemestre.state;

import cemestre.model.Combination;
import cemestre.model.CombinationSet;
import cemestre.model.PolicyInfo;
import cemestre.observer.StateEvent;
import cemestre.observer.StateObserver;
import cemestre.sorting.SortCriterion;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

public final class AppState {
    private final List<StateObserver> observers = new ArrayList<>();

    private CombinationSet data;
    private SortCriterion criterion;
    private CombinationFilter filter = CombinationFilter.ALL;
    private List<Combination> visible = List.of();
    private Combination selected;

    public AppState(SortCriterion initialCriterion) {
        this.criterion = Objects.requireNonNull(initialCriterion, "initialCriterion");
    }

    public void addObserver(StateObserver observer) {
        observers.add(Objects.requireNonNull(observer, "observer"));
    }

    public void removeObserver(StateObserver observer) {
        observers.remove(observer);
    }

    private void notifyObservers(StateEvent event) {
        for (StateObserver observer : List.copyOf(observers)) {
            observer.onStateChanged(event);
        }
    }

    public void load(CombinationSet newData) {
        this.data = Objects.requireNonNull(newData, "newData");
        this.selected = null;
        refreshVisible();
        notifyObservers(StateEvent.DATA_LOADED);
    }

    public void setCriterion(SortCriterion newCriterion) {
        Objects.requireNonNull(newCriterion, "newCriterion");
        if (newCriterion.equals(criterion)) {
            return;
        }
        this.criterion = newCriterion;
        refreshVisible();
        notifyObservers(StateEvent.CRITERION_CHANGED);
    }

    public void setFilter(CombinationFilter newFilter) {
        Objects.requireNonNull(newFilter, "newFilter");
        if (newFilter == filter) {
            return;
        }
        this.filter = newFilter;
        refreshVisible();
        notifyObservers(StateEvent.FILTER_CHANGED);
        if (selected != null && !visible.contains(selected)) {
            selected = null;
            notifyObservers(StateEvent.SELECTION_CHANGED);
        }
    }

    public void select(Combination combination) {
        if (combination != null && (data == null || !data.getCombinations().contains(combination))) {
            throw new IllegalArgumentException("la combinacion " + combination.getId() + " no esta cargada");
        }
        if (Objects.equals(combination, selected)) {
            return;
        }
        this.selected = combination;
        notifyObservers(StateEvent.SELECTION_CHANGED);
    }

    private void refreshVisible() {
        if (data == null) {
            visible = List.of();
            return;
        }
        List<Combination> result = new ArrayList<>();
        for (Combination combination : data.getCombinations()) {
            if (filter.accepts(combination)) {
                result.add(combination);
            }
        }
        result.sort(criterion.comparator());
        visible = List.copyOf(result);
    }

    public boolean hasData() {
        return data != null;
    }

    public List<Combination> getVisibleCombinations() {
        return visible;
    }

    public Optional<Combination> getSelected() {
        return Optional.ofNullable(selected);
    }

    public SortCriterion getCriterion() {
        return criterion;
    }

    public CombinationFilter getFilter() {
        return filter;
    }

    public int getTotalCount() {
        return data == null ? 0 : data.size();
    }

    public int getValidCount() {
        return data == null ? 0 : data.getValidCount();
    }

    public Optional<PolicyInfo> getPolicy() {
        return data == null ? Optional.empty() : Optional.of(data.getPolicy());
    }

    public List<String> getWarnings() {
        return data == null ? List.of() : data.getWarnings();
    }
}
