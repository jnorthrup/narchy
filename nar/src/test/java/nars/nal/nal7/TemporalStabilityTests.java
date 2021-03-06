package nars.nal.nal7;

import nars.NAR;
import nars.NARS;
import org.eclipse.collections.api.block.function.primitive.IntToObjectFunction;
import org.eclipse.collections.api.set.primitive.IntSet;
import org.eclipse.collections.impl.set.mutable.primitive.IntHashSet;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.Test;


/**
 * N independent events
 */
public class TemporalStabilityTests {

    protected static final int CYCLES = 500;


    static class T1 extends TemporalStabilityTest {

        private final @NotNull IntSet whens;
        private final IntToObjectFunction<String> eventer;
        private final int minT;
        private final int maxT;
        static final int tolerance = 0;


        T1(IntToObjectFunction<String> eventer, int... whens) {
            this.whens = new IntHashSet(whens).toImmutable();
            minT = this.whens.min();
            maxT = this.whens.max();
            this.eventer = eventer;
        }

        T1(int[] whens, int minT, int maxT, IntToObjectFunction<String> eventer) {
            this.whens = new IntHashSet(whens).toImmutable();
            this.minT = minT;
            this.maxT = maxT;
            this.eventer = eventer;
        }

        @Override
        public boolean validOccurrence(long o) {
            
            return (o >= minT-tolerance) && (o <= maxT+tolerance);
        }

        @Override
        public void input(NAR n) {
            int j = 0;
            for (int i : whens.toSortedArray()) {
                n.inputAt(i, eventer.valueOf(j++) + ". :|:");
            }
        }

    }












    private static final IntToObjectFunction<String> inheritencer = new IntToObjectFunction<String>() {
        @Override
        public String valueOf(int j) {
            char c = (char) ('a' + j);
            return c + ":" + c + "" + c;
        }
    };
    private static final IntToObjectFunction<String> implicator = new IntToObjectFunction<String>() {
        @Override
        public String valueOf(int j) {
            char c = (char) ('a' + j);
            return '(' + (c + "==>" + (c + "" + c)) + ')';
        }
    };
    private static final IntToObjectFunction<String> atomizer = new IntToObjectFunction<String>() {
        @Override
        public String valueOf(int j) {
            return String.valueOf((char) ('a' + j));
        }
    };
    private static final IntToObjectFunction<String> productor = new IntToObjectFunction<String>() {
        @Override
        public String valueOf(int j) {
            return '(' + atomizer.apply(j) + ')';
        }
    };
    private static final IntToObjectFunction<String> biproductor = new IntToObjectFunction<String>() {
        @Override
        public String valueOf(int j) {
            char c = (char) ('a' + j);
            return "(" + c + ',' + (c + "" + c) + ')';
        }
    };
    private static final IntToObjectFunction<String> linkedproductor = new IntToObjectFunction<String>() {
        @Override
        public String valueOf(int j) {
            char c = (char) ('a' + j);
            char d = (char) ('a' + (j + 1));
            return "(" + c + ',' + d + ')';
        }
    };
    private static final IntToObjectFunction<String> linkedinh= new IntToObjectFunction<String>() {
        @Override
        public String valueOf(int j) {
            char c = (char) ('a' + j);
            char d = (char) ('a' + (j + 1));
            return "(" + c + "-->" + d + ')';
        }
    };
    private static final IntToObjectFunction<String> linkedimpl= new IntToObjectFunction<String>() {
        @Override
        public String valueOf(int j) {
            char c = (char) ('a' + j);
            char d = (char) ('a' + (j + 1));
            return "(" + c + "==>" + d + ')';
        }
    };
    protected static final IntToObjectFunction<String> conjSeq2 = new IntToObjectFunction<String>() {
        @Override
        public String valueOf(int j) {
            char c = (char) ('a' + j);
            char d = (char) ('a' + (j + 1));
            return "(" + c + " &&+5 " + d + ')';
        }
    };
    protected static final IntToObjectFunction<String> conjInvertor = new IntToObjectFunction<String>() {
        @Override
        public String valueOf(int j) {
            char c = (char) ('a' + j);
            return "(" + c + " &&+5 (--," + c + "))";
        }
    };


    @Test
    void testTemporalStabilityInh3() {
        new T1(inheritencer, 1, 2, 5).test(CYCLES, NARS.tmp());
    }

    @Test
    void testTemporalStabilityImpl() {
        new T1(implicator, 1, 2, 5).test(CYCLES, NARS.tmp());
    }

    @Test
    void testTemporalStabilityAtoms() {
        new T1(atomizer, 1, 3).test(CYCLES, NARS.tmp());
    }

    @Test
    void testTemporalStabilityProd() {
        new T1(productor, 1, 2, 5).test(CYCLES, NARS.tmp());
    }
    @Test
    void testTemporalStabilityBiProd() {
        new T1(biproductor, 1, 2, 5).test(CYCLES, NARS.tmp());
    }

    @Test
    void testTemporalStabilityLinkedProd_easy() {
        new T1(linkedproductor, 1, 2).test(500, NARS.tmp());
    }

    @Test
    void testTemporalStabilityLinkedProd() {
        new T1(linkedproductor, 1, 2, 5).test(CYCLES, NARS.tmp());
    }

    @Test
    void testTemporalStabilityLinkedInh() {
        new T1(linkedinh, 1, 2, 5).test(CYCLES, NARS.tmp());
    }
    @Test
    void testTemporalStabilityLinkedImpl() {
        new T1(linkedimpl, 1, 2, 5).test(CYCLES, NARS.tmp());
    }

    @Test
    void testTemporalStabilityLinkedTemporalConjSmall() {
        new T1(new int[] { 1, 6 }, 1, 16, conjSeq2).test(100, NARS.tmp());
    }


    @Test
    void testTemporalStabilityLinkedTemporalConj() {
        new T1(new int[] { 1, 6, 11 }, 1, 16, conjSeq2).test(CYCLES*2, NARS.tmp());
    }

    @Test
    void testTemporalStabilityLinkedImplExt() {
        new T1(linkedimpl, 1, 2, 5).test(CYCLES, NARS.tmp());
    }
    @Test
    void testTemporalStabilityLinkedImplExt2() {

        

        @NotNull NAR n = NARS.tmp();

        T1 a = new T1(linkedimpl, 1, 2, 5, 10);
        T1 b = new T1(linkedinh, 1, 2, 5, 10);

        a.test(-1, n);
        b.test(-1, n);

        int time = CYCLES;
        n.run(time);




    }

}

