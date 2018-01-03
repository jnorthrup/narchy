package nars;

import com.google.common.base.Joiner;
import nars.audio.NARHear;
import nars.exe.MultiExec;
import nars.op.AtomicExec;
import nars.op.Operator;
import nars.op.java.Opjects;
import nars.op.nlp.Hear;
import nars.op.stm.ConjClustering;
import nars.term.atom.Atomic;
import nars.term.sub.Subterms;
import nars.time.RealTime;
import nars.time.Tense;
import org.jetbrains.annotations.Nullable;

import static nars.Op.BELIEF;

public class NARchy extends NARS {


    public static NAR ui() {
        NAR nar = new DefaultNAR(8, true)
                .exe(new MultiExec(512, 2, 64))
//                .exe(new AbstractExec(64) {
//                    @Override
//                    public boolean concurrent() {
//                        return true;
//                    }
//                })
                .deriverAdd(1, 6)
                .time(new RealTime.CS().durFPS(10f))
                //.memory("/tmp/nal")
                .get();


        ConjClustering conjClusterB = new ConjClustering(nar, BELIEF, (Task::isInput), 16, 64);
        //ConjClustering conjClusterG = new ConjClustering(nar, GOAL, true, false, 16, 64);

        new NARHear(nar);
        
        Hear.readURL(nar);

        installSpeech(nar);

        return nar;
    }

    public static NAR core() {
        /** TODO differentiate this from UI, for use in embeddeds/servers without GUI */
        return ui();
    }

    public static class Speech {
        private final NAR nar;
        private final AtomicExec sayer;
        private final Opjects op;

        public Speech() {
            //for proxy
            nar = null; sayer = null; op = null;
        }

        public Speech(NAR nar) {
            this.nar = nar;

            nar.onOp("say", sayer = new AtomicExec((t, n) -> {
                @Nullable Subterms args = Operator.args(t);
                if (args.AND(x -> !x.op().var)) {
                    String text = Joiner.on(", ").join(args);
                    if (text.isEmpty())
                        return;

//                    Term tokens = $.conj(Twokenize.twokenize(text).stream()
//                            .map(x -> $.func("say", $.the(x.toString())))
//                            .toArray(Term[]::new));

                    System.err.println(text);
                    //MaryTTSpeech.speak(text);

                    Atomic qt = $.quote(text);
                    n.believe($.func("say", qt), Tense.Present);
                    Hear.hearText(n, $.unquote(qt), n.self().toString(), 200, this.nar.priDefault(BELIEF));
                }
            }, 0.51f));


            this.op = new Opjects(nar);
            op.the("speech", this);

            chatty();
        }


        public void quiet() {
            sayer.exeThresh.set(1f);
        }

        public void normal() {
            sayer.exeThresh.set(0.75f);
        }

        public void chatty() {
            sayer.exeThresh.set(0.51f);
        }
    }

    public static void installSpeech(NAR nar) {


        //MaryTTSpeech.speak(""); //forces load of TTS so it will be ready ASAP and not load on the first use

        new Speech(nar);

//            try {
//                //nar.believe($.$("(hear:$1 ==>+1 say:$1)"), Tense.Eternal);
//                //nar.believe($.$("(say:$1 ==>+1 hear:$1)"), Tense.Eternal);
//                nar.goal($.$("say(#1)"), Tense.Eternal, 1f);
//                nar.goal($.$("(hear:#1 &&+1 say:#1)"), Tense.Eternal, 1f);
//                nar.goal($.$("((hear(#1) &&+1 hear(#2)) &&+1 say(#1,#2))"), Tense.Eternal, 1f);
//            } catch (Narsese.NarseseException e) {
//                e.printStackTrace();
//            }

    }
}
