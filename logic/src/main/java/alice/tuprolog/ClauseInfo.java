/*
 * tuProlog - Copyright (C) 2001-2002  aliCE team at deis.unibo.it
 *
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 2.1 of the License, or (at your option) any later version.
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this library; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA
 */
package alice.tuprolog;

import java.util.AbstractMap;
import java.util.IdentityHashMap;

/**
 * This class mantains information about a clause creation
 * (clause copy, final time T after renaming, validity stillValid Flag).
 * These information are necessary to the Theory Manager
 * to use the clause in a consistent way
 */
public class ClauseInfo {

    /**
     * referring clause
     */
    public final Struct clause;

    /**
     * head of clause
     */
    public final Struct head;

    /**
     * body of clause
     */
    public final SubGoalTree body;


    /**
     * if the clause is part of a theory in a lib (null if not)
     */
    public final String libName;


    

    /**
     * building a valid clause with a time stamp = original time stamp + NumVar in clause
     */
    public ClauseInfo(Struct clause_, String lib) {
        clause = clause_;
        libName = lib;
        head = (Struct) clause_.sub(0); 
        body = extractBody(clause_.sub(1));
    }

    public ClauseInfo(Struct head, SubGoalTree body, Struct clause_, String lib) {
        clause = clause_;
        libName = lib;
        this.head = head;
        this.body = body;
    }


    /**
     * Gets a clause from a generic Term
     */
    static SubGoalTree extractBody(Term body) {
        SubGoalTree r = new SubGoalTree();
        extractBody(r, body);
        return r;
    }

    private static void extractBody(SubGoalTree parent, Term body) {
        while (body instanceof Struct && ((Struct) body).name().equals(",")) {
            Term t = ((Struct) body).sub(0);
            if (t instanceof Struct && ((Struct) t).name().equals(",")) {
                extractBody(parent.addChild(), t);
            } else {
                parent.add(t);
            }
            body = ((Struct) body).sub(1);
        }
        parent.add(body);
    }


    /**
     * Gets the string representation
     * recognizing operators stored by
     * the operator manager
     */
    public String toString(OperatorManager op) {
        int p;
        if ((p = op.opPrio(":-", "xfx")) >= OperatorManager.OP_LOW) {
            String st = indentPredicatesAsArgX(clause.sub(1), op, p);
            String head = clause.sub(0).toStringAsArgX(op, p);
            return st.equals("true") ? head + ".\n" : head + " :-\n\t" + st + ".\n";
        }

        if ((p = op.opPrio(":-", "yfx")) >= OperatorManager.OP_LOW) {
            String st = indentPredicatesAsArgX(clause.sub(1), op, p);
            String head = clause.sub(0).toStringAsArgY(op, p);
            return st.equals("true") ? head + ".\n" : head + " :-\n\t" + st + ".\n";
        }

        if ((p = op.opPrio(":-", "xfy")) >= OperatorManager.OP_LOW) {
            String st = indentPredicatesAsArgY(clause.sub(1), op, p);
            String head = clause.sub(0).toStringAsArgX(op, p);
            return st.equals("true") ? head + ".\n" : head + " :-\n\t" + st + ".\n";
        }
        return (clause.toString());
    }


    











    /**
     * Perform copy for use in current engine's demostration
     *
     * @param idExecCtx Current ExecutionContext id
     */
    void copyTo(int idExecCtx, ExecutionContext target) {
        IdentityHashMap<Var, Var> v = new IdentityHashMap<>();

        Struct headCopy = (Struct) head.copy(v, idExecCtx);
        target.headClause = headCopy;

        SubGoalTree bodyCopy = new SubGoalTree();
        bodyCopy(body, bodyCopy, v, idExecCtx);
        target.goalsToEval = new SubGoalStore( bodyCopy );
        
    }

    private static void bodyCopy(SubGoalTree source, SubGoalTree destination, AbstractMap<Var, Var> map, int id) {
        for (SubTree s : source) {
            if (s.isLeaf()) {
                destination.add(((Term) s).copy(map, id));
            } else {
                SubGoalTree src = (SubGoalTree) s;
                SubGoalTree dest = destination.addChild();
                bodyCopy(src, dest, map, id);
            }
        }
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        ClauseInfo ci = (ClauseInfo)obj;
        return head.equals(ci.head) && body.equals(ci.body) && clause.equals(ci.clause); 
    }

    /**
     * Gets the string representation with default operator representation
     */
    public String toString() {
        
        String st = indentPredicates(clause.sub(1));
        return (clause.sub(0) + " :-\n\t" + st + ".\n");
    }

    static private String indentPredicates(Term t) {
        if (t instanceof Struct) {
            Struct co = (Struct) t;
            return co.name().equals(",") ? co.sub(0) + ",\n\t" + indentPredicates(co.sub(1)) : t.toString();
        } else {
            return t.toString();
        }
    }
    
    /*commented by Roberta Calegari fixed following issue 20 Christian Lemke suggestion
     * static private String indentPredicatesAsArgX(Term t,OperatorManager op,int p) {
        if (t instanceof Struct) {
            Struct co=(Struct)t;
            if (co.getName().equals(",")) {
                return co.getArg(0).toStringAsArgX(op,p)+",\n\t"+
                "("+indentPredicatesAsArgX(co.getArg(1),op,p)+")";
            } else {
                return t.toStringAsArgX(op,p);
            }
        } else {
            return t.toStringAsArgX(op,p);
        }
        
    }
    
    static private String indentPredicatesAsArgY(Term t,OperatorManager op,int p) {
        if (t instanceof Struct) {
            Struct co=(Struct)t;
            if (co.getName().equals(",")) {
                return co.getArg(0).toStringAsArgY(op,p)+",\n\t"+
                "("+indentPredicatesAsArgY(co.getArg(1),op,p)+")";
            } else {
                return t.toStringAsArgY(op,p);
            }
        } else {
            return t.toStringAsArgY(op,p);
        }
    }*/

    static private String indentPredicatesAsArgX(Term t, OperatorManager op, int p) {
        if (t instanceof Struct) {
            Struct co = (Struct) t;
            if (co.name().equals(",")) {
                int prio = op.opPrio(",", "xfy");
                StringBuilder sb = new StringBuilder(prio >= p ? "(" : "");
                sb.append(co.sub(0).toStringAsArgX(op, prio));
                sb.append(",\n\t");
                sb.append(indentPredicatesAsArgY(co.sub(1), op, prio));
                if (prio >= p) sb.append(')');

                return sb.toString();

            } else {
                return t.toStringAsArgX(op, p);
            }
        } else {
            return t.toStringAsArgX(op, p);
        }
    }

    static private String indentPredicatesAsArgY(Term t, OperatorManager op, int p) {
        if (t instanceof Struct) {
            Struct co = (Struct) t;
            if (co.name().equals(",")) {
                int prio = op.opPrio(",", "xfy");
                StringBuilder sb = new StringBuilder(prio > p ? "(" : "");
                sb.append(co.sub(0).toStringAsArgX(op, prio));
                sb.append(",\n\t");
                sb.append(indentPredicatesAsArgY(co.sub(1), op, prio));
                if (prio > p) sb.append(')');

                return sb.toString();
            } else {
                return t.toStringAsArgY(op, p);
            }
        } else {
            return t.toStringAsArgY(op, p);
        }
    }


}