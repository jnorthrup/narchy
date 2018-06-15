package nars.test.impl;

import nars.$;
import nars.NAR;
import nars.term.Compound;
import nars.term.Term;
import nars.test.TestNAR;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static nars.time.Tense.ETERNAL;

/**
 * TODO abstract edge() for different relation types:
 * similarity
 * implication
 * etc
 */
public class DeductiveMeshTest {


    public final Term q;

    public final List<Compound> coords;
    public final TestNAR test;


    public DeductiveMeshTest(@NotNull NAR n, @NotNull int... dims) {
        this(n, dims, -1);
    }

    public DeductiveMeshTest(@NotNull NAR n, @NotNull int[] dims, int timeLimit) {
        this(new TestNAR(n), dims, timeLimit);
    }

    public DeductiveMeshTest(@NotNull TestNAR n, @NotNull int[] dims, int timeLimit) {

        if (dims.length != 2)
            throw new UnsupportedOperationException("2-D only implemented");

        coords = $.newArrayList();
        Set<Term> edges = new HashSet();
        for (int x = 0; x < dims[0]; x++) {
            for (int y = 0; y < dims[1]; y++) {


                if (x > y) {
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

        edges.forEach(n.nar::believe);

        Term term = q = edge(0, 0, dims[0] - 1, dims[1] - 1);
        ask(n, term);

        if (timeLimit > 0)
            n.mustBelieve(timeLimit, q.toString(), 1f, 1f, 0.01f, 1f);

        this.test = n;
    }

    public void ask(@NotNull TestNAR n, Term term) {
        n.nar.question(term, ETERNAL, (q, a) -> System.out.println(a.proof()));
    }

    @Nullable
    private Term edge(int x1, int y1, int x2, int y2) {
        return $.sim(vertex(x1, y1), vertex(x2, y2));
    }

    private Term vertex(int x1, int y1) {
        return $.p($.the(x1), $.the(y1));
    }


}
