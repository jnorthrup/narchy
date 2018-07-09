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


import nars.$;
import nars.Op;
import nars.Param;
import nars.term.Variable;
import nars.term.anon.AnonID;
import org.jetbrains.annotations.Nullable;

import static nars.Op.VAR_DEP;

/**
 * Normalized variable
 * "highly immutable" and re-used
 */
public abstract class NormalizedVariable implements Variable, AnonID {


    /**
     * numerically-indexed variable instance cache; prevents duplicates and speeds comparisons
     */
    private static final NormalizedVariable[][] varCache = new NormalizedVariable[4][Param.MAX_INTERNED_VARS];

    static {

        for (Op o: new Op[]{Op.VAR_PATTERN, Op.VAR_QUERY, VAR_DEP, Op.VAR_INDEP}) {
            int t = opToVarIndex(o);
            for (byte i = 1; i < Param.MAX_INTERNED_VARS; i++) {
                varCache[t][i] = vNew(o, i);
            }
        }
    }

    public final short id;
    /*@Stable*/
    private final byte[] bytes;

    NormalizedVariable(/*@NotNull*/ Op type, byte num) {
        assert num > 0;

        id = AnonID.termToId(type, num);

        byte[] b = new byte[2];
        b[0] = type.id;
        b[1] = num;
        this.bytes = b;
    }


    public static Op typeIndex(char c) {
        switch (c) {
            case '%':
                return Op.VAR_PATTERN;
            case '#':
                return VAR_DEP;
            case '$':
                return Op.VAR_INDEP;
            case '?':
                return Op.VAR_QUERY;
        }
        throw new RuntimeException("invalid variable character");
    }

    private static int opToVarIndex(/*@NotNull*/ Op o) {
        return opToVarIndex(o.id);
    }

    private static int opToVarIndex(/*@NotNull*/ byte oid) {
        return oid - VAR_DEP.id;
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

    /**
     * TODO move this to TermBuilder
     */

    private static NormalizedVariable vNew(/*@NotNull*/ Op type, byte id) {
        switch (type) {
            case VAR_PATTERN:
                return new VarPattern(id);
            case VAR_QUERY:
                return new VarQuery(id);
            case VAR_DEP:
                switch (id) {
                    case 126:
                        return Op.imInt;
                    case 127:
                        return Op.imExt;
                    default:
                        assert (id > 0);
                        return new VarDep(id);
                }
            case VAR_INDEP:
                return new VarIndep(id);
            default:
                throw new UnsupportedOperationException();
        }
    }

    public static NormalizedVariable the(/*@NotNull*/ Op op, byte id) {
        return the(op.id, id);
    }

    public static NormalizedVariable the(/*@NotNull*/ byte op, byte id) {
        if (id > 0 && id < Param.MAX_INTERNED_VARS) {
            return varCache[NormalizedVariable.opToVarIndex(op)][id];
        } else {
            return vNew(Op.ops[op], id);
        }
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
    public byte anonType() {
        return bytes[0];
    }

    @Override
    public final boolean equals(Object obj) {
        return obj == this ||
                (obj instanceof NormalizedVariable
                        && ((NormalizedVariable) obj).id == id);
    }

    @Override
    public @Nullable NormalizedVariable normalize(byte vid) {
        if (anonNum() == vid)
            return this;
        else
            return $.v(op(), vid);
    }

    @Override
    public final int hashCode() {
        return id;
    }


    @Override
    public String toString() {
        return op().ch + Integer.toString(anonNum());
    }


}
