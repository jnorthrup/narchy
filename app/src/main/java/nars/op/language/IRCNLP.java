
package nars.op.language;

import nars.*;
import nars.attention.What;
import nars.derive.Derivers;
import nars.derive.impl.BatchDeriver;
import nars.exe.impl.UniExec;
import nars.op.TaskLeak;
import nars.op.language.util.IRC;
import nars.op.stm.ConjClustering;
import nars.term.Term;
import nars.time.clock.RealTime;
import nars.truth.Truth;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.pircbotx.exception.IrcException;
import org.pircbotx.hooks.events.MessageEvent;
import org.pircbotx.hooks.events.PrivateMessageEvent;
import org.pircbotx.hooks.types.GenericMessageEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;

import static nars.Op.*;
import static nars.time.Tense.ETERNAL;

/**
 * http:
 * <p>
 * $0.9;0.9;0.99$
 * <p>
 * $0.9;0.9;0.99$ (hear(?someone, $something) ==>+1 hear(I,$something)).
 * $0.9;0.9;0.99$ (((hear(#someone,#someThing) &&+1 hear(#someone,$nextThing)) && hear(I, #someThing)) ==>+1 hear(I, $nextThing)).
 * $0.9;0.9;0.99$ (((hear($someone,$someThing) &&+1 hear($someone,$nextThing)) <=> hear($someone, ($someThing,$nextThing)))).
 * $0.9;0.9;0.99$ (((I<->#someone) && hear(#someone, $something)) ==>+1 hear(I, $something)).
 * $0.9;0.9;0.99$ hear(I, #something)!
 * hear(I,?x)?
 * <p>
 * $0.9$ (($x,"the") <-> ($x,"a")).
 * ((($x --> (/,hear,#c,_)) &&+1 ($y --> (/,hear,#c,_))) ==> bigram($x,$y)).
 */
public class IRCNLP extends IRC {
    private static final Logger logger = LoggerFactory.getLogger(IRCNLP.class);


    private final NAR nar;


//    private final boolean hearTwenglish = true;


    private final String[] channels;
    private final MyLeakOut outleak;
    final Vocalization speech;

    boolean trace;


    public IRCNLP(NAR nar, String nick, String server, String... channels) {
        super(nick, server, channels);

        this.nar = nar;
        this.channels = channels;
        this.speech = new Vocalization(nar, 2f, this::send);


        outleak = new MyLeakOut(nar, channels);


        /*
        $0.9;0.9;0.99$ (hear(?someone, $something) ==>+1 hear(I,$something)).
 $0.9;0.9;0.99$ (((hear(#someone,#someThing) &&+1 hear(#someone,$nextThing)) && hear(I, #someThing)) ==>+1 hear(I, $nextThing)).
 $0.9;0.9;0.99$ (((hear($someone,$someThing) &&+1 hear($someone,$nextThing)) <=> hear($someone, ($someThing,$nextThing)))).
 $0.9;0.9;0.99$ (((I<->#someone) && hear(#someone, $something)) ==>+1 hear(I, $something)).
 $0.9;0.9;0.99$ hear(I, #something)!
 hear(I,?x)?

 $0.9$ (($x,"the") <-> ($x,"a")).
         */


    }

    /**
     * identical with IRCAgent, TODO share them
     */
    private class MyLeakOut extends TaskLeak {
        public final String[] channels;

        public MyLeakOut(NAR nar, String... channels) {
            super(8, nar);
            this.channels = channels;
        }

        @Override
        public float value() {
            return 1;
        }

        @Override
        protected float leak(Task next, What what) {
            boolean cmd = next.isCommand();
            if (cmd || (trace && !next.isDeleted())) {
                String s = (!cmd) ? next.toString() : next.term().toString();
                Runnable r = IRCNLP.this.send(channels, s);
                if (r != null) {
                    nar.runLater(r);
                    if (NAL.DEBUG && !next.isCommand())
                        logger.info("{}\n{}", next, next.proof());
                } else {

                }
                return cmd ? 0 : 1;
            }
            return 0;
        }

        @Override
        public boolean filter(@NotNull Task next) {
            if (trace || next.isCommand())
                return super.filter(next);
            return false;
        }
    }

    public void setTrace(boolean trace) {
        this.trace = trace;
    }


    void hear(String text, String src) {

        NARHear.hearIfNotNarsese(nar, text, src, (t) -> {
            return new NARHear(nar, NARHear.tokenize(t.toLowerCase()), src, 200);


        });
    }

    @Override
    public void onPrivateMessage(PrivateMessageEvent event) {

    }

    @Override
    public void onGenericMessage(GenericMessageEvent event) {

        if (event instanceof MessageEvent) {
            MessageEvent pevent = (MessageEvent) event;

            if (pevent.getUser().equals(irc.getUserBot())) {
                return;
            }

            String msg = pevent.getMessage().trim();

            String src = pevent.getUser().getNick();
            String channel = pevent.getChannel().getName();

            try {

                hear(msg, src);

            } catch (Exception e) {
                pevent.respond(e.toString());
            }


        }


    }


    public static void main(String[] args) {


        float durFPS = 1f;
        //NAR n = NARS.realtime(durFPS).get();
        //new MatrixDeriver(Derivers.nal(n, 0, 8), n);
        NAR n = new NARS.DefaultNAR(8, true)
                .exe(
                        //new MultiExec.WorkerExec(new Valuator.DefaultValuator(0.5f),4)
                        new UniExec()
                )
                .time(new RealTime.MS(false).durFPS(durFPS)).get();

        n.freqResolution.set(0.1f);
        n.confResolution.set(0.02f);

        n.termVolumeMax.set(32);

        BatchDeriver d = new BatchDeriver(Derivers.nal(n, 1, 8));
//        d.timing = new ActionTiming(n);

        ConjClustering conjClusterBinput = new ConjClustering(n, BELIEF,
                Task::isInput,
                //t->true,
                32, 256);
        n.start(conjClusterBinput);

        /*@NotNull Default n = new Default(new Default.DefaultTermIndex(4096),
            new RealTime.DS(true),
            new TaskExecutor(256, 0.25f));*/


//        new Thread(() -> {
//            try {
//                new TextUI(n, 1024);
//            } catch (IOException e) {
//                e.printStackTrace();
//            }
//        }).start();


        IRCNLP bot = new IRCNLP(n,

                "nar" + Math.round(64 * 1024 * Math.random()),
                "irc.freenode.net",
                "#nars"

        );
        new Thread(()-> {
            try {
                bot.start();
            } catch (IOException e) {
                e.printStackTrace();
            } catch (IrcException e) {
                e.printStackTrace();
            }
        }).start();



        Term HEAR = $.the("hear");


        n.onTask(t -> {


            long taskTime = t.mid();
            if (taskTime != ETERNAL) {
                if (t.isGoal() && t.isPositive()) { //t.isBeliefOrGoal() /* BOTH */) {
                    //long now = n.time();
                    //int dur = n.dur();
                    //if (taskTime >= now - dur) {
                    Term tt = t.term();
                        if (tt.op() == INH && HEAR.equals(tt.sub(1))) {
                            if (tt.subIs(0, PROD) && tt.sub(0).sub(0).op().taskable) {
                                bot.speak(tt.sub(0).sub(0), taskTime, t.truth());
                            }
                        }
                    //}
                }
            }
        }, GOAL);


        n.synch();

        //NARHear.readURL(n);
        //NARHear.readURL(n, "http://w3c.org");

        //n.logPriMin(System.out, 0.9f);
        n.log();



        n.startFPS(5f);
//        n.loop.throttle.set(0.5f);

//        while (true) {
//          n.run(1000);
//          Util.sleepMS(10);
//        }


        Thread.currentThread().setDaemon(true);
    }


    private void speak(Term word, long when, @Nullable Truth truth) {
        speech.speak(word, when, truth);
    }


    String s = "";
    int minSendLength = 1;

    protected float send(Term o) {
        Runnable r = null;
        synchronized (channels) {
            String w = $.unquote(o);
            boolean punctuation = w.equals(".") || w.equals("!") || w.equals("?");
            this.s += w;
            if (!punctuation)
                s += " ";
            if ((!s.isEmpty() && punctuation) || this.s.length() >= minSendLength) {

                r = IRCNLP.this.send(channels, this.s.trim());
                this.s = "";
            }
        }


        if (r != null) {
            r.run();

        }

        return 1;
    }

}
