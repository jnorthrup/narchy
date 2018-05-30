package nars.unify.op;

import nars.$;
import nars.Op;
import nars.derive.premise.PreDerivation;
import nars.term.control.AbstractPred;
import nars.term.control.PrediTerm;

import java.util.Collections;
import java.util.List;

import static nars.Op.VAR_PATTERN;

/**
 * requires a specific subterm to have minimum bit structure
 */
public final class SubTermStructure extends AbstractPred<PreDerivation> {

    /** higher number means a stucture with more enabled bits will be decomposed to its components */
    public static final int SPLIT_THRESHOLD = 4;

    public final int subterm;
    public final int bits;

    public static List<SubTermStructure> get(int subterm, int bits) {

        
        bits &= ~VAR_PATTERN.bit;

        int numBits = Integer.bitCount(bits);
        assert (numBits > 0);
        if ((numBits == 1) || (numBits > SPLIT_THRESHOLD)) {
            return Collections.singletonList(new SubTermStructure(subterm, bits));
        } else {
            List<SubTermStructure> components = $.newArrayList(numBits);
            for (Op o : Op.values()) {


                int b = o.bit;
                if ((bits & b) != 0) { 
                    components.add(new SubTermStructure(subterm, b));
                }
            }
            return components;
        }
    }

    private SubTermStructure(int subterm, int bits) {
        this(VAR_PATTERN, subterm, bits);
    }

    private SubTermStructure(/*@NotNull*/ Op matchingType, int subterm, int bits) {
        super($.func("subTermStruct", $.the(subterm), $.the(bits)));





        this.subterm = subterm;


        this.bits = filter(matchingType, bits);
        assert(this.bits > 0): "no filter effected";

    }

    @Override
    public boolean remainInAND(PrediTerm[] p) {
        for (PrediTerm x : p) {
            if (x instanceof TaskBeliefOp) {
                TaskBeliefOp t = (TaskBeliefOp)x;
                if (t.isOrIsNot && ((subterm == 0 && t.task) || (subterm == 1 && t.belief)) &&
                    (t.structure == bits)) {
                    return false; 
                }
            }
        }
        return true;
    }

    @Override
    public boolean test(PreDerivation ff) {
        
        
        
        return Op.hasAll((subterm == 0 ? ff._taskStruct : ff._beliefStruct), bits);
    }

    static int filter(/*@NotNull*/ Op matchingType, int bits) {
        bits &= (~matchingType.bit);

        

        return bits;
    }

    @Override
    public float cost() {
        return 0.2f;
    }
}
