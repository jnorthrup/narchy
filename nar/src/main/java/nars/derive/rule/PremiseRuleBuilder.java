package nars.derive.rule;

import nars.$;
import nars.term.Term;

import java.util.Set;
import java.util.TreeSet;

import static nars.$.$$;

public class PremiseRuleBuilder {

    public final Term taskPattern, beliefPattern;
    private final Term concPattern;
    final Set<Term> modifiers = new TreeSet();

    public PremiseRuleBuilder(String taskPattern, String beliefPattern, String concPattern) {
        this.taskPattern = $$(taskPattern);
        this.beliefPattern = $$(beliefPattern);
        this.concPattern = $$(concPattern);
    }

    public PremiseRule get() {
        return new PremiseRule(this);
    }

    /** the id/label for the premise rule */
    public Term term() {
        return $.p(taskPattern, beliefPattern, $.p(modifiers), concPattern); //TODO more
    }

    public PremiseRuleBuilder hasBelief() {
        modifiers.add($$("hasBelief()"));
        return this;
    }

    public PremiseRuleBuilder believe(byte taskIn, String truthFunction /* [timing] */) {
        modifiers.add($$("task(" + taskIn + ")")); //TODO
        //TODO
        return this;
    }
}
