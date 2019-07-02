package nars.derive.rule;

import com.google.common.collect.Lists;
import jcog.data.list.FasterList;

import java.util.List;

public abstract class Rules extends FasterList<PremiseRuleBuilder> {

    public Rules() {
        super(64);
    }

    public PremiseRuleBuilder rule(String taskPattern, String beliefPattern, String concPattern) {
        PremiseRuleBuilder p = new PremiseRuleBuilder(taskPattern, beliefPattern, concPattern);
        add(p);
        return p;
    }

    public List<PremiseRule> get() {
        return Lists.transform(this, x->x.get());
    }
}
