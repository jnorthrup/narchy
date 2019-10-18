package jcog.bloom.quantitative;

import jcog.bloom.BloomFilterBuilder;
import jcog.bloom.LeakySet;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
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

            long truePositives = 0L;
            for (String containedString : containedStrings) {
                if (filter.contains(containedString)) {
                    truePositives++;
                }
            }
            long trueNegatives = 0L;
            for (String string : nonContainedStrings) {
                if (!filter.contains(string)) {
                    trueNegatives++;
                }
            }
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
        List<String> strings = new ArrayList<>();
        for (int i = 0; i < count; i++) {
            String s = prefix + UUID.randomUUID();
            strings.add(s);
        }
        return strings;
    }

}
