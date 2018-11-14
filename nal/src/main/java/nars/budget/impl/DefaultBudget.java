package nars.budget.impl;

import nars.budget.Activator;
import nars.budget.Budget;
import nars.budget.Forgetting;
import nars.budget.derive.DefaultPuncWeightedDeriverBudget;

public class DefaultBudget extends Budget {

    public DefaultBudget() {
        super(new DefaultPuncWeightedDeriverBudget(), new Activator(true), new Forgetting());
    }
}
