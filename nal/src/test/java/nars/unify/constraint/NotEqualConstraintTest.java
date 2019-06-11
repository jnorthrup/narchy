package nars.unify.constraint;

import nars.term.Terms;
import org.junit.jupiter.api.Test;

import static nars.$.$$;
import static org.junit.jupiter.api.Assertions.assertTrue;

class NotEqualConstraintTest {


    @Test
    void testNeqRComConj() {
        //$.16 ((left&&rotate)-->((--,left)&&(--,rotate))). 7930⋈8160 %0.0;.23%
        assertTrue(Terms.eqRCom($$("left"), $$("((--,left)&&(--,rotate))") ) );
        assertTrue(Terms.eqRCom($$("--left"), $$("((--,left)&&(--,rotate))") ) );
        assertTrue(Terms.eqRCom($$("(left&&rotate)"), $$("((--,left)&&(--,rotate))") ) );
        assertTrue(Terms.eqRCom($$("(--left&&rotate)"), $$("((--,left)&&(--,rotate))") ) );
        assertTrue(Terms.eqRCom($$("(--left && --rotate)"), $$("((--,left)&&(--,rotate))") ) );
    }

}