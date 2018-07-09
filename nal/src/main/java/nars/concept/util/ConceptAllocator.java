package nars.concept.util;

import jcog.Util;
import jcog.math.MutableInteger;
import nars.concept.Concept;
import nars.concept.PermanentConcept;
import nars.concept.TaskConcept;
import org.eclipse.collections.api.block.function.primitive.IntToIntFunction;

import java.util.function.Consumer;

import static jcog.Util.clamp;

/** sets capacities for the various Concept features */
public final class ConceptAllocator implements Consumer<Concept> {

    public int beliefsMaxEte;
    public int goalsMaxEte;
    private final int beliefsMinEte;
    private final int goalsMinEte;
    private final MutableInteger questionsMax;


    private final IntToIntFunction termlinksCapacity, tasklinksCapacity;

    public int beliefsMaxTemp;
    public int beliefsMinTemp;
    public int goalsMaxTemp;
    public int goalsMinTemp;

    /**
     * minimum of 3 beliefs per belief table. for eternal, this allows revision between two goals to produce a third
     */
    public ConceptAllocator(int beliefsCapTotal, int goalsCapTotal, int questionsMax) {
        this(
                new MutableInteger(clamp(beliefsCapTotal / 4, 1, 6)),
                new MutableInteger(clamp(goalsCapTotal / 4, 1, 6)),
                new MutableInteger(Math.max(3, beliefsCapTotal)),
                new MutableInteger(Math.max(3, goalsCapTotal)),
                new MutableInteger(questionsMax),
                (vol) -> {


                    int maxLinks = 48;
                    return Math.round((maxLinks) / (1f + ((float) Math.sqrt(vol)) / 2f));


                },
                (vol) -> {


                    int maxLinks = 24;
                    return Math.round((maxLinks) / (1f + ((float) Math.sqrt(vol)) / 2f));


                }
        );
    }

    ConceptAllocator(MutableInteger beliefsMaxEte, MutableInteger goalsMaxEte,
                     MutableInteger beliefsMaxTemp, MutableInteger goalsMaxTemp,
                     MutableInteger questionsMax, IntToIntFunction termlinksCapacity, IntToIntFunction taskLinksCapacity) {
        this.beliefsMaxEte = beliefsMaxEte.intValue();
        this.beliefsMinEte = 2;
        this.beliefsMaxTemp = beliefsMaxTemp.intValue();
        this.beliefsMinTemp = 6;
        this.goalsMaxEte = goalsMaxEte.intValue();
        this.goalsMinEte = 2;
        this.goalsMaxTemp = goalsMaxTemp.intValue();
        this.goalsMinTemp = 6;
        this.questionsMax = questionsMax;

        this.termlinksCapacity = termlinksCapacity;
        this.tasklinksCapacity = taskLinksCapacity;
    }

    @Override public void accept(Concept c) {
        apply(c);

        if (c instanceof TaskConcept) {
            apply((TaskConcept)c);
        }
    }

    protected void apply(Concept c) {
        c.termlinks().setCapacity(linkCap(c, true));
        c.tasklinks().setCapacity(linkCap(c, false));
    }


    protected void apply(TaskConcept c) {

        int be = beliefCap(c, true, true);
        int bt = beliefCap(c, true, false);

        int ge = beliefCap(c, false, true);
        int gt = beliefCap(c, false, false);

        c.beliefs().setCapacity(be, bt);
        c.goals().setCapacity(ge, gt);
        c.questions().capacity(questionCap(c, true));
        c.quests().capacity(questionCap(c, false));
    }


    public int beliefCap(TaskConcept concept, boolean beliefOrGoal, boolean eternalOrTemporal) {
        int max, min;

        if (beliefOrGoal) {
            max = eternalOrTemporal ? beliefsMaxEte : beliefsMaxTemp;
            min = eternalOrTemporal ? beliefsMinEte : beliefsMinTemp;
        } else {
            max = eternalOrTemporal ? goalsMaxEte : goalsMaxTemp;
            min = eternalOrTemporal ? goalsMinEte : goalsMinTemp;
        }

        int c = Util.lerp(Util.unitize((-1 + concept.complexity()) / 32f), max, min);

        if (concept instanceof PermanentConcept) {
            if (!eternalOrTemporal) {
                c *= 2;
            } else {
                //                if (concept instanceof ActionConcept) {
                //                    c = 0;
                //                }
            }
        }

        return c;

    }

    public int linkCap(Concept concept, boolean termOrTask) {
        if (termOrTask) {

            return termlinksCapacity.valueOf(concept.volume());

        } else {
            return tasklinksCapacity.valueOf(concept.volume());
        }
    }

    public static int lerp(Concept c, MutableInteger _min, MutableInteger _max) {

        int min = _min.intValue();
        int max = _max.intValue();

        float v = c.complexity();
        float complexityFactor = ((v - 1) / 32);
        complexityFactor = Util.sqr(Util.unitize(complexityFactor));

        return Util.lerp(complexityFactor, max, min);
    }

    public final int questionCap(TaskConcept concept, boolean questionOrQuest) {
        return Util.lerp(1f - Math.min(1f, (((float) concept.volume()) / 32)), 1, questionsMax.intValue());
    }


}
