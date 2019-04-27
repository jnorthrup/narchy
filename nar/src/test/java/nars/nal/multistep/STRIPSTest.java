package nars.nal.multistep;

import nars.NAR;
import nars.NARS;
import nars.Narsese;
import org.junit.jupiter.api.Test;

/**
 * http://users.cecs.anu.edu.au/~patrik/pddlman/writing.html
 * http://fai.cs.uni-saarland.de/hoffmann/ff-domains.html
 */
class STRIPSTest {

    public static class BlocksWorld {

//https://github.com/caelan/pddlstream/blob/stable/examples/blocksworld/domain.pddl
//        (define (domain blocksworld)
//          (:requirements :strips :equality)
//          (:predicates (clear ?x)
//                       (on-table ?x)
//                       (arm-empty)
//                       (holding ?x)
//                       (on ?x ?y))

//
//          (:action pickup
//            :parameters (?ob)
//            :precondition (and (clear ?ob) (on-table ?ob) (arm-empty))
//            :effect (and (holding ?ob) (not (clear ?ob)) (not (on-table ?ob))
//                         (not (arm-empty))))
        /*
        ((--pickup($o) &&+1 (&|, clear($o),onTable($o),armEmpty)) ==>+1 ((&|,holding($o),--clear($o), --onTable($o), --armEmpty) &&+1 pickup($o)))
        */
//
//          (:action putdown
//            :parameters  (?ob)
//            :precondition (and (holding ?ob))
//            :effect (and (clear ?ob) (arm-empty) (on-table ?ob)
//                         (not (holding ?ob))))
        /*
        ((holding($o) &| --putdown($o)) ==>+1 ((&|, clear($o), armEmpty, onTable($o), --holding($o)) &&+1 putdown($o)))
        */
//
//          (:action stack
//            :parameters  (?ob ?underob)
//            :precondition (and  (clear ?underob) (holding ?ob))
//            :effect (and (arm-empty) (clear ?ob) (on ?ob ?underob)
//                         (not (clear ?underob)) (not (holding ?ob))))
//
//          (:action unstack
//            :parameters  (?ob ?underob)
//            :precondition (and (on ?ob ?underob) (clear ?ob) (arm-empty))
//            :effect (and (holding ?ob) (clear ?underob)
//        (not (on ?ob ?underob)) (not (clear ?ob)) (not (arm-empty)))))

    }
    @Test void testPDDL1() {

    }
    @Test
    void testBanana1() throws Narsese.NarseseException {
        NAR n = new NARS().tmp();
        n.termVolumeMax.set(36);
        n.input(
                /*
                A monkey is at location A in a lab. There is a box in location C. The monkey wants the bananas that are hanging from the ceiling in location B, but it needs to move the box and climb onto it in order to reach them.
                At(A), Level(low), BoxAt(C), BananasAt(B)
                */

                "At(A).",
                "Level(low).",
                "BoxAt(C).",
                "BananasAt(B).",

                /* Goal state:    Eat(bananas) */
                "Eat(bananas)!",

                
                "((At($X) &| Level(low)) ==>+1 (--At($X) &| At(#Y))).",

                "--(#x:(high) &| #x:(low)).", //mutually exclusive
                "--(At(#x) &| At(#y)).",
                "--(BoxAt(#x) &| BoxAt(#y)).",

                "(((At(#Location) &| BoxAt(#Location)) &| Level(low)) ==>+1 Level(high)).",

                
                "(((At(#Location) &| BoxAt(#Location)) &| Level(high)) ==>+1 Level(low)).",


                
               /* Preconditions:  At(X), BoxAt(X), Level(low)
               Postconditions: BoxAt(Y), not BoxAt(X), At(Y), not At(X) */
                "(((At(#X) &| BoxAt(#X)) &| Level(low)) ==>+1 (At(#Y) &| BoxAt(#Y))).",


                
               /* Preconditions:  At(Location), BananasAt(Location), Level(high)
               Postconditions: Eat(bananas) */
                "(((At(#Location) &| BananasAt(#Location)) &| Level(high)) ==>+1 Eat(bananas))."
        );
        n.run(1000);

    }
}
