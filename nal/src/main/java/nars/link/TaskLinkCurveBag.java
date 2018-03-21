package nars.link;

import jcog.bag.impl.CurveBag;
import jcog.list.FasterList;
import nars.NAR;
import nars.Param;
import nars.Task;
import nars.concept.Concept;
import nars.term.Term;
import nars.term.compound.util.Conj;
import nars.time.Tense;

import java.util.Map;

import static nars.Op.*;

public class TaskLinkCurveBag extends CurveBag<TaskLink> {

    static final float TEMPORAL_COMPRESSION_THRESHOLD =
            1.1f; //off
            //0.75f;

    public TaskLinkCurveBag(Map sharedMap) {
        super(Param.tasklinkMerge, sharedMap, 0);
    }



    /**
     * the state of the tasklink bag can be seen as one big virtual conjunction.
     * the goal here in compressing is to combine and replace each "cluster" of
     * related links (temporally, or otherwise) with one actual conjunction.
     *
     * attempt to compress clusters of nearby time ranges into one link.
     * conceptualizes any new terms to ensure a dynamic concept can be ready.
     *
     * TODO eternal links can be combined && with any other links
     * */
    public void compress(NAR nar) {

        if (size() < capacity()*TEMPORAL_COMPRESSION_THRESHOLD)
            return;

        final Conj[] beliefs = {new Conj()};
        final Conj[] questions = {new Conj()};
        final Conj[] goals = {new Conj()};
        forEach(x -> {
            if (!(x instanceof TaskLink.GeneralTaskLink))
                return;
            TaskLink.GeneralTaskLink g = (TaskLink.GeneralTaskLink)x;
            long when = Tense.dither(g.when(), nar);
            Term what = g.term(); //must be exact, not root

            Conj[] table = null;
            switch (g.punc()) {
                case BELIEF: table = beliefs; break; //maybe include a negation flag for beliefs
                case QUESTION: table = questions; break;
                case GOAL: table = goals; break;
                default:
                    return;
            }

            if (!table[0].add(what, when))
                table[0] = new Conj();  /* HACK just clear when it becomes contradicting */
        });
        compress(beliefs[0], BELIEF, nar);
        compress(questions[0], QUESTION, nar);
        compress(goals[0], GOAL, nar);
    }

    final static int MAX_EVENTS = 4;

    private void compress(Conj e, byte punc, NAR nar) {
        int maxVol = nar.termVolumeMax.intValue();

        e.event.forEachKeyValue((when, what)->{
            int eventCount = Conj.eventCount(what);
            if (eventCount > 1 && eventCount < MAX_EVENTS /* TODO allow choosing subset of events from a single time */) {
                Term c = e.term(when);
                if (c.volume() < maxVol && Task.validTaskTerm(c, punc, true)) {


                    FasterList<TaskLink.GeneralTaskLink> removed = new FasterList(eventCount);
                    final float[] pri = {0};
                    synchronized (items) {
                        e.forEachTerm(what, (Term t) -> {
                            TaskLink.GeneralTaskLink key = new TaskLink.GeneralTaskLink(t, punc, when, 0);
                            TaskLink.GeneralTaskLink r = (TaskLink.GeneralTaskLink) remove(key);
                            if (r != null) {
                                pri[0] += r.priElseZero();
                                removed.addIfNotNull(r);
                            }
                        });

                        if (removed.size() > 1) {
                            Concept cc = nar.conceptualize(c);
                            if (cc!=null) {
                                put(new TaskLink.GeneralTaskLink(c, punc, Tense.dither(when, nar), pri[0]));
                                //TODO check it was actually inserted
                                return; //SUCCESS
                            }
                        }

                        //FAIL
                        //  re-insert, sorry to waste your time
                        removed.forEach(this::put);
                    }
                }
            }

        });


    }

    @Override
    public void onRemove(TaskLink _value) {
        _value.reincarnate(this);
    }
}
