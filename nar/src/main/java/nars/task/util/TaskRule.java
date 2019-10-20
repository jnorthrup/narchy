package nars.task.util;

import nars.*;
import nars.term.Compound;
import nars.term.Term;
import nars.term.Variable;
import nars.term.atom.Bool;
import nars.term.util.TermException;
import nars.term.util.transform.VariableNormalization;
import nars.unify.Unify;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;

import static nars.term.Terms.compoundOrNull;

/**
 * matches a belief pattern and creates an identity result
 */
public class TaskRule extends TaskMatch {

    private static final Logger logger = LoggerFactory.getLogger(TaskRule.class);

    /** the output pattern */
    private final Compound output;

    private final Term input;
    private final Term id;

    public TaskRule(String input, String output, NAR nar) throws Narsese.NarseseException {
        super(nar);

        this.input = $.$(input);
        /** version of output with the original GenericVariable's, for display or reference purposes */
        var outputRaw = (Compound) Narsese.term(output, false);

        var varNorm = new VariableNormalization(outputRaw.subs() /* est */, 0);

        this.output = compoundOrNull(varNorm.applyCompound(outputRaw));
        if (this.output == null)
            throw new RuntimeException("output pattern is not compound");

        /** mapping of input variables to normalized variables */
        var io = varNorm.map;
        this.id = $.impl($.p(this.input, outputRaw, this.output), $.varQuery("what")).normalize();










    }

    private class MySubUnify extends Unify {

        private final Task x;

        MySubUnify(Task x) {
            super(Op.VAR_PATTERN, TaskRule.this.nar.random(), NAL.unify.UNIFICATION_STACK_CAPACITY,
                    nar.deriveBranchTTL.intValue() //HACK
            );
            this.x = x;
        }

        @Override
        public boolean match() {
            accept(x, xy);
            return false; //done
        }

    }

    @Override
    public boolean test(Task x) {
        if (super.test(x)) {

            var match = new MySubUnify(x);

            try {
                match.unify(input, x.term());
            } catch (TermException | TaskException e) {
                onError(e);
            }

            return true;
        }

        return false;
    }

    
    @Override
    public String toString() {
        return id.toString();
    }

    @Override
    protected void accept(Task X, Map<Variable, Term> xy) {

        var y = output.replace(xy);
        if (y instanceof Variable || y instanceof Bool) return;

        




































        y = compoundOrNull(y);
        if (y==null) return;

        y = y.normalize();
        if (y==null) return;

        if (!Task.validTaskTerm(y, X.punc(), false))
            return;

        var Y = Task.clone(X, y);
        if (Y != null) {
            logger.info("{}\t{}", X, Y);
            nar.input(Y);
        }
    }

}
