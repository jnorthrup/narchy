package nars.op;

import jcog.bag.Bag;
import jcog.bag.Sampler;
import jcog.bag.impl.CurveBag;
import jcog.data.ArrayHashRing;
import jcog.data.ArrayHashSet;
import jcog.math.random.SplitMix64Random;
import jcog.pri.Pri;
import jcog.pri.PriReference;
import jcog.pri.op.PriMerge;
import nars.NAR;
import nars.Task;
import nars.concept.Concept;
import nars.concept.PermanentConcept;
import nars.exe.Causable;
import nars.link.TaskLink;
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
 *
 * TODO needs to be thread-isolated
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
     * leaky novelty buffer
     */
    final ArrayHashRing<Term> visited = new ArrayHashRing(256);
    /**
     * location currently centered upon
     */
    Concept at = null;

    public Spider(NAR n, Iterable<Term> initialRoots) {
        super(n);
        initialRoots.forEach(roots::add);

        actions.setCapacity(32);

        //actions.put(new DeleteConcept(0.0001f));

        actions.put(new TravelTermlink(0.9f));
        actions.put(new TravelTasklink(0.9f));

        actions.put(new SqueezeTaskLinks(0.1f, 0.1f)); //soft
        for (byte p: new byte[]{BELIEF, GOAL, QUESTION, QUEST}) {
            //actions.put(new ClearTaskTable(0.00001f, p));
            actions.put(new SqueezeTaskTable(0.1f, p, 0.1f)); //soft
        }

        actions.put(new TravelAnon(0.001f));

        actions.put(new TravelHome(0.0001f));

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
            next();
        if (at == null || at.isDeleted())
            return -1; //no option

        actions.sample(rng, iterations, (a) -> {
            //System.out.println(at + " "  +a);
            a.accept(this);
        });

        return iterations;
    }

    @Override
    public float value() {
        return 0;
    }

    public boolean go(Concept c, boolean ifNotVisited) {
        if (!ifNotVisited) {
            visited.add(c.term());
        } else {
            if (!visited.add(c.term()))
                return false;
        }
        at = c;
        //logger.info("@{}", at);
        return true;
    }

    /**
     * select a random root and go there
     */
    public Concept next() {


        for (int retry = 0; retry < roots.size(); retry++) {
            //Term x = roots.get(rng);
            Term x = visited.isEmpty() ?  roots.get(rng) : visited.get(rng);
            if (x!=null) {
                Concept c = nar.conceptualize(x);
                if (c != null) {
                    go(c, false);
                    return c;
                }
            }
        }

        return null;
    }

    public boolean recentlyVisited(Term x) {
        return visited.contains(x);
    }

    protected boolean tryGo(Term t) {
        if (t != null) {
            if (visited.add(t)) {

                Concept d = nar.concept(t); //ualize?
                if (d != null) {

                    return go(d, true);
                }
            }
        }

        return false;
    }

    abstract static class SpiderAction extends Pri implements Consumer<Spider> {

        public SpiderAction(float p) {
            super(p);
        }

        @Override
        public String toString() {
            return super.toString() + " " + getClass().getSimpleName();
        }
    }

    private class TravelHome extends SpiderAction {

        public TravelHome(float p) {
            super(p);
        }

        @Override
        public void accept(Spider spider) {
            next();
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
            if (d != null) {
                if (go(d, true)) {
                    if (anon.hasVarQuery() || anon.hasXternal()) {
                        nar.question(anon);
                    } else {
                        Term i = INH.the(ct, anon);
                        if (Task.validTaskTerm(i))
                            nar.believe(i);
                    }
                }
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
            sampleAndVisitUnique(4, c);
        }


        protected void sampleAndVisitUnique(int maxTries, Concept c) {
            bag(c).sample(rng, maxTries, x -> {
                Term t = term(x);
                if (t!=null) {
                    if (!recentlyVisited(t)) {
                        if (tryGo(t)) {
                            return false; //done
                        }
                    }
                }
                return true; //keep trying
            });
        }

        protected Term term(Object x) {
            return ((PriReference<Term>) x).get();
        }

        protected Sampler bag(Concept c) {
            return c.termlinks();
        }
    }

    private class TravelTasklink extends TravelTermlink {

        public TravelTasklink(float p) {
            super(p);
        }

        protected Term term(Object x) {
            return ((TaskLink) x).term();
        }

        protected Sampler bag(Concept c) {
            return c.tasklinks();
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
                        int eteCap = ((DefaultBeliefTable) tt).eternal.capacity(); /* dont affect eternal */
                        //"squeeze" temporarily causing compression
                        tt.setCapacity(
                                eteCap,
                                ss);
                        //release, restore capacity
                        tt.setCapacity(
                                eteCap,
                                s);
                    }
                }
            }
        }


    }

    private class SqueezeTaskLinks extends SpiderAction /* squeeze bag */ {

        private final float ratio;

        public SqueezeTaskLinks(float p, float capacityRatio) {
            super(p);
            this.ratio = capacityRatio;
        }

        @Override
        public void accept(Spider spider) {
            Concept c = at;

            Bag table = c.tasklinks();
            int s = table.size();
            if (s > 0) {
                int ss = (int) Math.max(1, Math.floor(ratio * s));
                if (ss != s) {
                    table.setCapacity(ss);
                }
            }
        }


    }


}
