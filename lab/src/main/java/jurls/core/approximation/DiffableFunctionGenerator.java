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
@FunctionalInterface
public interface DiffableFunctionGenerator {

    GeneratorContext generate(
            int numInputs
    );
}
