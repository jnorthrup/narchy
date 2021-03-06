package jcog.learn.decision.impurity;

import java.util.List;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.function.ToDoubleFunction;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Gini index impurity calculation. Formula 2p(1 - p) - this is the expected error if we label examples in the leaf
 * randomly: positive with probability p and negative with probability 1 - p. The probability of a false positive is
 * then p(1 - p) and the probability of a false negative (1 - p)p.
 *
 * @author Ignas
 */
public class GiniIndexImpurityCalculation implements ImpurityCalculator {

    /**
     * {@inheritDoc}
     */
    @Override
    public <K, V> double impurity(K value, Supplier<Stream<Function<K , V>>> splitData) {
        List<V> labels = splitData.get().map(new Function<Function<K, V>, V>() {
            @Override
            public V apply(Function<K, V> x) {
                return x.apply(value);
            }
        }).distinct().collect(Collectors.toList());
        int s = labels.size();
        if (s > 1) {
            return labels.stream().mapToDouble(new ToDoubleFunction<V>() {
                @Override
                public double applyAsDouble(V l) {
                    double p = ImpurityCalculator.empiricalProb(value, splitData.get(), l);
                    return 2.0 * p * (1.0 - p);
                }
            }).sum();
        } else if (s == 1) {
            return 0.0; 
        } else {
            throw new IllegalStateException("This should never happen. Probably a bug.");
        }
    }

}
