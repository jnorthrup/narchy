package jcog.learn.decision;

import com.google.common.collect.Lists;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static jcog.learn.decision.label.BooleanLabel.FALSE_LABEL;
import static jcog.learn.decision.label.BooleanLabel.TRUE_LABEL;
import static org.junit.jupiter.api.Assertions.*;

class DecisionTreeGetLabelTest {

    private static final Object it = Boolean.TRUE;

    @Test
    void testGetLabelOnEmptyList() {
        DecisionTree tree = new DecisionTree();
        List<Function<Object,Object>> data = Lists.newArrayList();
        assertNull(DecisionTree.label(it, data.stream(), 0.9f));
    }

    @Test
    void testGetLabelOnSingleElement() {
        DecisionTree tree = new DecisionTree();
        List<Function<Object,Object>> data = Lists.newArrayList();
        data.add(new TestValue(TRUE_LABEL));
        assertEquals("true", DecisionTree.label(it, data.stream(),0.9f).toString());
    }

    @Test
    void testGetLabelOnTwoDifferent() {
        DecisionTree tree = new DecisionTree();
        List<Function<Object,Object>> data = Lists.newArrayList();
        data.add(new TestValue(TRUE_LABEL));
        data.add(new TestValue(FALSE_LABEL));
        assertNull(DecisionTree.label(it, data.stream(),0.9f));
    }

    @Test
    void testGetLabelOn95vs5() {
        DecisionTree tree = new DecisionTree();
        List<Function<Object, Object>> data = new ArrayList<>();
        for (int i1 = 0; i1 < 95; i1++) {
            TestValue testValue = new TestValue(TRUE_LABEL);
            data.add(testValue);
        }
        for (int i = 0; i < 5; i++) {
            data.add(new TestValue(FALSE_LABEL));
        }
        assertEquals("true", DecisionTree.label(it, data.stream(),0.9f).toString());
    }

    @Test
    void testGetLabelOn94vs6() {
        DecisionTree tree = new DecisionTree();

        List<Function<Object,Object>> homogenous = buildSample(96, 4);
        assertNotNull(DecisionTree.label(it, homogenous.stream(), 0.9f));


        List<Function<Object,Object>> nonhomogenous = buildSample(50, 50);
        assertNull(DecisionTree.label(it, nonhomogenous.stream(), 0.9f));
    }

    private static List<Function<Object,Object>> buildSample(int a, int b) {
        List<Function<Object, Object>> homogenous = new ArrayList<>();
        for (int i1 = 0; i1 < a; i1++) {
            TestValue testValue = new TestValue(TRUE_LABEL);
            homogenous.add(testValue);
        }
        for (int i = 0; i < b; i++)
            homogenous.add(new TestValue(FALSE_LABEL));
        return homogenous;
    }

}
