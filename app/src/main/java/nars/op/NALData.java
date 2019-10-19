package nars.op;

import jcog.Texts;
import jcog.data.list.FasterList;
import jcog.table.ARFF;
import jcog.table.DataTable;
import nars.$;
import nars.NAR;
import nars.Op;
import nars.Task;
import nars.task.NALTask;
import nars.term.Term;
import nars.term.var.NormalizedVariable;
import nars.term.var.UnnormalizedVariable;
import nars.truth.Truth;
import org.jetbrains.annotations.Nullable;

import java.util.Arrays;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.stream.IntStream;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

import static nars.Op.*;
import static nars.time.Tense.ETERNAL;

/**
 * translates a schema+data table to a set of beliefs and questions
 */
public class NALData {

    /**
     * all elements except a specified row become the subj of an impl to the element in the specified column
     */
    static BiFunction<Term, Term[], Term> predictsLast = (ctx, tt) -> {
        var lastCol = tt.length - 1;
        var subj = Arrays.copyOf(tt, lastCol);
        var pred = tt[lastCol];
        return $.inh($.p($.p(subj), pred), ctx);
    };

    public static void believe(NAR n, DataTable data, BiFunction<Term, Term[], Term> pointGenerator) {
        n.input(
                Stream.concat(metaBeliefs(n, data, pointGenerator), tasks(n, data, pointGenerator))
        );
    }

//    /** raw product representation of the row */
//    public static Function<Term[], Term> raw = $::p;
//
//    /** all elements except a specified row become the subj of an impl to the element in the specified column*/
//    public static BiFunction<Term, Term[], Term> predictsNth(int column) {
//        return (tt) -> {
//            Term[] subj = ArrayUtils.remove(tt, column);
//            Term pred = tt[column];
//            return $.inh($.p(subj), pred);
//        };
//    }

    public static void ask(NAR n, DataTable data, BiFunction<Term, Term[], Term> pointGenerator) {
        n.input(
                data(n, data, QUESTION, pointGenerator)
        );
    }

    /**
     * beliefs representing the schema's metadata
     */
    private static Stream<Task> metaBeliefs(NAR nar, DataTable a, BiFunction<Term, Term[], Term> pointGenerator) {
        var meta = new FasterList<Term>();

        var n = a.columnCount();
        var pattern = pointGenerator.apply(
                name(a),
                IntStream.range(0, n).mapToObj(i1 -> $.varDep(i1 + 1)).toArray(Term[]::new)
        );
        for (var i = 0; i < n; i++) {
            var ai = a.attrName(i);
            var attr = attrTerm(ai);

            meta.add(

                    IMPL.the(pattern.replace($.varDep(i + 1), $.varIndep(i + 1)),
                            $.inh($.varIndep(i + 1), attr))
            );

            var nom = a.categories(ai);
            if (nom != null) meta.add(INH.the(SETe.the($.$(nom)), attr));

        }

        return meta.stream().map(t -> NALTask.the(t.normalize(), BELIEF, $.t(1f, nar.confDefault(BELIEF)), nar.time(), ETERNAL, ETERNAL, nar.evidence()).pri(nar)
        );
    }

    private static Term attrTerm(String ai) {
        return $.$$(Texts.unquote(ai));
    }

    public static Stream<Task> tasks(NAR n, DataTable a, BiFunction<Term, Term[], Term> pointGenerator) {
        return data(n, a, (byte) 0, pointGenerator);
    }

    /**
     * if punc==0, then automatically decides whether belief or question
     * according to presence of a Query variable in a data point.
     * <p>
     * the pointGenerator transforms the raw components of a row
     * into a compound target (task content). how this is done
     * controls the semantics of the data point, with regard
     * to the application: prediction, optimization, etc.
     * <p>
     * for example,
     * (a,b,c,d) -> ((a,b,c)==>d)
     * is different from:
     * (a,b,c,d) -> ((a,b)==>(c,d))
     * and different from
     * (a,b,c,d) -> ((a,b,c)-->d)
     */
    public static Stream<Task> data(NAR n, DataTable a, byte punc, BiFunction<Term, Term[], Term> pointGenerator) {
        var now = n.time();
        return terms(a, pointGenerator).map(point -> {

            var p = punc != 0 ?
                    punc
                    :
                    (point.hasAny(VAR_QUERY) ? QUESTION : BELIEF);

            @Nullable Truth truth = p == QUESTION || p == QUEST ? null :
                    $.t(1f, n.confDefault(p));
            return NALTask.the(point.normalize(), p, truth, now, ETERNAL, ETERNAL, n.evidence()).pri(n);
        });
    }

    public static Stream<Term> terms(DataTable a, BiFunction<Term, Term[], Term> generator) {
        var ctx = name(a);
        return StreamSupport.stream(a.spliterator(), false).map(point -> {
            //ImmutableList point = instance.data;
            var n = point.columnCount();
            var t = new Term[n];
            for (var i = 0; i < n; i++) {
                var x = point.getObject(i);
                if (x instanceof String) t[i] = attrTerm((String) x);
                else if (x instanceof Number) t[i] = $.the((Number) x);
                else throw new UnsupportedOperationException();
            }
            return generator.apply(ctx, t);
        });
    }

    public static Term name(DataTable a) {
        var r = a instanceof DataTable ? ((ARFF) a).getRelation() : a.toString();
        if (r == null)
            r = ("ARFF_" + System.identityHashCode(a));
        return $.the(r);
    }


    /**
     * any (query) variables are qualified by wrapping in conjunction specifying their type in the data model
     */
    public static Function<Term[], Term> typed(DataTable dataset, Function<Term[], Term> pointGenerator) {
        return x -> {
            var y = pointGenerator.apply(x);
            if (y.hasAny(Op.VAR_QUERY)) {
                FasterList<Term> qVar = y.subterms().collect(s -> s.op() == VAR_QUERY, new FasterList());

                var typing = qVar.stream().map(q -> {
                    if (q instanceof UnnormalizedVariable) {
                        var col = q.toString().substring(1);
                        return INH.the(q, attrTerm(col));
                    } else {

                        assert (q instanceof NormalizedVariable);
                        var col = q.hashCode() - 1;
                        return INH.the(q, attrTerm(dataset.attrName(col)));
                    }
                }).toArray(Term[]::new);
                y = CONJ.the(y, CONJ.the(typing));
            }
            return y;
        };
    }
}
