package nars.language;

import com.google.common.collect.Lists;
import jcog.event.On;
import jcog.exe.Loop;
import jcog.io.Twokenize;
import nars.$;
import nars.NAR;
import nars.Narsese;
import nars.Task;
import nars.language.util.Twenglish;
import nars.op.Operator;
import nars.task.signal.Truthlet;
import nars.task.signal.TruthletTask;
import nars.term.Term;
import nars.term.atom.Atomic;
import org.apache.commons.lang3.StringEscapeUtils;
import org.jetbrains.annotations.NotNull;

import java.net.URL;
import java.nio.charset.Charset;
import java.util.List;
import java.util.function.Function;

import static nars.Op.BELIEF;
import static nars.truth.TruthFunctions.c2w;


/** sequential hearing and reading input abilities */
public class NARHear extends Loop {

    final static Atomic START = Atomic.the("start");

    private final NAR nar;
    //private final Term[] context;
    //private final Term[] contextAnonymous;
    private final List<Term> tokens;
    public final On onReset;
    private final Term context;
    int token;

    float priorityFactor = 1f;
    float confFactor = 1f;


    public static Loop hear(NAR nar, String msg, String src, int wordDelayMS) {
        return hear(nar, msg, src, wordDelayMS, 1f);
    }

    /**
     * set wordDelayMS to 0 to disable twenglish function
     */
    public static Loop hear(NAR nar, String msg, String src, int wordDelayMS, float pri) {
        return hearIfNotNarsese(nar, msg, src, (m) -> {
            return hearText(nar, msg, src, wordDelayMS, pri);
        });
    }

    public static Loop hearText(NAR nar, String msg, String src, int wordDelayMS, float pri) {
        assert (wordDelayMS > 0);
        List<Term> tokens = tokenize(msg);
        if (!tokens.isEmpty()) {
            NARHear hear = new NARHear(nar, tokens, src, wordDelayMS);
            hear.priorityFactor = pri;
            return hear;
        } else {
            return null;
        }
    }

    public static Loop hearIfNotNarsese(NAR nar, String msg, String src, Function<String, Loop> ifNotNarsese) {
        @NotNull List<Task> parsed = $.newArrayList();
        @NotNull List<Narsese.NarseseException> errors = $.newArrayList();

        try {
            Narsese.the().tasks(msg, parsed, nar);
        } catch (Narsese.NarseseException ignored) {
            //ignore and continue below
        }

        if (!parsed.isEmpty() && errors.isEmpty()) {
            logger.debug("narsese: {}", parsed);
            nar.input(parsed);
            return null;
        } else {
            return ifNotNarsese.apply(msg);
        }
    }

    public NARHear(NAR nar, @NotNull List<Term> msg, @NotNull String who, int wordDelayMS) {
        super();
        this.nar = nar;

        onReset = nar.eventClear.onWeak(this::onReset);
        tokens = msg;
        context = null; //TODO //who.isEmpty() ? null : $.the(who);

        //contextAnonymous = new Term[]{$.the("hear"), $.varDep(1), Op.Imdex};
        if (wordDelayMS > 0) {
            setPeriodMS(wordDelayMS);
        } else {
            //input all immediately
            Term prev = null;
            for (Term x : msg) {
                hear(prev, x);
                prev = x;
            }
        }
    }

    protected void onReset(NAR n) {
        stop();
        onReset.off();
    }


    @NotNull
    public static List<Term> tokenize(String msg) {
        return Lists.transform(Twokenize.tokenize(msg), Twenglish::spanToTerm);
    }

    @Override
    public boolean next() {
        if (token >= tokens.size()) {
            stop();
            return true;
        }

//        if (token > 0) {
//            hear(tokens.get(token-1), 0.5f); //word OFF
//        }

        hear(token > 0 ? tokens.get(token - 1) : START, tokens.get(token++)); //word ON
        return true;
    }

    private void hear(Term prev, Term next) {

        Term term =
                context != null ?
                        $.func("hear", next, context) :
                        $.func("hear", next);

        long now = nar.time();
        nar.input(
            new TruthletTask(
                term, //1 word
                BELIEF,
                Truthlet.impulse(
                        now, now+1 /* TODO use realtime to translate wordDelayMS to cycles */, 1f, 0f,
                        c2w(nar.confDefault(BELIEF)*confFactor)
                ),
                nar)
                .pri(nar.priDefault(BELIEF) * priorityFactor)
        );
    }

    static public void readURL(NAR nar) {
        nar.onOp("readURL", (t, n) -> {

            Term[] args = Operator.args(t).arrayClone();
            try {
//                String base = "simple.wikipedia.org";
//                //"en.wikipedia.org";
//                Wiki enWiki = new Wiki(base);

                String url = $.unquote(args[0]);
                //remove quotes
                //String page = enWiki.normalize(url.replace("\"", ""));
                //System.out.println(page);


                String html = com.google.common.io.Resources.toString(new URL(url), Charset.defaultCharset());

                html = StringEscapeUtils.unescapeHtml4(html);
                String strippedText = html.replaceAll("(?s)<[^>]*>(\\s*<[^>]*>)*", " ").toLowerCase();

                //System.out.println(strippedText);

                NARHear.hear(n, strippedText, url, 250, 0.1f);

                return Operator.log(n.time(), "Reading " + url + ":" + strippedText.length() + " characters");

            } catch (Exception e) {
                e.printStackTrace();
                return null;
            }
        });
    }

    public static void hear(NAR nar, String t, String source) {
        try {
            try {
                nar.input(t);
            } catch (Narsese.NarseseException e) {

                try {
                    NARHear.hear(nar, t, source, 0);
                } catch (Exception e1) {
                    nar.input(Operator.log(nar.time(), e1));
                }

            }
        } catch (Throwable tt) {
            nar.input(Operator.log(nar.time(), $.p(t, tt.toString())));
        }

    }
}
