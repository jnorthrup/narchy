/*
 * Concept.java
 *
 * Copyright (C) 2008  Pei Wang
 *
 * This file is part of Open-NARS.
 *
 * Open-NARS is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 2 of the License, or
 * (at your option) any later version.
 *
 * Open-NARS is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Open-NARS.  If not, see <http://www.gnu.org/licenses/>.
 */
package nars.concept;

import com.google.common.collect.Iterators;
import nars.Global;
import nars.NAR;
import nars.Symbols;
import nars.bag.Bag;
import nars.budget.Budget;
import nars.budget.Budgeted;
import nars.budget.policy.ConceptPolicy;
import nars.concept.table.BeliefTable;
import nars.concept.table.QuestionTable;
import nars.concept.table.TaskTable;
import nars.link.BLink;
import nars.task.Revision;
import nars.task.Task;
import nars.term.Term;
import nars.term.Termed;
import nars.term.Termlike;
import nars.truth.Truth;
import org.apache.commons.lang3.mutable.MutableFloat;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.PrintStream;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.function.BiFunction;
import java.util.function.Consumer;
import java.util.function.Function;

public interface Concept extends Termed, Comparable<Termlike> {

    @NotNull Bag<Task> tasklinks();

    @NotNull Bag<Termed> termlinks();

    @Nullable Map<Object, Object> meta();


    @Nullable
    Object put(@NotNull Object key, @Nullable Object value);

    /**
     * like Map.gett for getting data stored in meta map
     */
    @Nullable
    default <C> C get(@NotNull Object key) {
        Map m = meta();
        return null == m ? null : (C) m.get(key);
    }

    /** follows Map.compute() semantics */
    @NotNull
    <C> C meta(Object key, BiFunction value);


    @Nullable BeliefTable beliefs();

    @Nullable BeliefTable goals();

    @Nullable QuestionTable questions();

    @Nullable QuestionTable quests();

    @Nullable
    default Truth belief(long when, long now) {
        return hasBeliefs() ? beliefs().truth(now, when) : Truth.Null;
    }

    @Nullable
    default Truth desire(long when, long now) {
        return hasGoals() ? goals().truth(now, when) : Truth.Null;
    }

    @Nullable
    default Truth belief(long now) {
        return belief(now, now);
    }

    @Nullable
    default Truth desire(long now) {
        return desire(now, now);
    }

    void capacity(ConceptPolicy c);

    boolean contains(Task t);


    default boolean hasGoals() {
        return !goals().isEmpty();
    }

    default boolean hasBeliefs() {
        return !beliefs().isEmpty();
    }

    default boolean hasQuestions() { return !questions().isEmpty();    }

    default boolean hasQuests() {
        return !quests().isEmpty();
    }


    @Nullable
    default TaskTable tableFor(char punc) {
        switch(punc) {
            case Symbols.BELIEF: return beliefs();
            case Symbols.GOAL: return goals();
            case Symbols.QUESTION: return questions();
            case Symbols.QUEST: return quests();
            default:
                throw new UnsupportedOperationException();
        }
    }

    default BeliefTable tableAnswering(char punc) {
        switch (punc) {
            case Symbols.QUESTION: return beliefs();
            case Symbols.QUEST: return goals();
            default:
                throw new UnsupportedOperationException();
        }
    }


    default @Nullable Task merge(@NotNull Task x, @NotNull Task y, long when, @NotNull NAR nar) {
        long now = nar.time();
        return Revision.merge(x, y, now, when,
                ((BeliefTable)tableFor(y.punc())).truth(now, when)
        );
    }

    /**
     * process a task in this concept
     *
     * @return true if process affected the concept (ie. was inserted into a belief table)
     */
    @Nullable
    Task process(@NotNull Task task, @NotNull NAR nar);

    /**
     * attempt insert a tasklink into this concept's tasklink bag
     * return true if successfully inserted
     *
     * when a task is processed, a tasklink
     * can be created at the concept of its term
     *
     * @return whether the link successfully was completed
     */
    default boolean link(@NotNull Budgeted b, float scale, float minScale, @NotNull NAR nar, @Nullable MutableFloat conceptOverflow) {

        if (b instanceof Task) {
            linkTask((Task)b, scale);
        }

        linkAny(b, scale, minScale, nar, conceptOverflow);

        return true;
    }

    void linkAny(Budgeted b, float scale, float minScale, @NotNull NAR nar, @Nullable MutableFloat conceptOverflow);


    void linkTask(@NotNull Task t, float scale);



    default boolean link(@NotNull Budgeted b, float initialScale, @NotNull NAR nar, @Nullable MutableFloat conceptOverflow) {
        float p = b.priIfFiniteElseNeg1();
        if (p < 0)
            return false;

        return link(b, initialScale,
                nar.taskLinkThreshold.floatValue() / p, //minScale
                nar, conceptOverflow);
    }

    /**
     * @param thisTask  task with a term equal to this concept's
     * @param otherTask task with a term equal to another concept's
     * @return number of links created (0, 1, or 2)
     */
    default void crossLink(@NotNull Task thisTask, @NotNull Task otherTask, float scale, @NotNull NAR nar) {

        Concept other = nar.concept(otherTask);
        if (other == null || other.equals(this))
            return; //null or same concept

        float halfScale = scale / 2f;

        link(otherTask, halfScale, nar, null);

        other.link(thisTask, halfScale, nar, null);

    }

    /** link to a specific peer */
    default void linkPeer(Termed x, Budget b, float v) {
        BLink<Termed> existing = termlinks().get(x);

        /*
        Hebbian Learning:
            deltaWeight = (input[fromNeuron] -
                            output[toNeuron] * weight(fromNeuron,toNeuron)
							* output[toNeuron] * this.learningRate);
							*
            deltaWeight = input[toNeuron] * output[toNeuron] * learningRate;
            deltaWeight = (input - netInput) * output * this.learningRate; // is it right to use netInput here?
			deltaWeight = input * desiredOutput * this.learningRate;
         */


        boolean init;
        if (existing == null ) {
            termlinks().put(x, b, v, null);
            init = true;
        } else {
            init = false;
        }

        float a = b.pri();
        if (a == a /*!NaN */) {

            final float learningRate = v / termlinks().size();
            //System.out.println(this + " activating " + x);
            termlinks().forEach(tl -> {
                boolean active = tl == existing;
                if (active && init)
                    return; //dont modify the newly inserted link

                float outputTarget = active ? a : (1f - a);
                float output = outputTarget - tl.pri(); //nar.conceptPriority(x);
                float dp = output * learningRate;
                tl.priAdd(dp);
                //System.out.println(tl.toString2());
            });

            //TODO only sort if the order will have changed which is detectable
            termlinks().setRequiresSort();
        }

    }

    /** link to all existing termlinks, hierarchical and heterarchical */
    default void linkPeers(@NotNull Budgeted b, float scale, @NotNull NAR nar, boolean recurse) {
        List<Termed> targets = Global.newArrayList(termlinks().size());
        termlinks().forEach(tl -> targets.add(tl.get()));
        float subScale = scale / targets.size();
        targets.forEach(t -> {
            //System.out.println(Concept.this + " activate " + t + " " + b + "x" + subScale);
            termlinks().put(t, b, subScale, null); //activate the termlink
            nar.conceptualize(t, b, subScale, recurse ? subScale : 0f, null);
        });

    }


    default void visitTasks(@NotNull Consumer<Task> each, boolean includeConceptBeliefs, boolean includeConceptQuestions, boolean includeConceptGoals, boolean includeConceptQuests) {
        if (includeConceptBeliefs) beliefs().forEach(each);
        if (includeConceptQuestions) questions().forEach(each);
        if (includeConceptGoals) goals().forEach(each);
        if (includeConceptQuests) quests().forEach(each);
    }


    @NotNull
    default Iterator<Task> iterateTasks(boolean onbeliefs, boolean ongoals, boolean onquestions, boolean onquests) {

        TaskTable beliefs = onbeliefs ? beliefs() : null;
        TaskTable goals = ongoals ? goals() : null;
        TaskTable questions = onquestions ? questions() : null;
        TaskTable quests = onquests ? quests() : null;

        Iterator<Task> b1 = beliefs != null ? beliefs.iterator() : Collections.emptyIterator();
        Iterator<Task> b2 = goals != null ? goals.iterator() : Collections.emptyIterator();
        Iterator<Task> b3 = questions != null ? questions.iterator() : Collections.emptyIterator();
        Iterator<Task> b4 = quests != null ? quests.iterator() : Collections.emptyIterator();
        return Iterators.concat(b1, b2, b3, b4);
    }



    default void print() {
        print(System.out);
    }

    default void print(@NotNull PrintStream out) {
        print(out, true, true, true, true);
    }


    /**
     * prints a summary of all termlink, tasklink, etc..
     */
    default void print(@NotNull PrintStream out, boolean showbeliefs, boolean showgoals, boolean showtermlinks, boolean showtasklinks) {

        out.println("concept: " + toString());

        String indent = "  \t";

        Consumer<Task> printTask = s -> {
            out.print(indent);
            out.print(s);
            out.print(" ");
            out.print(s.lastLogged());
            out.println();
        };

        if (showbeliefs) {
            out.print(" Beliefs:");
            if (beliefs().isEmpty()) out.println(" none");
            else {
                out.println(); beliefs().forEach(printTask);
            }
            out.print(" Questions:");
            if (questions().isEmpty()) out.println(" none");
            else {
                out.println(); questions().forEach(printTask);
            }
        }

        if (showgoals) {
            out.print(" Goals:");
            if (goals().isEmpty()) out.println(" none");
            else {
                out.println();                goals().forEach(printTask);
            }
            out.print(" Quests:");
            if (questions().isEmpty()) out.println(" none");
            else {
                out.println();                quests().forEach(printTask);
            }
        }

        if (showtermlinks) {
            //out.print("TermLinkTemplates: ");
            //out.println(termlinkTemplates());

            out.println("\n TermLinks:");
            termlinks().forEach(b -> {
                out.print(indent);
                out.print(b.get() + " " + b.toBudgetString());
                out.print(" ");
            });
        }

        if (showtasklinks) {
            out.println("\n TaskLinks:");
            tasklinks().forEach(b -> {
                out.print(indent);
                out.print(b.get() + " " + b.toBudgetString());
                out.print(" ");
            });
        }

        out.println('\n');
    }




//    default Iterator<? extends Termed> getTermedAdjacents(boolean termLinks, boolean taskLinks) {
//        if (termLinks && taskLinks) {
//            return concat(
//                    getTermLinks().iterator(), getTaskLinks().iterator()
//            );
//        }
//        if (termLinks) {
//            return getTermLinks().iterator();
//        }
//        if (taskLinks) {
//            return getTaskLinks().iterator();
//        }
//
//        return null;
//    }

//    default Iterator<Term> adjacentTerms(boolean termLinks, boolean taskLinks) {
//        return transform(adjacentTermables(termLinks, taskLinks), Termed::getTerm);
//    }

//    default Iterator<Concept> adjacentConcepts(boolean termLinks, boolean taskLinks) {
//        final Iterator<Concept> termToConcept = transform(adjacentTerms(termLinks, taskLinks), new Function<Termed, Concept>() {
//            @Override
//            public Concept apply(final Termed term) {
//                return getMemory().concept(term.getTerm());
//            }
//        });
//        return filter(termToConcept, Concept.class); //should remove null's (unless they never get included anyway), TODO Check that)
//    }

    /**
     * Created by me on 3/23/16.
     */
    interface ConceptBuilder extends Function<Term, Termed> {

        @NotNull
        Bag<Task> taskbag();
        @NotNull
        Bag<Termed> termbag();

    }
}
