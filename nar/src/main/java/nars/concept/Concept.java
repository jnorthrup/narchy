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

import jcog.data.map.MetaMap;
import nars.NAR;
import nars.Op;
import nars.Task;
import nars.table.BeliefTable;
import nars.table.TaskTable;
import nars.table.question.QuestionTable;
import nars.term.Termed;
import nars.util.SoftException;

import java.io.IOException;
import java.util.function.Consumer;
import java.util.stream.Stream;

import static nars.Op.*;

public interface Concept extends Termed, MetaMap {

//    Bag<Tasklike,TaskLink> tasklinks();

    BeliefTable beliefs();

    BeliefTable goals();

    QuestionTable questions();

    QuestionTable quests();

    /** returns true if not already deleted */
    boolean delete(NAR nar);

    /** meta-table key, if present, signifies concept has been deleted */
    String DELETED = "-";

    default boolean isDeleted() {
        return meta(DELETED)==DELETED;
    }


    default Op op() { return term().op(); }


    default void print() {
        print(System.out);
    }
    default String printToString() {
        StringBuilder sb = new StringBuilder(1024);
        print(sb);
        return sb.toString();
    }


//    @Override
//    default Iterator<Concept> iterator() {
//        return Iterators.singletonIterator(this);
//    }

    default <A extends Appendable> A print(A out) {
        print(out, true, true);
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
                throw new UnsupportedOperationException("what kind of punctuation is: '" + punc + '\'');
        }
    }

//    /** the belief table for the punctuation, or which anwers questions/quests for it */
//    default BeliefTable tableAnswering(byte punc) {
//        switch (punc) {
//            case BELIEF:
//            case QUESTION:
//                return beliefs();
//            case GOAL:
//            case QUEST:
//                return goals();
//            default:
//                throw new UnsupportedOperationException("what kind of punctuation is: '" + punc + '\'');
//        }
//    }

    String printIndent = "  \t";

    /**
     * prints a summary of all termlink, tasklink, etc..
     */
    default void print(Appendable out, boolean showbeliefs, boolean showgoals) {

        try {
            out.append("concept: ").append(toString()).append('\t').append(getClass().toString()).append('\n');

//            Consumer<Prioritized> printBagItem = b -> {
//                try {
//                    out.append(printIndent);
//                    out.append(b.toString());
//
//                    out.append("\n");
//                } catch (IOException e) {
//                    e.printStackTrace();
//                }
//            };



            out.append('\n');
        } catch (IOException e) {
            e.printStackTrace();
        }

        Consumer<Task> printTask = s -> {
            try {
                out.append(printIndent);
                out.append(s.toString());
                out.append(" ");
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

    Stream<Task> tasks(boolean includeBeliefs, boolean includeQuestions, boolean includeGoals, boolean includeQuests);

    default Stream<Task> tasks() {
        return tasks(true,true,true,true);
    }

//    default void printSummary(PrintStream out, NAR n) {
//        long now = n.time();
//        out.println(target() +
//                "\t" +
//                "belief=" + beliefs().truth(now, n) + " $" + " , " +
//                "goal=" +   goals().truth(now, n)+ " $"
//        );
//    }

    default boolean remove(Task t) {
        return table(t.punc()).removeTask(t, true);
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

        @Override
        public String getMessage() {
            return "InvalidConceptTerm: " + term + " (" + term.getClass() + "): " + reason;
        }

    }


}
