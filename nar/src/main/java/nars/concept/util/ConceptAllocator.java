package nars.concept.util;

import nars.concept.Concept;
import nars.concept.TaskConcept;
import nars.table.BeliefTable;
import nars.table.BeliefTables;
import nars.table.TaskTable;
import nars.table.eternal.EternalTable;
import org.eclipse.collections.api.block.procedure.Procedure2;

import java.util.function.Consumer;
import java.util.function.ToIntFunction;

/** sets capacities for the various Concept features */
public final class ConceptAllocator implements Consumer<Concept> {


    private final ToIntFunction<Concept>
            beliefsEteCapacity;
    private final ToIntFunction<Concept> beliefsTempCapacity;
    private final ToIntFunction<Concept> goalsEteCapacity;
    private final ToIntFunction<Concept> goalsTempCapacity;
    private final ToIntFunction<Concept> questionsCapacity;
    private final ToIntFunction<Concept> questsCapacity;



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
            ToIntFunction<Concept> questionsCapacity, ToIntFunction<Concept> questsCapacity) {
        this.beliefsEteCapacity = beliefsEteCapacity;
        this.beliefsTempCapacity = beliefsTempCapacity;
        this.goalsEteCapacity = goalsEteCapacity;
        this.goalsTempCapacity = goalsTempCapacity;
        this.questionsCapacity = questionsCapacity;
        this.questsCapacity = questsCapacity;
    }

    @Override public final void accept(Concept c) {
        if (c instanceof TaskConcept) {
            apply((TaskConcept)c);
        }
    }


    private void setTaskCapacity(TaskConcept c, BeliefTable x, boolean beliefOrGoal) {
        if (x instanceof BeliefTables)
            ((BeliefTables)x).forEachWith(new Procedure2<BeliefTable, TaskConcept>() {
                @Override
                public void value(BeliefTable xx, TaskConcept C) {
                    ConceptAllocator.this._setTaskCapacity(C, xx, false);
                }
            }, c);
        else
            _setTaskCapacity(c, x, false);
    }

    private void _setTaskCapacity(TaskConcept c, BeliefTable t, boolean beliefOrGoal) {
        if (t instanceof EternalTable) {
            t.setTaskCapacity(beliefCap(c, beliefOrGoal, true));
        } else if (t instanceof TaskTable) {
            t.setTaskCapacity(beliefCap(c, beliefOrGoal, false));
        }
    }

    private void apply(TaskConcept c) {
        setTaskCapacity(c, c.beliefs(),true);
        setTaskCapacity(c, c.goals(),false);
        c.questions().setTaskCapacity(questionCap(c, true));
        c.quests().setTaskCapacity(questionCap(c, false));
    }


    private int beliefCap(TaskConcept concept, boolean beliefOrGoal, boolean eternalOrTemporal) {
        return (beliefOrGoal ?
                (eternalOrTemporal ?
                        beliefsEteCapacity : beliefsTempCapacity) :
                (eternalOrTemporal ?
                        goalsEteCapacity : goalsTempCapacity)).applyAsInt(concept);
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
