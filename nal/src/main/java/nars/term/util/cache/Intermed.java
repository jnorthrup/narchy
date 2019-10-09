package nars.term.util.cache;

import jcog.memoize.byt.ByteKeyExternal;
import nars.Op;
import nars.io.TermIO;
import nars.subterm.Subterms;
import nars.term.Compound;
import nars.term.Term;

/** interned terms and subterms implementations */
public enum Intermed  { ;


    public abstract static class InternedCompoundByComponents extends ByteKeyExternal {
        public final byte op;
        public final int dt;

        InternedCompoundByComponents(Op o, int dt) {
            super();
            this.op = o.id; this.dt = dt;
            TermIO.the.writeCompoundPrefix(o, dt, key);
        }

        public abstract Term[] subs();
    }

    public static final class InternedCompoundByComponentsArray extends InternedCompoundByComponents {
        public final transient Term[] subs;

        public InternedCompoundByComponentsArray(Op o, int dt, Term... subs) {
            super(o, dt);
            this.subs = subs;
            TermIO.the.writeSubterms(key, subs);
            commit();
        }

        @Override
        public Term[] subs() {
            return subs;
        }
    }
    public static final class InternedCompoundByComponentPair extends InternedCompoundByComponents {
        public final transient Term x, y;

        public InternedCompoundByComponentPair(Op o, int dt, Term x, Term y) {
            super(o, dt);
            this.x = x; this.y = y;
            TermIO.the.writeSubterms(key, x, y);
            commit();
        }

        @Override
        public Term[] subs() {
            return new Term[] { x, y };
        }
    }

    public static final class InternedCompoundByComponentsSubs extends InternedCompoundByComponents {

        private final Compound x;

        public InternedCompoundByComponentsSubs(Compound x) {
            super(x.op(), x.dt());
            this.x = x;
            TermIO.the.writeSubterms(x.subtermsDirect(), key);
            commit();
        }

        @Override public Term[] subs() {
            return x.arrayShared();
        }
    }

    public static final class InternedSubterms extends ByteKeyExternal {

        public final transient Term[] subs;

        public InternedSubterms(Term[] s) {
            super();
            this.subs = s;
            TermIO.the.writeSubterms(key, subs);
            commit();
        }
    }

    public static final class InternedCompoundTransform extends ByteKeyExternal {
        public final Term term;

        public InternedCompoundTransform(Term x) {
            super();
            this.term = x;
            TermIO.the.write(x, key);
            commit();
        }


    }


    public static class SubtermsKey extends ByteKeyExternal {
        public final Subterms subs;
    
        public SubtermsKey(Subterms s) {
            super();
            TermIO.the.writeSubterms(this.subs = s, key);
            commit();
        }
    }
}
