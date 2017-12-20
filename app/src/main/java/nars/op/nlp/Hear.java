package nars.op.nlp;

import com.google.common.collect.Lists;
import jcog.event.On;
import jcog.exe.Loop;
import jcog.io.Twokenize;
import nars.$;
import nars.NAR;
import nars.Narsese;
import nars.Task;
import nars.concept.Concept;
import nars.op.Operator;
import nars.term.Term;
import nars.term.atom.Atomic;
import org.apache.commons.lang.StringEscapeUtils;
import org.jetbrains.annotations.NotNull;

import java.net.URL;
import java.util.List;
import java.util.function.Function;

import static nars.Op.BELIEF;


public class Hear extends Loop {

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

    /** use 0 to disable the eternal off */
    float offConf;

    public static Loop hear(NAR nar, String msg, String src, int wordDelayMS) {
        return hear(nar ,msg ,src, wordDelayMS, 1f);
    }

    /** set wordDelayMS to 0 to disable twenglish function */
    public static Loop hear(NAR nar, String msg, String src, int wordDelayMS, float pri) {
        return hear(nar, msg, src, (m) -> {
            if (wordDelayMS > 0) {
                List<Term> tokens = tokenize(m);
                if (!tokens.isEmpty()) {
                    Hear hear = new Hear(nar, tokens, src, wordDelayMS);
                    hear.priorityFactor = pri;
                    return hear;
                }
            }
            return null;
        });
    }

    public static Loop hear(NAR nar, String msg, String src, Function<String,Loop> ifNotNarsese) {
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

    public Hear(NAR nar, @NotNull List<Term> msg, @NotNull String who, int wordDelayMS) {
        super( );
        this.nar = nar;

        onReset = nar.eventClear.onWeak(this::onReset);
        tokens = msg;
        context = null; //TODO //who.isEmpty() ? null : $.the(who);
        //contextAnonymous = new Term[]{$.the("hear"), $.varDep(1), Op.Imdex};
        setPeriodMS(wordDelayMS);
    }

    protected void onReset(NAR n) {
        stop();
        onReset.off();
    }


    @NotNull public static List<Term> tokenize(String msg) {
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

        hear(token > 0 ? tokens.get(token-1) : START, tokens.get(token++)); //word ON
        return true;
    }

    private void hear(Term prev, Term next) {

        Term term =
                context!=null ?
                        $.func("hear", next, context) :
                        $.func("hear", next);

        if (offConf > 0) {
            Concept concept = nar.concept(term);
            if (concept == null) {
                //input a constant negative bias - we dont hear the word when it is not spoken
                //only input when first conceptualized
                nar.believe(nar.priDefault(BELIEF) * priorityFactor,
                        term, nar.time(), 0.5f, offConf);
            }
        }

        float onConf = nar.confDefault(BELIEF) * confFactor;
        nar.believe(nar.priDefault(BELIEF) * priorityFactor,
                term, //1 word
                nar.time(), 1f, onConf);


    }

    static public void wiki(NAR nar) {
        nar.onOp( "readURL",  (t, n) -> {

            Term[] args = Operator.args(t).arrayClone();
            try {
//                String base = "simple.wikipedia.org";
//                //"en.wikipedia.org";
//                Wiki enWiki = new Wiki(base);

                String url = args[0].toString();
                //remove quotes
                //String page = enWiki.normalize(url.replace("\"", ""));
                //System.out.println(page);



                String html = //enWiki.getRenderedText(page);
                        new URL(url).openConnection().getContent().toString();
                html = StringEscapeUtils.unescapeHtml(html);
                String strippedText = html.replaceAll("(?s)<[^>]*>(\\s*<[^>]*>)*", " ").toLowerCase();

                //System.out.println(strippedText);

                Hear.hear(n, strippedText, url, 250, 0.1f);

                return Operator.log(n.time(), "Reading " + url + ":" + strippedText.length() + " characters");

            } catch (Exception e) {
                e.printStackTrace();
                return null;
            }
        });
    }
}
