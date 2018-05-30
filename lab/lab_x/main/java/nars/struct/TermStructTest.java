package nars.struct;


import nars.Op;
import nars.term.Atom;

import java.util.Arrays;

/**
 * Created by me on 8/28/15.
 */
public class TermStructTest {


    /** total number of different Op types */
    final static int OPS = Op.NONE.ordinal();
    final static int BELIEFS = 4;
    final static int GOALS = 4;
    final static int QUESTIONS = 2;
    public static int MAX_SUBTERMS = 6;
    public static int MAX_ATOM_LENGTH = 12;

    







    public static void main(String[] args) {

        TermCore core = new TermCore(1024*1024);

        TermSpect a = core.term(Atom.the("a").bytes(), Op.ATOM);

        TermSpect b = core.term(Atom.the("b").bytes(), Op.ATOM);

        TermSpect aIntB = core.term(Atom.the("a_b").bytes(), Op.INHERITANCE);
        aIntB.believe(1f, 0.9f);

        TermSpect aSimB = aIntB.the(Op.SIMILARITY);
        aSimB.believe(1f, 0.9f);

        System.out.println( core.index.size() );

        core.index.entrySet().forEach(e -> {
            System.out.println(Arrays.toString(e.getKey()) + " " + e.getValue());
        });

























    }

}
