package nars.op.language;

import com.google.common.collect.Lists;
import jcog.event.Off;
import jcog.exe.Loop;
import jcog.io.Twokenize;
import nars.$;
import nars.NAR;
import nars.Narsese;
import nars.Task;
import nars.concept.Operator;
import nars.op.language.util.Twenglish;
import nars.task.NALTask;
import nars.term.Functor;
import nars.term.Term;
import nars.term.atom.Atomic;
import nars.util.Timed;
import org.apache.commons.lang3.StringEscapeUtils;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.net.URL;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;
import java.util.stream.Collectors;

import static nars.Op.BELIEF;


/** sequential hearing and reading input abilities
 * TODO extend NARPart, add Loop as field
 * */
public class NARHear extends Loop {

    static final Atomic START = Atomic.the("start");

    private final NAR nar;
    
    
    private final List<Term> tokens;
    public final Off onReset;
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
        return hearIfNotNarsese(nar, msg, src, (m) -> hearText(nar, msg, src, wordDelayMS, pri));
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
            Narsese.tasks(msg, parsed, nar);
        } catch (Narsese.NarseseException ignored) {
            
        }

        if (!parsed.isEmpty() && errors.isEmpty()) {
            
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
        context = null; 

        
        if (wordDelayMS > 0) {
            setPeriodMS(wordDelayMS);
        }
            
            Term prev = null;
            for (Term x : msg) {
                hear(prev, x);
                prev = x;
            }

    }

    protected void onReset(Timed n) {
        stop();
        onReset.close();
    }


    public static @NotNull List<Term> tokenize(String msg) {
        List<Term> list = Twokenize.tokenize(msg).stream().map(Twenglish::spanToTerm).collect(Collectors.toList());
        return list;
    }

    @Override
    public boolean next() {
        if (token >= tokens.size()) {
            stop();
            return true;
        }





        hear(token > 0 ? tokens.get(token - 1) : START, tokens.get(token++)); 
        return true;
    }

    private void hear(Term prev, Term next) {

        Term term =
                context != null ?
                        $.func("hear", next, context) :
                        $.func("hear", next);

        long now = nar.time();
		//            new TruthletTask(
		//                target,
		//                BELIEF,
		//                Truthlet.impulse(
		//                        now, now+1 /* TODO use realtime to translate wordDelayMS to cycles */, 1f, 0f,
		//                        c2w(nar.confDefault(BELIEF)*confFactor)
		//                ),
		//                nar)
		nar.input(
			((Task) NALTask.the(term, BELIEF, $.t(1, (nar.confDefault(BELIEF) * confFactor)), now, now,
				Math.round(now + nar.dur()), nar.evidence())).<Task>pri(nar.priDefault(BELIEF) * priorityFactor)
        );
    }

    public static void readURL(NAR nar) {
        nar.setOp(Atomic.atom("readURL"), (t, n) -> {

            Term[] args = Functor.args(t.term()).arrayClone();
            try {


                return readURL(n, $.unquote(args[0]));

            } catch (Exception e) {
                e.printStackTrace();
                return null;
            }
        });
    }

    public static @NotNull Task readURL(NAR n, String url) throws IOException {



        String html = com.google.common.io.Resources.toString(new URL(url), Charset.defaultCharset());

        html = StringEscapeUtils.unescapeHtml4(html);
        String strippedText = html.replaceAll("(?s)<[^>]*>(\\s*<[^>]*>)*", " ").toLowerCase();


        NARHear.hear(n, strippedText, url, 250, 0.1f);

        return Operator.log(n.time(), "Reading " + url + ':' + strippedText.length() + " characters");
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
