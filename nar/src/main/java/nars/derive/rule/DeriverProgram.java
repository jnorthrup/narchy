package nars.derive.rule;

import jcog.Texts;
import nars.NAR;
import nars.Op;
import nars.control.Cause;
import nars.derive.PreDerivation;
import nars.derive.PreDeriver;
import nars.derive.action.How;
import nars.derive.action.PatternHow;
import nars.derive.util.Forkable;
import nars.term.control.AND;
import nars.term.control.FORK;
import nars.term.control.PREDICATE;
import nars.term.control.SWITCH;

import java.io.PrintStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * compiled derivation rules
 * what -> can
 * TODO subclass to Weighted deriver runner; and make a non-weighted subclass
 */
public class DeriverProgram {

    public final PREDICATE<PreDerivation> what;

    /**
     * the causes that this is responsible for, ie. those that may be caused by this
     */
    /*@Stable*/ public final Cause[] cause;

    /*@Stable*/ public final How[] branch;

    public final PreDeriver pre;
    public final NAR nar;


    DeriverProgram(final PREDICATE<PreDerivation> what, final How[] actions, final PreDeriver pre, final NAR nar) {

        this.nar = nar;

        this.what = what;

        this.branch = actions; assert (actions.length > 0);

        final List<RuleCause> list = new ArrayList<>();
        for (final How b : actions) {
            for (final RuleCause ruleCause : Arrays.asList(b.why)) {
                list.add(ruleCause);
            }
        }
        this.cause = list.toArray(new Cause[0]);

        this.pre = pre;
    }


    public DeriverProgram print() {
        return print(System.out);
    }

    public DeriverProgram print(final PrintStream p) {
        print(p, 0);
        return this;
    }

    public void print(final PrintStream p, final int indent) {
        print(what, p, indent);
    }

    protected void print(final Object x, final PrintStream out, final int indent) {

        Texts.indent(indent);

        if (x instanceof DeriverProgram) {

            final DeriverProgram r = (DeriverProgram) x;
            r.print(out, indent);

        } else if (x instanceof Forkable) {

            final Forkable b = (Forkable)x;

            out.println(b.getClass().getSimpleName().toLowerCase() + " {");
            for (final short c : b.can) {
                print(branch[(int) c], out, indent+2);
            }
            Texts.indent(indent);out.println("}");


        } else if (x instanceof How) {
            final How a = (How)x;

            out.println(a.why.id + " ==> {");
            // + ((PremisePatternAction.TruthifyDeriveAction) a).unify;
            //TODO
            //                out.println(((DirectPremiseUnify)x).taskPat + ", " + ((DirectPremiseUnify)x).beliefPat + " ==> {");
            //                print(((DirectPremiseUnify)x).taskify, out, indent + 2);
            //                Texts.indent(indent);
            //                out.println("}");
            final Object aa = a instanceof PatternHow.TruthifyDeriveAction ? Arrays.toString(((PatternHow.TruthifyDeriveAction) a).constraints) + " ..." : a.toString();

            print(aa, out, indent + 2);

            Texts.indent(indent);out.println("}");

        } else if (x instanceof AND) {
            out.println("and {");
            final AND ac = (AND) x;
            ac.subStream().forEach(b->
                print(b, out, indent + 2)
            );
            Texts.indent(indent);
            out.println("}");
        } /*else if (p instanceof Try) {
            out.println("eval {");
            Try ac = (Try) p;
            int i = 0;
            for (PrediTerm b : ac.branches) {
                TermTrie.indent(indent + 2);
                out.println(i + ":");
                print(b, out, indent + 4);
                i++;
            }
            TermTrie.indent(indent);
            out.println("}");
        } */ else if (x instanceof FORK) {

            out.println("fork {");
            for (final PREDICATE b : ((FORK) x).branch)
                print(b, out, indent + 2);
            Texts.indent(indent);
            out.println("}");

        } else if (x instanceof SWITCH) {
            final SWITCH sw = (SWITCH) x;
            out.println("switch(op(" + (sw.taskOrBelief ? "task" : "belief") + ")) {");
            int i = -1;
            for (final PREDICATE b : sw.swtch) {
                i++;
                if (b == null) continue;

                Texts.indent(indent + 2);
                out.println('"' + Op.values()[i].toString() + "\": {");
                print(b, out, indent + 4);
                Texts.indent(indent + 2);
                out.println("}");

            }
            Texts.indent(indent);
            out.println("}");
        } else {
            out.print( /*Util.className(p) + ": " +*/ x);
            out.println();
        }


    }


}
