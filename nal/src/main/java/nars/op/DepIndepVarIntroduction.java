package nars.op;

import nars.$;
import nars.Op;
import nars.term.Term;
import nars.term.Terms;
import org.eclipse.collections.api.list.primitive.ByteList;
import org.eclipse.collections.api.tuple.Pair;
import org.eclipse.collections.impl.map.mutable.primitive.ObjectByteHashMap;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.function.ToIntFunction;

import static nars.Op.*;

/**
 * 1-iteration DepVar and IndepVar introduction that emulates and expands the original NAL6 Variable Introduction Rules
 */
public class DepIndepVarIntroduction extends VarIntroduction {

    public static final DepIndepVarIntroduction the = new DepIndepVarIntroduction();

    final static int ConjOrStatementBits = Op.IMPL.bit | Op.CONJ.bit; //NOT including similarity or inheritance because variables acorss these would be loopy

    private final static int DepOrIndepBits = Op.VAR_INDEP.bit | Op.VAR_DEP.bit | Op.VAR_PATTERN.bit;

    /** sum by complexity if passes include filter */
    private static final ToIntFunction<Term> depIndepFilter = t ->
            t.hasAny(DepOrIndepBits | Op.NEG.bit) ? 0 : 1;

    @Override
    public Pair<Term, Map<Term, Term>> apply(Term x, Random r) {
        if (x.hasAny(ConjOrStatementBits))
            return super.apply(x, r);
        else
            return null;
    }

    @Override
    protected Term next(Term input, Random shuffle) {
        return Terms.nextRepeat(input, depIndepFilter, 2, shuffle);
    }

    @Nullable
    @Override
    protected Term next(Term input, Term selected, byte order) {

        if (selected.equals(Imdex))
            return null;

        Op inOp = input.op();
        List<ByteList> paths = $.newArrayList(1);
        int minPathLength = inOp.statement ? 2 : 0;
        input.pathsTo(selected, (path, t) ->  {
            if (path.size() >= minPathLength)
                paths.add(path.toImmutable());
            return true; //TODO may be able to terminate early if we know this is the last one
        });
        int pSize = paths.size();
        //if (pSize == 0)
        if (pSize <= 1)
            return null;

        //byte[][] paths = pp.toArray(new byte[pSize][]);

//        //detect an invalid top-level indep var substitution
//        if (inOp.statement) {
//            Iterator<byte[]> pp = paths.iterator();
//            while (pp.hasNext()) {
//                byte[] p = pp.next();
//                if (p.length < 2)
//                    pp.remove();;
//                for (byte[] r : paths)
//                    if (r.length < 2)
//                        return null; //substitution would replace something at the top level of a statement}
//            }
//        }


        //use the innermost common parent to decide the type of variable.
        //in case there is no other common parent besides input itself, use that.
        Term commonParent = input.commonParent(paths);
        Op commonParentOp = commonParent.op();

        //decide what kind of variable can be introduced according to the input operator
        boolean depOrIndep;
        switch (commonParentOp) {
            case CONJ:
                depOrIndep = true;
                break;
            case IMPL:
                depOrIndep = false;
                break;
            default:
                return null; //????
                //throw new UnsupportedOperationException();
        }


        ObjectByteHashMap<Term> m = new ObjectByteHashMap<>(0);
        for (int path = 0; path < pSize; path++) {
            ByteList p = paths.get(path);
            Term t = null; //root
            int pathLength = p.size();
            for (int i = -1; i < pathLength-1 /* dont include the selected term itself */; i++) {
                t = (i == -1) ? input : t.sub(p.get(i));
                Op o = t.op();

                if (!depOrIndep && validIndepVarSuperterm(o)) {
                    byte inside = (byte) (1 << p.get(i + 1));
                    m.updateValue(t, inside, (previous) -> (byte) ((previous) | inside));
                } else if (depOrIndep && validDepVarSuperterm(o)) {
                    m.addToValue(t, (byte) 1);
                }
            }
        }


        if (!depOrIndep) {
            //at least one impl/equiv must have both sides covered
            return (m.anySatisfy(b -> b == 0b11)) ?
                    $.v(VAR_INDEP, order) /*varIndep(order)*/ : null;

        } else {
            //at least one conjunction must contain >=2 path instances
            return m.anySatisfy(b -> b >= 2) ?
                    $.v(VAR_DEP, order)  /* $.varDep(order) */ : null;
        }

    }

    public static boolean validDepVarSuperterm(Op o) {
        return /*o.statement ||*/ o == CONJ;
    }

    public static boolean validIndepVarSuperterm(Op o) {
        return o.statement;
        //return o == IMPL || o == EQUI;
    }


}

//        protected boolean introduce(Term x) {
//            return true;
////            return x instanceof Compound &&
////                   introducer.rng.nextFloat() <=
////                            1f / (1 + x.size())
////                           //1f / Math.sqrt(x.volume())
////                           //1f / x.volume()
////                           //0.5f;
////                           //(1f / (1f + x.size()))
////            ;
//        }

