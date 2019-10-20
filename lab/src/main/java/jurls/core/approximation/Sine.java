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
public class Sine implements DiffableFunctionSource {

    private final DiffableFunctionSource x;

    public Sine(DiffableFunctionSource x) {
        this.x = x;
    }

    @Override
    public String valueToSource(SourceEnvironment se) {
        var xv = x.valueToSource(se);
        var y = se.allocateVariable();

        se.append(y).append("Math.sin(").append(xv).append(");").nl();

        return y;
    }

    @Override
    public String partialDeriveToSource(SourceEnvironment se) {
        var xv = x.valueToSource(se);
        var xdv = x.partialDeriveToSource(se);
        var y = se.allocateVariable();

        se.assign(y).append("-").append(xdv).append(" * ").append("Math.cos(").
                append(xv).append(");").nl();

        return y;
    }
}
