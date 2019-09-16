package nars.derive;

import jcog.time.UsageNS;
import nars.derive.op.Truthify;
import nars.derive.rule.RuleWhy;
import nars.term.control.PREDICATE;
import org.HdrHistogram.AtomicHistogram;

public class DeriveActionProfiled extends DeriveAction {

    static final UsageNS<DeriveActionProfiled> usage = new UsageNS();
    final AtomicHistogram meter;

    public DeriveActionProfiled(RuleWhy cause, Truthify truthify, PREDICATE<Derivation> yy) {
        super(cause, truthify, yy);
        meter = usage.the(this);
    }

    @Override
    public boolean run(PostDerivable p) {
        long start = System.nanoTime();
        boolean r = super.run(p);
        long end = System.nanoTime();
        meter.recordValue(end-start);
        return r;
    }
}
