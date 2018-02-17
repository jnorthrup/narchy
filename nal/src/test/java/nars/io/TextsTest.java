package nars.io;

import jcog.Texts;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;


public class TextsTest {

    @Test
    public void testN2() {
        
        assertEquals("1.0", Texts.n2(1.00f));
        assertEquals(".50", Texts.n2(0.5f));
        assertEquals(".09", Texts.n2(0.09f));
        assertEquals(".10", Texts.n2(0.1f));
        assertEquals(".01", Texts.n2(0.009f));
        assertEquals("0.0", Texts.n2(0.001f));
        assertEquals(".01", Texts.n2(0.01f));
        assertEquals("0.0", Texts.n2(0.0f));
        
        
    }

    
//    public static CharSequence toString(Term term) {
//        if (term instanceof Statement) {
//            Statement s = (Statement)term;
//            /*
//            Rope r = cat(
//                valueOf(STATEMENT_OPENER.ch),
//                toString(s.getSubject()),
//                valueOf(' '),
//                s.operate().toString(),
//                valueOf(' '),
//                toString(s.getPredicate()),
//                valueOf(STATEMENT_CLOSER.ch));
//            */
//
//            return new PrePostCharRope(STATEMENT_OPENER, STATEMENT_CLOSER, Rope.cat(
//                toString(s.getSubject()),
//                valueOf(' '),
//                s.op().toString(),
//                valueOf(' '),
//                toString(s.getPredicate())
//            ));
//        }
//        else if (term instanceof Compound) {
//            Compound ct = (Compound)term;
//
//            Rope[] tt = new Rope[ct.length()];
//            int i = 0;
//            for (final Term t : ct.term) {
//                tt[i++] = Rope.cat(String.valueOf(Symbols.ARGUMENT_SEPARATOR), toString(t));
//            }
//
//            Rope ttt = Rope.cat(tt);
//            return Rope.cat(String.valueOf(COMPOUND_TERM_OPENER), ct.op().toString(), ttt, String.valueOf(COMPOUND_TERM_CLOSER));
//
//        }
//        else
//            return term.toString();
//    }
//
//    @Test
//    public void testRope() throws Narsese.NarseseException {
//        NAR n = new Default();
//
//        String term1String ="<#1 --> (&,boy,(/,taller_than,{Tom},_))>";
//        Term term1 = n.term(term1String);
//
//        Rope tr = (Rope)toString(term1);
//
//        //visualize(tr, System.out);
//
//        //Sentence s = new Sentence(term1, '.', new DefaultTruth(1,1), new Stamper(n.memory, Tense.Eternal));
//
//    }


}
