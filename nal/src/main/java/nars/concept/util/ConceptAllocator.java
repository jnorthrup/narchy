package nars.concept.util;

import nars.concept.Concept;
import nars.concept.TaskConcept;
import nars.table.BeliefTable;
import nars.table.eternal.EternalTable;
import nars.table.temporal.TemporalBeliefTable;

import java.util.function.Consumer;
import java.util.function.ToIntFunction;

/** sets capacities for the various Concept features */
public final class ConceptAllocator implements Consumer<Concept> {


    private final ToIntFunction<Concept>
            beliefsEteCapacity, beliefsTempCapacity,
            goalsEteCapacity, goalsTempCapacity, questionsCapacity, questsCapacity,
            termlinksCapacity, tasklinksCapacity;

//    /**
//     * minimum of 3 beliefs per belief table. for eternal, this allows revision between two goals to produce a third
//     */
//    public ConceptAllocator(int beliefsCapTotal, int goalsCapTotal, int questionsMax) {
//        this(
//                new MutableInteger(clamp(beliefsCapTotal / 4, 1, 6)),
//                new MutableInteger(clamp(goalsCapTotal / 4, 1, 6)),
//                new MutableInteger(Math.max(3, beliefsCapTotal)),
//                new MutableInteger(Math.max(3, goalsCapTotal)),
//                new MutableInteger(questionsMax),
//                (Concept c) -> {
//                    int maxLinks = 48;
//                    return Math.round((maxLinks) / (1f + ((float) Math.sqrt(c.volume())) / 2f));
//                },
//                (Concept c) -> {
//                    int maxLinks = 24;
//                    return Math.round((maxLinks) / (1f + ((float) Math.sqrt(c.volume())) / 2f));
//                }
//        );
//int max, min;
//
//        if (beliefOrGoal) {
//        max = eternalOrTemporal ? beliefsMaxEte : beliefsMaxTemp;
//        min = eternalOrTemporal ? beliefsMinEte : beliefsMinTemp;
//    } else {
//        max = eternalOrTemporal ? goalsMaxEte : goalsMaxTemp;
//        min = eternalOrTemporal ? goalsMinEte : goalsMinTemp;
//    }
//
//    int c = Util.lerp(Util.unitize((-1 + concept.complexity()) / 32f), max, min);
//        this.beliefsMaxEte = beliefsMaxEte.intValue();
//        this.beliefsMinEte = 2;
//        this.beliefsMaxTemp = beliefsMaxTemp.intValue();
//        this.beliefsMinTemp = 6;
//        this.goalsMaxEte = goalsMaxEte.intValue();
//        this.goalsMinEte = 2;
//        this.goalsMaxTemp = goalsMaxTemp.intValue();
//        this.goalsMinTemp = 6;
//        this.questionsMax = questionsMax;
//    }

    public ConceptAllocator(
                     ToIntFunction<Concept> beliefsEteCapacity, ToIntFunction<Concept> beliefsTempCapacity,
                     ToIntFunction<Concept> goalsEteCapacity, ToIntFunction<Concept> goalsTempCapacity,
                     ToIntFunction<Concept> questionsCapacity, ToIntFunction<Concept> questsCapacity,
                     ToIntFunction<Concept> termlinksCapacity, ToIntFunction<Concept> taskLinksCapacity) {
        this.beliefsEteCapacity = beliefsEteCapacity;
        this.beliefsTempCapacity = beliefsTempCapacity;
        this.goalsEteCapacity = goalsEteCapacity;
        this.goalsTempCapacity = goalsTempCapacity;
        this.questionsCapacity = questionsCapacity;
        this.questsCapacity = questsCapacity;
        this.termlinksCapacity = termlinksCapacity;
        this.tasklinksCapacity = taskLinksCapacity;
    }

    @Override public final void accept(Concept c) {
        apply(c);

        if (c instanceof TaskConcept) {
            apply((TaskConcept)c);
        }
    }

    private void apply(Concept c) {
        c.termlinks().setCapacity(linkCap(c, true));
        c.tasklinks().setCapacity(linkCap(c, false));
    }


    private void setBeliefTableCapacity(TaskConcept c, BeliefTable t, boolean beliefOrGoal) {
        if (t instanceof EternalTable) {
            ((EternalTable) t).setCapacity(beliefCap(c, beliefOrGoal, true));
        } else if (t instanceof TemporalBeliefTable) {
            ((TemporalBeliefTable) t).setCapacity(beliefCap(c, beliefOrGoal, false));
        }
    }

    private void apply(TaskConcept c) {
        c.beliefs().tables.forEach(t -> setBeliefTableCapacity(c, t, true));
        c.goals().tables.forEach(t -> setBeliefTableCapacity(c, t, false));
        c.questions().setCapacity(questionCap(c, true));
        c.quests().setCapacity(questionCap(c, false));
    }


    private int beliefCap(TaskConcept concept, boolean beliefOrGoal, boolean eternalOrTemporal) {
        return (beliefOrGoal ?
                (eternalOrTemporal ?
                        beliefsEteCapacity : beliefsTempCapacity) :
                (eternalOrTemporal ?
                        goalsEteCapacity : goalsTempCapacity)).applyAsInt(concept);
    }

    private int linkCap(Concept concept, boolean termOrTask) {
        return (termOrTask ?  termlinksCapacity : tasklinksCapacity).applyAsInt(concept);
    }

    private int questionCap(TaskConcept concept, boolean questionOrQuest) {
        return (questionOrQuest ? questionsCapacity : questsCapacity).applyAsInt(concept);
    }

//    @deprcpublic static int lerp(Concept c, MutableInteger _min, MutableInteger _max) {
//
//        int min = _min.intValue();
//        int max = _max.intValue();
//
//        float v = c.complexity();
//        float complexityFactor = ((v - 1) / 32);
//        complexityFactor = Util.sqr(Util.unitize(complexityFactor));
//
//        return Util.lerp(complexityFactor, max, min);
//    }


}
