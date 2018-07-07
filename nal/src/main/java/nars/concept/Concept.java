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
 * along with Open-NARS.  If not, see <http:
 */
package nars.concept;

import com.google.common.collect.Iterators;
import jcog.bag.Bag;
import jcog.data.map.MetaMap;
import jcog.pri.PriReference;
import jcog.pri.Priority;
import nars.NAR;
import nars.Task;
import nars.concept.util.ConceptState;
import nars.link.TaskLink;
import nars.link.TermlinkTemplates;
import nars.table.BeliefTable;
import nars.table.QuestionTable;
import nars.table.TaskTable;
import nars.term.Term;
import nars.term.Termed;
import nars.util.SoftException;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.io.PrintStream;
import java.util.Iterator;
import java.util.function.Consumer;
import java.util.stream.Stream;

import static jcog.Texts.n4;
import static nars.Op.*;

public interface Concept extends Termed, MetaMap, Iterable<Concept> {
    Concept[] EmptyArray = new Concept[0];

    /*@NotNull*/ Bag<?,TaskLink> tasklinks();

    /*@NotNull*/ Bag<Term,PriReference<Term>> termlinks();

    /*@NotNull*/ BeliefTable beliefs();

    /*@NotNull*/ BeliefTable goals();

    /*@NotNull*/ QuestionTable questions();

    /*@NotNull*/ QuestionTable quests();

    void delete(NAR nar);

    default boolean isDeleted() {
        return state() == ConceptState.Deleted;
    }


    default void print() {
        print(System.out);
    }
    default String printToString() {
        StringBuilder sb = new StringBuilder(1024);
        print(sb);
        return sb.toString();
    }


    @Override
    default Iterator<Concept> iterator() {
        return Iterators.singletonIterator(this);
    }

    default <A extends Appendable> A print(@NotNull A out) {
        print(out, true, true, true, true);
        return out;
    }


    /*@NotNull*/
    default TaskTable table(byte punc) {
        switch (punc) {
            case BELIEF:
                return beliefs();
            case GOAL:
                return goals();
            case QUESTION:
                return questions();
            case QUEST:
                return quests();
            default:
                throw new UnsupportedOperationException("what kind of punctuation is: '" + punc + "'");
        }
    }

    default BeliefTable tableAnswering(byte punc) {
        switch (punc) {
            case BELIEF:
            case QUESTION:
                return beliefs();
            case GOAL:
            case QUEST:
                return goals();
            default:
                throw new UnsupportedOperationException("what kind of punctuation is: '" + punc + "'");
        }
    }

    String printIndent = "  \t";

    /**
     * prints a summary of all termlink, tasklink, etc..
     */
    default void print(Appendable out, boolean showbeliefs, boolean showgoals, boolean showtermlinks, boolean showtasklinks) {

        try {
            out.append("concept: ").append(toString()).append('\t').append(getClass().toString()).append('\n');

            Consumer<Priority> printBagItem = b -> {
                try {
                    out.append(printIndent);
                    out.append(b.toString());
                    
                    out.append("\n");
                } catch (IOException e) {
                    e.printStackTrace();
                }
            };

            if (showtermlinks) {
                
                

                out.append("\n TermLinks: ").append(String.valueOf(termlinks().size())).append(String.valueOf('/')).append(String.valueOf(termlinks().capacity())).append('\n');

                termlinks().forEach(printBagItem);
            }

            if (showtasklinks) {
                out.append("\n TaskLinks: ").append(String.valueOf(tasklinks().size())).append(String.valueOf('/')).append(String.valueOf(tasklinks().capacity())).append('\n');

                tasklinks().forEach(printBagItem);
            }

            out.append('\n');
        } catch (IOException e) {
            e.printStackTrace();
        }

                Consumer<Task> printTask = s -> {
            try {
                out.append(printIndent);
                out.append(s.toString());
                out.append(" ");
                Object ll = s.lastLogged();
                if (ll != null)
                    out.append(ll.toString());
                out.append('\n');
            } catch (IOException e) {
                e.printStackTrace();
            }
        };

        try {
            if (showbeliefs) {
                out.append(" Beliefs:");
                if (beliefs().isEmpty()) out.append(" none").append('\n');
                else {
                    out.append('\n');
                    beliefs().forEachTask(printTask);
                }
                out.append(" Questions:");
                if (questions().isEmpty()) out.append(" none").append('\n');
                else {
                    out.append('\n');
                    questions().forEachTask(printTask);
                }
            }

            if (showgoals) {
                out.append(" Goals:");
                if (goals().isEmpty()) out.append(" none").append('\n');
                else {
                    out.append('\n');
                    goals().forEachTask(printTask);
                }
                out.append(" Quests:");
                if (questions().isEmpty()) out.append(" none").append('\n');
                else {
                    out.append('\n');
                    quests().forEachTask(printTask);
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }


    }







    /*@NotNull*/ ConceptState state();

    /**
     * returns the previous state
     */
    ConceptState state(ConceptState c);

    /** should not include itself, although this will be included with these templates on activation
     *  should use something like an ArrayList which supports fast random access by index
     * */
    TermlinkTemplates templates();

    Stream<Task> tasks(boolean includeBeliefs, boolean includeQuestions, boolean includeGoals, boolean includeQuests);

    default Stream<Task> tasks() {
        return tasks(true,true,true,true);
    }

    default void printSummary(PrintStream out, NAR n) {
        long now = n.time();
        out.println(term() +
                "\t" +
                "belief=" + beliefs().truth(now, n) + " $" + n4(beliefs().priSum()) + " , " +
                "goal=" +   goals().truth(now, n)+ " $" + n4(goals().priSum())
        );
    }

    default void remove(Task t) {
        table(t.punc()).removeTask(t);
    }


    /**
     * Created by me on 9/13/16.
     */
    final class InvalidConceptException extends SoftException {

        final Termed term;
        final String reason;

        public InvalidConceptException(Termed term, String reason) {
            this.term = term;
            this.reason = reason;
        }

        @NotNull
        @Override
        public String getMessage() {
            return "InvalidConceptTerm: " + term + " (" + term.getClass() + "): " + reason;
        }

    }


}
