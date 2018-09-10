package nars.op.language;

import nars.NAR;
import nars.Param;
import nars.Task;
import nars.bag.leak.TaskLeak;
import nars.op.language.util.IRC;
import org.jetbrains.annotations.NotNull;
import org.pircbotx.hooks.events.MessageEvent;
import org.pircbotx.hooks.events.PrivateMessageEvent;
import org.pircbotx.hooks.types.GenericMessageEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * $0.9;0.9;0.99$
 *
 * $0.9;0.9;0.99$ (hear(?someone, $something) ==>+1 hear(I,$something)).
 * $0.9;0.9;0.99$ (((hear(#someone,#someThing) &&+1 hear(#someone,$nextThing)) && hear(I, #someThing)) ==>+1 hear(I, $nextThing)).
 * $0.9;0.9;0.99$ (((hear($someone,$someThing) &&+1 hear($someone,$nextThing)) <=> hear($someone, ($someThing,$nextThing)))).
 * $0.9;0.9;0.99$ (((I<->#someone) && hear(#someone, $something)) ==>+1 hear(I, $something)).
 * $0.9;0.9;0.99$ hear(I, #something)!
 * hear(I,?x)?
 *
 * $0.9$ (($x,"the") <-> ($x,"a")).
 * ((($x --> (/,hear,#c,_)) &&+1 ($y --> (/,hear,#c,_))) ==> bigram($x,$y)).
 */
public class IRCAgent extends IRC {
    private static final Logger logger = LoggerFactory.getLogger(IRCAgent.class);

    
    private final NAR nar;
    

    private final boolean hearTwenglish = false;

    final int wordDelayMS = 100; 
    private final MyLeakOut out;

    boolean trace;

    public IRCAgent(NAR nar, String nick, String server, String... channels) {
        super(nick, server, channels);

        this.nar = nar;


        out = new MyLeakOut(nar, channels);
















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

    public void setTrace(boolean trace) {
        this.trace = trace;
    }

    
















































    void hear(String text, String src) {
        NARHear.hear(nar, text, src, hearTwenglish ? wordDelayMS : -1);
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

            //        if (channel.equals("unknown")) return;
            if (msg.startsWith("//"))
                return; //comment or previous output



            String src = pevent.getUser().getNick(); 
            String channel = pevent.getChannel().getName();

            try {

                hear(msg, src);

            } catch (Exception e) {
                pevent.respond(e.toString());
            }


            
            
        }


    }


//    public static void main(String[] args) {
//
//
//
//        @NotNull NAR n = NARchy.ui();
//
//
//        n.termVolumeMax.set(20);
//
//
//
//
//
//
//
//
//
//
//
//
//
//
//
//
//
//        IRCAgent bot = new IRCAgent(n,
//                "experiment1", "irc.freenode.net",
//
//                "#netention"
//
//        );
//
//        n.onOpN("trace", (arg, nn) -> {
//            if (arg.subs() > 0) {
//                switch (arg.sub(0).toString()) {
//                    case "on": bot.setTrace(true); break;
//                    case "off": bot.setTrace(false);  bot.out.clear(); break;
//                }
//            }
//        });
//
//
//        /*
//        n.on("readToUs", (Command) (a, t, nn) -> {
//            if (t.length > 0) {
//                String url = $.unquote(t[0]);
//                if (canReadURL(url)) {
//                    try {
//
//                        Term[] targets;
//                        if (t.length > 1 && t[1] instanceof Compound) {
//                            targets = ((Compound)t[1]).terms();
//                        } else {
//                            targets = null;
//                        }
//
//                        Collection<String> lines = IOUtil.readLines(new URL(url).openStream());
//
//                        new RateIterator<String>(lines.iterator(), 2)
//                                .threadEachRemaining(l -> {
//
//                                    bot.hear(l, nn.self().toString());
//
//                                    if (targets == null) {
//                                        bot.broadcast(l);
//                                    } else {
//                                        for (Term u : targets)
//                                            bot.send($.unquote(u), l);
//                                    }
//
//                                }).start();
//                    } catch (IOException e) {
//                        e.printStackTrace();
//                    }
//                }
//            }
//        });
//        */
//
//
//        /*
//
//        try {
//            new RateIterator<Task>(
//                NQuadsRDF.stream(n,
//                    new File("/home/me/Downloads/nquad")), 500)
//                        .threadEachRemaining(n::inputLater).start();
//        } catch (FileNotFoundException e) {
//            e.printStackTrace();
//        }
//        */
//
//
//
//        n.startFPS(10f);
//
//        try {
//            bot.start();
//        } catch (IOException | IrcException e) {
//            e.printStackTrace();
//        }
//
//
//
//
//
//
//    }

    public void send(@NotNull String target, String l) {
        irc.send().message(target, l);
    }

    static boolean canReadURL(String url) {
        return url.startsWith("https://gist.githubusercontent");
    }

    private class MyLeakOut extends TaskLeak {
        private final String[] channels;

        public MyLeakOut(NAR nar, String... channels) {
            super(8, nar);
            this.channels = channels;
        }


        @Override
        protected float leak(Task next) {
            boolean cmd = next.isCommand();
            if (cmd || (trace && !next.isDeleted())) {
                String s = (!cmd) ? next.toString() : next.term().toString();
                Runnable r = IRCAgent.this.send(channels, s);
                if (r!=null) {
                    nar.runLater(r);
                    if (Param.DEBUG && !next.isCommand())
                        logger.info("{}\n{}", next, next.proof());
                } else {
                    
                }
                return cmd ? 0 : 1; 
            }
            return 0;
        }

        @Override
        public boolean preFilter(@NotNull Task next) {
            if (trace || next.isCommand())
                return super.preFilter(next);
            return false;
        }

        @Override
        public float value() {
            return 1;
        }
    }





























}
































































































































































































































































































































