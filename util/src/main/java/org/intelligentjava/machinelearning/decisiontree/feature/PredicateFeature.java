package org.intelligentjava.machinelearning.decisiontree.feature;

import java.util.function.Function;
import java.util.function.Predicate;

/**
 * Feature type that splits data into 2 sublists: true, false (in that order)
 *
 *
 * @param <L> Feature data type (string, number)
 * @author Ignas
 */
public class PredicateFeature<L> implements Predicate<Function<String,L>> {

    /**
     * Data column used by feature.
     */
    private final String column; // TODO multiple columns per feature

    /**
     * Predicate used for splitting.
     */
    private final Predicate<L> predicate;

    /**
     * Feature Label used for visualization and testing the tree.
     */
    private final String label;

    /**
     * Constructor.
     *
     * @param column       Column in data table.
     * @param predicate    Predicate used for splitting. For example if value is equal to some value, or is more/less.
     * @param featureValue Feature value used by predicate when comparing with data.
     */
    private PredicateFeature(String column, Predicate<L> predicate, String label) {
        super();
        this.column = column;
        this.predicate = predicate;
        this.label = label;
    }

    /**
     * Default static factory method which creates a feature. Default feature splits data whose column value is equal provided feature value.
     * For example PredicateFeature.newFeature("name", "john") will split data into 2 sublists - one where all entries has name = john and another one with different names.
     *
     * @param column       Column to use in data.
     * @param featureValue Feature value.
     * @return New feature.
     */
    public static <T> Predicate<Function<String,T>> feature(String column, T featureValue) {
        return new PredicateFeature<>(column, P.isEqual(featureValue), String.format("%s = %s", column, featureValue));
    }

    /**
     * Static factory method to create a new feature. This one accepts any predicate.
     *
     * @param column    Column to use in data.
     * @param predicate Predicate to use for splitting.
     * @return New feature.
     */
    public static <T> Predicate<Function<String,T>> feature(String column, Predicate<T> predicate, String predicateString) {
        return new PredicateFeature<>(column, predicate, String.format("%s %s", column, predicateString));
    }

    /**
     * {@inheritDoc}
     */
    @SuppressWarnings("unchecked")
    @Override
    public boolean test(Function<String,L> x) { // TODO implement other splits (in different type of feature)
        L y = x.apply(column);
        return y!=null && predicate.test(y);
    }

    @Override
    public String toString() {
        return label;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((column == null) ? 0 : column.hashCode());
        result = prime * result + ((label == null) ? 0 : label.hashCode());
        result = prime * result + ((predicate == null) ? 0 : predicate.hashCode());
        return result;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (obj == null)
            return false;
        if (getClass() != obj.getClass())
            return false;
        @SuppressWarnings("rawtypes")
        PredicateFeature other = (PredicateFeature) obj;
        if (column == null) {
            if (other.column != null)
                return false;
        } else if (!column.equals(other.column))
            return false;
        if (label == null) {
            if (other.label != null)
                return false;
        } else if (!label.equals(other.label))
            return false;
        if (predicate == null) {
            if (other.predicate != null)
                return false;
        } else if (!predicate.equals(other.predicate))
            return false;
        return true;
    }

}
