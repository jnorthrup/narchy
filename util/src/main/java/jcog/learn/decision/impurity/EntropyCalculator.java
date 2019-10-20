package jcog.learn.decision.impurity;


import com.google.common.math.DoubleMath;

import java.util.List;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Entropy calculator. -p log2 p - (1 - p)log2(1 - p) - this is the expected information, in bits, conveyed by somebody
 * telling you the class of a randomly drawn example; the purer the set of examples, the more predictable this message
 * becomes and the smaller the expected information.
 *
 * @author Ignas
 */
public class EntropyCalculator implements ImpurityCalculator {

    /**
     * {@inheritDoc}
     */
    @Override
    public <K, V> double impurity(K value, Supplier<Stream<Function<K, V>>> splitData) {
        var labels = splitData.get().map((x) -> x.apply(value)).distinct().collect(Collectors.toList());
        if (labels.size() > 1) {
            // TODO this can be done faster by comparing each all at once
            return labels.stream().mapToDouble(l -> {
                var p = ImpurityCalculator.empiricalProb(value, splitData.get(), l);
                return -1.0 * p * DoubleMath.log2(p) - ((1.0 - p) * DoubleMath.log2(1.0 - p));
            }).sum();
        } else if (labels.size() == 1) {
            return 0.0; 
        } else {
            throw new IllegalStateException("This should never happen. Probably a bug.");
        }
    }

}
