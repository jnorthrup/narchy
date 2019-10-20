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
import org.eclipse.collections.impl.map.mutable.UnifiedMap;
import org.eclipse.collections.impl.map.mutable.primitive.ByteByteHashMap;

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
        x = x.unneg();

        /* A statement sentence is not allowed to have a independent variable as subj or pred"); */
        switch (x.varIndep()) {
            case 0:
                return true;
            case 1:
                return Task.fail(x, "singular independent variable", safe);
            default:
                var xx = x.subterms();
                if (x.op().statement && xx.AND(Termlike::hasVarIndep)) {
                    return validIndepBalance(x, safe); //indep appearing in both, test for balance
                } else {
					return !xx.hasAny(Op.StatementBits) ? Task.fail(xx, "InDep variables must be subterms of statements", safe) : xx.AND(s -> validIndep(s, safe));
                }

        }

    }

    private static boolean validIndepBalance(Term t, boolean safe) {


        var statements = new FasterList<ByteList>();
        //ByteObjectHashMap<List<ByteList>> indepVarPaths = new ByteObjectHashMap<>(4);
        UnifiedMap<Term,List<ByteList>> indepVarPaths = new UnifiedMap(0, 0.5f);

        t.pathsTo(
                (Term x) -> {
                    var xo = x.op();
                    return (xo == VAR_INDEP) || (xo.statement && x.varIndep() > 0);
                },
                x -> x.hasAny(Op.StatementBits | Op.VAR_INDEP.bit),
                (path, indepVarOrStatement) -> {
                    if (!path.isEmpty()) {
                        var p = path.toImmutable();
                        var s = (indepVarOrStatement.op() == VAR_INDEP) ?
                            indepVarPaths.getIfAbsentPut(
                                    //((VarIndep) indepVarOrStatement).id(),
                                    indepVarOrStatement,
                                    () -> new FasterList<>(2))
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

        var rootIsStatement = t.op().statement;


        var nStatements = (byte) statements.size();
        var count = new ByteByteHashMap(nStatements);
        nextPath: for (var varPaths : indepVarPaths) {

            if (!count.isEmpty())
                count.clear();

            for (var p : varPaths) {
                if (rootIsStatement) {
                    var branch = p.get(0);
                    if (Util.branchOr((byte) -1, count, branch) == 0b11)
                        continue nextPath;
                }

                var pSize = p.size();

                nextStatement:
                for (byte k = 0; k < nStatements; k++) {
                    var statement = statements.get(k);
                    var statementPathLength = statement.size();
                    if (statementPathLength > pSize)
                        break;

                    for (var i = 0; i < statementPathLength; i++) {
                        if (p.get(i) != statement.get(i))
                            break nextStatement;
                    }

                    var lastBranch = p.get(statementPathLength);
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
