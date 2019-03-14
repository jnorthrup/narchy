package nars.op;

import jcog.math.FloatRange;
import jcog.math.IntRange;
import jcog.pri.op.PriMerge;
import nars.NAR;
import nars.time.event.DurService;
import nars.link.TaskLink;
import nars.link.TaskLinkBag;

import static nars.Task.p;

/** randomly shifts tasklink punctuation */
public class PuncNoise extends DurService {
    public final IntRange tasklinksPerDuration = new IntRange(32, 1, 128);
    public final FloatRange strength = new FloatRange(0.5f, 0, 1f);

    public PuncNoise(NAR n) {
        super(n);
    }

    @Override
    protected void run(NAR n, long dt) {
        TaskLinkBag b = nar.attn.links;
        if (b!=null) {
            int i = Math.min(b.size(), tasklinksPerDuration.intValue());
            if (i > 0)
                b.sample(n.random(), i, this::noise);
        }
    }

    protected void noise(TaskLink t) {
        //TODO this is actually a 'blur' effect
        float s = strength.floatValue();
        //TODO fully atomic
        float before = t.pri();
        float after = t.priMult(1-s);
        float delta = after-before;
        if (delta == delta) {
            float amt = delta / 4;
            for (int i = 0; i < 4; i++) {
                ((TaskLink.GeneralTaskLink) t).priMerge(p(i), amt, PriMerge.plus);
            }
        }

    }
}
