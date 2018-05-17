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
                .believe("in(john,playground)") //john is in the playground.
                .believe("in(bob,office)") //Bob is in the office.
                .ask("in(john,?where)") //Where is john?
                .mustBelieve(100, "in(john,playground)", 1f, 0.73f) //note that the 0.90 conf result should have been provided as an answer to the question, not as a belief. the lower conf version is a side effect so we'll test for that at least
        ;


        ////(1) Basic Factoid QA with Single Supporting Fact
        ////john is in the playground.
        //<(*,{john},{playground}) --> isin>.
        ////Bob is in the office.
        //<(*,bob,office) --> isin>.
        ////Where is john?
        //<(*,{john},{?where}) --> isin>?
        ////A:playground
        ////Answer <(*,{john},{playground}) --> isin>. %1.00;0.90%
    }

    @Test
    public void test2() {

        //(2) Factoid QA with Two Supporting Facts
        //background knowledge (multiple input for priority boost to have the answer faster ^^)
        //if something is picked, it means that the object which is picked is where the person is

        TestNAR t = test;

        //t.log();
        t.nar.freqResolution.set(0.1f);
        t.nar.termVolumeMax.set(40);

        t.believe("((pick(#Person,$Object) &&+1 inside(#Person,$Place)) ==>+1 inside($Object,$Place))")
                .inputAt(1,"pick(john,football). :|:") ////John picked up the football.
                .inputAt(2,"inside(john,playground). :|:") ////John is in the playground.
                .input("inside(bob,office).") ////Bob is in the office.
                .input("inside(bob,kitchen).") ////Bob went to the kitchen.
                .input("$0.9 inside(football,?where)?") ////Where is the football?
                .mustOutput( 1400,
                        "inside(football,playground)", BELIEF,
                        1f, 1f, 0.5f, 0.99f, 0); ////A: playground

    }

    /**
     * TODO find a better problem representation, this one isnt good
     */
    @Test
    public void test19() {

        //(19) Path Finding

        TestNAR t = test;
        t.confTolerance(0.9f);
        t.nar.termVolumeMax.set(40);
        t.nar.freqResolution.set(0.25f);

        t.input("$0.9 ((&&, start($1,$2), at( $1,$B,$C), at( $B,$2,$C2) ) ==> ( path( id,$C,id,$C2)   && chunk( $1,$2,$B) )).")
                .input("$0.9 ((&&, start($1,$2), at( $1,$B,$C), at( $2,$B,$C2) ) ==> ( path( id,$C,neg,$C2)  && chunk( $1,$2,$B) )).")
                .input("$0.9 ((&&, start($1,$2), at( $B,$1,$C), at( $B,$2,$C2) ) ==> ( path( neg,$C,id,$C2)  && chunk( $1,$2,$B) )).")
                .input("$0.9 ((&&, start($1,$2), at( $B,$1,$C), at( $2,$B,$C2) ) ==> ( path( neg,$C,neg,$C2) && chunk( $1,$2,$B) )).")
                .input("at(kitchen,hallway,south).") //The kitchen is north of the hallway.
                .input("at(den,hallway,west).") //The den is east of the hallway.
                .input("start(den,kitchen).") //How do you go from den to kitchen?
                .input("$0.9 path(?a,?b,?c,?d)?")
                .mustBelieve(500, "path(id,west,neg,south)", 1f, 0.75f); //A:west,north


    }


}
