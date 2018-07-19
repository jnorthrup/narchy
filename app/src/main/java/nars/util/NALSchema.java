package nars.util;

import jcog.Texts;
import jcog.data.list.FasterList;
import jcog.io.arff.ARFF;
import nars.$;
import nars.NAR;
import nars.Op;
import nars.Task;
import nars.task.NALTask;
import nars.term.Term;
import nars.term.var.NormalizedVariable;
import nars.term.var.UnnormalizedVariable;
import org.apache.commons.lang3.ArrayUtils;
import org.eclipse.collections.api.list.ImmutableList;

import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import static nars.Op.*;
import static nars.time.Tense.ETERNAL;

/** schema+data -> beliefs/questions */
public class NALSchema {

    public static void believe(NAR n, ARFF data, Function<Term[], Term> pointGenerator) {
        n.input(
                Stream.concat(metaBeliefs(n, data, pointGenerator), data(n, data, pointGenerator))
        );
    }

    public static void ask(NAR n, ARFF data, Function<Term[], Term> pointGenerator) {
        n.input(
                data(n, data, QUESTION, pointGenerator)
        );
    }

    /** raw product representation of the row */
    public static Function<Term[], Term> raw = $::p;

    /** all elements except a specified row become the subj of an impl to the element in the specified column*/
    public static Function<Term[], Term> predictsNth(int column) {
        return (tt) -> {
            Term[] subj = ArrayUtils.remove(tt, column);
            Term pred = tt[column];
            return $.inh($.p(subj), pred);
        };
    }
    /** all elements except a specified row become the subj of an impl to the element in the specified column*/
    public static Function<Term[], Term> predictsLast = (tt) -> {
        int lastCol = tt.length - 1;
        Term[] subj = Arrays.copyOf(tt, lastCol);
        Term pred = tt[lastCol];
        return $.inh($.p(subj), pred);
    };



    /** beliefs representing the schema's metadata */
    public static Stream<Task> metaBeliefs(NAR nar, ARFF a, Function<Term[], Term> pointGenerator) {
        List<Term> meta = new FasterList();

        int n = a.attrCount();
        Term pattern = pointGenerator.apply(
            IntStream.range(0,n).mapToObj(i -> $.varDep(i+1)).toArray(Term[]::new)
        );
        for (int i = 0; i < n; i++) {
            String ai = a.attrName(i);
            Term attr = attrTerm(ai);

            meta.add(
                
                    IMPL.the(pattern.replace($.varDep(i+1), $.varIndep(i+1)),
                            $.inh($.varIndep(i+1), attr))
            );

            String[] nom = a.categories(ai);
            if (nom!=null) {
                meta.add(INH.the(SETe.the($.$(nom)), attr));
            }

        }

        return meta.stream().map(t ->
            new NALTask(t.normalize(), BELIEF, $.t(1f, nar.confDefault(BELIEF)),
                    nar.time(),
                    ETERNAL, ETERNAL,
                    nar.evidence()
            ).pri(nar)
        );
    }

    static Term attrTerm(String ai) {
        return $.$$(Texts.unquote(ai));
    }

    public static Stream<Task> data(NAR n, ARFF a, Function<Term[], Term> pointGenerator) {
        return data(n, a, (byte)0, pointGenerator);
    }

    /** if punc==0, then automatically decides whether belief or question
     * according to presence of a Query variable in a data point.
     *
     * the pointGenerator transforms the raw components of a row
     * into a compound term (task content). how this is done
     * controls the semantics of the data point, with regard
     * to the application: prediction, optimization, etc.
     *
     * for example,
     *      (a,b,c,d) -> ((a,b,c)==>d)
     *          is different from:
     *      (a,b,c,d) -> ((a,b)==>(c,d))
     *          and different from
     *      (a,b,c,d) -> ((a,b,c)-->d)
     *
     */
    public static Stream<Task> data(NAR n, ARFF a, byte punc, Function<Term[],Term> pointGenerator) {
        long now = n.time();
        return terms(a, pointGenerator).map(point->{

            byte p = punc != 0 ?
                    punc 
                    :
                    (point.hasAny(VAR_QUERY) ? QUESTION : BELIEF); 

            return new NALTask(point.normalize(), p, p==QUESTION || p == QUEST ? null :
                $.t(1f, n.confDefault(p)),
                    now,
                    ETERNAL,
                    ETERNAL, n.evidence()
            ).pri(n);
        });
    }

    public static Stream<Term> terms(ARFF a, Function<Term[],Term> generator) {
        return a.stream().map(instance->{
            ImmutableList point = instance.data;
            int n = point.size();
            Term[] t = new Term[n];
            for (int i = 0; i < n; i++) {
                Object x = point.get(i);
                if (x instanceof String) {
                    t[i] = attrTerm((String)x); 
                } else if (x instanceof Number) {
                    t[i] = $.the((Number)x);
                } else {
                    throw new UnsupportedOperationException();
                }
            }
            return generator.apply(t);
        });
    }


    /** any (query) variables are qualified by wrapping in conjunction specifying their type in the data model */
    public static Function<Term[], Term> typed(Function<Term[], Term> pointGenerator, ARFF dataset) {
        return (x) -> {
            Term y = pointGenerator.apply(x);
            if (y.hasAny(Op.VAR_QUERY)) {
                Set<Term> qVar = y.subterms().toSet(s -> s.op()==VAR_QUERY);

                Term[] typing = qVar.stream().map(q -> {
                    if (q instanceof UnnormalizedVariable) {
                        String col = q.toString().substring(1);
                        return INH.the(q, attrTerm(col));
                    } else {
                        
                        assert (q instanceof NormalizedVariable);
                        int col = ((NormalizedVariable) q).id - 1;
                        return INH.the(q, attrTerm(dataset.attrName(col)));
                    }
                }).toArray(Term[]::new);
                y = CONJ.the(y, CONJ.the(typing));
            }
            return y;
        };
    }
}
