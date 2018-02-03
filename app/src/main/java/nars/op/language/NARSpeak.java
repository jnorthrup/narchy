package nars.op.language;

import com.google.common.base.Joiner;
import jcog.event.ListTopic;
import jcog.event.Topic;
import nars.$;
import nars.NAR;
import nars.NAgent;
import nars.Narsese;
import nars.op.AtomicExec;
import nars.op.Operator;
import nars.op.java.Opjects;
import nars.subterm.Subterms;
import nars.term.atom.Atomic;
import nars.time.Tense;
import net.beadsproject.beads.ugens.Clock;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;

import static nars.Op.BELIEF;

/** TODO make extend NARService and support start/re-start */
public class NARSpeak {
    private final NAR nar;
    private final AtomicExec sayer;
//    private final Opjects op;

    /** emitted on each utterance */
    public final Topic<Object> spoken = new ListTopic();

    public NARSpeak(NAR nar) {
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

                spoken.emit(text);
                //System.err.println(text);
                //MaryTTSpeech.speak(text);

                Atomic qt = $.quote(text);
                n.believe($.func("say", qt), Tense.Present);
                NARHear.hearText(n, $.unquote(qt), n.self().toString(), 200, this.nar.priDefault(BELIEF));
            }
        }, 0.51f));


//        this.op = new Opjects(nar);
        //SpeechControl speechControl = new SpeechControl();
//        op.the("speech", SpeechControl.class);

        //speechControl.chatty();
    }

    public class SpeechControl {

        public SpeechControl() { }

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

    //MaryTTSpeech.speak(""); //forces load of TTS so it will be ready ASAP and not load on the first use

//            try {
//                //nar.believe($.$("(hear:$1 ==>+1 say:$1)"), Tense.Eternal);
//                //nar.believe($.$("(say:$1 ==>+1 hear:$1)"), Tense.Eternal);
//                nar.goal($.$("say(#1)"), Tense.Eternal, 1f);
//                nar.goal($.$("(hear:#1 &&+1 say:#1)"), Tense.Eternal, 1f);
//                nar.goal($.$("((hear(#1) &&+1 hear(#2)) &&+1 say(#1,#2))"), Tense.Eternal, 1f);
//            } catch (Narsese.NarseseException e) {
//                e.printStackTrace();
//            }


    /** 'speechd' speech dispatcher - executes via command line */
    public static class CmdlineSpeechDispatcher {

        public CmdlineSpeechDispatcher(NARSpeak s) {
            s.spoken.on(this::speak);
        }

        private void speak(Object x) {
            String s = x.toString();
            try {
                //TODO semaphore to limit # of simultaneous voices
                Process p = new ProcessBuilder().command("/usr/bin/speak", "\"" + s +"\"").start();
                p.onExit().handle((z,y)->{
                    //System.out.println("done: " + z);
                    return null;
                });
            } catch (IOException e) {
                e.printStackTrace();
            }

        }

    }

    private static class VocalCommentary {
        public VocalCommentary(Clock ac, NAgent a) {

            new NARSpeak(a.nar);

            try {
                a.nar.goal($.$("speak(ready)"), Tense.Present, 1f, 0.9f);
//                a.nar.believe($("(" + a.sad + " =|> speak(sad))."));
//                a.nar.goal($("(" + a.sad + " &| speak(sad))"));
                a.nar.believe($.$("(" + a.happy + " =|> speak(happy))."));
                a.nar.goal($.$("(" + a.happy + " &| speak(happy))"));
            } catch (Narsese.NarseseException e) {
                e.printStackTrace();
            }

        }
    }
}
