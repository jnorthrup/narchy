package nars.derive.rule;

import com.google.common.collect.Lists;
import jcog.data.list.FasterList;

import java.util.List;

public abstract class Rules extends FasterList<PremiseRuleBuilder0> {

    public Rules() {
        super(64);
    }

    public PremiseRuleBuilder0 rule(String taskPattern, String beliefPattern, String concPattern) {
        PremiseRuleBuilder0 p = new PremiseRuleBuilder0(taskPattern, beliefPattern, concPattern);
        add(p);
        return p;
    }

    public List<PremiseRuleBuilder> get() {
        return Lists.transform(this, x->x.get());
    }
}
