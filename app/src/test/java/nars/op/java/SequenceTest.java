//package nars.op.java;
//
//import nars.$;
//import nars.Param;
//import nars.Task;
//import nars.index.term.TermIndex;
//
//import nars.nar.Default;
//import nars.nlp.Twenglish;
//import nars.term.Compound;
//import nars.term.Term;
//import nars.term.Termed;
//import nars.term.atom.Atomic;
//import nars.time.Tense;
//import nars.truth.Truth;
//import org.jetbrains.annotations.Nullable;
//
//import java.util.ArrayList;
//import java.util.List;
//import java.util.TreeSet;
//
//import static nars.Op.ATOM;
//
///**
// * Created by me on 4/1/16.
// */
//public class SequenceTest {
//
//    final static Atomic hear = $.the("hear");
//    final static Atomic heard = $.the("heard");
//
//    public static void main(String[] args) {
//
//
//
//
//        Default n = new Default(1000, 8, 2, 2);
//        //n.DEFAULT_QUESTION_PRIORITY = 0.1f;
//        //n.shortTermMemoryHistory.set(4);
//
//        n.on(new TermFunction("vocalize") {
//
//            @Nullable
//            @Override
//            public Object function(Compound arguments, TermIndex i) {
//                System.out.println("VOCALIZE: " + arguments);
//                return true;
//            }
//        });
//
//        n.logSummaryGT(System.out, 0.1f);
//
//        int repeats = 7;
//        int w = 0;
//
//        n.input("(hear:{#current} ==>+0 vocalize(#current)). :|: %1.00;0.99%");
//
//        int words = 2;
//        Term last = null;
//        List<Term> s =
//                //Twenglish.tokenize("this is a sentence.");
//                Twenglish.tokenize("a b c d e f g");
//        int wordTime = 5;
//        for (int r = 0; r < repeats; r++) {
//            for (int i = 0; i < s.size(); i++) {
//
//                if (last != null)
//                    n.believe(hear(heard, last), Tense.Present, 0f, 0.9f);//.step();
//
//                w = (w + 1) % words;
//
//                last = s.get(i);
//                n.believe(hear(hear, last), Tense.Present, 1f, 0.99f).run(wordTime);
//
//            }
//        }
//
//        n.run(wordTime*4);
//
//        hallucinate(n, s, wordTime);
//
//        TreeSet<Task> beliefs = new TreeSet(Truth.compareConfidence);
//
//        n.forEachActiveConcept(c -> {
//            Task t = c.beliefs().match(n.time());
//            if (t!=null)
//                beliefs.add(t);
//
//
//        });
//
//        beliefs.forEach(System.out::println);
//        //beliefs.forEach(t->System.out.println(t.explanation()));
//    }
//
//    public static void hallucinate(Default n, List<Term> s, int wordTime) {
//
//
//
//        Termed next = s.get(0);
//        List<Termed> spoken = new ArrayList();
//
//        for (int i = 0; i < 10; i++) {
//
//            spoken.add(next);
//            n.input(hear(hear, next) + ". :|: %1.0;0.9%"); //subvocalization, thinking to self
//
//
//            n.run(wordTime / 2);
//
//            //n.input("(hear:{" + next + "} ==> ?x)? :/:");
//            //n.input("(hear:{" + next + "} ==> ?x)?");
//            //n.input(hear($.$("?X")) + "? :/:");
//
//            n.run(wordTime / 2); //TODO integrate the concept priorities for the next set of cycles
//
//            //List<BLink<? extends Termed>> top = new ArrayList();
//            int max = 5;
//            List<Termed> top = new ArrayList();
//            Termed finalNext = next;
//            n.core.active.forEachWhile(ww -> {
//                Termed x = ww.get();
//                if (x.op() == ATOM && !x.equals(hear) && !x.equals(finalNext))
//                    top.add(x);
//                return top.size() < max;
//            });
//            if (top.isEmpty()) {
//                //..
//                continue;
//            }
//
//            next = top.get(0);
//
//            System.out.println(top);
//
//
//        }
//
//        System.out.println();
//        System.out.println(spoken);
//    }
//
//    public static Term hear(Term pred, Termed word) {
//
//        return $.$(pred + ":{" + word + "}");
//        //return $.image(1, hear, $.sete(word.term()) );
//    }
//}
