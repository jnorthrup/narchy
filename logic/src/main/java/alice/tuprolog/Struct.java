/*
 * tuProlog - Copyright (C) 2001-2007 aliCE team at deis.unibo.it
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

import org.eclipse.collections.impl.map.mutable.UnifiedMap;

import java.util.*;

/**
 * Struct class represents both compound prolog term
 * and atom term (considered as 0-arity compound).
 */
public class Struct extends Term {

    /**
     * name of the structure
     */
    private String name;
    /**
     * args array
     */
    private Term[] subs;

    /**
     * to speedup hash map operation
     */
    private String key;
    /**
     * primitive behaviour
     */
    private transient PrologPrim primitive;
    /**
     * it indicates if the term is resolved
     */
    private boolean resolved;

    /**
     * Builds a Struct representing an atom
     */
    public Struct(String f) {
        this(Term.EmptyTermArray, f);
        resolved = true;
    }


    /**
     * Builds a compound, with an array of arguments
     */
    public Struct(String f, Term... argList) {
        this(argList/*.clone()*/, f);
//        for (int i = 0; i < argList.length; i++)
//            if (argList[i] == null)
//                throw new InvalidTermException("Arguments of a Struct cannot be null");
//            else
//                subs[i] = argList[i];
    }


    /**
     * Builds a structure representing an empty list
     */
    private Struct() {
        this(Term.EmptyTermArray, "[]");
        resolved = true;
    }


    /**
     * Builds a list providing head and tail
     */
    public Struct(Term h, Term t) {
        this(new Term[]{h, t}, ".");
    }

    /**
     * Builds a list specifying the elements
     */
    public Struct(Term[] argList) {
        this(argList, 0);
    }

    private Struct(Term[] argList, int index) {
        this(".", new Term[2]);
        if (index < argList.length) {
            subs[0] = argList[index];
            subs[1] = new Struct(argList, index + 1);
        } else {
            name = "[]";
            subs = EmptyTermArray;
            resolved = true;
        }
    }

    /**
     * Builds a compound, with a linked list of arguments
     */
    Struct(String f, List<Term> al) {
        this(al.toArray(Term.EmptyTermArray), f);
    }

    public Struct(String name_, int subs) {
        this(subs > 0 ? new Term[subs] : Term.EmptyTermArray, name_);
    }

    private Struct(Term[] subs, String name_) {
        if (name_ == null)
            throw new InvalidTermException("The functor of a Struct cannot be null");

        int arity = subs.length;
        if (name_.isEmpty() && arity > 0)
            throw new InvalidTermException("The functor of a non-atom Struct cannot be an empty string");

        name = name_;
        key = name + '/' + arity;
        this.subs = subs;
        resolved = arity == 0;
    }


    final static Struct EmptyList = new Struct() {
        @Override
        public void append(Term t) {
            throw new UnsupportedOperationException();
        }

        @Override
        public boolean isList() {
            return true;
        }

        @Override
        public boolean isEmptyList() {
            return true;
        }

        @Override
        public int listSize() {
            return 0;
        }

        @Override
        public boolean isAtomic() {
            return true;
        }
    };

    public static Struct emptyList() {
        return EmptyList;
    }

    public static Struct emptyListMutable() {
        return new Struct();
    }


    /**
     * @return
     */
    final String key() {
        return key;
    }

    /**
     * arity: Gets the number of elements of this structure
     */
    public final int subs() {
        return subs.length;
    }

    /**
     * Gets the functor name  of this structure
     */
    public final String name() {
        return name;
    }

    /**
     * Gets the i-th element of this structure
     * <p>
     * No bound check is done
     */
    public final Term sub(int index) {
        return subs[index];
    }

    /**
     * Sets the i-th element of this structure
     * <p>
     * (Only for internal service)
     */
    void setSub(int index, Term argument) {
        subs[index] = argument;
    }

    /**
     * Gets the i-th element of this structure
     * <p>
     * No bound check is done. It is equivalent to
     * <code>getArg(index).getTerm()</code>
     */
    public Term subResolve(int index) {
        Term s = subs[index];
        return s instanceof Var ? s.term() : s;
    }

    @Override
    public boolean isAtom() {
        return subs() == 0;
    }

    @Override
    public boolean isCompound() {
        return subs() > 0;
    }

    @Override
    public boolean isAtomic() {
        return subs() == 0;
    }

    @Override
    public boolean isList() {
        return isEmptyList() || (subs() == 2 && name.equals(".") && subs[1].isList());
    }

    @Override
    public boolean isGround() {
        if (!isAtomic()) {
            Term[] a = this.subs;
            for (int i = 0; i < subs(); i++) {
                if (!a[i].isGround())
                    return false;
            }
        }

        return true;
    }

    /**
     * Check is this struct is clause or directive
     */
    public boolean isClause() {
        return subs() > 1 && name.equals(":-") && subs[0].term() instanceof Struct;

    }

    /**
     * Gets an argument inside this structure, given its name
     *
     * @param name name of the structure
     * @return the argument or null if not found
     */
    public Struct sub(String name) {
        if (subs() == 0) {
            return null;
        }
        for (int i = 0; i < subs(); i++) {
            if (subs[i] instanceof Struct) {
                Struct s = (Struct) subs[i];
                if (s.name().equals(name)) {
                    return s;
                }
            }
        }
        for (int i = 0; i < subs(); i++) {
            if (subs[i] instanceof Struct) {
                Struct s = (Struct) subs[i];
                Struct sol = s.sub(name);
                if (sol != null) {
                    return sol;
                }
            }
        }
        return null;
    }


    /**
     * Test if a term is greater than other
     */
    @Override
    public boolean isGreater(Term t) {
        t = t.term();
        if (!(t instanceof Struct)) {
            return true;
        } else {
            Struct ts = (Struct) t;
            int tarity = ts.subs();
            if (subs() > tarity) {
                return true;
            } else if (subs() == tarity) {
                int nc = name.compareTo(ts.name);
                if (nc > 0) {
                    return true;
                } else if (nc == 0) {
                    Term[] bb = ts.subs;
                    if (this.subs != bb) {
                        for (int c = 0; c < subs(); c++) {
                            Term a = this.subs[c];
                            Term b = bb[c];
                            if (a == b) continue;
                            if (a.isGreater(b)) {
                                return true;
                            } else if (!a.equals(b)) {
                                return false;
                            }
                        }
                    }
                }
            }
        }
        return false;
    }

    @Override
    public boolean isGreaterRelink(Term t, ArrayList<String> vorder) {
        t = t.term();
        if (!(t instanceof Struct)) {
            return true;
        } else {
            Struct ts = (Struct) t;
            int tarity = ts.subs();
            if (subs() > tarity) {
                return true;
            } else if (subs() == tarity) {

                if (name.compareTo(ts.name) > 0) {
                    return true;
                } else if (name.compareTo(ts.name) == 0) {
                    for (int c = 0; c < subs(); c++) {

                        if (subs[c].isGreaterRelink(ts.subs[c], vorder)) {
                            return true;
                        } else if (!subs[c].isEqual(ts.subs[c])) {
                            return false;
                        }
                    }
                }
            }
        }
        return false;
    }

    /**
     * Test if a term is equal to other
     */
    @Override
    public boolean isEqual(Term t) {

        if (t == this) return true;

        t = t.term();

        if (t == this) return true;

        if (t instanceof Struct) {
            Struct ts = (Struct) t;
            if (subs() == ts.subs() && name.equals(ts.name)) {
                if (this.subs != ts.subs) {
                    for (int c = 0; c < subs(); c++) {
                        if (!subs[c].equals(ts.subs[c])) {
                            return false;
                        }
                    }

                }
                return true;
            } else {
                return false;
            }
        } else {
            return false;
        }
    }


    public boolean isConstant() {
        if (!isAtomic()) {
            for (Term x : subs) {
                if (x instanceof Var)
                    return false;
                if ((x instanceof Struct) && (!((Struct) x).isConstant()))
                    return false;
            }
        }
        return true;
    }

    /**
     * Gets a copy of this structure
     *
     * @param vMap is needed for register occurence of same variables
     */
    @Override
    public Term copy(Map<Var, Var> vMap, int idExecCtx) {

        if (!(vMap instanceof IdentityHashMap) && isConstant())
            return this;

        final int arity = this.subs();
        Term[] xx = this.subs, yy = null;

        for (int c = 0; c < arity; c++) {
            Term x = xx[c];
            Term y = x.copy(vMap, idExecCtx);
            if (x != y) {
                if (yy == null)
                    yy = xx.clone();
            }

            if (yy != null)
                yy[c] = y;
        }
        if (yy == null)
            return this; //unchanged

        Struct t = new Struct(name, yy);
        t.resolved = resolved;
        t.key = key;
        t.primitive = primitive;

        return t;
    }


    /**
     * Gets a copy of this structure
     *
     * @param vMap     is needed for register occurence of same variables
     */
    @Override
    Term copy(Map<Var, Var> vMap, Map<Term, Var> substMap) {

        if (!(vMap instanceof IdentityHashMap) && isGround())
            return this;

        Struct t = new Struct(name, new Term[subs()]);
        t.resolved = false;
        t.key = key;
        t.primitive = null;
        Term[] thatArg = t.subs;
        Term[] thisArg = this.subs;
        final int arity = this.subs();

        for (int c = 0; c < arity; c++) {
            Term xc = thisArg[c];
            Term yc;
            if (xc instanceof Var || (xc instanceof Struct && (!((Struct) xc).isConstant()))) {

                if (substMap == null)
                    substMap = new IdentityHashMap<>();

                yc = xc.copy(vMap, substMap);
            } else
                yc = xc;

            thatArg[c] = yc;
        }
        return t;
    }


    @Override
    public void resolveTerm() {
        if (resolvable())
            resolveTerm(null, now());
    }

    /**
     * resolve term
     */
    @Override
    void resolveTerm(long count) {
        if (resolvable())
            resolveTerm(null, count);
    }

    private boolean resolvable() {
        if (resolved)
            return false;
        if (subs() == 0 || isGround()) {
            resolved = true;
            return false;
        }
        return true;
    }


    /**
     * Resolve name of terms
     *
     * @param vl    list of variables resolved
     * @param count start timestamp for variables of this term
     * @return next timestamp for other terms
     */
    private void resolveTerm(Map<String, Var> vl, final long count) {

        if (resolved)
            return;

        Term[] arg = this.subs;
        int arity = this.subs();
        for (int c = 0; c < arity; c++) {
            Term term = arg[c];
            if (term == null)
                continue;

            term = term.term();

            if (term instanceof Var) {
                Var t = (Var) term;
                t.setTimestamp(count);
                if (!t.isAnonymous()) {

                    Var found = null;
                    String tName = t.name();
                    if (vl != null && !vl.isEmpty()) {
                        found = vl.get(tName);
                    }
                    if (found != null) {
                        arg[c] = found;
                    } else {
                        if (vl == null) vl = new UnifiedMap(); //construct to share recursively
                        vl.putIfAbsent(tName, t);
                    }
                }
            } else if (term instanceof Struct) {

                if (vl == null) vl = new UnifiedMap(); //construct to share recursively

                ((Struct) term).resolveTerm(vl, count);
            }
        }
        resolved = true;
    }


    /**
     * Is this structure an empty list?
     */
    @Override
    public boolean isEmptyList() {
        return subs() == 0 && name.equals("[]");
    }

    /**
     * Gets the head of this structure, which is supposed to be a list.
     * <p>
     * <p>
     * Gets the head of this structure, which is supposed to be a list.
     * If the callee structure is not a list, throws an <code>UnsupportedOperationException</code>
     * </p>
     */
    public Term listHead() {
        if (!isList())
            throw new UnsupportedOperationException("The structure " + this + " is not a list.");
        return subs[0].term();
    }

    /**
     * use with caution
     */
    public Term[] subArrayShared() {
        return subs;
    }

    /**
     * Gets the tail of this structure, which is supposed to be a list.
     * <p>
     * <p>
     * Gets the tail of this structure, which is supposed to be a list.
     * If the callee structure is not a list, throws an <code>UnsupportedOperationException</code>
     * </p>
     */
    public Struct listTail() {
        if (!isList())
            throw new UnsupportedOperationException("The structure " + this + " is not a list.");
        return (Struct) subs[1].term();
    }

    /**
     * Gets the number of elements of this structure, which is supposed to be a list.
     * <p>
     * <p>
     * Gets the number of elements of this structure, which is supposed to be a list.
     * If the callee structure is not a list, throws an <code>UnsupportedOperationException</code>
     * </p>
     */
    public int listSize() {

        if (!isList())
            throw new UnsupportedOperationException("The structure " + this + " is not a list.");

        Struct t = this;
        int count = 0;
        while (!t.isEmptyList()) {
            count++;
            t = (Struct) t.subs[1].term();
        }
        return count;
    }

    /**
     * Gets an iterator on the elements of this structure, which is supposed to be a list.
     * <p>
     * <p>
     * Gets an iterator on the elements of this structure, which is supposed to be a list.
     * If the callee structure is not a list, throws an <code>UnsupportedOperationException</code>
     * </p>
     */
    public Iterator<Term> listIterator() {
        if (!isList())
            throw new UnsupportedOperationException("The structure " + this + " is not a list.");
        return new StructIterator(this);
    }


    /**
     * Gets a list Struct representation, with the functor as first element.
     */
    Struct toList() {
        Struct t = emptyList();
        Term[] arg = this.subs;

        for (int c = subs() - 1; c >= 0; c--) {
            t = new Struct(arg[c].term(), t);
        }
        return new Struct(new Struct(name), t);
    }


    /**
     * Gets a flat Struct from this structure considered as a List
     * <p>
     * If this structure is not a list, null object is returned
     */
    Struct fromList() {
        Term ft = subs[0].term();
        if (!ft.isAtomic())
            return null;

        Struct at = (Struct) subs[1].term();
        List<Term> al = new LinkedList<>();
        while (!at.isEmptyList()) {
            if (!at.isList())
                return null;

            al.add(at.subResolve(0));
            at = (Struct) at.subResolve(1);
        }
        return new Struct(((Struct) ft).name, al);
    }


    /**
     * Appends an element to this structure supposed to be a list
     */
    public void append(Term t) {
        if (isEmptyList()) {
            subs = new Term[]{t, emptyListMutable()};
            name = ".";
            key = "./2"; /* Added by Paolo Contessi */
        } else if (subs[1].isList()) {
            ((Struct) subs[1]).append(t);
        } else {
            subs[1] = t;
        }
    }


    /**
     * Try to unify two terms
     *
     * @param y   the term to unify
     * @param vl1 list of variables unified
     * @param vl2 list of variables unified
     * @return true if the term is unifiable with this one
     */
    @Override
    boolean unify(Collection<Var> vl1, Collection<Var> vl2, Term y) {

        if (this == y) return true;

        y = y.term();

        if (this == y) return true;

        if (y instanceof Struct) {
            Struct yy = (Struct) y;
            final int arity = this.subs();
            if (arity == yy.subs() && name.equals(yy.name)) {
                Term[] xarg = this.subs;
                Term[] yarg = yy.subs;
                for (int c = 0; c < arity; c++) {
                    if (c > 0 && xarg[c] == xarg[c - 1] && yarg[c] == yarg[c - 1])
                        continue; //repeat term, skip

                    if (!xarg[c].unify(vl1, vl2, yarg[c]))
                        return false;

                }
                return true;
            }
        } else if (y instanceof Var) {
            return y.unify(vl2, vl1, this);
        }
        return false;
    }


    /**
     * Set primitive behaviour associated at structure
     */
    void setPrimitive(PrologPrim b) {
        primitive = b;
    }

    /**
     * Get primitive behaviour associated at structure
     */
    public PrologPrim getPrimitive() {
        return primitive;
    }


    /**
     * Check if this term is a primitive struct
     */
    public boolean isPrimitive() {
        return primitive != null;
    }


    /**
     * Gets the string representation of this structure
     * <p>
     * Specific representations are provided for lists and atoms.
     * Names starting with upper case letter are enclosed in apices.
     */
    public String toString() {

        switch (name) {
            case "[]":
                if (subs() == 0) return "[]";
                break;
            case ".":
                if (subs() == 2) return '[' + toString0() + ']';
                break;
            case "{}":
                return '{' + toString0_bracket() + '}';
        }
        String s = (Parser.isAtom(name) ? name : '\'' + name + '\'');
        if (subs() > 0) {
            s = s + '(';
            for (int c = 1; c < subs(); c++) {
                s = s + (!(subs[c - 1] instanceof Var) ? subs[c - 1].toString() : ((Var) subs[c - 1]).toStringFlattened()) + ',';
            }
            s = s + (!(subs[subs() - 1] instanceof Var) ? subs[subs() - 1].toString() : ((Var) subs[subs() - 1]).toStringFlattened()) + ')';
        }
        return s;
    }

    private String toString0() {
        Term h = subs[0].term();
        Term t = subs[1].term();
        if (t.isList()) {
            Struct tl = (Struct) t;
            if (tl.isEmptyList()) {
                return h.toString();
            }
            return (h instanceof Var ? ((Var) h).toStringFlattened() : h.toString()) + ',' + tl.toString0();
        } else {
            String h0 = h instanceof Var ? ((Var) h).toStringFlattened() : h.toString();
            String t0 = t instanceof Var ? ((Var) t).toStringFlattened() : t.toString();
            return (h0 + '|' + t0);
        }
    }

    private String toString0_bracket() {
        if (subs() == 0) {
            return "";
        } else if (subs() == 1 && !((subs[0] instanceof Struct) && ((Struct) subs[0]).name().equals(","))) {
            return subs[0].term().toString();
        } else {

            Term head = ((Struct) subs[0]).subResolve(0);
            Term tail = ((Struct) subs[0]).subResolve(1);
            StringBuilder buf = new StringBuilder(head.toString());
            while (tail instanceof Struct && ((Struct) tail).name().equals(",")) {
                head = ((Struct) tail).subResolve(0);
                buf.append(',').append(head);
                tail = ((Struct) tail).subResolve(1);
            }
            buf.append(',').append(tail);
            return buf.toString();

        }
    }

    private String toStringAsList(PrologOperators op) {
        Term h = subs[0];
        Term t = subs[1].term();
        if (t.isList()) {
            Struct tl = (Struct) t;
            if (tl.isEmptyList()) {
                return h.toStringAsArgY(op, 0);
            }
            return (h.toStringAsArgY(op, 0) + ',' + tl.toStringAsList(op));
        } else {
            return (h.toStringAsArgY(op, 0) + '|' + t.toStringAsArgY(op, 0));
        }
    }

    @Override
    String toStringAsArg(PrologOperators op, int prio, boolean x) {

        if (name.equals(".") && subs() == 2) {
            return subs[0].isEmptyList() ? "[]" : '[' + toStringAsList(op) + ']';
        } else if (name.equals("{}")) {
            return ('{' + toString0_bracket() + '}');
        }

        int p = 0;
        if (subs() == 2) {
            if ((p = op.opPrio(name, "xfx")) >= PrologOperators.OP_LOW) {
                return (
                        ((x ? p >= prio : p > prio) ? "(" : "") +
                                subs[0].toStringAsArgX(op, p) +
                                ' ' + name + ' ' +
                                subs[1].toStringAsArgX(op, p) +
                                ((x ? p >= prio : p > prio) ? ")" : ""));
            }
            if ((p = op.opPrio(name, "yfx")) >= PrologOperators.OP_LOW) {
                return (
                        ((x ? p >= prio : p > prio) ? "(" : "") +
                                subs[0].toStringAsArgY(op, p) +
                                ' ' + name + ' ' +
                                subs[1].toStringAsArgX(op, p) +
                                ((x ? p >= prio : p > prio) ? ")" : ""));
            }
            if ((p = op.opPrio(name, "xfy")) >= PrologOperators.OP_LOW) {
                return !name.equals(",") ? ((x ? p >= prio : p > prio) ? "(" : "") +
                        subs[0].toStringAsArgX(op, p) +
                        ' ' + name + ' ' +
                        subs[1].toStringAsArgY(op, p) +
                        ((x ? p >= prio : p > prio) ? ")" : "") : ((x ? p >= prio : p > prio) ? "(" : "") +
                        subs[0].toStringAsArgX(op, p) +

                        ',' +
                        subs[1].toStringAsArgY(op, p) +
                        ((x ? p >= prio : p > prio) ? ")" : "");
            }
        } else if (subs() == 1) {
            if ((p = op.opPrio(name, "fx")) >= PrologOperators.OP_LOW) {
                return (
                        ((x ? p >= prio : p > prio) ? "(" : "") +
                                name + ' ' +
                                subs[0].toStringAsArgX(op, p) +
                                ((x ? p >= prio : p > prio) ? ")" : ""));
            }
            if ((p = op.opPrio(name, "fy")) >= PrologOperators.OP_LOW) {
                return (
                        ((x ? p >= prio : p > prio) ? "(" : "") +
                                name + ' ' +
                                subs[0].toStringAsArgY(op, p) +
                                ((x ? p >= prio : p > prio) ? ")" : ""));
            }
            if ((p = op.opPrio(name, "xf")) >= PrologOperators.OP_LOW) {
                return (
                        ((x ? p >= prio : p > prio) ? "(" : "") +
                                subs[0].toStringAsArgX(op, p) +
                                ' ' + name + ' ' +
                                ((x ? p >= prio : p > prio) ? ")" : ""));
            }
            if ((p = op.opPrio(name, "yf")) >= PrologOperators.OP_LOW) {
                return (
                        ((x ? p >= prio : p > prio) ? "(" : "") +
                                subs[0].toStringAsArgY(op, p) +
                                ' ' + name + ' ' +
                                ((x ? p >= prio : p > prio) ? ")" : ""));
            }
        }
        String v = (Parser.isAtom(name) ? name : '\'' + name + '\'');
        if (subs() == 0) {
            return v;
        }
        v = v + '(';
        for (p = 1; p < subs(); p++) {
            v = v + subs[p - 1].toStringAsArgY(op, 0) + ',';
        }
        v = v + subs[subs() - 1].toStringAsArgY(op, 0);
        v = v + ')';
        return v;
    }

    @Override
    public Term iteratedGoalTerm() {
        return ((subs() == 2) && name.equals("^")) ?
                subResolve(1).iteratedGoalTerm() : super.iteratedGoalTerm();
    }

    /**/

    /**
     * This class represents an iterator through the arguments of a Struct list.
     *
     * @see Struct
     */
    static class StructIterator implements Iterator<Term>, java.io.Serializable {
        Struct list;

        StructIterator(Struct t) {
            this.list = t;
        }

        @Override
        public boolean hasNext() {
            return !list.isEmptyList();
        }

        @Override
        public Term next() {
            if (list.isEmptyList())
                throw new NoSuchElementException();


            Term head = list.subResolve(0);
            list = (Struct) list.subResolve(1);
            return head;
        }

        @Override
        public void remove() {
            throw new UnsupportedOperationException();
        }

    }
}