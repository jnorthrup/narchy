package org.intelligentjava.machinelearning.decisiontree;

import com.google.common.collect.Lists;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.function.Function;
import java.util.function.Predicate;

import static java.util.stream.Collectors.toList;
import static org.intelligentjava.machinelearning.decisiontree.data.SimpleValue.data;
import static org.intelligentjava.machinelearning.decisiontree.feature.PredicateFeature.feature;
import static org.intelligentjava.machinelearning.decisiontree.label.BooleanLabel.FALSE_LABEL;
import static org.intelligentjava.machinelearning.decisiontree.label.BooleanLabel.TRUE_LABEL;
import static org.junit.jupiter.api.Assertions.assertEquals;

public class DecisionTreeFindBestSplitTest {
    
    @Test
    public void testBooleanSplit() {
        DecisionTree<String, Object> tree = new DecisionTree();
        String labelColumnName = "answer";
        
        String[] headers = {labelColumnName, "x1", "x2"};
        List<Function<String,Object>> dataSet = Lists.newArrayList();
        dataSet.add(data(headers, TRUE_LABEL, true, true));
        dataSet.add(data(headers, FALSE_LABEL, true, false));
        dataSet.add(data(headers, FALSE_LABEL, false, true));
        dataSet.add(data(headers, FALSE_LABEL, false, false));
        
        List<Predicate<Function<String,Object>>> features = List.of(
            feature("x1", true),
            feature("x2", true),
            feature("x1", false),
            feature("x2", false)
        );
        
        
        Predicate<Function<String,Object>> bestSplit = tree.bestSplit(labelColumnName, ()->dataSet.stream(), features.stream());
        assertEquals("x1 = true", bestSplit.toString());
        
        List<List<Function<String,Object>>> split = DecisionTree.split(bestSplit, dataSet).collect(toList());
        
        
        assertEquals(TRUE_LABEL, split.get(0).get(0).apply(labelColumnName));
        assertEquals(FALSE_LABEL, split.get(0).get(1).apply(labelColumnName));
        assertEquals(FALSE_LABEL, split.get(1).get(0).apply(labelColumnName));
        assertEquals(FALSE_LABEL, split.get(1).get(1).apply(labelColumnName));

        
        Predicate<Function<String,Object>> newBestSplit = tree.bestSplit(labelColumnName,()->split.get(0).stream(), features.stream());
        assertEquals("x2 = true", newBestSplit.toString());

        List<List<Function<String,Object>>> newSplit = DecisionTree.split(newBestSplit, split.get(0)).collect(toList());
        assertEquals(TRUE_LABEL, newSplit.get(0).get(0).apply(labelColumnName));
        assertEquals(FALSE_LABEL, newSplit.get(1).get(0).apply(labelColumnName));
    }

}
