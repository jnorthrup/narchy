package nars.derive.rule;

import jcog.time.UsageNS;
import nars.derive.model.Derivation;
import nars.derive.op.Truthify;
import nars.term.control.PREDICATE;
import org.HdrHistogram.AtomicHistogram;

public class DeriveActionProfiled extends DeriveAction {

    static final UsageNS<DeriveActionProfiled> usage = new UsageNS();
    final AtomicHistogram meter;

    DeriveActionProfiled(PremiseRuleProto.RuleWhy cause, Truthify truthify, PREDICATE<Derivation> yy) {
        super(cause, truthify, yy);
        meter = usage.the(this);
    }

    @Override
    public boolean test(Derivation d) {
        long start = System.nanoTime();
        boolean r = super.test(d);
        long end = System.nanoTime();
        meter.recordValue(end-start);
        return r;
    }
}
