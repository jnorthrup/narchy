//package nars.nlp;
//
//import org.eclipse.collections.api.set.primitive.MutableCharSet;
//import org.eclipse.collections.api.tuple.primitive.CharFloatPair;
//import org.eclipse.collections.impl.map.mutable.primitive.CharFloatHashMap;
//import org.eclipse.collections.impl.set.mutable.primitive.CharHashSet;
//import nars.Global;
//import nars.NAR;
//import nars.Symbols;
//import nars.concept.Concept;
//import nars.data.Range;
//import nars.nar.Default;
//import nars.op.NarQ;
//import nars.op.NarQ.InputTask;
//import nars.op.java.Lobjects;
//import nars.op.java.MethodOperator;
//import nars.task.Task;
//import nars.term.Termed;
//import nars.util.data.MutableInteger;
//import org.apache.commons.lang3.mutable.MutableFloat;
//
//import java.util.ArrayList;
//import java.util.Collection;
//import java.util.List;
//import java.util.TreeSet;
//import java.util.function.DoubleSupplier;
//
///**
// * Tool for measuring ability to learn (predict) symbol
// * pattern sequences (ie. grammars)
// *
// * Two modes:
// *      synchronous - where the symbol advances at a regular scheduler
// *      asynch - where NARS invokes a 'next' method to trigger the next
// */
//public class GrammarTest2 {
//
//    @Range(min=0, max=1f)
//    public final MutableFloat executionFilter;
//
//    int delay = 0;
//
//    @Range(min=1, max=64)
//    public final MutableInteger charRate = new MutableInteger(32);
//
//
//    protected float reward = 0; //current reward
//
//    public final Tape tape;
//    protected final Default n;
//    public NarQ q;
//
//    public GrammarTest2() throws Exception  {
//        n = new Default(2000, 8, 3, 3);
//
//        this.executionFilter = n.executionThreshold;
//        Lobjects o = new Lobjects(n);
//        this.tape = o.the("tape", Tape.class,
//                //"aaabbbaaabbb               ",
//                "abbbccc",
//                charRate);
//    }
//
//    protected void run() throws Exception {
//        Global.DEBUG = true;
//
//
//
//
//
//        q = new NarQ(n);
//
//        //n.input("(#x-->(/,^Tape,current,tape,(),_))? :/:");
//        //n.input("<?x ==> ($y-->(/,^Tape,current,tape,(),_))>?");
//
//        //n.input("($1 --> ")
//
//        //INPUTS
//        /*i.add(new DoubleSupplier() {
//            @Override public double getAsDouble() {
//                return tape.phase();
//            }
//        });*/
//        q.input.addAll(tape.getCharSensors());
//
//        q.goal.put(() -> {
//            float s = tape.prevScore;
//            //float p = tape.charPeriod.intValue() != 1 ? 1f - tape.phase(n.time()) : 1f;
//            float p = 1f;
//            return (reward = s * p);
//        }, new MutableFloat(1f));
//
//        //q.outs.add(NarQ.NullAction);
//        //q.outs.add(new InputTask(n, n.term("Tape(prev, tape, (), #1)")));
//        q.output.add(new InputTask(n, n.term("Tape(current, tape, (), #1)")));
//        //q.outs.addAll(tape.getPredictActions(n,false, false));
//        q.output.addAll(tape.getPredictActions(n,true, false));
//
//        //n.logSummaryGT(System.out, 0.5f);
//
//        q.power.setValue(0.3f);
//        n.conceptActivation.setValue(0.9f);
//
//        //n.memory.executionThreshold.setValue(0.55f);
//        n.premiser.confMin.setValue(0.01f);
//
//        //n.memory.executionThreshold.setValue(.6f);
//        n.DEFAULT_JUDGMENT_PRIORITY = 0.5f;
//        n.DEFAULT_GOAL_PRIORITY = 0.5f;
//
//        n.shortTermMemoryHistory.set(2);
//        n.cyclesPerFrame.set(2);
//
//        //n.initNAL9();
//
//
//        n.onFrame((nn) -> {
//           tape.setTime(nn.time(), this::onStep);
//        });
//
//        tape.charSet();
//
//
//        //for (int f = 0; f < frames; f++) {
//        while (true) {
//
//            n.step();
//        }
//
//        //dump(n);
//
//    }
//
//    private void dump(Default n) {
//        n.core.active.forEach(10, b -> {
//            Concept c = b.get();
//            if (c.hasBeliefs())
//                c.beliefs().print(System.out);
//        });
//    }
//
//    public static void main(String[] args) throws Exception {
//        new GrammarTest2().run();
//    }
//
//
////    public void train() {
////
////        //n.input("UnitValTaskInc(move,h,((--,true)),#x). :|:  %0.0;0.75%");
////        n.input("UnitValTaskInc(move,h,((--,true)),#x)! :|:  %1.0;0.75%");
////        //n.input("UnitValTaskInc(move,h,(true),#x). :|:  %0.0;0.75%");
////        n.input("UnitValTaskInc(move,h,(true),#x)! :|: %1.0;0.75%");
////        n.input("(true -->(/,^UnitValTaskInc,move,h,(?p),_))! :|:");
////        n.input("((--,true) -->(/,^UnitValTaskInc,move,h,(?p),_))! :|:");
////        n.input("((--,true) -->(/,^UnitValTaskInc,move,h,(?p),_))! :|:");
////
////
//////        if (Math.random() < 0.15f) {
////        //h.inc(Math.random() < 0.5);
//////        }
////        ////(-1-->(/,^UnitVal_compare,h,(0.61368304,0.1),_)).
////        //n.input("(-1-->(/,^UnitVal_compare,h,#p,_))! %0%");
////        //n.input("(1-->(/,^UnitVal_compare,h,#p,_))! %0%");
////
////
////        //d.input("h(1)! :|: %0.65%");
////        //d.input("h(0)! :|: %0.65%");
////        //d.input("<dx --> zero>! :|:");
////        //d.input("<dy --> zero>! :|:");
////    }
//
//    protected void onStep() {
//
//    }
//
//    /** Grammar Trainer */
//    public static class Tape {
//
//        public final String buffer;
//
//        public final CharHashSet chars;
//
//        public StringBuilder log;
//        float prevScore, totalScore = 0;
//        private long time;
//        final MutableInteger charPeriod; //cycles per char
//        protected char current;
//        private char prev;
//        protected char next;
//        private long currentTime;
//        protected float coherence;
//
//        public Tape(String tape, MutableInteger charRate) {
//            this.buffer = tape;
//            this.charPeriod = charRate;
//            time = 0;
//            this.log = new StringBuilder(1024);
//
//            chars = new CharHashSet();
//            chars.addAll(tape.toCharArray());
//
//
//            setTime(0, null);
//        }
//
//        private Character[] chars() {
//            TreeSet<Character> s = charSet();
//            return s.toArray(new Character[s.size()]);
//        }
//
//        public TreeSet<Character> charSet() {
//            TreeSet<Character> s = new TreeSet();
//            chars.forEach(s::add);
//            return s;
//        }
//
////        public char predict(char next) {
////            char actual = c(i++);
////            log.append(actual).append(next); //pairs of chars
////            System.out.println(next + " ?= " + actual + " " + score());
////            score += (actual == next) ? 1 : 0;
////            return actual;
////        }
//
//        private char c(long p) {
//            if (p < 0) return '?';
//            return buffer.charAt( (int)(p % buffer.length()) );
//        }
//
//
//        private void setTime(long t, Runnable onStep) {
//            this.currentTime = t;
//
//            score();
//
//            if (onStep!=null)
//                onStep.run();
//
//            //System.out.println("time=" + t + ", " + (time % charRate));
//            double cp = charPeriod.intValue();
//            if (t % cp == (cp-1)) {
//
//
//
//                this.time++;
//
//                current = c(time);
//                current();  //inputs as belief
//                prev = c(time-1);
//                prev(); //inputs as belief
//                next = c(time+1);
//
//            }
//
//
//
//
//        }
//
//        private void score() {
//
//            //(tt.freq())-0.5f * tt.conf();
//
//            //reward and punish more as it becomes sooner to next update
//            //float p = (1f+phase/2f)/charPeriod * strength;
//
//            //float equalsCurrent = current == c ? 0.5f : 0;
//            //float equalsNext = next == c ? 1 : 0;
//
//            if (!votes.isEmpty()) {
//                CharFloatPair m = votes.keyValuesView().maxBy(CharFloatPair::getTwo);
//
//                float total = (float)votes.keyValuesView().sumOfFloat(CharFloatPair::getTwo);
//
//                if (m != null) {
//                    char predicted = m.getOne();
//                    float coh = m.getTwo() / total;
//
//                    float s = (predicted == next) ? 1 : -1;
//
//                    //            System.out.println("@" + time + " " + prev + "," + current + "," + next +
//                    //                    "  ... score+="+ ds + ", total=" + totalScore
//                    //                    //+ "\n\t" + tt
//                    //            );
//                    float score = s;// * coh;
//
//                    totalScore += score;
//                    prevScore = score;
//                    coherence = coh;
//
//                }
//
//                MutableCharSet keys = votes.keySet();
//                float decay = 0.5f;
//                keys.forEach(c -> {
//                    votes.put(c, votes.getIfAbsent(c, 0) * decay);
//                });
//                return;
//
//            }
//
//            prevScore = 0f;
//            coherence = 0;
//        }
//
//        public char prev() {
//            return prev;
//        }
//
//        public char current() {
//            return current;
//        }
//
//        private char next() {
//            return next;
//        }
//
//
//
//        /** time since last update to next char */
//        private float phase(long t) {
//            float cp = charPeriod.intValue();
//            if (cp == 1)
//                return 1f;
//            return (t % cp) / ((float)(cp)-1);
//        }
//
//
//
//        private List<DoubleSupplier> getCharSensors() {
//            List<DoubleSupplier> d = Global.newArrayList();
//            for (char c : chars()) {
//                d.add(new DoubleSupplier() {
//                    @Override public double getAsDouble() {
//                        //measure of recency/relevancy
//                        if (current == c) return 1f;
//                        //if (prev == c) return -0.5f;
//                        return 0f;
//
////                        switch (dt) {
////                            case 0: return current == c ? 1 : -1;
////                            case -1: return prev == c ? 1 : -1;
////                            default:
////                                return 0; //invalid deltatime
////                        }
//                    }
//
//                });
//            }
//            return d;
//        }
//
//        public void predict(String s) {
//            final int l = s.length();
//            if (l==3)                  {
//                if ((s.charAt(0) == '\"') && (s.charAt(2) == '\"'))
//                   predict(s.charAt(1));
//            } else if (l==1) {
//                predict(s.charAt(0));
//            }
//
//        }
//
//        final CharFloatHashMap votes = new CharFloatHashMap();
//
//        private void predict(char c) {
//            final Task tt = MethodOperator.invokingTask();
//
//            float strength =  tt.motivation();// * phase(tt.occurrence());
//            votes.put(c, votes.getIfAbsent(c, 0) + strength);
//
//
//            /*System.out.println(time + "," + prev + "," + current + "," + c +
//                    //","+ strength +
//                    "," + totalScore +","+ tt.log() + "," + tt.truth()
//                    //+ "\n\t" + tt
//            );*/
//
//        }
//
//        private Collection<NarQ.Action> getPredictActions(NAR n, boolean goalOrBelief, boolean invert) {
//            List<NarQ.Action> l = new ArrayList();
//            for (char c : chars()) {
//                Termed t = n.term("Tape(predict, tape, (\"" + c + "\"), #1)");
//                l.add(
//                    new InputTask(n, t, goalOrBelief ? Symbols.GOAL : Symbols.BELIEF, invert)
//                );
//            }
//            return l;
//        }
//
//        char _prediction() {
//            if (votes.isEmpty())
//                return 'a'-1;
//            CharFloatPair m = votes.keyValuesView().maxBy(CharFloatPair::getTwo);
//            return m.getOne();
//        }
//    }
//}
