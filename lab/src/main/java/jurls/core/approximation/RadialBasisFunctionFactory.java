    /*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package jurls.core.approximation;

import java.util.List;

/**
 *
 * @author thorsten2
 */
public class RadialBasisFunctionFactory implements ActivationFunctionFactory {

    @Override
    public DiffableFunctionSource newInstance(GeneratorContext gc, List<DiffableFunctionSource> xs) {
        DiffableFunctionSource[] fs = xs.stream().map(x -> new Product(x, x)).toArray(DiffableFunctionSource[]::new);

        Scalar p = gc.newParameter(-1);
        p.setUpperBound(0);
        return new Exp(new Product(p, new Sum(fs)));
    }

    @Override
    public double getDelta() {
        return -10;
    }

}
