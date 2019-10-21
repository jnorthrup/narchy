package jcog.bloom.quantitative;

import jcog.bloom.BloomFilterBuilder;
import jcog.bloom.LeakySet;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.UUID;
import java.util.function.IntFunction;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

/**
 * Created by jeff on 16/05/16.
 */
class FalsePositiveRateTest {

    @Test
    void whenContinuouslyAddingElements_falsePositivesIncrease() {
        LeakySet<String> filter = BloomFilterBuilder.get()
                .withSize(10000)
                .buildFilter();
        final int batchSize = 1000;
        final int numberOfBatches = 10;

        for (int currentBatchNumber = 0; currentBatchNumber < numberOfBatches; currentBatchNumber++) {
            List<String> containedStrings = randomStrings("a", batchSize);
            List<String> nonContainedStrings = randomStrings("b", batchSize);

            for (String s : containedStrings) {
                filter.add(s);
            }

            long truePositives = containedStrings.stream().filter(filter::contains).count();
            long trueNegatives = nonContainedStrings.stream().filter(new Predicate<String>() {
                @Override
                public boolean test(String string) {
                    return !filter.contains(string);
                }
            }).count();
            double falsePositiveRate = 100.0 * (batchSize - trueNegatives) / batchSize;
            double falseNegativeRate = 100.0 * (batchSize - truePositives) / batchSize;
            double accuracy = 100.0 * (truePositives + trueNegatives) / (2 * batchSize);

            System.err.printf(
                    "N:%6d : FPR:%.2f%% FNR:%.2f%% ACC:%.2f%%\n",
                    (currentBatchNumber * batchSize),
                    falsePositiveRate,
                    falseNegativeRate,
                    accuracy);
        }
    }

    private static List<String> randomStrings(String prefix, int count) {
        List<String> strings = IntStream.range(0, count).mapToObj(new IntFunction<String>() {
            @Override
            public String apply(int i) {
                return prefix + UUID.randomUUID();
            }
        }).collect(Collectors.toList());
        return strings;
    }

}
