package org.intelligentjava.machinelearning.decisiontree;

import jcog.learn.QuantileDiscretize1D;
import org.eclipse.collections.api.block.function.primitive.IntToFloatFunction;
import org.intelligentjava.machinelearning.decisiontree.feature.DiscretizedScalarFeature;
import org.jetbrains.annotations.Nullable;

import java.util.Comparator;
import java.util.function.Function;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import static java.util.stream.Collectors.toList;

/**
 * row x column matrices of real-number values
 * <p>
 * TODO abstract this by extracting the numeric table into its own class,
 * then this becomes a DecisionTreeBuilder which can be used to generate different trees
 * from a common builder instance
 */
public class RealDecisionTree extends DecisionTree<Integer, Float> {

    public final FloatTable<String> table;
    public final DiscretizedScalarFeature[] cols;
    @Nullable
    private final String[] rangeLabels;


    /**
     * classify a data sample
     * the field should have the same ordering as the input
     * but the classification target row will not be accessed
     * <p>
     * if using an array directly, as a convention, you may put a NaN in that
     * array cell to clarify.
     */
    public float get(float... row) {
        return get((i) -> row[i]);
    }

    /* default: i >= 1
     * gradually reduces pressure on leaf precision
     */
    final IntToFloatFunction depthToPrecision;


    public RealDecisionTree(FloatTable<String> table, int predictCol, int maxDepth, int discretization) {
        this(table, predictCol, maxDepth, IntStream.range(0, discretization).mapToObj(String::valueOf).toArray(String[]::new));
    }

    public RealDecisionTree(FloatTable<String> table, int predictCol, int maxDepth, String... rangeLabels) {
        super();

        this.table = table;
        assert(table.size() > 0);

        int discretization = rangeLabels.length;
        assert (discretization > 1);
        assert (table.cols.length > 1);
        maxDepth(maxDepth);

        depthToPrecision = (i) -> {
            float p = (0.9f / (1 + (i - 1) / ((float) maxDepth)));
            return p;
        };

        this.cols = IntStream.range(0, table.cols.length).mapToObj(x -> new DiscretizedScalarFeature(x, table.cols[x], discretization,
                //new Gasolinear()
                new QuantileDiscretize1D()
        ))
                .toArray(DiscretizedScalarFeature[]::new);

        switch (discretization) {
            case 2:
                this.rangeLabels = new String[]{"LO", "HI"};
                break;
            case 3:
                this.rangeLabels = new String[]{"LO", "MD", "HI"};
                break;
            case 4:
                this.rangeLabels = new String[]{"LO", "M-", "M+", "HI"};
                break;
            default:
                this.rangeLabels = null;
                break;
        }


        update(table.rows.stream().peek(row -> {
            int i = 0;
            for (float x : row)
                cols[i++].learn(x);
        }), predictCol);

    }


    void update(Stream<float[]> rows, int column) {

        //System.out.println(classifiers);

        put(column, rows.map((r) -> (Function<Integer,Float>) i -> r[i]).collect(toList()),

                //the classifiers from every non-target column
                Stream.of(cols).
                        filter(x -> x.num != column).
                        flatMap(f -> f.classifiers(rangeLabels)).
                        collect(toList()),

                depthToPrecision
        );
    }

    public Node<Float> min() {
        return leaves().min(centroidComparator).get();
    }

    public Node<Float> max() {
        return leaves().max(centroidComparator).get();
    }



    static final Comparator<Node<Float>> centroidComparator = (a, b) -> Float.compare(a.label, b.label);

}
