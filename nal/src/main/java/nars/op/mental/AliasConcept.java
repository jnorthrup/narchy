package nars.op.mental;

import jcog.pri.bag.Bag;
import nars.NAR;
import nars.Task;
import nars.concept.Concept;
import nars.concept.TaskConcept;
import nars.control.proto.Remember;
import nars.table.BeliefTable;
import nars.table.QuestionTable;
import nars.term.Term;
import nars.term.atom.Atom;
import nars.unify.Unify;

/**
 * the proxy concepts present a bidirectional facade between a referenced and an alias term (alias term can be just a serial # atom wrapped in a product).
 * <p>
 * it replaces the index entry for the referenced with itself and also adds itself so from its start it intercepts all references to itself or the aliased (abbreviated) term whether this occurrs at the top level or as a subterm in another term (or as a subterm in another abbreviation, etc..)
 * <p>
 * the index is usually a weakmap or equivalent in which abbreviations can be forgotten as well as any other concept.
 * <p>
 * seen from a superterm containing one, it appears as a simple volume=2 concept meanwhile it could be aliasing a concept much larger than it. common "phrase" concepts with a volume >> 2 are good candidates for abbreviation. but when printed, the default toString() method is proxied so it will automatically decompress on output (or other serialization).
 */
public final class AliasConcept extends TaskConcept {

    /** the abbreviated (decompressed) concept that this abbreviates */
    public final Concept abbr;


    AliasConcept(Term id, Concept abbr) {
        super(
                id,
                null, null, null, null,
                abbr.linker(), //shared
                new Bag[]{abbr.termlinks(), abbr.tasklinks()});

        this.abbr = abbr;


    }

    @Override
    public void add(Remember t, NAR n) {
        if (abbr.isDeleted()) {
            t.reject();
        } else {
            ((TaskConcept) abbr).add(Remember.the(Task.clone(t.input, abbr.term()), n), n);
        }

    }

    @Override
    public BeliefTable beliefs() {
        return abbr.beliefs();
    }

    @Override
    public BeliefTable goals() {
        return abbr.goals();
    }

    @Override
    public QuestionTable questions() {
        return abbr.questions();
    }

    @Override
    public QuestionTable quests() {
        return abbr.quests();
    }


    public static class AliasAtom extends Atom {


        public final Term target;

        protected AliasAtom(String id, Term target) {
            super(id);
            this.target = target;
        }



        @Override
        public boolean unify(Term y, Unify subst) {

            if (super.unify(y, subst))
                return true;

            Term target = this.target;
            if (y instanceof AliasAtom) {


                return target.unify(((AliasAtom) y).target, subst);
            }


            return target.unify(y, subst);
        }

    }


}
