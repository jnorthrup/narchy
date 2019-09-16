package nars.derive.rule;

import nars.$;
import nars.term.Term;

import java.util.Set;
import java.util.TreeSet;

import static nars.$.$$;

public class PremiseRuleBuilder0 {

    public final Term taskPattern, beliefPattern;
    public final Term concPattern;

    final Set<Term> modifiers = new TreeSet();

    public PremiseRuleBuilder0(String taskPattern, String beliefPattern, String concPattern) {
        this($$(taskPattern), $$(beliefPattern), $$(concPattern));
    }
    public PremiseRuleBuilder0(Term taskPattern, Term beliefPattern, Term concPattern) {
        this.taskPattern = taskPattern;
        this.beliefPattern = beliefPattern;
        this.concPattern = concPattern;
    }

    public PremiseRuleBuilder get() {
        return new PremiseRuleBuilder(this);
    }

    /** the id/label for the premise rule */
    public Term term() {
        return $.p(taskPattern, beliefPattern, $.p(modifiers), concPattern); //TODO more
    }

    public PremiseRuleBuilder0 hasBelief() {
        modifiers.add($$("hasBelief()"));
        return this;
    }

    public PremiseRuleBuilder0 believe(byte taskIn, String truthFunction /* [timing] */) {
        modifiers.add($$("task(" + taskIn + ")")); //TODO
        //TODO
        return this;
    }
}
