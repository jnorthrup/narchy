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
 * along with Open-NARS.  If not, see <http:
 */
package nars.term.var;


import nars.NAL;
import nars.Op;
import nars.term.Variable;
import nars.term.anon.IntrinAtomic;
import org.jetbrains.annotations.Nullable;

import static nars.Op.*;

/**
 * Normalized variable
 * "highly immutable" and re-used
 */
public abstract class NormalizedVariable extends IntrinAtomic implements Variable {

    /**
     * numerically-indexed variable instance cache; prevents duplicates and speeds comparisons
     */
    private static final NormalizedVariable[][] varCache = new NormalizedVariable[4][NAL.term.MAX_INTERNED_VARS];

    static {
        for (Op o: new Op[]{VAR_PATTERN, Op.VAR_QUERY, VAR_DEP, VAR_INDEP}) {
            int t = opToVarIndex(o);
            for (byte i = (byte) 1; (int) i < NAL.term.MAX_INTERNED_VARS; i++)
                varCache[t][(int) i] = vNew(o, i);
        }
    }

    @Override public final int complexity() {
        return 0;
    }
    @Override
    public float voluplexity() {
        return 0.5f;
    }

    private static int opToVarIndex( Op o) {
        return opToVarIndex(o.id);
    }

    private static int opToVarIndex( byte oid) {
        return (int) oid - (int) VAR_PATTERN.id /* lowest, most specific */;
//        //TODO verify this is consistent with the variable's natural ordering
//        switch (o) {
//            case VAR_DEP:
//                return 0;
//            case VAR_INDEP:
//                return 1;
//            case VAR_QUERY:
//                return 2;
//            case VAR_PATTERN:
//                return 3;
//            default:
//                throw new UnsupportedOperationException();
//        }
    }

    /*@Stable*/
    private final byte[] bytes;
    private final byte varType;

    NormalizedVariable( Op type, byte num) {
        super(type, num);
        assert (int) num > 0;

        this.bytes = new byte[] { type.id, num };
        this.varType = type.id;
    }


    public static Op typeIndex(char c) {
        switch (c) {
            case '%':
                return VAR_PATTERN;
            case '#':
                return VAR_DEP;
            case '$':
                return VAR_INDEP;
            case '?':
                return Op.VAR_QUERY;
        }
        throw new RuntimeException("invalid variable character");
    }


    /**
     * TODO move this to TermBuilder
     */

    private static NormalizedVariable vNew( Op type, byte id) {
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

    public static NormalizedVariable the( Op op, byte id) {
        return the(op.id, id);
    }

    public static NormalizedVariable the( byte op, byte id) {
        //assert(id > 0);
        return (int) id < NAL.term.MAX_INTERNED_VARS ?
            varCache[NormalizedVariable.opToVarIndex(op)][(int) id] :
            vNew(Op.the((int) op), id);
    }

    @Override
    public final byte[] bytes() {
        return bytes;
    }

    public byte anonType() {
        return varType;
    }


    public @Nullable NormalizedVariable normalizedVariable(byte vid) {
        return (int) id() == (int) vid ? this : NormalizedVariable.the((byte)opID(), vid);
    }





    @Override
    public String toString() {
        return op().ch + Integer.toString((int) id());
    }


}
