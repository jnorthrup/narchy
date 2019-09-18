package nars.derive.rule;

import jcog.Texts;
import nars.NAR;
import nars.Op;
import nars.control.Why;
import nars.derive.Derivation;
import nars.derive.PreDeriver;
import nars.derive.action.PatternPremiseAction;
import nars.derive.action.PremiseAction;
import nars.derive.op.FORKABLE;
import nars.derive.op.PremiseUnify;
import nars.term.control.AND;
import nars.term.control.FORK;
import nars.term.control.PREDICATE;
import nars.term.control.SWITCH;

import java.io.PrintStream;
import java.util.stream.Stream;

/**
 * compiled derivation rules
 * what -> can
 * TODO subclass to Weighted deriver runner; and make a non-weighted subclass
 */
public class DeriverProgram {

    public final PREDICATE<Derivation> what;

    /**
     * the causes that this is responsible for, ie. those that may be caused by this
     */
    /*@Stable*/ public final Why[] why;

    public final PremiseAction[] branch;

    public final PreDeriver pre;
    public final NAR nar;


    DeriverProgram(PREDICATE<Derivation> what, PremiseAction[] actions, PreDeriver pre, NAR nar) {

        this.nar = nar;

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



    public void print() {
        print(System.out);
    }

    public void print(PrintStream p) {
        print(p, 0);
    }

    public void print(PrintStream p, int indent) {
        print(what, p, indent);
    }

    protected void print(Object x, PrintStream out, int indent) {

        Texts.indent(indent);

        if (x instanceof DeriverProgram) {

            DeriverProgram r = (DeriverProgram) x;
            r.print(out, indent);

        } else if (x instanceof FORKABLE) {

            FORKABLE b = (FORKABLE)x;

            out.println(b + " {");
            for (short c : b.can) {
                print(branch[c], out, indent+2);
            }
            Texts.indent(indent);out.println("}");


        } else if (x instanceof PremiseAction) {
            PremiseAction a = (PremiseAction)x;

            out.println(a.why.id + " ==> {");
            Object aa = a;
            if (a instanceof PatternPremiseAction.TruthifyDeriveAction)
                aa = ((PatternPremiseAction.TruthifyDeriveAction)a).action;
            else
                aa = a.toString();

            print(aa, out, indent + 2);

            Texts.indent(indent);out.println("}");

        } else if (x instanceof PremiseUnify) {
            out.println(((PremiseUnify)x).term() + " ==> {");
            print(((PremiseUnify)x).taskify, out, indent + 2);
            Texts.indent(indent);
            out.println("}");
        } else if (x instanceof AND) {
            out.println("and {");
            AND ac = (AND) x;
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
            for (PREDICATE b : ((FORK) x).branch)
                print(b, out, indent + 2);
            Texts.indent(indent);
            out.println("}");

        } else if (x instanceof SWITCH) {
            SWITCH sw = (SWITCH) x;
            out.println("switch(op(" + (sw.taskOrBelief ? "task" : "belief") + ")) {");
            int i = -1;
            for (PREDICATE b : sw.swtch) {
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