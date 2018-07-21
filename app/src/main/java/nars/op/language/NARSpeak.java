package nars.op.language;

import jcog.Util;
import jcog.event.ListTopic;
import jcog.event.Topic;
import nars.$;
import nars.NAR;
import nars.NARS;
import nars.Narsese;
import nars.op.java.Opjects;
import nars.term.Term;
import nars.time.Tense;
import spacegraph.audio.speech.NativeSpeechDispatcher;

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
    public static class VocalCommentary {
        public VocalCommentary(Term happy, NAR nar) {

            nar.on1("str", (Term t) -> $.quote(t.toString()));

            try {
                nar.want($.$("say(ready)"), Tense.Present, 1f, 0.9f);
                nar.believe($.$("(" + happy + " =|> say(happy))"));
                nar.want($.$("(" + happy + " &| say(happy))"));
                nar.believe($.$("(" + happy.neg() + " =|> say(sad))"));
                nar.want($.$("(" + happy.neg() + " &| say(sad))"));
                nar.want($.$("(#x &| say(#x))"));
                nar.believe($.$("($x =|> say($x))"));
                nar.want($.$("say(#1)"));
            } catch (Narsese.NarseseException e) {
                e.printStackTrace();
            }

        }
    }

    public static void main(String[] args) throws Narsese.NarseseException {
        NAR n = NARS.realtime(10f).get();

        NARSpeak speak = new NARSpeak(n);
        speak.spoken.on(new NativeSpeechDispatcher()::speak);


        n.startFPS(2f);

        n.log();

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
