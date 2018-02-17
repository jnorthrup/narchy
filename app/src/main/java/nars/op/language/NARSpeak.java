package nars.op.language;

import jcog.Util;
import jcog.event.ListTopic;
import jcog.event.Topic;
import nars.*;
import nars.op.java.Opjects;
import nars.time.Tense;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;

/** TODO make extend NARService and support start/re-start */
public class NARSpeak {
    private final NAR nar;
    //private final AtomicExec sayer;
    private final Opjects op;

    /** emitted on each utterance */
    public final Topic<Object> spoken = new ListTopic();
    private final SpeechControl speech;

    public NARSpeak(NAR nar) {
        this.nar = nar;


//        nar.onOp("say", sayer = new AtomicExec((t, n) -> {
//            @Nullable Subterms args = Operator.args(t);
//            if (args.AND(x -> !x.op().var)) {
//                String text = Joiner.on(", ").join(args);
//                if (text.isEmpty())
//                    return;
//
////                    Term tokens = $.conj(Twokenize.twokenize(text).stream()
////                            .map(x -> $.func("say", $.the(x.toString())))
////                            .toArray(Term[]::new));
//
//                spoken.emitAsync(text, nar.exe);
//
//                //System.err.println(text);
//                //MaryTTSpeech.speak(text);
//
//                n.believe(t, Tense.Present);
//
//                Atomic qt = $.quote(text);
//                NARHear.hearText(n, $.unquote(qt), n.self().toString(), 200, this.nar.priDefault(BELIEF));
//            }
//        }, 0.51f));


        this.op = new Opjects(nar);

        speech = op.the(nar.self(), new SpeechControl(), this);

        op.alias("say", nar.self(), "speak");

    }

    public class SpeechControl {

        public SpeechControl() { }

        public void speak(Object text) {

            spoken.emitAsync(text, nar.exe);

            //System.err.println(text);
            //MaryTTSpeech.speak(text);

            //Atomic qt = $.quote(text);
            //NARHear.hearText(n, $.unquote(qt), n.self().toString(), 200, this.nar.priDefault(BELIEF));
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
    public static class NativeSpeechDispatcher {

        static final Logger logger = LoggerFactory.getLogger(NativeSpeechDispatcher.class);

        //static final int MAX_POLYPHONY = 8;
        //final Semaphore polyphony = new Semaphore(MAX_POLYPHONY, true);
        //final BlockingQueue<Object> q = new ArrayBlockingQueue(MAX_POLYPHONY);

        public NativeSpeechDispatcher(NARSpeak s) {
            s.spoken.on(this::speak);
        }

        public String[] command(String s) {
            return new String[]{
                //"/usr/bin/spd-say", "\"" + s + "\"" //speech-dispatcher -- buffers messages and does not allow multiple voices
                "/usr/bin/espeak-ng", "\"" + s + "\"" //espeak-ng (next generation) -- directly synthesize on command
            };
        }

        private void speak(Object x) {
            String s = x.toString();
            try {
//                try {
//                    if (q.offer)
//                    if (polyphony.tryAcquire(1, TimeUnit.SECONDS)) {

                        //TODO semaphore to limit # of simultaneous voices
                        Process p = new ProcessBuilder()
                                .command(command(s))
                                .start();
                        p.onExit().handle((z, y) -> {
                            //System.out.println("done: " + z);
                            //polyphony.release();
                            return null;
                        }).exceptionally(t->{
                            logger.warn("speech error: {} {}", s, t);
                            //polyphony.release();
                            return null;
                        });
//                    } else {
//                        logger.warn("insufficient speech polyphony, ignored: {}", s);
//                    }

            } catch (IOException e) {
                logger.warn("speech error: {} {}", s, e);
            }

        }

    }

    public static class VocalCommentary {
        public VocalCommentary(NAgent a) {

            new NARSpeak(a.nar);

            try {
                a.nar.goal($.$("speak(ready)"), Tense.Present, 1f, 0.9f);
//                a.nar.believe($("(" + a.sad + " =|> speak(sad))."));
//                a.nar.goal($("(" + a.sad + " &| speak(sad))"));
                a.nar.believe($.$("(" + a.happy + " =|> speak(happy))"));
                a.nar.goal($.$("(" + a.happy + " &| speak(happy))"));
                a.nar.believe($.$("(" + a.happy.neg() + " =|> speak(sad))"));
                a.nar.goal($.$("(" + a.happy.neg() + " &| speak(sad))"));
                a.nar.goal($.$("speak(#1)"));
            } catch (Narsese.NarseseException e) {
                e.printStackTrace();
            }

        }
    }

    public static void main(String[] args) throws Narsese.NarseseException {
        NAR n = NARS.realtime(10f).get();

        NARSpeak speak = new NARSpeak(n);
        new NARSpeak.NativeSpeechDispatcher(speak);

        //new NARSpeak.VocalCommentary(tc);
        n.startFPS(2f);

        n.log();

        n.input("say(abc)! :|:");
        while (true) {
            Util.sleep(2500);
            String word;
            switch (n.random().nextInt(3)) {
                default:
                case 0: word = "x"; break;
                case 1: word = "y"; break;
                case 2: word = "z"; break;
            }
            n.input("say(" + word + ")! :|:");
        }
    }
}
