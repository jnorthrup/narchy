package nars.test.impl;

import nars.$;
import nars.NAR;
import nars.term.Term;
import nars.test.TestNAR;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.HashSet;
import java.util.Set;

/**
 * TODO abstract edge() for different relation types:
 * similarity
 * implication
 * etc
 */
public class DeductiveMeshTest {


    public final Term q;

    public final TestNAR test;

    public final Set<Term> coords;
    public final Set<Term> edges;


    public DeductiveMeshTest(@NotNull NAR n, @NotNull int... dims) {
        this(n, dims, -1);
    }

    public DeductiveMeshTest(@NotNull NAR n, @NotNull int[] dims, int timeLimit) {
        this(new TestNAR(n), dims, timeLimit);
    }

    public DeductiveMeshTest(@NotNull TestNAR n, @NotNull int[] dims, int timeLimit) {

        if (dims.length != 2)
            throw new UnsupportedOperationException("2-D only implemented");

        coords = new HashSet();
        edges = new HashSet();
        for (int x = 0; x < dims[0]; x++) {
            for (int y = 0; y < dims[1]; y++) {
                coords.add($.INSTANCE.p(x,y));

                /*if (x > y)*/ {
                    if (x > 0)
                        edges.add(edge(x, y, x - 1, y));
                    if (y > 0)
                        edges.add(edge(x, y, x, y - 1));
                    if (x < dims[0] - 1)
                        edges.add(edge(x, y, x + 1, y));
                    if (y < dims[1] - 1)
                        edges.add(edge(x, y, x, y + 1));
                }
            }
        }

        NAR nar = n.nar;
        for (Term edge : edges)
            nar.believe(edge);

        Term term = q = edge(0, 0, dims[0] - 1, dims[1] - 1);
        ask(n, term);

        if (timeLimit > 0)
            n.mustBelieve((long) timeLimit, q.toString(), 1f, 1f, 0.01f, 1f);

        this.test = n;
    }

    public void ask(TestNAR n, Term term) {
        n.nar.question(term);
//        n.nar.question(term, ETERNAL, (q, a) -> {
//            //System.out.println(a.proof())
//        });
    }

    private @Nullable
    static Term edge(int x1, int y1, int x2, int y2) {
        return $.INSTANCE.sim(vertex(x1, y1), vertex(x2, y2));
    }

    private static Term vertex(int x1, int y1) {
        return $.INSTANCE.p($.INSTANCE.the(x1), $.INSTANCE.the(y1));
    }


}
