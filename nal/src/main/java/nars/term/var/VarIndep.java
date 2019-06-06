package nars.term.var;

import jcog.Util;
import jcog.data.list.FasterList;
import nars.Op;
import nars.Task;
import nars.subterm.Subterms;
import nars.term.Term;
import nars.term.Termlike;
import org.eclipse.collections.api.PrimitiveIterable;
import org.eclipse.collections.api.list.primitive.ByteList;
import org.eclipse.collections.api.list.primitive.ImmutableByteList;
import org.eclipse.collections.impl.map.mutable.primitive.ByteByteHashMap;
import org.eclipse.collections.impl.map.mutable.primitive.ByteObjectHashMap;

import java.util.List;

import static nars.Op.VAR_INDEP;


/**
 * normalized indep var
 */
public final class VarIndep extends NormalizedVariable {

    VarIndep(byte id) {
        super(VAR_INDEP, id);
    }

    public static boolean validIndep(Term x, boolean safe) {
        /* A statement sentence is not allowed to have a independent variable as subj or pred"); */
        switch (x.varIndep()) {
            case 0:
                return true;
            case 1:
                return Task.fail(x, "singular independent variable", safe);
            default:
                if (!x.hasAny(Op.StatementBits)) {
                    return Task.fail(x, "InDep variables must be subterms of statements", safe);
                } else {
                    Subterms xx = x.subterms();
                    if (x.op().statement && xx.AND(Termlike::hasVarIndep)) {
                        return validIndepBalance(x, safe); //indep appearing in both, test for balance
                    } else {
                        return xx.AND(s -> validIndep(s, safe));
                    }
                }
        }

    }

    public static boolean validIndepBalance(Term t, boolean safe) {


        FasterList</* length, */ ByteList> statements = new FasterList<>(4);
        ByteObjectHashMap<List<ByteList>> indepVarPaths = new ByteObjectHashMap<>(4);

        t.pathsTo(
                (Term x) -> {
                    Op xo = x.op();
                    return (xo == VAR_INDEP) || (xo.statement && x.varIndep() > 0);
                },
                x -> x.hasAny(Op.StatementBits | Op.VAR_INDEP.bit),
                (ByteList path, Term indepVarOrStatement) -> {
                    if (!path.isEmpty()) {
                        ImmutableByteList p = path.toImmutable();
                        List<ByteList> s = (indepVarOrStatement.op() == VAR_INDEP) ?
                            indepVarPaths.getIfAbsentPut(((VarIndep) indepVarOrStatement).id(), () -> new FasterList<>(2))
                            :
                            statements;

                        s.add(p);
                    }
                    return true;
                });

        if (indepVarPaths.anySatisfy(p -> p.size() < 2))
            return false;

        if (statements.size() > 1)
            statements.sortThisByInt(PrimitiveIterable::size);

        boolean rootIsStatement = t.op().statement;


        byte nStatements = (byte) statements.size();
        ByteByteHashMap count = new ByteByteHashMap(nStatements);
        nextPath: for (List<ByteList> varPaths : indepVarPaths) {

            if (!count.isEmpty())
                count.clear();

            for (int j = 0, varPathsSize = varPaths.size(); j < varPathsSize; j++) {
                ByteList p = varPaths.get(j);

                if (rootIsStatement) {
                    byte branch = p.get(0);
                    if (Util.branchOr((byte) -1, count, branch) == 0b11)
                        continue nextPath;
                }

                int pSize = p.size();

                nextStatement: for (byte k = 0; k < nStatements; k++) {
                    ByteList statement = statements.get(k);
                    int statementPathLength = statement.size();
                    if (statementPathLength > pSize)
                        break;

                    for (int i = 0; i < statementPathLength; i++) {
                        if (p.get(i) != statement.get(i))
                            break nextStatement;
                    }

                    byte lastBranch = p.get(statementPathLength);
                    //assert (lastBranch == 0 || lastBranch == 1) : lastBranch + " for path " + p + " while validating target: " + t;


                    if (Util.branchOr(k, count, lastBranch) == 0b11)
                        continue nextPath;
                }
            }
            return Task.fail(t, "InDep variables must be balanced across a statement", safe);
        }
        return true;
    }

    @Override
    public final Op op() {
        return VAR_INDEP;
    }

    @Override
    public final int vars() {
        return 1;
    }

    @Override
    public final int varIndep() {
        return 1;
    }


}
