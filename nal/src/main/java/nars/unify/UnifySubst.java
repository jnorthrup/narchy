package nars.unify;

import nars.Op;
import nars.Param;
import nars.term.Term;
import nars.term.Termed;
import nars.util.TimeAware;

import java.util.function.Predicate;

/**
 * not thread safe, use 1 per thread (do not interrupt matchAll)
 */
public class UnifySubst extends Unify {

    //static final Logger logger = LoggerFactory.getLogger(UnifySubst.class);

    /*@NotNull*/
    public final TimeAware timeAware;


    final Predicate<Term> target;
    private Term a;
    int matches = 0;

    public UnifySubst(Op varType, /*@NotNull*/ TimeAware n, Predicate<Term> target, int ttl) {
        super(varType, n.random(), Param.UnificationStackMax, ttl);
        this.timeAware = n;
        this.target = target;
    }

    @Override
    public UnifySubst unify(/*@NotNull*/ Term x, /*@NotNull*/ Term y, boolean finish) {
        this.a = y;
        super.unify(x, y, finish);
        return this;
    }


    @Override
    public void tryMatch() {

        //TODO combine these two blocks to use the same sub-method

        //try {


        //try {

        //        } catch (InvalidTermException e) {
//            return null;
//        }
        Termed aa = apply(a); //a.replace(xy);
        if (aa != null) {
            Term aaa = aa.term();
            if (aaa.op().conceptualizable) {
                matches++;
                if (!target.test(aaa)) {
                    stop();
                }
            }
        }


//        }
//        catch (InvalidTermException e) {
//            if (Param.DEBUG)
//                logger.warn("{}",e);
//
//        }

//        if ((aa == null) ||
//        //Op aaop = aa.op();
//        //only set the values if it will return true, otherwise if it returns false the callee can expect its original values untouched
//            ((a.op() == Op.VAR_QUERY) && (aa.op().in(Op.VarDepOrIndep)))
//         ) {
//            return false;
//        }

//        Term bb = applySubstituteAndRenameVariables(b, yx);
//        if (bb == null) return false;
//        Op bbop = bb.op();
//        if (bbop == Op.VAR_QUERY && (bbop == Op.VAR_INDEP || bbop == Op.VAR_DEP))
//            return false;


        //return matches < maxMatches; //determines how many
    }

    public int matches() {
        return matches;
    }

}
