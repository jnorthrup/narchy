package nars.task.util;

import nars.*;
import nars.term.Compound;
import nars.term.Term;
import nars.term.Variable;
import nars.term.atom.Bool;
import nars.unify.Unify;
import nars.util.term.transform.VariableNormalization;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;

import static nars.term.Terms.compoundOrNull;

/**
 * matches a belief pattern and creates an identity result
 */
public class TaskRule extends TaskMatch {

    static final Logger logger = LoggerFactory.getLogger(TaskRule.class);

    /** the output pattern */
    public final Compound output;

    /** version of output with the original GenericVariable's, for display or reference purposes */
    private final Compound outputRaw;

    /** mapping of input variables to normalized variables */
    private final Map<Variable, Variable> io;

    private final Term input;
    private final Term id;

    public TaskRule(String input, String output, NAR nar) throws Narsese.NarseseException {
        super(nar);

        this.input = $.$(input);
        this.outputRaw = (Compound) Narsese.term(output, false);

        VariableNormalization varNorm = new VariableNormalization(outputRaw.subs() /* est */, 0);

        this.output = compoundOrNull(outputRaw.transform(varNorm));
        if (this.output == null)
            throw new RuntimeException("output pattern is not compound");

        this.io = varNorm.map;
        this.id = $.impl($.p(this.input, outputRaw, this.output), $.varQuery("what")).normalize();

//        setTerm(new TermMatch(input) {
//
//            @Override
//            public boolean test(Term p) {
//
//                return true;
//            }
//        });

    }

    private class MySubUnify extends Unify {

        private final Task x;

        public MySubUnify(Task x) {
            super(Op.VAR_PATTERN, TaskRule.this.nar.random(), Param.UnificationStackMax, nar.deriveTTL.intValue());
            this.x = x;
        }

        @Override
        public void tryMatch() {
            accept(x, xy);
            stop();
        }

    }

    @Override
    public boolean test(Task x) {
        if (super.test(x)) {

            final MySubUnify match = new MySubUnify(x);

            try {
                match.unify(input, x.term(), true);
            } catch (Term.InvalidTermException | InvalidTaskException e) {
                onError(e);
            }

            return true;
        }

        return false;
    }

    @NotNull
    @Override
    public String toString() {
        return id.toString();
    }

    @Override
    protected void accept(Task X, Map<Term, Term> xy) {

        Term y = output.replace(xy);
        if (y instanceof Variable || y instanceof Bool) return;

        //        if (r == null)
//            return null;
//
//        //unnegate and check for an apparent atomic term which may need decompressed in order to be the task's content
//        boolean negated;
//        Term s = r;
//        if (r.op() == NEG) {
//            s = r.unneg();
//            if (s instanceof Variable)
//                return null; //throw new InvalidTaskException(r, "unwrapped variable"); //should have been prevented earlier
//
//            negated = true;
//            if (s instanceof Compound) {
//                return (Compound) r; //its normal compound inside the negation, handle it in Task constructor
//            }
//        } else if (r instanceof Compound) {
//            return (Compound) r; //do not uncompress any further
//        } else if (r instanceof Variable) {
//            return null;
//        } else {
//            negated = false;
//        }
//
//        if (!(s instanceof Compound)) {
//            Compound t = compoundOrNull(nar.post(s));
//            if (t == null)
//                return null; //throw new InvalidTaskException(r, "undecompressible");
//            else
//                return (Compound) $.negIf(t, negated); //done
////            else
////            else if (s.op()==NEG)
////                return (Compound) $.negIf(post(s.unneg(), nar));
////            else
////                return (Compound) $.negIf(s, negated);
//        }
//        //its a normal negated compound, which will be unnegated in task constructor
//        return (Compound) s;
        y = compoundOrNull(y);
        if (y==null) return;

        y = y.normalize();
        if (y==null) return;

        if (!Task.validTaskTerm(y, X.punc(), false))
            return;

        Task Y = Task.clone(X, y);
        if (Y != null) {
            logger.info("{}\t{}", X, Y);
            nar.input(Y);
        }
    }

}
