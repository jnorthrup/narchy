package nars.op;

import jcog.bag.impl.CurveBag;
import jcog.data.ArrayHashSet;
import jcog.math.random.SplitMix64Random;
import jcog.pri.Pri;
import jcog.pri.PriReference;
import jcog.pri.op.PriMerge;
import nars.NAR;
import nars.concept.Concept;
import nars.concept.PermanentConcept;
import nars.exe.Causable;
import nars.link.TaskLink;
import nars.op.stm.STMLinkage;
import nars.table.BeliefTable;
import nars.table.DefaultBeliefTable;
import nars.table.TaskTable;
import nars.term.Term;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.function.Consumer;

import static nars.Op.*;

/**
 * generic graph walker that executes a variety of tasks it can choose from
 * as it navigates the concept graph while also building an annotated
 * internal model of it that includes features not explicitly represented
 * in the basic NARS architecture
 */
public class Spider extends Causable {

    private static final Logger logger = LoggerFactory.getLogger(Spider.class);
    final ArrayHashSet<Term> roots = new ArrayHashSet<>();
    /**
     * high-speed local rng; re-seeded on starting()
     */
    final SplitMix64Random rng = new SplitMix64Random(1);
    final CurveBag<SpiderAction> actions = new CurveBag<>(PriMerge.replace, new HashMap());
    /**
     * location currently centered upon
     */
    Concept at = null;


    public Spider(NAR n, Iterable<Term> initialRoots) {
        super(n);
        initialRoots.forEach(roots::add);

        actions.setCapacity(32);
        actions.put(new TravelHome(0.01f));
        actions.put(new TravelTermlink(0.02f));
        actions.put(new TravelTasklink(0.02f));
        actions.put(new TravelAnon(0.1f));
        actions.put(new DeleteConcept(0.01f));

        for (byte p: new byte[]{BELIEF, GOAL, QUESTION, QUEST}) {
            actions.put(new ClearTaskTable(0.02f, p));
            actions.put(new SqueezeTaskTable(0.1f, p, 0.9f)); //soft
            actions.put(new SqueezeTaskTable(0.1f, p, 0.7f)); //strong
        }

    }

    @Override
    protected void starting(NAR nar) {
        super.starting(nar);
        rng.setSeed(nar.random().nextLong());
    }

    @Override
    protected int next(NAR n, int iterations) {
        assert (!actions.isEmpty());

        if (at == null || at.isDeleted())
            home();
        if (at == null || at.isDeleted())
            return -1; //no option

        actions.sample(rng, iterations, (a) -> {
            a.accept(this);
        });

        return iterations;
    }

    @Override
    public float value() {
        return 0;
    }

    public void go(Concept c) {
        logger.info("@{}", at);
        at = c;

    }

    /**
     * select a random root and go there
     */
    public Concept home() {


        for (int retry = 0; retry < roots.size(); retry++) {
            Term x = roots.get(rng);
            Concept c = nar.conceptualize(x);
            if (c != null) {
                go(c);
                return c;
            }
        }

        return null;
    }

    abstract static class SpiderAction extends Pri implements Consumer<Spider> {

        public SpiderAction(float p) {
            super(p);
        }

    }

    private class TravelHome extends SpiderAction {

        public TravelHome(float p) {
            super(p);
        }

        @Override
        public void accept(Spider spider) {
            home();
        }
    }

    /**
     * travel to the anon meta-concept of the current term (if not already anon),
     * creating a new concept if necessary and linking to it
     */
    private class TravelAnon extends SpiderAction {

        public TravelAnon(float p) {
            super(p);
        }

        @Override
        public void accept(Spider spider) {
            Concept c = at;
            if (c.term().op().atomic)
                return; //no

            Term ct = at.term();
            Term anon = ct.anon();
            if (anon.equals(ct))
                return; //already at anon


            Concept d = nar.conceptualize(anon);
            if (d!=null) {
                go(d);
                STMLinkage.link(c, nar.priDefault(BELIEF), d, nar);
            }

        }
    }

    private class TravelTermlink extends SpiderAction {

        public TravelTermlink(float p) {
            super(p);
        }

        @Override
        public void accept(Spider spider) {
            Concept c = at;
            if (c == null)
                return;
            Term t = sample(c);
            if (t != null) {
                Concept d = nar.conceptualize(t);
                if (d != null && c != d)
                    go(d);
            }
        }

        protected Term sample(Concept c) {
            PriReference<Term> tl = c.termlinks().sample(rng);
            return tl != null ? tl.get() : null;
        }
    }

    private class TravelTasklink extends TravelTermlink {

        public TravelTasklink(float p) {
            super(p);
        }

        @Override
        protected Term sample(Concept c) {
            TaskLink tl = c.tasklinks().sample(rng);
            return tl != null ? tl.term() : null;
        }
    }

    private class DeleteConcept extends SpiderAction {

        public DeleteConcept(float p) {
            super(p);
        }

        @Override
        public void accept(Spider spider) {
            Concept c = at;
            if (!(c instanceof PermanentConcept)) {
                c.delete(nar);
            }
        }
    }

    private class ClearTaskTable extends SpiderAction {

        private final byte punc;

        public ClearTaskTable(float p, byte punc) {
            super(p);
            this.punc = punc;
        }

        @Override
        public void accept(Spider spider) {
            Concept c = at;
            if (!(c instanceof PermanentConcept)) {
                c.table(punc).clear();
            }
        }
    }

    /**
     * reduce capacity by some amount causing tasks to get merged,
     * dropped, or eternalized. for now this only applies to temporal tables
     */
    private class SqueezeTaskTable extends SpiderAction {

        private final byte punc;
        private final float ratio;

        public SqueezeTaskTable(float p, byte punc, float capacityRatio) {
            super(p);
            this.punc = punc;
            this.ratio = capacityRatio;
        }

        @Override
        public void accept(Spider spider) {
            Concept c = at;

            TaskTable table = c.table(punc);
            if (table instanceof DefaultBeliefTable) {
                BeliefTable tt = (BeliefTable) table;
                int s = table.size();
                if (s > 0) {
                    int ss = (int) Math.max(1, Math.floor(ratio * s));
                    if (ss != s) {
                        tt.setCapacity(((DefaultBeliefTable) tt).eternal.capacity() /* dont affect eternal */,
                                ss);
                    }
                }
            }
        }


    }


}
