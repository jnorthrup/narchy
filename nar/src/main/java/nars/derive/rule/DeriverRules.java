package nars.derive.rule;

import nars.control.Why;
import nars.derive.Derivation;
import nars.derive.DeriveAction;
import nars.derive.PreDeriver;
import nars.term.control.PREDICATE;

import java.io.PrintStream;
import java.util.stream.Stream;

/**
 * compiled derivation rules
 * what -> can
 * TODO subclass to Weighted deriver runner; and make a non-weighted subclass
 */
public class DeriverRules {

    public final PREDICATE<Derivation> what;

    /**
     * the causes that this is responsible for, ie. those that may be caused by this
     */
    /*@Stable*/ public final Why[] why;

    public final DeriveAction[] branch;

    public final PreDeriver pre;


    DeriverRules(PREDICATE<Derivation> what, DeriveAction[] actions, PreDeriver pre) {

        this.what = what;

        this.branch = actions; assert (actions.length > 0);

        this.why = Stream.of(actions).flatMap(b -> Stream.of(b.why)).toArray(Why[]::new);

        this.pre = pre;
    }

    /**
     * the conclusions that in which this deriver can result
     */
    public Why[] causes() {
        return why;
    }

    public void printRecursive() {
        printRecursive(System.out);
    }

    public void printRecursive(PrintStream p) {
        PremiseRuleCompiler.print(what, p);
        for (Object x : branch)
            PremiseRuleCompiler.print(x, p);
    }


    public void print(PrintStream p) {
        print(p, 0);
    }

    public void print(PrintStream p, int indent) {
        PremiseRuleCompiler.print(what, p, indent);
        for (Object x : branch)
            PremiseRuleCompiler.print(x, p, indent);
    }


}
