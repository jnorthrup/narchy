package nars.derive.premise;

import jcog.Util;
import nars.$;
import nars.derive.PreDerivation;
import nars.term.Term;
import nars.term.atom.Atom;
import nars.term.atom.Atomic;

import java.util.Arrays;
import java.util.function.Function;

abstract public class PremiseTermAccessor implements Function<PreDerivation, Term> {

    /** root component id: 0=task, 1=belief ... others could be defined later */
    final int rootID;
    private final Atom term;

    PremiseTermAccessor(int id, String term) {
        this.rootID = id;
        this.term = Atomic.atom(term);
    }

    @Override
    public final String toString() { return term.toString(); }

    public Function<PreDerivation, Term> path(byte... path) {
        return (path.length == 0) ? this : new SubRootTermAccessor(path);
    }

    private class SubRootTermAccessor implements Function<PreDerivation, Term> {

        private final byte[] path;
        private final int hash;
        private final Term pathID;

        SubRootTermAccessor(byte... path) {
            assert(path.length>0);
            this.pathID = $.func(term, $.p(path));
            this.path = path;
            this.hash = Util.hashCombine(rootID, Util.hash(path));
        }

        @Override
        public String toString() {
            return pathID.toString();
        }

        @Override
        public int hashCode() {
            return hash;
        }

        @Override
        public boolean equals(Object obj) {
            if (this == obj) return true;
            if (!(obj instanceof SubRootTermAccessor)) return false;
            SubRootTermAccessor s = (SubRootTermAccessor)obj;
            return hash == s.hash && rootID == s.rootID() && Arrays.equals(path, s.path);
        }

        private int rootID() {
            return rootID;
        }

        @Override
        public Term apply(PreDerivation d) {
            return PremiseTermAccessor.this.apply(d).subPath(path);
        }
    }
}