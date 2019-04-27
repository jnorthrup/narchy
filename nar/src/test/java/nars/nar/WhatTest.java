/*
 * Here comes the text of your license
 * Each line should be prefixed with  *
 */
package nars.nar;


import nars.NAL;
import nars.NAR;
import nars.NARS;
import org.junit.jupiter.api.Test;

class WhatTest {

    /** fundamental test that demonstrates the proof of concept:
     *      a NAR with N separate, isolated attentions,
     *      sharing concepts (and their contained beliefs),
     *      and also sharing cpu time (in some proportion).
     */
    @Test void testCompartmentalization1() {
        NAL<NAL<NAR>> n = NARS.tmp();

    }

}
