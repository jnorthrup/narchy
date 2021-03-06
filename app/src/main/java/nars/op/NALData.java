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
import tech.tablesaw.api.Row;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.function.Predicate;
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
    static BiFunction<Term, Term[], Term> predictsLast = new BiFunction<Term, Term[], Term>() {
        @Override
        public Term apply(Term ctx, Term[] tt) {
            int lastCol = tt.length - 1;
            Term[] subj = Arrays.copyOf(tt, lastCol);
            Term pred = tt[lastCol];
            return $.INSTANCE.inh($.INSTANCE.p($.INSTANCE.p(subj), pred), ctx);
        }
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

        int n = a.columnCount();
        List<nars.term.Variable> list = new ArrayList<>();
        for (int i1 = 0; i1 < n; i1++) {
            nars.term.Variable variable = $.INSTANCE.varDep(i1 + 1);
            list.add(variable);
        }
        Term pattern = pointGenerator.apply(
                name(a),
                list.toArray(new Term[0])
        );
        FasterList<Term> meta = new FasterList<Term>();
        for (int i = 0; i < n; i++) {
            String ai = a.attrName(i);
            Term attr = attrTerm(ai);

            meta.add(

                    IMPL.the(pattern.replace($.INSTANCE.varDep(i + 1), $.INSTANCE.varIndep(i + 1)),
                            $.INSTANCE.inh($.INSTANCE.varIndep(i + 1), attr))
            );

            String[] nom = a.categories(ai);
            if (nom != null) meta.add(INH.the(SETe.the($.INSTANCE.$(nom)), attr));

        }

        return meta.stream().map(new Function<Term, Task>() {
                                     @Override
                                     public Task apply(Term t) {
                                         return NALTask.the(t.normalize(), BELIEF, $.INSTANCE.t(1f, nar.confDefault(BELIEF)), nar.time(), ETERNAL, ETERNAL, nar.evidence()).pri(nar);
                                     }
                                 }
        );
    }

    private static Term attrTerm(String ai) {
        return $.INSTANCE.$$(Texts.INSTANCE.unquote(ai));
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
        long now = n.time();
        return terms(a, pointGenerator).map(new Function<Term, Task>() {
            @Override
            public Task apply(Term point) {

                byte p = (int) punc != 0 ?
                        punc
                        :
                        (point.hasAny(VAR_QUERY) ? QUESTION : BELIEF);

                @Nullable Truth truth = (int) p == (int) QUESTION || (int) p == (int) QUEST ? null :
                        $.INSTANCE.t(1f, n.confDefault(p));
                return NALTask.the(point.normalize(), p, truth, now, ETERNAL, ETERNAL, n.evidence()).pri(n);
            }
        });
    }

    public static Stream<Term> terms(DataTable a, BiFunction<Term, Term[], Term> generator) {
        Term ctx = name(a);
        return StreamSupport.stream(a.spliterator(), false).map(new Function<Row, Term>() {
            @Override
            public Term apply(Row point) {
                //ImmutableList point = instance.data;
                int n = point.columnCount();
                Term[] t = new Term[n];
                for (int i = 0; i < n; i++) {
                    Object x = point.getObject(i);
                    if (x instanceof String) t[i] = attrTerm((String) x);
                    else if (x instanceof Number) t[i] = $.INSTANCE.the((Number) x);
                    else throw new UnsupportedOperationException();
                }
                return generator.apply(ctx, t);
            }
        });
    }

    public static Term name(DataTable a) {
        String r = a instanceof DataTable ? ((ARFF) a).getRelation() : a.toString();
        if (r == null)
            r = ("ARFF_" + System.identityHashCode(a));
        return $.INSTANCE.the(r);
    }


    /**
     * any (query) variables are qualified by wrapping in conjunction specifying their type in the data model
     */
    public static Function<Term[], Term> typed(DataTable dataset, Function<Term[], Term> pointGenerator) {
        return new Function<Term[], Term>() {
            @Override
            public Term apply(Term[] x) {
                Term y = pointGenerator.apply(x);
                if (y.hasAny(Op.VAR_QUERY)) {
                    FasterList<Term> qVar = y.subterms().collect(new Predicate<Term>() {
                        @Override
                        public boolean test(Term s) {
                            return s.op() == VAR_QUERY;
                        }
                    }, new FasterList());

                    Term[] typing = qVar.stream().map(new Function<Term, Term>() {
                        @Override
                        public Term apply(Term q) {
                            if (q instanceof UnnormalizedVariable) {
                                String col = q.toString().substring(1);
                                return INH.the(q, attrTerm(col));
                            } else {

                                assert (q instanceof NormalizedVariable);
                                int col = q.hashCode() - 1;
                                return INH.the(q, attrTerm(dataset.attrName(col)));
                            }
                        }
                    }).toArray(Term[]::new);
                    y = CONJ.the(y, CONJ.the(typing));
                }
                return y;
            }
        };
    }
}
