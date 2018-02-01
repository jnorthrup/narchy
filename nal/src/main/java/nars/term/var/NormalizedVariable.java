/*
 * Variable.java
 *
 * Copyright (C) 2008  Pei Wang
 *
 * This file is part of Open-NARS.
 *
 * Open-NARS is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 2 of the License, or
 * (at your option) any later version.
 *
 * Open-NARS is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Open-NARS.  If not, see <http://www.gnu.org/licenses/>.
 */
package nars.term.var;


import nars.$;
import nars.Op;
import nars.Param;
import nars.term.anon.AnonID;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Normalized variable
 * "highly immutable" and re-used
 */
public abstract class NormalizedVariable implements Variable, AnonID {


    public final short id;

//    static final byte DEP_ORD = Op.VAR_DEP.id;
//    static {
//
//        //test for the expected ordering of the variable op ordinals
//        assert(Op.VAR_PATTERN.id - DEP_ORD == 3);
//    }

    private final byte[] bytes;

    protected NormalizedVariable(/*@NotNull*/ Op type, byte num) {
        assert(num > 0);
        assert(num < Byte.MAX_VALUE);

        id = AnonID.termToId(type, num );

        byte[] b = new byte[2];
        b[0] = type.id;
        b[1] = num;
        this.bytes = b;
    }


    @Override
    public final byte[] bytes() {
        return bytes;
    }

    @Override
    public final short anonID() {
        return id;
    }

    @Override
    public byte anonNum() {
        return bytes[1];
    }

    //@Override abstract public boolean equals(Object other);

    @Override
    public final boolean equals(Object obj) {
        return obj == this ||
                (obj instanceof NormalizedVariable
                        && ((NormalizedVariable) obj).id == id);
    }


    //    @Override
//    public boolean equals(Object that) {
//        boolean e = that == this ||
//                (that instanceof AbstractVariable && that.hashCode() == hash);
//
//        if (e) {
//            if (!toString().equals(that.toString()))
//                System.err.println("warning: " + this + " and " + that + " are not but considered equal");
//            return true;
//        } else {
//            if (toString().equals(that.toString()))
//                System.err.println("warning: " + this + " and " + that + " are but considered not equal");
//            return false;
//        }
//    }


    @Override
    public @Nullable Variable normalize(byte vid) {
        if (anonNum() == vid)
            return this;
        else
            return $.v(op(), vid);
    }


    @Override
    public final int hashCode() {
        return id;
    }


    @NotNull
    @Override
    public String toString() {
        return op().ch + Integer.toString(anonNum());
    }

    /**
     * numerically-indexed variable instance cache; prevents duplicates and speeds comparisons
     */
    private static final NormalizedVariable[][] varCache = new NormalizedVariable[4][Param.MAX_INTERNED_VARS];

    @NotNull
    public static Op typeIndex(char c) {
        switch (c) {
            case '%':
                return Op.VAR_PATTERN;
            case '#':
                return Op.VAR_DEP;
            case '$':
                return Op.VAR_INDEP;
            case '?':
                return Op.VAR_QUERY;
        }
        throw new RuntimeException("invalid variable character");
    }

    //    @NotNull
//    static Op varCacheFor(Op c) {
//        switch (c) {
//            case Op.VAR_PATTERN:
//                return varCache[0]
//            case '#':
//                return varCacheFor
//            case '$':
//                return Op.VAR_INDEP;
//            case '?':
//                return Op.VAR_QUERY;
//        }
//        throw new RuntimeException(c + " not a variable");
//    }
    public static int opToVarIndex(/*@NotNull*/ Op o) {
        switch (o) {
            case VAR_PATTERN:
                return 0;
            case VAR_DEP:
                return 1;
            case VAR_INDEP:
                return 2;
            case VAR_QUERY:
                return 3;
            default:
                throw new UnsupportedOperationException();
        }
    }


//    @Override
//    abstract public int volume();

    static {
        //precompute cached variable instances
        for (Op o : new Op[]{Op.VAR_PATTERN, Op.VAR_QUERY, Op.VAR_DEP, Op.VAR_INDEP}) {
            int t = opToVarIndex(o);
            for (byte i = 1; i < Param.MAX_INTERNED_VARS; i++) {
                varCache[t][i] = vNew(o, i);
            }
        }
    }

    /**
     * TODO move this to TermBuilder
     */
    @NotNull
    static NormalizedVariable vNew(/*@NotNull*/ Op type, byte id) {
        switch (type) {
            case VAR_PATTERN:
                return new VarPattern(id);
            case VAR_QUERY:
                return new VarQuery(id);
            case VAR_DEP:
                return new VarDep(id);
            case VAR_INDEP:
                return new VarIndep(id);
            default:
                throw new UnsupportedOperationException();
        }
    }

    public static NormalizedVariable the(/*@NotNull*/ Op type, byte id) {
        assert(id > 0);
        if (id < Param.MAX_INTERNED_VARS) {
            return varCache[NormalizedVariable.opToVarIndex(type)][id];
        } else {
            return vNew(type, id);
        }
    }




    //    //TODO replace this with a generic counting method of how many subterms there are present
//    public static int numPatternVariables(Term t) {
//        t.value(new TermToInt()) //..
////        final int[] has = {0};
////        t.recurseTerms((t1, superterm) -> {
////            if (t1.op() == Op.VAR_PATTERN)
////                has[0]++;
////        });
////        return has[0];
//    }


}
