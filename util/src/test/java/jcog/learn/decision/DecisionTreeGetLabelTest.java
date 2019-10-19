package jcog.learn.decision;

import com.google.common.collect.Lists;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.function.UnaryOperator;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import static jcog.learn.decision.label.BooleanLabel.FALSE_LABEL;
import static jcog.learn.decision.label.BooleanLabel.TRUE_LABEL;
import static org.junit.jupiter.api.Assertions.*;

class DecisionTreeGetLabelTest {

    private static final Object obj = Boolean.TRUE;

    @Test
    void testGetLabelOnEmptyList() {
        DecisionTree tree = new DecisionTree();
        List<UnaryOperator<Object>> data = Lists.newArrayList();
        assertNull(DecisionTree.label(obj, 0.9f, data.stream()));
    }

    @Test
    void testGetLabelOnSingleElement() {
        DecisionTree tree = new DecisionTree();
        List<UnaryOperator<Object>> data = Lists.newArrayList();
        data.add(new TestValue(TRUE_LABEL));
        assertEquals("true", DecisionTree.label(obj, 0.9f, data.stream()).toString());
    }

    @Test
    void testGetLabelOnTwoDifferent() {
        DecisionTree tree = new DecisionTree();
        List<UnaryOperator<Object>> data = Lists.newArrayList();
        data.add(new TestValue(TRUE_LABEL));
        data.add(new TestValue(FALSE_LABEL));
        assertNull(DecisionTree.label(obj, 0.9f, data.stream()));
    }

    @Test
    void testGetLabelOn95vs5() {
        DecisionTree tree = new DecisionTree();
        List<UnaryOperator > data = IntStream.range(0, 95).mapToObj(i1 -> new TestValue(TRUE_LABEL)).collect(Collectors.toList());
        IntStream.range(0, 5).mapToObj(i -> new TestValue(FALSE_LABEL)).forEachOrdered(data::add);
        assertEquals("true", DecisionTree.label(obj, 0.9f,(Stream ) data.stream()).toString());
    }

    @Test
    void testGetLabelOn94vs6() {
        DecisionTree tree = new DecisionTree();

        List<UnaryOperator<Object>> homogenous = buildSample(96, 4);
        assertNotNull(DecisionTree.label(obj, 0.9f, homogenous.stream()));


        List<UnaryOperator<Object>> nonhomogenous = buildSample(50, 50);
        assertNull(DecisionTree.label(obj, 0.9f, nonhomogenous.stream()));
    }

    private static List<UnaryOperator<Object>> buildSample(int a, int b) {
        List<UnaryOperator<Object>> homogenous = IntStream.range(0, a).mapToObj(i1 -> new TestValue(TRUE_LABEL)).collect(Collectors.toList());
        for (int i = 0; i < b; i++)
            homogenous.add(new TestValue(FALSE_LABEL));
        return homogenous;
    }

}
