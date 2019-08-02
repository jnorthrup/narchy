package nars.nal.nal8;

import nars.$;
import nars.NAR;
import nars.NARS;
import nars.Task;
import nars.table.BeliefTable;
import nars.term.Term;
import nars.test.analyze.BeliefContradictionDetector;
import nars.truth.Truth;
import org.junit.jupiter.api.Test;

import static nars.Op.IMPL;
import static nars.time.Tense.ETERNAL;
import static org.junit.jupiter.api.Assertions.*;

/** test propagation of belief and goal truth among nodes in
 * parameterically generated implication
 * networks
 */
class ImplicationNetworkTest {


    int cycles = 800;

    NAR n = NARS.tmp(6);
    {
        n.termVolMax.set(5);
        new BeliefContradictionDetector(n);
    }

    final Term a = $.the("a");
    final Term b = $.the("b");
    final Term c = $.the("c");

    @Test
    void testEternal_A_PosBelief_ToBC() {






        n.believe(IMPL.the(a, b));
        n.believe(IMPL.the(b, c));
        n.believe(a);
        n.run(cycles);

        BeliefTable aBeliefs = n.concept(a).beliefs();
        Truth aBelief = aBeliefs.truth(ETERNAL, n);
        

        
        
        

        BeliefTable bBeliefs = n.concept(b).beliefs();
        Truth bBelief = bBeliefs.truth(ETERNAL, n);
        
        

        
        

        Truth cBelief = n.concept(c).beliefs().truth(ETERNAL, n);
        

        System.out.println("a: " + aBelief);
        System.out.println("b: " + bBelief);
        System.out.println("c: " + cBelief);

        assertEquals(aBelief.freq(), bBelief.freq(), n.freqResolution.floatValue());
        assertEquals(bBelief.freq(), cBelief.freq(), n.freqResolution.floatValue());
        assertTrue(aBelief.conf() - bBelief.conf() > n.confResolution.floatValue()*2);
        assertTrue(bBelief.conf() - cBelief.conf() > n.confResolution.floatValue()*2);

    }

    @Test
    void testEternal_A_PosGoal_ToBC() {





        n.believe(IMPL.the(a, b));
        n.believe(IMPL.the(b, c));
        n.want(a);
        n.run(cycles);

        BeliefTable aGoals = n.tableDynamic(a, false);
        Truth aGoal = aGoals.truth(ETERNAL, n);
        

        
        
        //assertEquals(1, aGoals.size());

        BeliefTable bGoals = n.tableDynamic(b, false);
        Truth bGoal = bGoals.truth(ETERNAL, n);
        //n.concept(b).print();
        assertNotNull(bGoal);
        assertEquals(1, bGoals.taskCount());

        
        

        BeliefTable cGoals = n.concept(c).goals();
        Truth cGoal = cGoals.truth(ETERNAL, n);
        //n.concept(c).print();
        

//        System.out.println("a: " + aGoal);
//        System.out.println("b: " + bGoal);
//        System.out.println("c: " + cGoal);

        assertEquals(aGoal.freq(), bGoal.freq(), n.freqResolution.floatValue());
        assertEquals(bGoal.freq(), cGoal.freq(), n.freqResolution.floatValue());
        assertTrue(aGoal.conf() - bGoal.conf() > n.confResolution.floatValue()*2);
        assertTrue(bGoal.conf() - cGoal.conf() > n.confResolution.floatValue()*2);

    }

    @Test
    void testEternal_A_NegBelief_ToBC() {


//        NAL.DEBUG = true;
        //n.log();
//        n.onTask((t)->{
//            System.err.println(t);
//            System.out.println(MetaGoal.proof(t, n));
//        });

        n.believe(IMPL.the(a, b));
        n.believe(IMPL.the(b, c));
        n.believe(a.neg());


        n.run(cycles);

        BeliefTable aBeliefs = n.concept(a).beliefs();


        Task bBelief = n.belief(b);
        //System.out.println(MetaGoal.proof(bBelief, n));
        //System.out.println(bBelief.proof());
        assertTrue(bBelief == null || bBelief.conf() < 0.5f /* TODO refine thresh */);

    }

    @Test
    void testEternal_A_NegBelief_NegToBC_AB_only() {


        n.believe(IMPL.the(a.neg(), b));
        n.believe(a.neg());
        n.run(cycles);

        BeliefTable aa = n.concept(a).beliefs();
        BeliefTable bb = n.concept(b).beliefs();

        aa.print();
        
        bb.forEachTask(x -> System.out.println(x.proof()));

        Truth bBelief = bb.truth(ETERNAL, n);


        assertEquals("%1.0;.81%" , bBelief.toString());

    }

    @Test
    void testEternal_A_NegBelief_NegToBC() {



        n.believe(IMPL.the(a.neg(), b));
        n.believe(IMPL.the(b, c));
        n.believe(a.neg());
        n.run(cycles);

        BeliefTable aa = n.concept(a).beliefs();
        BeliefTable bb = n.concept(b).beliefs();
        BeliefTable cc = n.concept(c).beliefs();
        aa.print();
        bb.print();
        
        cc.forEachTask(x -> System.out.println(x.proof()));

        Truth bBelief = bb.truth(ETERNAL, n);
        assertEquals("%1.0;.81%" ,bBelief.toString());

        Truth cBelief = cc.truth(ETERNAL, n);
        assertEquals("%1.0;.73%" ,cBelief.toString());
    }

    @Test
    void testEternal_A_NegBelief_NegToB_NegToC() {



        n.believe(IMPL.the(a.neg(), b).neg());
        n.believe(IMPL.the(b.neg(), c));
        n.believe(a.neg());
        n.run(cycles);

        BeliefTable aBeliefs = n.concept(a).beliefs();
        aBeliefs.print();

        {
            BeliefTable bBeliefs = n.concept(b).beliefs();
            bBeliefs.print();
            Truth bBelief = bBeliefs.truth(ETERNAL, n);
            assertEquals("%0.0;.81%", bBelief.toString());
        }

        Truth cBelief = n.concept(c).beliefs().truth(ETERNAL, n);

        assertEquals("%1.0;.73%" ,cBelief.toString());
    }

}

