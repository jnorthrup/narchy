/*
 * Copyright (C) 2014 me
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http:
 */
package nars.term.util;

import com.google.common.collect.Iterators;
import jcog.data.list.FasterList;
import nars.$;
import nars.Narsese;
import nars.Op;
import nars.io.IO;
import nars.subterm.Subterms;
import nars.term.*;
import nars.term.atom.IdempotentBool;
import nars.term.util.builder.TermBuilder;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.Assertions;

import java.util.List;
import java.util.function.Predicate;
import java.util.function.Supplier;

import static nars.$.*;
import static nars.Op.NEG;
import static nars.Op.PROD;
import static nars.time.Tense.DTERNAL;
import static org.junit.jupiter.api.Assertions.*;

/**
 * static utilities for Term and Subterms tests
 */
public enum TermTest { ;


    public static void assertEquivalentTerm(String term1String, String term2String) {

        try {


            Termed term1 = $.INSTANCE.$(term1String);
            Termed term2 = $.INSTANCE.$(term2String);


            assertEq(term1.term(), term2.term());

        } catch (Exception e) {
            fail(e::toString);
        }
    }

    public static Term assertStable(String is) {
        return assertEq(is,is);
    }

    public static Term assertEq(Term x, Term y) {


        //noinspection RedundantCast
        assertEq((Termlike)x, ((Termlike)y));

        assertEquals(0, y.compareTo(x));
        assertEquals(0, x.compareTo(y));
        assertEquals(0, x.compareTo(x));
        assertEquals(0, y.compareTo(y));


        Op xo = x.op();
        assertEquals(xo, y.op());
        if (xo ==NEG) {
            assertTrue(x instanceof Neg);
            assertTrue(y instanceof Neg);
        }

        assertEquals(x.opBit(), y.opBit());
        assertEquals(x.subs(), y.subs());

        Subterms xs = x.subterms();
        Subterms ys = y.subterms();
        assertEq(xs, ys);


        assertArrayEquals(x.subterms().arrayShared(), y.subterms().arrayShared());
        assertEquals(x.subterms().hashCodeSubterms(), y.subterms().hashCodeSubterms());
        assertEquals(x.subterms().hashCode(), y.subterms().hashCode());

        if (x instanceof Compound)
            Assertions.assertEquals(orderedRecursion(x), orderedRecursion(y));

        //return assertEq(y, x.toString(), x);
        return x;
    }

    public static Term assertEq(String exp, String is) {
        Term t = INSTANCE.$$(is);
        return assertEq(exp, is, t);
    }

    private static Term assertEq(Object exp, String is, Term x) {
        assertEquals(exp, exp instanceof String ? x.toString() : x, new Supplier<String>() {
            @Override
            public String get() {
                return is + " reduces to " + exp;
            }
        });


        //test for stability:
        Term y = null;
        try {
            y = $.INSTANCE.$(x.toString());
        } catch (Narsese.NarseseException e) {
            e.printStackTrace();
            fail(new Supplier<String>() {
                @Override
                public String get() {
                    return x + " -> " + e;
                }
            });
        }
        //assertEquals(u, t, ()-> is + " unstable:\n0:\t" + t + "\n1:\t" + u);
        assertEquals(x, y);

        try {
            Term z = y.anon();
        } catch (Throwable e) {
            fail(e::toString);
        }

        return x;
    }

    public static Term assertEq(Term z, String x) {
        Term y = INSTANCE.$$(x);
        assertEq(z, y);
        assertEq(z, x, y);
        return y;
    }

    public static void assertEq(String exp, Term x) {
        assertEquals(exp, x.toString(), new Supplier<String>() {
            @Override
            public String get() {
                return exp + " != " + x;
            }
        });
    }

    public static void assertEq(Termlike x, Termlike y) {

        Assertions.assertEquals(x, x);
        Assertions.assertEquals(y, y);
        Assertions.assertEquals(x, y);
        Assertions.assertEquals(y, x);


        Assertions.assertEquals(x.toString(), y.toString());
        Assertions.assertEquals(x.hashCode(), y.hashCode());
        Assertions.assertEquals(x.volume(), y.volume());
        Assertions.assertEquals(x.height(), y.height());
        Assertions.assertEquals(x.complexity(), y.complexity());
        Assertions.assertEquals(x.voluplexity(), y.voluplexity());
        Assertions.assertEquals(x.structure(), y.structure());
        Assertions.assertEquals(x.structureSurface(), y.structureSurface());
        Assertions.assertEquals(x.vars(), y.vars());
        Assertions.assertEquals(x.hasVars(), y.hasVars());
        Assertions.assertEquals(x.hasVarDep(), y.hasVarDep());
        Assertions.assertEquals(x.varDep(), y.varDep());
        Assertions.assertEquals(x.hasVarIndep(), y.hasVarIndep());
        Assertions.assertEquals(x.varIndep(), y.varIndep());
        Assertions.assertEquals(x.hasVarQuery(), y.hasVarQuery());
        Assertions.assertEquals(x.varQuery(), y.varQuery());
        Assertions.assertEquals(x.hasVarPattern(), y.hasVarPattern());
        Assertions.assertEquals(x.varPattern(), y.varPattern());


    }

    private static List<Term> orderedRecursion(Term x) {
        List<Term> m = new FasterList(x.volume()*2);
        x.recurseTermsOrdered(new Predicate<Term>() {
            @Override
            public boolean test(Term t) {
                m.add(t);
                return true;
            }
        });
        return m;
    }

    public static void assertEq(Compound a, Compound b) {
        assertEq((Subterms)a, b);
    }

    public static void assertEq(Subterms a, Subterms b) {
        //noinspection RedundantCast
        assertEq((Termlike)a, (Termlike)b);

        assertTrue(Iterators.elementsEqual(a.iterator(), b.iterator()));
        int s = a.subs();
        assertEquals(s, b.subs());
        for (int i = 0; i < s; i++)
            assertEq(a.sub(i), b.sub(i));

        assertEquals(TermBuilder.newCompound(PROD, a), TermBuilder.newCompound(PROD, b));
        assertEquals(TermBuilder.newCompound(PROD, b), TermBuilder.newCompound(PROD, a));
        assertEquals($.INSTANCE.pFast(a), $.INSTANCE.pFast(b));
        assertEquals(TermBuilder.newCompound(PROD, DTERNAL, a), TermBuilder.newCompound(PROD, DTERNAL, b));

        Assertions.assertEquals(a.hashCodeSubterms(), b.hashCodeSubterms());
        Assertions.assertEquals(a.structureSurface(), b.structureSurface());

        Assertions.assertEquals(0, Subterms.compare(a, b));
        Assertions.assertEquals(0, Subterms.compare(a, b));

        boolean aNorm = a.isNormalized();
        boolean bNorm = b.isNormalized();
        assertEquals(aNorm, bNorm, new Supplier<String>() {
            @Override
            public String get() {
                return a + " (" + a.getClass() + ") normalized=" + aNorm + ", " + " (" + b.getClass() + ") normalized=" + bNorm;
            }
        });
        assertEquals(a.isSorted(), b.isSorted());
        assertEquals(a.isCommuted(), b.isCommuted());

        {
            byte[] bytesExpected = IO.termToBytes($.INSTANCE.pFast(a));
            byte[] bytesActual = IO.termToBytes($.INSTANCE.pFast(b));
            assertArrayEquals(bytesExpected, bytesActual);
        }
        {
            if (a.subs() > 0) {
                byte[] bytesExpected = IO.termToBytes($.INSTANCE.sFast(a));
                byte[] bytesActual = IO.termToBytes($.INSTANCE.sFast(b));
                assertArrayEquals(bytesExpected, bytesActual);
            }
        }
//        {
//            byte[] bytesExpected = IO.termToBytes(PROD.the(a));
//            byte[] bytesActual = IO.termToBytes(PROD.the(b));
//            assertArrayEquals(bytesExpected, bytesActual);
//        }
//
//        {
//            byte[] bytesExpected = IO.termToBytes(PROD.the(a));
//            byte[] bytesActual = IO.termToBytes(PROD.the(b));
//            assertArrayEquals(bytesExpected, bytesActual);
//        }
//
//        {
//            if (a.subs() > 0) {
//                byte[] bytesExpected = IO.termToBytes(SETe.the(a));
//                byte[] bytesActual = IO.termToBytes(SETe.the(b));
//                assertArrayEquals(bytesExpected, bytesActual);
//            }
//        }

    }

    /** HACK may need to split this into two functions, one which accepts bool result and others which only accept a thrown exception */
    public static void assertInvalidTerms(@NotNull String... inputs) {
        for (@NotNull String s : inputs) {

            try {
                Term e = Narsese.term(s);
                assertTrue(e instanceof IdempotentBool, new Supplier<String>() {
                    @Override
                    public String get() {
                        return s + " should not be parseable but got: " + e;
                    }
                });

            } catch (Narsese.NarseseException | TermException e) {
                assertTrue(true);
            }
        }
    }

    @Deprecated public static void testParse(String expected, String input) {
        Termed t = null;
        try {
            t = $.INSTANCE.$(input);
        } catch (Narsese.NarseseException e) {
            fail(e);
        }
        if (expected == null)
            expected = input;
        assertEquals(expected, t.toString());
    }

    public static void testParse(String s) {
        testParse(null, s);
    }

}
