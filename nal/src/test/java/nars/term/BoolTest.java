package nars.term;

import nars.$;
import nars.*;
import nars.io.IO;
import nars.op.Equal;
import nars.truth.Truth;
import nars.truth.func.NALTruth;
import org.jetbrains.annotations.Nullable;
import org.junit.jupiter.api.Test;

import static nars.$.or;
import static nars.$.*;
import static nars.Op.*;
import static nars.term.atom.Bool.*;
import static nars.term.util.TermTest.assertEq;
import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Bool and Tautology tests
 */
public class BoolTest {

    @Test
    void testBoolType() {
        assertEquals("(true,false)", $.p(True, False).toString());
    }

    @Test
    void testBoolBytes() {

        assertEquals(2, True.bytes().length);
        assertEquals(2, False.bytes().length);
        assertEquals(2, Null.bytes().length);
        assertEquals(Null, IO.bytesToTerm(Null.bytes()));
        assertEquals(True, IO.bytesToTerm(True.bytes()));
        assertEquals(False, IO.bytesToTerm(False.bytes()));
    }

    @Test void testBoolLabel() {
        assertEquals(True, $$("true"));
        assertEquals(False, $$("false"));
        //assertEquals(Null, $$("null"));
    }

    @Test
    void testNegationTautologies() {
        assertEquals(True, True.unneg());
        assertEquals(False, True.neg());
        assertEquals(True, False.unneg());
        assertEquals(True, False.neg());
        assertEquals(Null, Null.neg());
        assertEquals(Null, Null.unneg());
    }


    /** same tautological assumptions should hold in equal(x,y) results */
    @Test void testEqualOperatorTautologies() {
        //TODO finish
        NAR n = NARS.shell();
        assertEquals(True, Equal.the(True,True));
        assertEquals(False, Equal.the(True,False));
        assertEquals(Null, Equal.the(True,Null));
        assertEquals(Null, Equal.the(False,Null));
        assertEq("(y-->x)", Equal.the($$("x:y"),True));
        assertEq("(--,(y-->x))", Equal.the($$("x:y"),False));

//        assertEquals("[equal(true,true)]", Evaluation.eval($$("equal(true,true)"), n).toString());
//        assertEquals("[equal(false,false)]", Evaluation.eval($$("equal(false,false)"), n).toString());
        //assertEquals("[null]", Evaluation.eval($$("equal(null,null)"), n).toString());
    }

    @Test
    void testStatementTautologies() {
        for (Op o: new Op[]{INH, SIM, IMPL}) {
            assertEq(True, o.the(True, True));
            assertEq(True, o.the(False, False));
            assertEq(Null, o.the(Null, Null));
        }

        //allow
//        assertEq("(x-->true)", INH.the(x, Bool.True));
//        assertEq("(x-->false)", INH.the(x, Bool.False));
//        assertEq("(true-->x)", INH.the(Bool.True, x));
//        assertEq("(false-->x)", INH.the(Bool.False, x));
//        assertEq("(x<->true)", SIM.the(Bool.True, x));
//        assertEq("(x<->false)", SIM.the(Bool.False, x));
//        assertEq("((--,x)-->true)", INH.the(x.neg(), Bool.True));

        assertEq(True, INH.the(True, True));
        assertEq(True, SIM.the(True, True));
        assertEq(False, INH.the(True, False));
        assertEq(False, INH.the(False, True));
        assertEq(False, SIM.the(True, False));
        assertEq(True, SIM.the(True, True));

        assertEquals(0, True.compareTo(True));
        assertEquals(0, False.compareTo(False));
        assertEquals(0, Null.compareTo(Null));

        assertEquals(-False.compareTo(True), True.compareTo(False));

    }


    @Test
    void testImplicationTautologies() {

        assertEq(Null, IMPL.the(Null, x));
        assertEq(Null, IMPL.the(x, Null));


        assertEq(x, IMPL.the(True, x));
        assertEq(Null, IMPL.the(False, x));

        assertEq(x, IMPL.the(x, True));
        assertEq(x.neg(), IMPL.the(x, False));

    }



    @Test
    void testDiffTautologies() {

        @Nullable Truth selfDiff = NALTruth.Difference.apply($.t(1, 0.9f), $.t(1f, 0.9f), null, 0);
        assertEquals($.t(0, 0.81f), selfDiff);

        @Nullable Truth negDiff = NALTruth.Difference.apply($.t(0, 0.9f), $.t(1f, 0.9f), null, 0);
        assertEquals($.t(0, 0.81f), negDiff);

        @Nullable Truth posDiff = NALTruth.Difference.apply($.t(1, 0.9f), $.t(0f, 0.9f), null, 0);
        assertEquals($.t(1, 0.81f), posDiff);


        for (String o: new String[]{Op.DIFFe, Op.DIFFi}) {

            String diff = o;

//
//            assertEq(Bool.False, "(x" + diff + "x)");
//            assertEq(
//
//                    Bool.True,
//                    "(x" + diff + "(--,x))");
//            assertEq(
//                    Bool.True,
//                    "((--,x)" + diff + "x)");

//
//            assertEq("(false-->y)", "((x" + diff + "x)-->y)");
//            assertEq("(true-->y)", "(--(x" + diff + "x)-->y)");
//
//
//            assertEq("(y-->false)", "(y --> (x" + diff + "x))");
//            assertEq("(y-->true)", "(y --> --(x" + diff + "x))");


//            assertEquals(Bool.False, $.diff(x, x));
//            assertEquals(Bool.True, $.diff(x, x.neg()));
//            assertEquals(Bool.True, $.diff(x.neg(), x));

//            assertEquals(Null, $.diff(x, Bool.False));
//            assertEquals(Null, $.diff(x, Bool.True));


//            assertEquals(Bool.False, $.diff(Bool.True, Bool.True));
//            assertEquals(Bool.False, $.diff(Bool.False, Bool.False));
//            assertEquals(Null, $.diff(Null, Null));

//            assertEquals(Bool.True, $.diff(Bool.True, Bool.False));
//            assertEquals(Bool.False, $.diff(Bool.False, Bool.True));


        }
    }


    @Test
    void testIntersectionTautologies() {
        for (Op o: new Op[]{SECTe, SECTi}) {

            String sect = o.str;


            assertEquals(x, o.the(x, x));
            assertEq(Null, o.the(x, x.neg()));

            assertEquals(x, o.the(x, True));
            assertEquals(Null /* False ?  */, o.the(x, False));
            assertEquals(Null, o.the(x, Null));
        }
    }

    @Test
    void testSetTautologies() {
        //TODO
    }

    private static final Term x = $$("x");
    private static final Term y = $$("y");
    private static final Term z = $$("z");


    /** Huntington conj/disj tautologies */
    @Test void testConjTautologies() {
        //a∧true == a		# neutral element (Huntington axiom)
        assertEq(x, and(x, True));
        //a∨false == a		# neutral element (Huntington axiom)
        assertEq(x, or(x, False));
        //a∧¬a == false		# complement (Huntington axiom) induces Principium contradictionis
        assertEq(False, and(x, x.neg()));
        //a∨¬a == true		# complement (Huntington axiom) induces Tertium non datur, law of excluded middle (Russel/Whitehead. Principia Mathematica. 1910, 101 *2.11)
        assertEq(True, or(x, x.neg()));

        //a∧a == a		# idempotent
        assertEq(x, and(x, x));
        //a∨a == a		# idempotent
        assertEq(x, or(x, x));

        //a∧false == false	# (dual to neutral element)
        assertEq(False, and(False, x));
        //a∨true == true		# (dual to neutral element)
        assertEq(True, or(True, x));

        //a∧(a∨b) == a		# absorbtion
        assertEq(x, or(x, and(x, or(x, y))));
        //a∨(a∧b) == a		# absorbtion <=(a),(c),(idem)
        assertEq(x, or(x, or(x, and(x, y))));

        //¬(a∧b) == ¬a∨¬b		# deMorgan
        assertEq(or(x.neg(), y.neg()), and(x,y).neg());
        //¬(a∨b) == ¬a∧¬b		# deMorgan
        assertEq(and(x.neg(), y.neg()), or(x,y).neg());

        //half deMorgan
        assertEq(or(x, y.neg()), and(x.neg(),y).neg());

        assertEq(False, and(False, True));
        assertEq(True, and(True, True));
        assertEq(False, and(False, False));
        assertEq(Null, and(Null, x));
        assertEq(Null, and(Null, Null));


    }

    @Test void wtfAndAnotherOne() {
        assertEq(and(y, x.neg()), and(y, and(x,y).neg()));
    }

    @Test void testConjFactor2() {
        assertEq(False, and(and(x, y), and(x, y.neg())));
    }
    @Test void testConjFactor3() {
        assertEq("(&&,x,y,z)", and(and(x, y), and(x, z)));
    }

    @Test void testDisjFactor2() {
        assertEq(x, or(and(x, y), and(x, y.neg())) );
    }
    @Test void testDisjFactor2_0() {
        assertEq(x, or(x, and(x, y), and(x, y.neg())) );
    }

    @Test void testDisjFactor2Neg() {
        assertEq(x.neg(), or(and(x.neg(), y), and(x.neg(), y.neg())) );
    }


    @Test void testDisjFactor1PosPos() {
        assertEq(x,
                or(x, and(x, y)));
    }
    @Test void testDisjFactor1PosNeg() {
        assertEq(or(x,y),
                or(x, and(x.neg(), y)));
    }
    @Test void testDisjFactor1NegPos() {
        assertEq(or(x.neg(), y),
                or(x.neg(), and(x, y)));
    }
    @Test void testDisjFactor1NegNeg() {
        assertEq(x.neg(),
                or(x.neg(), and(x.neg(), y)));
    }


    @Test void testDisjFactor3() {
        assertEq(x, or(and(x, y), and(x, y.neg()), and(x,z)) );
    }


    @Test void testHuntington3() {
        //¬(¬a∨b) ∨ ¬(¬a∨¬b) == a	# Hungtington3
        assertEq(x, or(or(x.neg(), y).neg(), or(x.neg(), y.neg()).neg()));
    }

    @Test void testRobbinsAxiom3a() {
        //¬(a∨b) ∨ ¬(a∨¬b) == ¬a	# Robbins Algebra axiom3
        assertEq(x.neg(), or( or(x,y).neg(), or(x,y.neg()).neg() ) );
    }

    @Test void testRobbinsAxiom3() {
        //¬(¬(a∨b) ∨ ¬(a∨¬b)) == a	# Robbins Algebra axiom3
        assertEq(x, or( or(x,y).neg(), or(x,y.neg()).neg() ).neg() );
    }


/*
from: 'orbital' , file: semantic-equivalence.utf8.txt
#
# Huntington axioms of boolean algebraic logic
#
a∧b == b∧a		# commutative (Huntington axiom)
a∨b == b∨a		# commutative (Huntington axiom)
(a∧b)∨c == (a∨c)∧(b∨c)	# distributive (Huntington axiom)
(a∨b)∧c == (a∧c)∨(b∧c)	# distributive (Huntington axiom)
a∧true == a		# neutral element (Huntington axiom)
a∨false == a		# neutral element (Huntington axiom)
a∧¬a == false		# complement (Huntington axiom) induces Principium contradictionis
a∨¬a == true		# complement (Huntington axiom) induces Tertium non datur, law of excluded middle (Russel/Whitehead. Principia Mathematica. 1910, 101 *2.11)
#
# alternative form of Huntington axioms (from Huntington[33b] and [33a])
# for the logic basis (¬,∨)
#
a∨b == b∨a		# commutative (Huntington axiom)
(a∨b)∨c == a∨(b∨c)	# associative
¬(¬a∨b) ∨ ¬(¬a∨¬b) == a	# Hungtington3
#
# Robbins algebra (after 1990 proven to be equivalent to alternative Huntington axioms)
# for the logic basis (¬,∨)
#
a∨b == b∨a		# commutative (Huntington axiom)
(a∨b)∨c == a∨(b∨c)	# associative
¬(¬(a∨b) ∨ ¬(a∨¬b)) == a	# Robbins Algebra axiom3
#
# laws derived from Huntington axioms
#
(a∧b)∧c == a∧(b∧c)	# associative
(a∨b)∨c == a∨(b∨c)	# associative
a∧a == a		# idempotent
a∨a == a		# idempotent
a∧(a∨b) == a		# absorbtion
a∨(a∧b) == a		# absorbtion <=(a),(c),(idem)
¬(¬a) == a		# involution "duplex negatio est affirmatio". (induces duality forall a exists b: b = ¬a. dualities: a ¬a, ∧ ∨)
¬(a∧b) == ¬a∨¬b		# deMorgan
¬(a∨b) == ¬a∧¬b		# deMorgan
a∧false == false	# (dual to neutral element)
a∨true == true		# (dual to neutral element)
#
# additional equivalences
#
a→b == ¬b→¬a		# contra positition [Lex contrapositionis]
a→b == ¬a∨b		# material implication
¬(a→b) == a∧¬b		# negated implication
a→b == ¬(a∧¬b)
|= a↔a		# reflexive
a↔b == b↔a		# commutative
(a↔b)↔c == a↔(b↔c)	# associative
a↔b == (a→b)∧(b→a)	# coimplication (alias '↔' introduction or elimination)
a↔b == (a∧b) ∨ (¬a∧¬b)	# equivalence in DNF
a↔b == (a∨¬b) ∧ (¬a∨b)	# equivalence in CNF
¬a↔b == a↔¬b	# "¬ behaves like scalar-multiplication in scalar-product"
¬a↔b == ¬(a↔b)	# "¬ behaves like scalar-multiplication in scalar-product"
a^b == ¬(a↔b)		# "duality" of equivalence and antivalence
a^b == ¬a↔b		#
a^b == b^a		# commutative
a^b == (a∧¬b) ∨ (¬a∧b)	# antivalence in DNF
a^b == (a∨b) ∧ (¬a∨¬b)	# antivalence in CNF
a^a == false
(a^b)^c == a^(b^c)	# associative
(a^b)^c == a↔b↔c	#
(a→b)→c == a→b && b→c
a∧b→c == a→(b→c)	# exportation / importation [Lex exportationis, Lex importationis]
¬a == a→false		# not in INF
¬a == a↔false		# not with equivalence
#
# some important tautologies from axioms
#
|= ¬(a∧¬a)		# Principium contradictionis
|= a ∨ ¬a		# Tertium non datur, law of excluded middle (Russel/Whitehead. Principia Mathematica. 1910, 101 *2.11)
|= a → a		# self implication (c reflexive)
#
# implicative properties of |= and thus inference rules
#
p→q, p |= q		# Modus (ponendo) ponens	 (resp. assuming p→q, p is sufficient for q. repeated application is forward chaining)
p→q, ¬q |= ¬p		# Modus (tollendo) tollens (resp. assuming p→q, q is necessary for p. repeated application is backward chaining)
p→q, q→r |= p→r	# hypothetical "syllogism" Principle of Syllogism (due to affinity to mode Barbara)
p∨q, ¬p |= q		# disjunctive "syllogism"
p, q |= p∧q		# conjunction
p |= p∨q		# weakening addition (alias '∨' introduction)
p∧q |= p		# weakening subtraction (alias '∧' elimination)
a |= b→a		# weakening conditional
a→b, b→c |= a→c	# transitivity
#
# tautological properties of == aka |=∨ (thus inference rules, as well)
#
p→¬p == ¬p
(p→q), (p→r) == p→(q∧r)
p→(q→r) == (p∧q)→r	# chain rule
p→(q→r) == (p∧q)→r	# distribute
p→(q→r) == (p→q)→(p→r)	# distributive
# Rules for quantifiers
# some rules
p→(p→q) |= p→q	# rule of reduction
p→(q→r) |= q→(p→r)	# Law of Permutation, the 'commutative principle' (Russel/Whitehead. Principia Mathematica. 1910, 99 *2.04)
¬p→p |= p		# consequentia mirabilis
(p→r), (q→s) |= (p∧q)→(r∧s)	# Praeclarum Theorema
|= p→(q→p)		# principle of simplification (Russel/Whitehead. Principia Mathematica. 1910, 100 *2.03)
|= p→p			# principle of identity (Russel/Whitehead. Principia Mathematica. 1910, 101 *2.08)
|= p→¬¬p		# Affirmatio est duplex negatio, principle of double negation (Russel/Whitehead. Principia Mathematica. 1910, 101 f)
|= ¬¬p→p		# Duplex negatio est affirmatio, principle of double negation (Russel/Whitehead. Principia Mathematica. 1910, 101 f)
false |= a		# 'ex falso quodlibet'
#
# some less important
#
# diverse
p∨q == ¬p→q		# ∨ as ¬,→
p∧q == ¬(p→¬q)		# ∧ as ¬,→
¬p→p == p		# self proof
p→¬p |= ¬p		# self contradiction
p→q, ¬p→q |= q	# reasoning by cases
¬(p→q) == p∧¬q		# negative implication
¬(p↔q) == (p∨q)∧(¬p∨¬q) # negative equivalence
¬(p↔q) == p↔¬q
p↔¬q == ¬p↔q
p→q |= (p∨r)→(q∨r)
p→r, q→r |= (p∨q)→r
|= (f→g) ∨ (g→f)	# material implication has strange causal relations
#
# definitions
#
X≠Y == ¬(X=Y)
# a nor b == ¬(a∨b)		# Peirce function
# a nand b == ¬(a∧b)
*/

}
