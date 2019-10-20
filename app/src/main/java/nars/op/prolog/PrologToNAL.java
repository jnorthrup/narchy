package nars.op.prolog;

import alice.tuprolog.NumberTerm;
import alice.tuprolog.Struct;
import alice.tuprolog.Theory;
import alice.tuprolog.Var;
import jcog.TODO;
import jcog.Util;
import nars.$;
import nars.Op;
import nars.term.Term;
import nars.term.Termlike;
import nars.term.Variable;
import org.eclipse.collections.api.set.MutableSet;
import org.eclipse.collections.impl.map.mutable.UnifiedMap;
import org.eclipse.collections.impl.set.mutable.UnifiedSet;

import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

import static nars.Op.CONJ;

public class PrologToNAL {

    public static final Term QUESTION_GOAL = $.the("?-");

    public static Iterable<Term> N(Theory t) {
        return N((Iterable)t);
    }

    public static Iterable<Term> N(Iterable<alice.tuprolog.Term> t) {
        

        return StreamSupport.stream(t.spliterator(), false).map(PrologToNAL::N).collect(Collectors.toList());








    }

    private static Term N(alice.tuprolog.Term t) {
        if (t instanceof alice.tuprolog.Struct) {
            var s = (Struct) t;
            var name = s.name();
            switch (name) {
                /* "=:=": identity(X,Y) */
                /* "=\=": --identity(X,Y) */
                /* "=": unify(X,Y) */
                /* "<": lessThan(X,Y) etc */

                case ":-":
                    assert(s.subs()==2);
                    var pre = N(s.sub(1));
                    var post = N(s.sub(0));


                    var impl = $.impl(pre, post);
                    pre = impl.sub(0);
                    post = impl.sub(1);

                    if (pre.varQuery()>0 && post.varQuery()>0) {
                        MutableSet<Variable> prev = new UnifiedSet();
                        pre.recurseTerms(Termlike::hasVarQuery, (a) -> {
                            if (a.op() == Op.VAR_QUERY)
                                prev.add((Variable)a);
                            return true;
                        }, null);
                        MutableSet<Variable> posv = new UnifiedSet();
                        post.recurseTerms(Termlike::hasVarQuery, (a) -> {
                            if (a.op() == Op.VAR_QUERY)
                                posv.add((Variable)a);
                            return true;
                        }, null);

                        var common = prev.intersect(posv);
                        var cs = common.size();
                        if (cs > 0) {
                            Map<Term, Term> x = new UnifiedMap(cs);
                            for (var c : common) {
                                x.put(c, $.varIndep(c.toString().substring(1)));
                            }
                            impl = impl.replace(x);
                        }
                    }

                    return impl;
                case ",":
                    return CONJ.the(N(s.sub(0)), N(s.sub(1)));
                default:
                    Term atom = $.the(name);
                    var arity = s.subs();
                    if (arity == 0) {
                        return atom;
                    } else {
                        return $.inh(
                                $.p((Term[])Util.map(0, arity, Term[]::new, i -> N(s.sub(i)))),
                                atom);
                    }
            }
        } else if (t instanceof Var) {
            return $.varQuery(((Var) t).name());
            
        } else if (t instanceof NumberTerm.Int) {
            return $.the(((NumberTerm.Int)t).intValue());
        } else {
            throw new TODO(t + " (" + t.getClass() + ") untranslatable");
        }
    }

}
