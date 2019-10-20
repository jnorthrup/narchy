    /*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package jurls.core.approximation;

    import java.util.ArrayList;
    import java.util.List;

/**
 *
 * @author thorsten2
 */
public class RadialBasisFunctionFactory implements ActivationFunctionFactory {

    @Override
    public DiffableFunctionSource newInstance(GeneratorContext gc, List<DiffableFunctionSource> xs) {
        List<Product> list = new ArrayList<>();
        for (DiffableFunctionSource x : xs) {
            Product product = new Product(x, x);
            list.add(product);
        }
        DiffableFunctionSource[] fs = list.toArray(new DiffableFunctionSource[0]);

        Scalar p = gc.newParameter(-1.0);
        p.setUpperBound((double) 0);
        return new Exp(new Product(p, new Sum(fs)));
    }

    @Override
    public double getDelta() {
        return -10.0;
    }

}
