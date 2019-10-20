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
public class Cosine implements DiffableFunctionSource {

    private final DiffableFunctionSource x;

    public Cosine(DiffableFunctionSource x) {
        this.x = x;
    }

    @Override
    public String partialDeriveToSource(SourceEnvironment se) {
        var xdv = x.partialDeriveToSource(se);
        var xvv = x.valueToSource(se);
        var y = se.allocateVariable();
        se.append(y).append(" = ").append("-").append(xdv).
                append(" * Math.sin(").append(xvv).append(");").nl();
        return y;
    }

    @Override
    public String valueToSource(SourceEnvironment se) {
        var xv = x.valueToSource(se);
        var y = se.allocateVariable();
        se.append(y).append(" = ").append("Math.cos(").append(xv).append(");").nl();
        return y;
    }

}
