package nars.concept.signal;

import jcog.Util;
import nars.*;
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
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;

public class ConceptorTest {

    @Test
    public void test1() throws Narsese.NarseseException {
        NAR n = NARS.shell();

        Scalar sin = new Scalar($.the("sin_x"), (t) -> Util.unitize(0.5f + 0.5f * (float) Math.sin(t)), n);
        n.run(1);

        //https://en.wikipedia.org/wiki/Root_mean_square
        n.on(new Conceptor("RMS") {
            @Override
            public Concept apply(Term term, Subterms param) {

                if (param.subs() == 3) {
                    Term target = param.sub(0);
                    //Concept x = n.concept(s.sub(0));
                    //if (x instanceof Signal) {
                    Term st = param.sub(1);
                    if (st.op() == INT) {
                        Term en = param.sub(2);
                        if (en.op() == INT) {
                            int windowStart = ((Int) st).id; //TODO dither
                            int windowEnd = ((Int) en).id; //TODO dither
                            if (windowEnd - windowStart > 0) {
                                return new Scalar(id(param), (t) -> {
                                    //TODO limit max # samples
                                    @Nullable Concept c = n.conceptualizeDynamic(target);
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

        Term rmsA = $$("RMS(sin_x, -5, 0)");
        n.input(rmsA + "?");
        for (int i = 0; i < 10; i++) {
            System.out.println(n.belief(sin, n.time()));
            System.out.println("\t" + n.belief(rmsA, n.time()));
            n.run(1);
        }


    }

    @Test
    public void testTaskStamps() throws Narsese.NarseseException {
        NAR n = NARS.shell();

        n.on(new Conceptor("pixel") {

            @Override
            public Concept apply(Term term, Subterms args) {
                if (args.subs()==2) {
                    Int x = (Int)args.sub(0);
                    Int y = (Int)args.sub(1);
                    return new Scalar(id(args), (t)->{
                        return 0.5f;
                    }, n);
                } else
                    return null;
            }
        });

        Term p00 = $$("pixel(0, 0)");
        n.input(p00 + "?");
        assertEquals("%.50;.90%", n.beliefTruth(p00, n.time()).toString());

        Task pNow = n.belief(p00, n.time());
        Task pNext = n.belief(p00, n.time()+1);
        Task pBefore = n.belief(p00, n.time()-1);

        assertNotEquals(pNow, pNext);
        assertEquals(1, pNow.stamp().length);
        assertEquals(1, pNext.stamp().length);
        assertNotEquals(pNext.stamp()[0], pNow.stamp()[0]);
        assertNotEquals(pNow.stamp()[0], pBefore.stamp()[0]);

        Task pNow2 = n.belief(p00, n.time());
        assertEquals(pNow2.stamp()[0], pNow.stamp()[0]);
    }


    @Test
    public void testBitmap() throws Narsese.NarseseException {
        NAR n = NARS.tmp();

        int w = 4, h = 4;
        n.on(new Conceptor("pixel") {

//            private TemplateTermLinker templates = new TemplateTermLinker(Op.EmptyTermArray) {
//                @Override
//                public Concept[] concepts(NAR nar, boolean conceptualize) {
//
//                    //random pixel
//                    int x = nar.random().nextInt(w);
//                    int y = nar.random().nextInt(h);
//                    Term c = id(Int.the(x), Int.the(y));
//                    clear();
//                    add(c);
//                    concepts = 1;
//                    return super.concepts(nar, conceptualize);
//
////                    return new Concept[] { nar.conceptualize(c) };
//                }
//            };
//            @Override
//            public TemplateTermLinker linker() {
//                return templates;
//            }

            @Override
            public Concept apply(Term term, Subterms args) {
                if (args.subs() == 2) {
                    Term xx = args.sub(0);
                    Term yy = args.sub(1);
                    if (xx.op()==INT && yy.op()==INT) {
                        Int x = (Int) xx;
                        Int y = (Int) yy;
                        return new Scalar(id(args), (t) -> {
                            return 0.5f;
                        }, n);
                    }
                }

                return null;
            }
        });

        n.log();
        Term p00 = $$("pixel(#x, #y)");
        n.input(p00 + "?");

        n.input("pixel(1,1)? :|:");
        n.run(50);


    }

}
