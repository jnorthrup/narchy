package nars.term.util.transform;

import nars.Op;
import nars.term.Term;
import nars.term.Variable;
import org.eclipse.collections.impl.map.mutable.UnifiedMap;

import java.util.HashMap;
import java.util.Map;

/**
 * Variable normalization
 * <p>
 * Destructive mode modifies the input Compound instance, which is
 * fine if the concept has been created and unreferenced.
 * <p>
 * The target 'destructive' is used because it effectively destroys some
 * information - the particular labels the input has attached.
 */
public class VariableNormalization extends VariableTransform {

    /**
     * indexing offset of assigned variable id's
     */
    private final int offset;

    public int count;


    public final Map<Variable /* Input Variable */, Variable /*Variable*/> map;

    @Override
    public Term applyVariable(Variable atomic) {
		return atomic.equals(Op.VarAuto) ? newVariableIncreasingCount(atomic) : map.computeIfAbsent(atomic, this::newVariableIncreasingCount);
    }




    private Variable newVariableIncreasingCount( Variable x) {
        ++count;
        return newVariable(x);
    }


    protected Variable newVariable( Variable x) {


        int vid = this.count + offset;

        return x.normalizedVariable((byte)vid);

    }


    protected VariableNormalization() {
        this(new HashMap<>(4, 0.9f), 0);
    }

    public VariableNormalization(int size /* estimate */, int offset) {
        this(new UnifiedMap<>(size), offset);
    }

//    public VariableNormalization( Map<Variable, Variable> r) {
//        this(r, 0);
//    }

    private VariableNormalization( Map<Variable, Variable> r, int offset) {
        this.offset = offset;
        this.map = r;

        
        
        
    }





































}
