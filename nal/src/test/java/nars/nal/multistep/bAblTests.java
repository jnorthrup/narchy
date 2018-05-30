package nars.nal.multistep;

import nars.test.TestNAR;
import nars.util.NALTest;
import org.junit.jupiter.api.Test;

import static nars.Op.BELIEF;

/**
 * see bAbl.nal
 */
public class bAblTests extends NALTest {



    @Test
    public void test1() throws nars.Narsese.NarseseException {

        test
                .believe("in(john,playground)") 
                .believe("in(bob,office)") 
                .ask("in(john,?where)") 
                .mustBelieve(100, "in(john,playground)", 1f, 0.73f) 
        ;


        
        
        
        
        
        
        
        
        
    }

    @Test
    public void test2() {

        
        
        

        TestNAR t = test;

        
        t.nar.freqResolution.set(0.1f);
        t.nar.termVolumeMax.set(40);

        t.believe("((pick(#Person,$Object) &&+1 inside(#Person,$Place)) ==>+1 inside($Object,$Place))")
                .inputAt(1,"pick(john,football). :|:") 
                .inputAt(2,"inside(john,playground). :|:") 
                .input("inside(bob,office).") 
                .input("inside(bob,kitchen).") 
                .input("$0.9 inside(football,?where)?") 
                .mustOutput( 1400,
                        "inside(football,playground)", BELIEF,
                        1f, 1f, 0.5f, 0.99f, 0); 

    }

    /**
     * TODO find a better problem representation, this one isnt good
     */
    @Test
    public void test19() {

        

        TestNAR t = test;
        t.confTolerance(0.9f);
        t.nar.termVolumeMax.set(40);
        t.nar.freqResolution.set(0.25f);

        t.input("$0.9 ((&&, start($1,$2), at( $1,$B,$C), at( $B,$2,$C2) ) ==> ( path( id,$C,id,$C2)   && chunk( $1,$2,$B) )).")
                .input("$0.9 ((&&, start($1,$2), at( $1,$B,$C), at( $2,$B,$C2) ) ==> ( path( id,$C,neg,$C2)  && chunk( $1,$2,$B) )).")
                .input("$0.9 ((&&, start($1,$2), at( $B,$1,$C), at( $B,$2,$C2) ) ==> ( path( neg,$C,id,$C2)  && chunk( $1,$2,$B) )).")
                .input("$0.9 ((&&, start($1,$2), at( $B,$1,$C), at( $2,$B,$C2) ) ==> ( path( neg,$C,neg,$C2) && chunk( $1,$2,$B) )).")
                .input("at(kitchen,hallway,south).") 
                .input("at(den,hallway,west).") 
                .input("start(den,kitchen).") 
                .input("$0.9 path(?a,?b,?c,?d)?")
                .mustBelieve(500, "path(id,west,neg,south)", 1f, 0.75f); 


    }


}
