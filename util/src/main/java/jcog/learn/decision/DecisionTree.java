package jcog.learn.decision;

import jcog.TODO;
import jcog.data.list.FasterList;
import jcog.learn.decision.feature.DiscretizedScalarFeature.CentroidMatch;
import jcog.learn.decision.impurity.GiniIndexImpurityCalculation;
import jcog.learn.decision.impurity.ImpurityCalculator;
import jcog.util.StreamReplay;
import org.eclipse.collections.api.block.function.primitive.IntToFloatFunction;
import org.eclipse.collections.api.tuple.primitive.ObjectBooleanPair;
import org.eclipse.collections.impl.tuple.primitive.PrimitiveTuples;
import org.jetbrains.annotations.Nullable;

import java.io.PrintStream;
import java.util.*;
import java.util.function.BiConsumer;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

import static java.util.stream.Collectors.*;
import static jcog.learn.decision.DecisionTree.DecisionNode.leaf;

/**
 * Decision tree implementation.
 *
 * @author Ignas
 */
public class DecisionTree<K, V> {

    /**
     * When data is considered homogeneous and node becomes leaf and is labeled. If it is equal 1.0 then absolutely all
     * data must be of the same label that node would be considered a leaf.
     */
    public static final float DEFAULT_PRECISION = 0.90f;


    /**
     * Impurity calculation method.
     */
    private final ImpurityCalculator impurity = new GiniIndexImpurityCalculation();
    /**
     * Max depth parameter. Growth of the tree is stopped once this depth is reached. Limiting depth of the tree can
     * help with overfitting, however if depth will be set too low tree will not be acurate.
     */
    private int maxDepth = 15;
    /**
     * Root node.
     */
    private DecisionNode<V> root;

    /**
     * Returns Label if data is homogeneous.
     */
    protected static <K, V> V label(K value, Stream<Function<K, V>> data, float homogenityPercentage) {

        Map<V, Long> labelCount = data.collect(groupingBy((x) -> x.apply(value), counting()));

        long totalCount = labelCount.values().stream().mapToLong(x -> x).sum();
        for (Map.Entry<V, Long> e: labelCount.entrySet()) {
            long nbOfLabels = e.getValue();
            if ((nbOfLabels / (double) totalCount) >= homogenityPercentage) {
                return e.getKey();
            }
        }
        return null;
    }

    protected static <K, V> Stream<List<Function<K, V>>> split(Predicate<Function<K, V>> p, List<Function<K, V>> data) {
        return split(p, data::stream);
    }

    /**
     * Split data according to if it has this feature.
     *
     * @param data Data to by split by this feature.
     * @return Sublists of split data samples.
     */
    static <K, V> Stream<List<Function<K, V>>> split(Predicate<Function<K, V>> p, Supplier<Stream<Function<K, V>>> data) {

        Map<Boolean, List<Function<K, V>>> split = data.get().collect(partitioningBy(p));
        List<Function<K, V>> ifTrue = split.get(true);
        List<Function<K, V>> ifFalse = split.get(false);

        return Stream.of(ifTrue, ifFalse);

    }

    /**
     * Differs from getLabel() that it always return some label and does not look at homogenityPercentage parameter. It
     * is used when tree growth is stopped and everything what is left must be classified so it returns majority label for the data.
     */
    static <K, V> V majority(K value, Stream<Function<K, V>> data) {

        return data.collect(groupingBy(x -> x.apply(value), counting()))
                .entrySet().stream().max(Map.Entry.comparingByValue()).get().getKey();
    }

    private static void printSubtree(DecisionNode<?> node, PrintStream o) {
        if (!node.isEmpty() && node.get(0) != null) {
            print(node.get(0), true, "", o);
        }
        print(node, o);
        if (node.size() > 1 && node.get(1) != null) {
            print(node.get(1), false, "", o);
        }
    }

    private static void print(DecisionNode node, PrintStream o) {
        o.print(node);
        o.println();
    }

    private static <K> void print(DecisionNode<?> node, boolean isRight, K indent, PrintStream o) {
        if (!node.isEmpty() && node.get(0) != null) {
            print(node.get(0), true, indent + (isRight ? "        " : " |      "), o);
        }
        o.print(indent);
        if (isRight) {
            o.print(" /");
        } else {
            o.print(" \\");
        }
        o.print("----- ");
        print(node, o);
        if (node.size() > 1 && node.get(1) != null) {
            print(node.get(1), false, indent + (isRight ? " |      " : "        "), o);
        }
    }

    public DecisionTree maxDepth(int d) {
        this.maxDepth = d;
        return this;
    }

    /**
     * Get root.
     */
    public DecisionNode<V> root() {
        return root;
    }

    public Stream<DecisionNode.LeafNode<V>> leaves() {
        return root != null ? root.recurse().filter(DecisionNode::isLeaf).map(n -> (DecisionNode.LeafNode<V>) n).distinct() : Stream.empty();
    }

    /**
     * Trains tree on training data for provided features.
     *
     * @param value        The value column being learned
     * @param trainingData List of training data samples.
     * @param features     List of possible features.
     */
    public DecisionNode<V> put(K value, Stream<Function<K, V>> trainingData, Stream<Predicate<Function<K, V>>> features, IntToFloatFunction precision) {
        root = put(value, StreamReplay.replay(trainingData), features, 1, precision);
        return root;
    }

    /**
     * constant precision
     */
    public DecisionNode put(K value, Collection<Function<K, V>> data, Stream<Predicate<Function<K, V>>> features, float precision) {
        return put(value, data.stream(), features, (depth) -> precision);
    }

    public DecisionNode put(K value, Collection<Function<K, V>> data, Collection<Predicate<Function<K, V>>> features) {
        return put(value, data, features.stream());
    }

    public DecisionNode put(K value, Collection<Function<K, V>> data, Iterable<Predicate<Function<K, V>>> features) {
        return put(value, data, StreamSupport.stream(features.spliterator(), false));
    }

    /**
     * default constant precision
     */
    public DecisionNode put(K value, Collection<Function<K, V>> data, Stream<Predicate<Function<K, V>>> features) {
        return put(value, data, features, DEFAULT_PRECISION);
    }

    /**
     * Grow tree during training by splitting data recusively on best feature.
     *
     * @param data     List of training data samples.
     * @param features List of possible features.
     * @return Node after split. For a first invocation it returns tree root node.
     */
    protected DecisionNode<V> put(K key, Supplier<Stream<Function<K, V>>> d, Stream<Predicate<Function<K, V>>> features, int currentDepth, IntToFloatFunction depthToPrecision) {


        V currentNodeLabel;
        if ((currentNodeLabel = label(key, d.get(), depthToPrecision.valueOf(currentDepth))) != null) {
            return leaf(currentNodeLabel);
        }

        boolean stoppingCriteriaReached = currentDepth >= maxDepth;
        if (stoppingCriteriaReached) {
            return leaf(majority(key, d.get()));
        }


        Supplier<Stream<Predicate<Function<K, V>>>> f = StreamReplay.replay(features);

        Predicate<Function<K, V>> split = bestSplit(key, d, f.get());

        DecisionNode<V> branch = split(split, d).map(

                subsetTrainingData -> subsetTrainingData.isEmpty() ?

                        leaf(majority(key, d.get()))

                        :

                        put(key, StreamReplay.replay(subsetTrainingData.stream()),
                                f.get().filter(p -> !p.equals(split)),
                                currentDepth + 1,
                                depthToPrecision
                        ))

                .collect(Collectors.toCollection(() -> DecisionNode.feature(split)));

        return (branch.size() == 1) ? branch.get(0) : branch;

    }

    /**
     * Classify a sample.
     *
     * @param value Data sample
     * @return Return label of class.
     */
    public V get(Function<K, V> value) {
        DecisionNode<V> node = root;
        while (!node.isLeaf()) {

            node = node.get(node.feature.test(value) ? 0 : 1);
        }
        return node.label;
    }

    /**
     * Finds best feature to split on which is the one whose split results in lowest impurity measure.
     */
    protected Predicate<Function<K, V>> bestSplit(K value, Supplier<Stream<Function<K, V>>> data, Stream<Predicate<Function<K, V>>> features) {
        final double[] currentImpurity = {Double.POSITIVE_INFINITY};

        return features.reduce(null, (bestSplit, feature) -> {
            double calculatedSplitImpurity =
                    split(feature, data).filter(x -> !x.isEmpty()).map(sd -> StreamReplay.replay(sd.stream())).mapToDouble(splitData ->
                            impurity.impurity(value, splitData)).average().orElse(Double.POSITIVE_INFINITY);
            if (calculatedSplitImpurity < currentImpurity[0]) {
                currentImpurity[0] = calculatedSplitImpurity;
                return feature;
            }
            return bestSplit;
        });
    }

    public void explain(BiConsumer<List<ObjectBooleanPair<DecisionNode<V>>>, DecisionNode.LeafNode<V>> c) {
        root.explain(c, new FasterList());
    }

    public void print() {
        print(System.out);
    }

    public void print(PrintStream o) {
        printSubtree(root, o);
    }

    /**
     * requires V to be Comparable
     */
    public SortedMap<DecisionNode.LeafNode<V>, List<ObjectBooleanPair<DecisionNode<V>>>> explanations() {
        SortedMap<DecisionNode.LeafNode<V>, List<ObjectBooleanPair<DecisionNode<V>>>> explanations = new TreeMap();
        explain((path, result) -> explanations.put(result, new FasterList(path) /* clone it */));
        return explanations;
    }

    /** var is the name of the target value */
    public SortedMap<DecisionNode.LeafNode<V>, List<String>> explainedConditions() {
        SortedMap<DecisionNode.LeafNode<V>, List<String>> map = new TreeMap<>();
        explanations().entrySet().forEach((e) -> {
            DecisionNode.LeafNode<V> result = e.getKey();
            List<String> cond = e.getValue().stream().map(c ->
                c.getOne().condition(c.getTwo())
            ).filter(x -> !"false".equals(x)).collect(toList());
            if (!cond.isEmpty())
                map.put(result, cond);
        });
        return map;
    }

    public static class DecisionNode<V> extends FasterList<DecisionNode<V>> implements Comparable<V> {


        /**
         * Node's feature used to split it further.
         */
        public final Predicate feature;

        public final V label;
        private final int hash;

        DecisionNode(Predicate feature) {
            this(feature, null);
        }

        DecisionNode(V label) {
            this(null, label);
        }

        private DecisionNode(@Nullable Predicate feature, @Nullable V label) {
            super(0);
            this.label = label;
            this.feature = feature;
            this.hash = Objects.hash(label, feature);
        }

        public static <V> DecisionNode<V> feature(Predicate feature) {
            return new DecisionNode<>(feature);
        }

        public static <V> DecisionNode<V> leaf(V label) {
            return new LeafNode<>(label);
        }

        @Override
        public boolean add(DecisionNode<V> x) {
            return !contains(x) && super.add(x);
        }

        public Stream<DecisionNode<V>> recurse() {
            return isEmpty() ? Stream.of(this) :
                    Stream.concat(Stream.of(this), stream().flatMap(DecisionNode::recurse));
        }

        public boolean isLeaf() {
            return feature == null || isEmpty();
        }

        public String toString() {
            return feature != null ? feature.toString() : label.toString();
        }

        @Override
        public final int hashCode() {
            return hash;
        }

        @Override
        public boolean equals(Object that) {
            if (this == that) return true;
            else {
                if (feature != null)
                    if (!feature.equals(((DecisionNode) that).feature))
                        return false;
                return Objects.equals(label, (((DecisionNode) that).label));
            }
        }

        @Override
        public int compareTo(Object o) {
            if (o == this) return 0;
            DecisionNode n = (DecisionNode) o;
            if (feature != null) {
                int f = Integer.compare(feature.hashCode(), n.feature.hashCode());
                if (f != 0)
                    return f;
            }
            return ((Comparable) label).compareTo(n.label);
        }

        public void explain(BiConsumer<List<ObjectBooleanPair<DecisionNode<V>>>, LeafNode<V>> c, FasterList<ObjectBooleanPair<DecisionNode<V>>> path) {


            int s = size();
            if (s == 2) {

                explain(c, path, 0);
                explain(c, path, 1);

            } else if (s == 0) {
//
//            } else {
                throw new UnsupportedOperationException("predicate?");
            }

        }

        /** compose an expression that matches the condition of the decision of this node */
        public String condition(boolean isTrue) {
            assert(feature!=null);

            if (feature instanceof CentroidMatch) {
                return ((CentroidMatch)feature).condition(isTrue);
            } else {
                throw new TODO();
            }
        }

        public void explain(BiConsumer<List<ObjectBooleanPair<DecisionNode<V>>>, LeafNode<V>> c, FasterList<ObjectBooleanPair<DecisionNode<V>>> path, int child) {

            assert (size()==2 && (child == 0 || child == 1)); //TODO n-ary split

            boolean isTrue = child == 0;
            path.add(PrimitiveTuples.pair(this, isTrue));
            get(child).explain(c, path);
            path.removeLastFast();
        }


        public static class LeafNode<V> extends DecisionNode<V> {
            public LeafNode(V label) {
                super(label);
            }

            @Override
            public void explain(BiConsumer<List<ObjectBooleanPair<DecisionNode<V>>>, LeafNode<V>> c, FasterList<ObjectBooleanPair<DecisionNode<V>>> path) {
                c.accept(path, this);
            }

            @Override
            public boolean equals(Object that) {
                return this == that;
            }

            @Override
            public int compareTo(Object o) {
                if (this == o) return 0;
                if (!(o instanceof LeafNode)) return -1;
                int x = ((Comparable) label).compareTo(((DecisionNode) o).label);
                if (x != 0) return x;
                return Integer.compare(System.identityHashCode(this), System.identityHashCode(o));
            }

            @Override
            public String toString() {
                return label.toString();
            }


            @Override
            public boolean add(DecisionNode x) {
                throw new UnsupportedOperationException();
            }

            @Override
            public void add(int index, DecisionNode element) {
                throw new UnsupportedOperationException();
            }
        }
    }

    public void printExplanations() {
        printExplanations(System.out);
    }

    public void printExplanations(PrintStream out) {
        explainedConditions().forEach((leaf, path) -> out.println(leaf + "\n\t" + path));
    }
}
