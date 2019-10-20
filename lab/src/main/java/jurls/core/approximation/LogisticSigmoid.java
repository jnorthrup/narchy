/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package jurls.core.approximation;

/**
 *
 * @author thorsten
 */
public class LogisticSigmoid implements DiffableFunctionSource {

    private final DiffableFunctionSource x;

    public LogisticSigmoid(DiffableFunctionSource x) {
        this.x = x;
    }

    @Override
    public String valueToSource(SourceEnvironment se) {
        var xv = x.valueToSource(se);
        var y = se.allocateVariable();

        se.assign(y).append("1 / (1 + Math.exp(-").append(xv).append("));").nl();
        return y;
    }

    @Override
    public String partialDeriveToSource(SourceEnvironment se) {
        var xv = valueToSource(se);
        var xdv = x.partialDeriveToSource(se);
        var y = se.allocateVariable();

        se.assign(y).append(xdv).append(" * ").append(xv).append(" * (1.0 - ").
                append(xv).append(");").nl();
        
        return y;
    }
}
