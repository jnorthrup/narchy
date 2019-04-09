package nars.nal.multistep;

import nars.test.NALTest;
import nars.test.TestNAR;
import org.junit.jupiter.api.Test;

import static nars.Op.BELIEF;

/**
 * see bAbl.nal
 */
class bAblTests extends NALTest {



    @Test
    void test1() {

        test
                .believe("in(john,playground)") 
                .believe("in(bob,office)") 
                .ask("in(john,?where)") 
                .mustBelieve(100, "in(john,playground)", 1f, 0.73f) 
        ;


        
        
        
        
        
        
        
        
        
    }

    @Test
    void test2() {

        
        
        

        TestNAR t = test;

        
        t.nar.freqResolution.set(0.1f);
        t.nar.termVolumeMax.set(17);

        t
                .inputAt(0, "((holds(#who,$what) &&+1 inside(#who,$where)) ==>+1 inside($what,$where)).")
                .inputAt(1,"holds(john,football). :|:") 
                .inputAt(2,"inside(john,playground). :|:") 
                //.inputAt(2,"inside(bob,office).")
                //.inputAt(2,"inside(bob,kitchen).")
                .inputAt(2,"inside(football,?where)?")
                .mustOutput( 400,
                        "inside(football,playground)", BELIEF,
                        1f, 1f, 0.35f, 0.99f, 3);

    }

    /**
     * TODO find a better problem representation, this one isnt good
     */
    @Test
    void test19() {

        

        TestNAR t = test;
        t.confTolerance(0.9f);
        t.nar.termVolumeMax.set(35);
        t.nar.freqResolution.set(0.25f);
        t.nar.beliefPriDefault.pri(0.1f);
        t.nar.questionPriDefault.set(0.8f);

        t.input("((&&, start($1,$2), at( $1,$B,$C), at( $B,$2,$C2) ) ==> ( path( id,$C,id,$C2)   && chunk( $1,$2,$B) )).")
                .input("((&&, start($1,$2), at( $1,$B,$C), at( $2,$B,$C2) ) ==> ( path( id,$C,neg,$C2)  && chunk( $1,$2,$B) )).")
                .input("((&&, start($1,$2), at( $B,$1,$C), at( $B,$2,$C2) ) ==> ( path( neg,$C,id,$C2)  && chunk( $1,$2,$B) )).")
                .input("((&&, start($1,$2), at( $B,$1,$C), at( $2,$B,$C2) ) ==> ( path( neg,$C,neg,$C2) && chunk( $1,$2,$B) )).")
                .input("at(kitchen,hallway,south).") 
                .input("at(den,hallway,west).") 
                .input("start(den,kitchen).") 
                .input("$0.9 path(?a,?b,?c,?d)?")
                .mustBelieve(2500, "path(id,west,neg,south)", 1f, 0.75f);


    }


}
