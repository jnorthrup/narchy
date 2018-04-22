
package nars.op.language;

import nars.*;
import nars.bag.leak.TaskLeak;
import nars.op.language.util.IRC;
import nars.term.Term;
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

import static nars.Op.INH;
import static nars.Op.PROD;
import static nars.util.time.Tense.ETERNAL;

/**
 * http://englishlearning.webgarden.es/menu/1st-and-2nd-eso-year/easy-reading-texts
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

    //private final Talk talk;
    private final NAR nar;
    //private float ircMessagePri = 0.9f;

    private final boolean hearTwenglish = true;

    //    final int wordDelayMS = 25; //for serializing tokens to events: the time in millisecond between each perceived (subvocalized) word, when the input is received simultaneously
    private final String[] channels;
    private final MyLeakOut outleak;
    final SpeechPlan speech;

    boolean trace;

//    final ArrayBag<String, PriReference<String>> out = new PLinkArrayBag<>(16, PriMerge.max, new ConcurrentHashMap());
//    final ArrayBag<String, PriReference<String>> prevOut = new PLinkArrayBag<>(512, PriMerge.max, new ConcurrentHashMap());

    public IRCNLP(NAR nar, String nick, String server, String... channels) {
        super(nick, server, channels);

        this.nar = nar;
        this.channels = channels;
        this.speech = new SpeechPlan(nar, 2f, this::send);

//        new Thread(()->{
//            while (true) {
//                if (!out.isEmpty()) {
//                    try {
//                        PLink<String> next = out.pop();
//                        String s = next.get();
//                        send(channels, s).run();
//                        prevOut.commit();
//                        prevOut.put(next);
//                    } catch (Exception e) {
//                        e.printStackTrace();
//                    }
//                }
//                Util.pause(5500);
//            }
//        }).start();

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
//        nar.input(
//                "$0.9;0.9;0.99$ (hear(?someone, $something) ==>+0 hear(I,$something)).",
//                "$0.9;0.9;0.99$ (((hear(#someone,#someThing) &&+1 hear(#someone,$nextThing)) &&+0 hear(I, #someThing)) ==>+1 hear(I, $nextThing)).\n",
//                //"$0.9;0.9;0.99$ (((hear(#someone,$someThing) &&+1 hear(#someone,$nextThing)) <=> hear($someone, ($someThing,$nextThing)))).",
//                "$0.9;0.9;0.99$ hear(I, #something)!",
//                //"(((I<->#someone) && hear(#someone, $something)) ==>+1 hear(I, $something)).",
//                "$0.9;0.9;0.99$ hear(I,?x)?"
//        );
//        final Atomic HEAR = $.the("hear");
//        final Atomic I = $.the("I");
//        nar.onTask(tt->{
//            //HACK
//            if (tt.isBelief()) {
//                if (Math.abs(tt.occurrence()-nar.time()) < 100) {
//                    if (tt.op() == INH && tt.term(0).op() == PROD && tt.term(1).equals(HEAR)) {
//                        Compound arg = (Compound) tt.term(0);
//                        if (arg.term(0).equals(I)) {
//                            Term w = arg.term(1);
//                            if (!(w instanceof Variable)) {
//                                say(channels, w);
//                            }
//                        }
//                    }
//                }
//            }
//        });

    }

    /**
     * identical with IRCAgent, TODO share them
     */
    private class MyLeakOut extends TaskLeak {
        public final String[] channels;

        public MyLeakOut(NAR nar, String... channels) {
            super(8, 0.05f, nar);
            this.channels = channels;
        }

        @Override
        public float value() {
            return 1;
        }

        @Override
        protected float leak(Task next) {
            boolean cmd = next.isCommand();
            if (cmd || (trace && !next.isDeleted())) {
                String s = (!cmd) ? next.toString() : next.term().toString();
                Runnable r = IRCNLP.this.send(channels, s);
                if (r != null) {
                    nar.runLater(r);
                    if (Param.DEBUG && !next.isCommand())
                        logger.info("{}\n{}", next, next.proof());
                } else {
                    //..?
                }
                return cmd ? 0 : 1; //no cost for command outputs
            }
            return 0;
        }

        @Override
        public boolean preFilter(@NotNull Task next) {
            if (trace || next.isCommand())
                return super.preFilter(next);
            return false;
        }
    }

    public void setTrace(boolean trace) {
        this.trace = trace;
    }

    //    abstract class IRCBotOperator extends TermProcedure {
//
//
//        public IRCBotOperator(String id) {
//            super(id);
//        }
//
//
//        @Nullable
//        @Override
//        public final Object function(Compound arguments, TermIndex i) {
//            Object y = function(arguments);
//
//            if (y!=null)
//                send( y.toString().replace("\n"," ") );
//
//            //loop back as hearing
//            //say($.quote(y.toString()), $.p(nar.self, $.the("controller")));
//
//            return y;
//        }
//
//        protected abstract Object function(Compound arguments);
//
//
//    }

//    abstract class IRCBotOperator implements Operator.CommandOperator {
//
//
//        @Nullable
//        @Override
//        public final synchronized void run(Atom op, Term[] args, NAR nar) {
//
//            Object y = function(args);
//
//            if (y != null)
//                broadcast(y.toString().replace("\n", " "));
//
//            //loop back as hearing
//            //say($.quote(y.toString()), $.p(nar.self, $.the("controller")));
//
//            return null;
//        }
//
//        protected abstract Object function(Term[] arguments);
//
//    }

    void hear(String text, String src) {

        NARHear.hearIfNotNarsese(nar, text, src, (t) -> {
            return new NARHear(nar, NARHear.tokenize(t.toLowerCase()), src, 200);
//            Compound f = $.func("SENTENCE", Hear.tokenize(t));
//            nar.believe(0.5f, f, Tense.Present, 1f, 0.9f);
//            return null;
        });
    }

    @Override
    public void onPrivateMessage(PrivateMessageEvent event) {
        //hear(event.getMessage(), event.getUser().toString());
    }

    @Override
    public void onGenericMessage(GenericMessageEvent event) {

        if (event instanceof MessageEvent) {
            MessageEvent pevent = (MessageEvent) event;

            if (pevent.getUser().equals(irc.getUserBot())) {
                return; //ignore own messages (echo)
            }

            String msg = pevent.getMessage().trim();

            String src = pevent.getUser().getNick(); //TODO use hostmask ?
            String channel = pevent.getChannel().getName();

            try {

                hear(msg, src);

            } catch (Exception e) {
                pevent.respond(e.toString());
            }


            //logger.info("hear({},{}): {}", channel, nick, msg);
            //talk.hear(msg, context(channel, nick), ircMessagePri);
        }


    }


    public static void main(String[] args) {

        //

        float durFPS = 20f;
        NAR n = NARS.realtime(durFPS).get();

        n.activationRate.set(0.2f);
        n.forgetRate.set(1f);

        n.freqResolution.set(0.2f);
        n.confResolution.set(0.05f);

        n.termVolumeMax.set(48);

        /*@NotNull Default n = new Default(new Default.DefaultTermIndex(4096),
            new RealTime.DS(true),
            new TaskExecutor(256, 0.25f));*/

//        NARS n = new NARS(new RealTime.DS(true), new XorShift128PlusRandom(1), 1);
//        n.addNAR(16, 0.25f);
        //n.addNAR(512, 0.25f);


        //n.logBudgetMin(System.out, 0.75f);

        new Thread(() -> {
            try {
                new TextUI(n, 1024);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }).start();


        IRCNLP bot = new IRCNLP(n,
                //"exp" + Math.round(10000 * Math.random()),
                "nar" + Math.round(64 * 1024 * Math.random()),
                "irc.freenode.net",
                "#123xyz"
                //"#netention"
        );


        Term HEAR = $.the("hear");
        //

        n.onTask(t -> {
            //if (t.isGoal()) {
            //feedback
            Term tt = t.term();
            long start = t.start();
            if (start != ETERNAL) {
                if (t.isBeliefOrGoal() /* BOTH */) {
                    long now = n.time();
                    int dur = n.dur();
                    if (start >= now - dur) {
                        if (tt.op()==INH && HEAR.equals(tt.sub(1))) {
                            if (tt.subIs(0, PROD) && tt.sub(0).subIs(0, Op.ATOM)) {
                                bot.speak(tt.sub(0).sub(0), start, t.truth());
                            }
                        }
                    }
                }
            }
        });


        //n.log();

//        for (int i = 0; i < 2; i++) {
//            Hear.hear(n, "do you know what is a sentence?", "", 100, 0.5f);
//            Util.sleep(1000);
//
//            Hear.hear(n, "i know that and this is a sentence.", "", 100, 0.5f);
//            Util.sleep(1000);
//        }

        //n.clear();

//        new Loop(1 / 15f) {
//
//            final Term[] promptWord = new Term[]{
//                    $.varDep(1),
//                    $.quote("and"),
//                    $.quote("a"),
//                    $.quote("is"),
//                    //$.quote("then")
//                    /* so, now, etc */
//            };
//
//            @Override
//            public boolean next() {
//                try {
//                    Term word = promptWord[n.random().nextInt(promptWord.length)];
//                    n.input("$0.9 hear(" + word + "). :|:");
//                } catch (Narsese.NarseseException e) {
//                    e.printStackTrace();
//                }
//                return true;
//            }
//        };

        NARHear.readURL(n);
        n.logPriMin(System.out, 0.9f);

        n.start();

        try {
            bot.start();
        } catch (IOException | IrcException e) {
            e.printStackTrace();
        }


//        n.on("say", (x, aa, nn) -> {
//            String msg = Joiner.on(' ').join(
//                Stream.of(aa).map(t -> {
//                    if (t.op()== Op.VAR_DEP) {
//                        return "a";
//                    }
//                    return $.unquote(t);
//                }).toArray()
//            );
//
//            System.out.println(Arrays.toString(aa));
//
//            if (!bot.prevOut.contains(msg)) {
//                bot.out.commit();
//                bot.out.put(new PLink<String>(msg, 1f));
//            }
//        });
//
//        //n.log();
//        //n.logBudgetMin(System.out, 0.6f);
//        //n.log(System.out, x -> x instanceof Task && ((Task)x).isGoal());
//
//        n.input(
//                "( (RANGE:{$range} && SENTENCE:$x) ==> SENTENCE:slice($x, $range)).",
//
//                //"((VERB:{$V} && FRAG($X,$V,$Y)) ==> (((/,MEANS,$X,_),(/,MEANS,$Y,_)) --> (/,MEANS,$V,_))).",
//
//                //"((FRAG:$XY && ($XY <-> flat((FRAG($X), and, FRAG($Y))))) <=> ((/,MEANS,$X,_) && (/,MEANS,$Y,_))).",
//
////                "((VERB:{$V} && SENTENCE($X,$V))                   ==> (((/,MEANS,$X,_),(/,MEANS,$X,_)) --> (/,MEANS,$V,_))).",
////                "((VERB:{$V} && SENTENCE($X,$V,$Y))                ==> (((/,MEANS,$X,_),(/,MEANS,$Y,_)) --> (/,MEANS,$V,_))).",
////                "((&&, VERB:{$V}, ADV:{$A}, SENTENCE($X,$V,$A,$Y)) ==> (((/,MEANS,$X,_),(/,MEANS,$Y,_)) --> (/,MEANS,($V|$A),_))).",
////                "((&&, VERB:{$V}, PREP:{$P}, SENTENCE($X,$V,$P,$Y)) ==> (((/,MEANS,$X,_),$P,(/,MEANS,$Y,_)) --> (/,MEANS,$V,_))).",
////                "((&&, VERB:{$V}, DET:{#a}, SENTENCE($X,$V,#a,$Y)) ==> (((/,MEANS,$X,_),{(/,MEANS,$Y,_)}) --> (/,MEANS,$V,_))).",
////                "((&&, VERB:{$V}, DET:{#a}, SENTENCE(#a,$X,$V,$Y)) ==> (({(/,MEANS,$X,_)},(/,MEANS,$Y,_)) --> (/,MEANS,$V,_))).",
//
//                "(SENTENCE($x,\"is\",$y) <=> ($x --> $y)).",
//                "(SENTENCE($x,\"is\",\"a\",$y) <=> ({$x} --> $y)).",
//                "(SENTENCE($x,\"has\",$y) <=> ($x --> [$y])).",
//                "(SENTENCE($x,\"isnt\",$y) <=> (--,($x --> $y))).",
//                "(SENTENCE($x,\"and\",$y,\"are\",$z) <=> (($x & $y)-->$z)).",
//                "(SENTENCE($x,\"and\",$y) <=> ($x && $y)).",
//                "(SENTENCE($x,\"or\",$y) <=> ($x || $y)).",
//                "(SENTENCE($x,\"then\",$y) <=> ($x &&+1 $y)).",
//                "(SENTENCE($x,\"after\",$y) <=> ($x &&-1 $y)).",
//                "(($x --> \"verb\") <=> VERB:{$x}).",
//                "(($x --> \"noun\") <=> NOUN:{$x}).",
//                "(($x --> \"adverb\") <=> ADV:{$x}).",
//                "(($x --> \"preposition\") <=> PREP:{$x}).",
//                "(#x <-> \"it\"). %0.5;0.5%",
//                "(#x <-> \"that\"). %0.5;0.5%",
//                "(#x <-> \"the\"). %0.5;0.5%",
//                "(#x <-> \"what\"). %0.5;0.5%",
//                "(#x <-> \"who\"). %0.5;0.5%",
//                "(#x <-> \"where\"). %0.5;0.5%",
//                "(#x <-> \"which\"). %0.5;0.5%",
//
//                //NAL9 integration
//                "(believe <-> \"believe\").",
//                "(wonder <-> \"wonder\").",
//                "(want <-> \"want\").",
//
//                //"((WHERE_PREP:{#in} && FRAG(#in,$where)) ==> WHERE:(/,MEANS,$where,_))."
//
//                //"(FRAG<->SENTENCE).", //sentence is also its own fragment
//
//                "RANGE:{(0,2),(0,3),(1,4)}.",
//                //"RANGE:{(0,3),(1,4),(4,7),(0,7)}.",
//                //"RANGE:{(0,3)}.",
//                //"RANGE:{0,1,2,3,4,(0,1),(0,2),(0,3)}.",
//
//                "VERB:{\"is\",\"maybe\",\"isnt\",\"was\",\"willBe\",\"wants\",\"can\",\"likes\"}.",
//                "DET:{\"a\",\"the\"}.",
//                "PREP:{\"for\",\"on\",\"in\",\"with\"}.",
//                "ADV:{\"never\",\"always\",\"maybe\"}.",
//
////                "SENTENCE(tom,is,never,sky).",
////                "SENTENCE(tom,is,always,cat).",
////                "SENTENCE(tom,is,cat).",
////                "SENTENCE(tom,is,a,cat).",
////                "SENTENCE(tom,likes,the,sky).",
////                "SENTENCE(tom,likes,maybe,cat).",
////                "SENTENCE(the,sky,is,blue).",
////                "SENTENCE(a,cat,likes,blue).",
////                "SENTENCE(sky,wants,the,blue).",
////                "SENTENCE(sky,is,always,blue).",
////
////            "SENTENCE(yes,wants,no).",
////            "SENTENCE(yes,is,no).",
////            "SENTENCE(yes,isnt,no).",
////
////            "SENTENCE(it,is,my,good).",
////            //"SENTENCE(it,is,bad,and,it,can,good).",
////            "SENTENCE(good,was,bad).",
////            "SENTENCE(right,willBe,wrong).",
////            "SENTENCE(true,can,false).",
//
//
//
//                //"$0.9$ (?y --> (/,MEANS,?x,_))?",
//
//                //"$0.9$ SENTENCE(?z,?x,?y)?"
//
//                //"$0.9$ SENTENCE(?y)?",
//
//                //"$0.9 (VERB:{$v} ==> say(DO, $v))."
//                //"$0.9 say(#y)!",
//
//                "$0.9 (SENTENCE:$y ==> say:$y).",
//                "$0.9 (SENTENCE:#y && say:#y)!"
//                //"say(cat,is,blue)!",
//                //"say(#x,is,#y)!"
//
//
//        );


    }


    private void speak(Term word, long when, @Nullable Truth truth) {
        speech.speak(word, when, truth);
    }


    String s = "";
    int minSendLength = 24;

    protected float send(Term o) {
        Runnable r = null;
        synchronized (channels) {
            String w = $.unquote(o);
            boolean punctuation = w.equals(".") || w.equals("!") || w.equals("?");
            this.s += w;
            if (!punctuation)
                s += " ";
            if ((!s.isEmpty() && punctuation) || this.s.length() > minSendLength) {
                //speak($.the(s), 0, IRCNLP.this);
                r = IRCNLP.this.send(channels, this.s.trim());
                this.s = "";
            }
        }


        if (r != null) {
            r.run();
            //nar.runLater(r);
        }

        return 1;
    }

}
