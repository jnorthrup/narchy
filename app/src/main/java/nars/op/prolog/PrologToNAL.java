package nars.op.prolog;

import alice.tuprolog.NumberTerm;
import alice.tuprolog.Struct;
import alice.tuprolog.Theory;
import alice.tuprolog.Var;
import com.google.common.collect.Iterables;
import jcog.TODO;
import jcog.Util;
import nars.$;
import nars.Op;
import nars.term.Term;
import nars.term.Termlike;
import nars.term.var.Variable;
import org.eclipse.collections.api.set.MutableSet;
import org.eclipse.collections.impl.map.mutable.UnifiedMap;
import org.eclipse.collections.impl.set.mutable.UnifiedSet;

import java.util.Map;

import static nars.Op.CONJ;

public class PrologToNAL {

    public static final Term QUESTION_GOAL = $.the("?-");

    public static Iterable<nars.term.Term> N(Theory t) {
        return N((Iterable)t);
    }

    public static Iterable<nars.term.Term> N(Iterable<alice.tuprolog.Term> t) {
        //System.out.println(t);

        return Iterables.transform(t, PrologToNAL::N);

//        Iterator<? extends Term> xx = t.iterator();
//        while (xx.hasNext()) {
//            Term z = xx.next();
//            System.out.println(z);
//        }
//
//        return y;
    }

    private static nars.term.Term N(alice.tuprolog.Term t) {
        if (t instanceof alice.tuprolog.Term) {
            Struct s = (Struct) t;
            String name = s.name();
            switch (name) {
                /* "=:=": identity(X,Y) */
                /* "=\=": --identity(X,Y) */
                /* "=": unify(X,Y) */
                /* "<": lessThan(X,Y) etc */

                case ":-":
                    assert(s.subs()==2);
                    nars.term.Term pre = N(s.sub(1)); //reverse, prolog is backwards
                    nars.term.Term post = N(s.sub(0));

                    //convert to implication first, then promote variables on the resulting pre/post
                    Term impl = $.impl(pre, post);
                    pre = impl.sub(0);
                    post = impl.sub(1);

                    if (pre.varQuery()>0 && post.varQuery()>0) {
                        MutableSet<nars.term.var.Variable> prev = new UnifiedSet();
                        pre.recurseTerms(Termlike::hasVarQuery, (a) -> {
                            if (a.op() == Op.VAR_QUERY)
                                prev.add((Variable)a);
                            return true;
                        }, null);
                        MutableSet<nars.term.var.Variable> posv = new UnifiedSet();
                        post.recurseTerms(Termlike::hasVarQuery, (a) -> {
                            if (a.op() == Op.VAR_QUERY)
                                posv.add((Variable)a);
                            return true;
                        }, null);

                        MutableSet<nars.term.var.Variable> common = prev.intersect(posv);
                        int cs = common.size();
                        if (cs > 0) {
                            Map<nars.term.Term,nars.term.Term> x = new UnifiedMap(cs);
                            for (nars.term.var.Variable c : common) {
                                x.put(c, $.varIndep(c.toString().substring(1)));
                            }
                            impl = impl.replace(x);
                        }
                    }

                    return impl;
                case ",":
                    return CONJ.the(N(s.sub(0)), N(s.sub(1)));
                default:
                    nars.term.Term atom = $.the(name);
                    int arity = s.subs();
                    if (arity == 0) {
                        return atom;
                    } else {
                        return $.inh(
                                $.p((nars.term.Term[])Util.map(0, arity, i -> N(s.sub(i)), nars.term.Term[]::new)),
                                atom);
                    }
            }
        } else if (t instanceof Var) {
            return $.varQuery(((Var) t).name());
            //throw new RuntimeException(t + " untranslated");
        } else if (t instanceof NumberTerm.Int) {
            return $.the(((NumberTerm.Int)t).intValue());
        } else {
            throw new TODO(t + " (" + t.getClass() + ") untranslatable");
        }
    }

}
