package nars.unify.instrument;

import jcog.Texts;
import nars.derive.Derivation;
import nars.derive.action.op.PremiseUnify;
import nars.term.control.AND;
import nars.term.control.FORK;
import nars.term.control.PREDICATE;
import org.fusesource.jansi.Ansi;

public class DebugDerivationPredicate extends InstrumentedDerivationPredicate {

    public DebugDerivationPredicate(PREDICATE<Derivation> inner) {
        super(inner);
    }

    @Override
    protected void onEnter(PREDICATE<Derivation> p, Derivation d) {

    }

    @Override
    protected void onExit(PREDICATE<Derivation> p, Derivation d, boolean returnValue, Throwable thrown, long nanos) {
        if (p instanceof FORK || p instanceof AND) {

        } else {

            Ansi.Color fg;
            if (p instanceof PremiseUnify) {
                fg = Ansi.Color.MAGENTA;
            } else {
                fg = Ansi.Color.YELLOW;
            }
            Ansi ansi = Ansi.ansi();
            String pAnsi = ansi
                    .a(Texts.iPad(nanos, 6) + "nS ")
                    .a(' ')
                    .a(d.toString())
                    .a(' ')
                    
                    .fg(fg).a(p.toString()).fg(Ansi.Color.DEFAULT)
                    .a(' ')
                    .a((thrown != null ? (" "+thrown) : ' '))
                    .fg(returnValue ? Ansi.Color.GREEN : Ansi.Color.RED)
                    
                    .a(returnValue ? "TRUE" : "FALS").fg(Ansi.Color.DEFAULT).newline().toString();

            System.out.print(pAnsi);
        }
    }
}
