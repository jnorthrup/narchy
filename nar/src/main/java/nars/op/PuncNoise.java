//package nars.op;
//
//import jcog.math.FloatRange;
//import jcog.math.IntRange;
//import jcog.pri.op.PriMerge;
//import nars.$;
//import nars.NAR;
//import nars.attention.TaskLinkWhat;
//import nars.link.AtomicTaskLink;
//import nars.link.TaskLink;
//import nars.link.TaskLinkBag;
//import nars.term.Term;
//import nars.time.part.DurLoop;
//
//import static nars.Op.IMPL;
//import static nars.Op.VAR_QUERY;
//import static nars.Task.p;
//
///**
// * randomly shifts tasklink punctuation
// * TODO make this a How */
//public class PuncNoise extends DurLoop {
//    public final IntRange tasklinksPerDuration = new IntRange(8, 1, 128);
//    public final FloatRange strength = new FloatRange(0.05f, 0, 1f);
//
//    public PuncNoise(NAR n) {
//        super($.p(n.self(), $.identity(PuncNoise.class)));
//        n.add(this);
//    }
//
//    @Override
//    protected void run(NAR n, long dt) {
//        n.what.forEach(ww -> {
//           if (ww instanceof TaskLinkWhat) {
//               TaskLinkBag b = ((TaskLinkWhat)ww).links.links;
//               if (b!=null) {
//                   int i = Math.min(b.size(), tasklinksPerDuration.intValue());
//                   if (i > 0)
//                       b.sample(n.random(), i, this::noise);
//               }
//           }
//        });
//    }
//
//    protected void noise(TaskLink t) {
//        //TODO this is actually a 'blur' effect
//        float s = strength.floatValue();
//        //TODO fully atomic
//        float before = t.pri();
//        float after = t.priMult(1-s);
//        float delta = before-after;
//        if (delta != delta)
//            return;
//
//        Term f = t.from();
//        boolean noGoalOrQuest = f.op() == IMPL;
//        boolean noBeliefOrGoal = f.hasAny(VAR_QUERY);
//        int div = 4;
//        if (noGoalOrQuest)
//            div -= 2;
//        if (noBeliefOrGoal)
//            div -= 2;
//        if (div <= 1)
//            return;
//        float amt = delta / div;
//        for (int i = 0; i < 4; i++) {
//            if (noGoalOrQuest && (i == 2 || i == 3))
//                continue;
//            if (noBeliefOrGoal && (i == 0 || i == 2))
//                continue;
//            ((AtomicTaskLink) t).mergeComponent(p(i), amt, PriMerge.plus);
//        }
//
//    }
//}
