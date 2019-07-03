package nars.term;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import static nars.$.$$;
import static nars.Op.CONJ;
import static nars.term.atom.Bool.False;
import static nars.term.util.TermTest.assertEq;
import static nars.time.Tense.DTERNAL;

@Disabled
public class ConjToSectTest {
    @Test
    void reduce_ConjToSect_subj_intersection_2ary() {
        assertEq("(x-->(a&&b))", "((x-->a) && (x-->b))"); //intersection
        assertEq("(x-->((--,b)&&a))", "((x-->a) && --(x-->b))"); //diff
    }

    @Test
    void reduce_ConjToSect_subj_intersection_2ary_ok_also() {
        assertEq("(vy-->((--,0)&&-1))", CONJ.the(DTERNAL, $$("(--,(vy-->0))"), $$("(vy-->-1)")));
    }

    @Test
    void reduce_ConjToSect_subj_intersection_2ary_contradiction() {
        assertEq(False, "(&&, (x-->a), --(x-->a))"); //contradiction
        assertEq(False, "(&&, (x-->a), (x--> --a))"); //contradiction
    }

    @Test
    void reduce_ConjToSect_subj_intersection_3ary_contradiction() {
        assertEq(False, "(&&, (x-->a), --(x-->a), (x-->b))"); //contradiction
    }

    @Test
    void reduce_ConjToSect_subj_intersection_3ary() {
        assertEq("(x-->(&&,a,b,c))", "(&&, (x-->a), (x-->b), (x --> c))"); //intersection
        assertEq("(x-->(&&,(--,a),b,c))", "(&&, --(x-->a), (x-->b), (x --> c))"); //intersection w/ diff


        assertEq("((x-->(&&,a,b,c))&&z)", "(&&, (x-->a), (x-->b), (x --> c), z)"); //with non-inh

        assertEq("((x-->(&&,a,b,c))&&(z-->w))", "(&&, (x-->a), (x-->b), (x --> c), (z-->w))"); //with other inh
        assertEq("((x-->(&&,a,b,c))&&(z &&+1 w))", "(&&, (x-->a), (x-->b), (x --> c), (z &&+1 w))"); //with non-inh CONJ
    }

    @Test
    void reduce_ConjToSect_subj_intersection_4ary_split() {
        assertEq("((x-->(a&&b))&&(y-->(c&&d)))", "(&&, (x-->a), (x-->b), (y --> c), (y --> d))"); //intersection
    }

    @Test
    void reduce_ConjToSect_pred_union_1() {
        assertEq("(--,(((--,a)&&(--,b))-->x))", "((a-->x) && (b-->x))");
    }

}
