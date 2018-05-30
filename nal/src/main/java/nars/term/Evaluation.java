package nars.term;

import jcog.data.ArrayHashSet;
import jcog.list.FasterList;
import jcog.util.ArrayIterator;
import jcog.util.CartesianIterator;
import jcog.version.VersionMap;
import jcog.version.Versioning;
import nars.$;
import nars.NAR;
import nars.Op;
import nars.subterm.Subterms;
import nars.term.atom.Atom;
import nars.term.atom.Atomic;
import nars.term.control.AbstractPred;
import nars.unify.match.EllipsisMatch;
import nars.util.term.transform.TermTransform;
import org.eclipse.collections.api.map.ImmutableMap;
import org.eclipse.collections.api.map.MutableMap;
import org.jetbrains.annotations.Nullable;

import java.util.Iterator;
import java.util.List;
import java.util.Random;
import java.util.Set;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Predicate;

import static nars.Op.*;

public class Evaluation {

    public static final Atom TRUE = (Atom) Atomic.the("\"" + True + '"');

    static final ThreadLocal<Evaluation> solving = ThreadLocal.withInitial(Evaluation::new);

    final List<Predicate<VersionMap<Term, Term>>[]> proc = new FasterList(1);

    final Versioning v = new Versioning(32, 128);

    final VersionMap<Term, Term> subst = new VersionMap(v);


    private Evaluation() {

    }

    public static Evaluation start() {
        return solving.get().clear();
    }

    public static Evaluation the() {
        return solving.get();
    }

    public static Term solve(Consumer<Evaluation> c) {
        Evaluation s = Evaluation.the();
        if (s != null) {
            c.accept(s);
        }
        return null;
    }

    public static Set<Term> solveAll(Term x, NAR nar) {
        return solveAll(x, nar.functors);
    }

    public static ArrayHashSet<Term> solveAll(Term x, TermContext context) {
        ArrayHashSet<Term> all = new ArrayHashSet<>();
        solve(x, context, (y) -> {



            all.add(y);

            return true;
        });
        return !all.isEmpty() ? all : ArrayHashSet.EMPTY;
    }

    public static boolean solve(Term x, TermContext context, Predicate<Term> each) {

        Term y = needsEvaluation(x, context);
        if (y == null)
            return each.test(x); 

        Evaluation s = Evaluation.clear();
        

        return s.get(y, each);
    }

    @Nullable
    private static Term needsEvaluation(Term x, TermContext context) {

        if (!possiblyNeedsEval(x))
            return null;

        MyFunctorResolver ft = new MyFunctorResolver(context);
        Term y = x.transform(ft);

        if (y == Null) {
            return Null;
        }

        if (!ft.hasFunctor) {
            return null;
        }

        return y;
    }

    public static boolean possiblyNeedsEval(Term x) {
        return x.hasAll(Op.FuncBits);
    }

    public static Evaluation clear() {
        Evaluation e = Evaluation.the();
        e.reset();
        return e;
    }

    public static Term solveAny(Term x, TermContext context, Random random) {
        ArrayHashSet<Term> results = solveAll(x, context);
        return results.get(random);
    }

    static protected Term eval(Term c) {
































        /*if (hasAll(opBits))*/






        /*if (subterms().hasAll(opBits))*/

        Subterms uu = c.subterms();
        Term[] xy = null;
        
        Op o = c.op();
        
        
        int ellipsisAdds = 0, ellipsisRemoves = 0;

        for (int i = 0, n = uu.subs(); i < n; i++) {
            Term xi = xy != null ? xy[i] : uu.sub(i);
            Term yi = possiblyNeedsEval(xi) ? eval(xi) : xi;
            if (xi != yi) {
                if (yi == null) {
                    
                } else {
                    if (yi == Null)
                        return Null;



                    if (yi instanceof EllipsisMatch) {
                        int ys = yi.subs();
                        ellipsisAdds += ys;
                        ellipsisRemoves++;
                    }

                    if (xi.getClass() != yi.getClass() || !xi.equals(yi)) {
                        if (xy == null) {
                            xy = ((Compound) c).arrayClone(); 
                        }
                        xy[i] = yi;
                    }
                }
            }
        }


        Term u;
        if (xy != null) {
            if (ellipsisAdds > 0) {
                
                xy = EllipsisMatch.flatten(xy, ellipsisAdds, ellipsisRemoves);
            }

            u = o.compound(c.dt(), xy);
            o = u.op(); 
            uu = u.subterms(); 
        } else {
            u = c;
        }


        
        
        if (o == INH && uu.hasAll(Op.FuncInnerBits)) {
            Term pred, subj;
            if ((pred = uu.sub(1)) instanceof Functor && (subj = uu.sub(0)).op() == PROD) {

                Term v = ((Function<Subterms, Term>) pred).apply(subj.subterms());
                if (v instanceof AbstractPred) {
                    u = $.the(((Predicate) v).test(null));
                } else if (v == null) {
                    
                } else {
                    u = v; 
                }
            }
        }

        if (u != c && (u.equals(c) && u.getClass() == c.getClass()))
            return c; 

        return u;
    }

    private void reset() {
        proc.clear();
        v.reset();
    }

    public boolean get(Term _x, Predicate<Term> each) {
        Iterator<Predicate<VersionMap<Term, Term>>[]> pp;

        Term x = eval(_x);
        if (x.hasAny(Op.BOOL))
            x = boolFilter(_x, x);

        int np = proc.size();

        switch (np) {
            case 0:
                
                return each.test(x);





            default: {
                Iterable[] aa = new Iterable[np];
                for (int i = 0; i < np; i++) {
                    Predicate<VersionMap<Term, Term>>[] pi = proc.get(i);
                    aa[i] = () -> ArrayIterator.get(pi);
                }
                pp = new CartesianIterator<Predicate<VersionMap<Term, Term>>>(Predicate[]::new, aa);
                break;
            }
        }

        int start = v.now();

        nextPermute:
        while (pp.hasNext()) {

            Term y;

            v.revert(start);

            Predicate<VersionMap<Term, Term>>[] n = pp.next();
            assert (n.length > 0);

            for (Predicate p : n) {
                if (!p.test(subst)) {
                    continue nextPermute;
                }
            }

            y = x.replace(subst);
            if (y == null)
                continue;

            Term z = eval(y);

            if (z.hasAny(Op.BOOL))
                z = boolFilter(y, z);

            if (z != null && !each.test(z))
                return false;

            if (np < proc.size()) { 
                int before = v.now();
                if (!get(z, each)) 
                    return false;
                v.revert(before);
            }

        }


        return true;
    }

    protected Term boolFilter(Term y, Term z) {
        if (z == Null)
            return Null;
        if (z == True || z == False || z.hasAny(Op.BOOL)) {
            boolean hasFalse = z == False || z.ORrecurse(t -> t == False); 



            
            
            return hasFalse ? False : True;
        }
        return z;
    }

        public void replace (Term x, Term xx){
            replace(subst(x, xx));
        }

        public void replace (Term x, Term xx, Term y, Term yy){
            replace(subst(x, xx, y, yy));
        }

        public void replace (Predicate...r){
            proc.add(r);
        }

        private Predicate<VersionMap<Term, Term>> subst (Term x, Term xx){
            return (m) -> {
                Term px = m.get(x);
                if (px != null) {
                    return px.equals(xx); 
                } else {
                    m.tryPut(x, xx);
                    return true;
                }
            };
        }

        public Predicate<VersionMap<Term, Term>> subst (Term x, Term xx, Term y, Term yy){
            return (m) -> subst(x, xx).test(m) && subst(y, yy).test(m);
        }

        /**
         * interface necessary for evaluating terms
         */
        public interface TermContext extends Function<Term, Termed> {


            /**
             * elides superfluous .term() call
             */
            default Term applyTermIfPossible(/*@NotNull*/ Term x, Op supertermOp, int subterm) {






                Termed y = apply(x);
                return y != null ? y.term() : x;
            }


            class MapTermContext implements TermContext {
                private final ImmutableMap<Term, Term> resolvedImm;

                public MapTermContext(MutableMap<Term, Term> resolved) {
                    this(resolved.toImmutable());
                }

                public MapTermContext(ImmutableMap<Term, Term> resolvedImm) {
                    this.resolvedImm = resolvedImm;
                }

                @Override
                public Termed apply(Term term) {
                    if (term.op() == ATOM) {
                        Term r = resolvedImm.get(term);
                        if (r != null)
                            return r;
                    }
                    return term;
                }
            }
        }

        private static class MyFunctorResolver implements TermTransform {
            private final TermContext context;

            public boolean hasFunctor;

            public MyFunctorResolver(TermContext context) {
                this.context = context;
            }

            @Override
            public @Nullable Termed transformAtomic(Term z) {
                if (z instanceof Functor)
                    hasFunctor = true; 

                if (z.op() == ATOM) {
                    Term zz = context.applyTermIfPossible(z, null, 0);
                    if (zz instanceof Functor)
                        hasFunctor = true;
                    return zz;
                }
                return z;
            }

        }
    }
