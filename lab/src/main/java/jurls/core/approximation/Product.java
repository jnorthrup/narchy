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
public class Product implements DiffableFunctionSource {

    private final DiffableFunctionSource a;
    private final DiffableFunctionSource b;

    public Product(DiffableFunctionSource a, DiffableFunctionSource b) {
        this.a = a;
        this.b = b;
    }

    @Override
    public String valueToSource(SourceEnvironment se) {
        var av = a.valueToSource(se);
        var bv = b.valueToSource(se);
        var y = se.allocateVariable();

        se.assign(y).append(av).append(" * ").append(bv).append(";").nl();

        return y;
    }

    @Override
    public String partialDeriveToSource(SourceEnvironment se) {
        var av = a.valueToSource(se);
        var adv = a.partialDeriveToSource(se);
        var bv = b.valueToSource(se);
        var bdv = b.partialDeriveToSource(se);
        var y = se.allocateVariable();

        se.assign(y).append(av).append(" * ").append(bdv).
                append(" + ").append(adv).append(" * ").append(bv).
                append(";").nl();

        return y;
    }
}
