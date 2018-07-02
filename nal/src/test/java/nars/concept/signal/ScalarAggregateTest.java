package nars.concept.signal;

import jcog.Util;
import nars.$;
import nars.NAR;
import nars.NARS;
import nars.Narsese;
import nars.concept.Concept;
import nars.concept.Scalar;
import nars.subterm.Subterms;
import nars.term.Conceptor;
import nars.term.Term;
import nars.term.atom.Int;
import nars.truth.Truth;
import org.jetbrains.annotations.Nullable;
import org.junit.jupiter.api.Test;

import static nars.$.$$;
import static nars.Op.INT;

public class ScalarAggregateTest {

    @Test
    public void test1() throws Narsese.NarseseException {
        NAR n = NARS.shell();

        Scalar sin = new Scalar($.the("sin_x"), (t) -> Util.unitize(0.5f + 0.5f * (float) Math.sin(t)), n);
        n.run(1);

        //https://en.wikipedia.org/wiki/Root_mean_square
        n.on(new Conceptor("RMS") {
            @Override
            public Concept apply(Term term, Subterms param) {

                if (param.subs() == 2) {
                    //Concept x = n.concept(s.sub(0));
                    //if (x instanceof Signal) {
                    Term st = param.sub(0);
                    if (st.op() == INT) {
                        Term en = param.sub(1);
                        if (en.op() == INT) {
                            int windowStart = ((Int) st).id; //TODO dither
                            int windowEnd = ((Int) en).id; //TODO dither
                            if (windowEnd - windowStart > 0) {
                                return new Scalar(id(term, param), (t) -> {
                                    //TODO limit max # samples
                                    @Nullable Concept c = n.conceptualizeDynamic(term);
                                    if (c!=null) {
                                        float sum = 0;
                                        int count = 0;
                                        for (long i = t + windowStart; i < t + windowEnd; i += n.dur()) {
                                            Truth v = c.beliefs().truth(i, n);
                                            if (v!=null) {
                                                float f = v.freq();
                                                sum += f*f;
                                                count++;
                                            }
                                        }
                                        if (count > 0) {
                                            return (float)Math.sqrt((sum/count));
                                        }
                                    }

                                    return Float.NaN;
                                }, n);
                            }
                        }
                        //    }
                    }
                }
                return null;
            }

        });

        Term rmsA = $$("RMS(sin_x, (-5, 0))");
        n.input(rmsA + "?");
        for (int i = 0; i < 10; i++) {
            System.out.println(n.belief(sin, n.time()));
            System.out.println("\t" + n.belief(rmsA, n.time()));
            n.run(1);
        }


    }
}
