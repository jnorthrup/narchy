package nars.op.language;

import jcog.Util;
import jcog.event.ListTopic;
import jcog.event.Topic;
import nars.*;
import nars.agent.NAgent;
import nars.concept.Concept;
import nars.derive.Derivers;
import nars.derive.deriver.MatrixDeriver;
import nars.op.java.Opjects;
import nars.term.Functor;
import nars.term.Term;
import org.jetbrains.annotations.Nullable;
import spacegraph.audio.speech.NativeSpeechDispatcher;

import static nars.$.$$;
import static nars.agent.FrameTrigger.durs;

/**
 * TODO make extend NARService and support start/re-start
 */
public class NARSpeak {
    private final NAR nar;

    private final Opjects op;

    /**
     * emitted on each utterance
     */
    public final Topic<Object> spoken = new ListTopic();
    public final SpeechControl speech;

    public NARSpeak(NAR nar) {
        this.nar = nar;


        this.op = new Opjects(nar);

        speech = op.the(nar.self(), new SpeechControl(), this);

        op.alias("say", nar.self(), "speak");

    }

    public class SpeechControl {

        public SpeechControl() {
            chatty();
        }

        public void speak(Object... text) {

            spoken.emitAsync(text, nar.exe);


        }

        public void quiet() {
            op.exeThresh.set(1f);
        }

        public void normal() {
            op.exeThresh.set(0.75f);
        }

        public void chatty() {
            op.exeThresh.set(0.51f);
        }
    }


    /**
     * TODO abstract to more general commentary triggered by any provided event term
     */
    public static class VocalCommentary extends NAgent {
        public VocalCommentary(NAR nar) {
            super("VocalCommentary", durs(1), nar);

            NativeSpeechDispatcher out = new NativeSpeechDispatcher() {
                @Override
                public void speak(String x) {
                    super.speak(x);
                    NARHear.hear(nar, x, nar.self().toString(), nar.dur()/2);
                }
            };

            Vocalization v = new Vocalization(nar, 2f, out::speak);

            //NARSpeak speak = new NARSpeak(nar);
            //speak.spoken.on(out::speak);
            nar.onOp("say", (t,n)->{

                if (t.end() >= n.time()-n.dur()) {
                    Term x = Functor.funcArgsArray(t.term())[0];
                    if (!x.op().var) {
                        v.speak(x, t.end(), t.truth());
                    }
                }
            });



            nar.on1("str", (Term t) -> t.op().var ? t : $.quote(t.toString()));

            Term sayWhat = $$("say(?1)");

            Term saySomething = $$("say(#1)");
            alwaysQuestion(() -> sayWhat, false);
            alwaysQuest(() -> sayWhat, false);

//            alwaysWant(saySomething, 1f);

//            Term sayIt = $.$$("(#x &| say(str(#x)))");
//            alwaysWant(()->sayIt, 1f);

            onFrame(()->{
                if (nar.random().nextFloat() < 0.1f) {
                    @Nullable Concept x = nar.attn.sample(nar.random());
                    if (x.volume() < 5) {
                        try {
                            nar.input("say(" + $.quote(x.toString()) + ")! |");
                        } catch (Narsese.NarseseException e) {
                            e.printStackTrace();
                        }
                    }
                }
            });

            try {
//                nar.want($.$("say(ready)"), Tense.Present, 1f, 0.9f);
//                nar.believe($.$("(" + happy + " =|> say(happy))"));
//                nar.want($.$("(" + happy + " &| say(happy))"));
//                nar.believe($.$("(" + happy.neg() + " =|> say(sad))"));
//                nar.want($.$("(" + happy.neg() + " &| say(sad))"));
                //nar.want($.$("(#x &| say(str(#x)))"));
                nar.believe($.$("($x =|> say(str($x)))"));
                //nar.want($.$("say(#1)"));
            } catch (Narsese.NarseseException e) {
                e.printStackTrace();
            }

        }
    }

    public static void main(String[] args) throws Narsese.NarseseException {
        NAR n = NARS.realtime(10f).get();

        new MatrixDeriver(Derivers.nal(n, 1, 8
                //"curiosity.nal"
                , "motivation.nal"));

        NARSpeak speak = new NARSpeak(n);
        speak.spoken.on(new NativeSpeechDispatcher()::speak);


        n.startFPS(10f);

        //n.log();

        n.onTask(x -> {
           if (x.isGoal() && !x.isInput())
               System.out.println(x.proof());
        });

        n.input("say(abc)! :|:");
        while (true) {
            Util.sleepMS(2500);
            String word;
            switch (n.random().nextInt(3)) {
                default:
                case 0:
                    word = "x";
                    break;
                case 1:
                    word = "y";
                    break;
                case 2:
                    word = "z";
                    break;
            }
            n.input("say(" + word + ")! :|:");
        }


    }
}
