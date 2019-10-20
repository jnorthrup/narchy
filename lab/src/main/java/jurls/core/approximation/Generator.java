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
 * @author thorsten
 */
public class Generator {

    public static DiffableFunctionGenerator generateWavelets(
            int numFeatures
    ) {
        return numInputs -> {
            GeneratorContext gc = new GeneratorContext(numInputs);
            List<DiffableFunctionSource> xs = new ArrayList<>();

            for (int i = 0; i < numFeatures; ++i) {

                List<Product> list = new ArrayList<>();
                for (Scalar input : gc.getInputScalars()) {
                    Product f = new Product(
                            gc.newParameter(1.0),
                            new Sum(
                                    gc.newParameter(-1.0, (double) 0),
                                    input
                            )
                    );
                    Product product = new Product(f, f);
                    list.add(product);
                }
                DiffableFunctionSource s = new Sum(list.toArray(new DiffableFunctionSource[0]));

                DiffableFunctionSource g = new Cosine(new Product(gc.newParameter(50.0), s));

                Scalar p = gc.newParameter(-5.0);
                p.setUpperBound((double) 0);
                xs.add(new Product(new Exp(new Product(p, s)), g));
            }
            xs.add(gc.newParameter((double) 0));

            DiffableFunctionSource f = new Sum(
                    gc.newParameter(0.5),
                    new Product(
                            gc.newParameter(0.01),
                            new Sum(xs.toArray(new DiffableFunctionSource[xs.size()]))
                    )
            );

            gc.setDiffableFunctionSource(f);
            return gc;
        };
    }

    public static DiffableFunctionGenerator generateFFNN(
            ActivationFunctionFactory hiddenLayer,
            ActivationFunctionFactory outputLayer,
            int numFeatures
    ) {
        return numInputs -> {
            GeneratorContext gc = new GeneratorContext(numInputs);
            List<DiffableFunctionSource> xs = new ArrayList<>();

            for (int i = 0; i < numFeatures; ++i) {
                List<DiffableFunctionSource> ys = new ArrayList<>();
                for (Scalar input : gc.getInputScalars()) {
                    Product product = new Product(
                            gc.newParameter((double) 0, 10.0),
                            new Sum(
                                    gc.newParameter(-1.0, (double) 0),
                                    input
                            )
                    );
                    ys.add(product);
                }

                ys.add(gc.newParameter((double) 0));
                xs.add(
                        new Product(
                                gc.newParameter(-1.0, 1.0),
                                hiddenLayer.newInstance(gc, ys)
                        )
                );
            }
            xs.add(gc.newParameter((double) 0));

            DiffableFunctionSource f = new Sum(
                    gc.newParameter(0.5),
                    new Product(
                            gc.newParameter(0.01),
                            outputLayer.newInstance(gc, xs)
                    )
            );

            gc.setDiffableFunctionSource(f);
            return gc;
        };
    }

    public static DiffableFunctionGenerator generateFourierBasis(
            int numFeatures
    ) {
        return numInputs -> {
            GeneratorContext gc = new GeneratorContext(numInputs);
            List<DiffableFunctionSource> xs = new ArrayList<>();

            for (int i = 1; i <= numFeatures; ++i) {
                List<DiffableFunctionSource> ys = new ArrayList<>();
                for (Scalar input : gc.getInputScalars()) {
                    Product product = new Product(
                            gc.newParameter((double) 0, 1.0),
                            new Sum(
                                    gc.newParameter(-1.0, (double) 0),
                                    input
                            )
                    );
                    ys.add(product);
                }

                ys.add(gc.newParameter(-1.0, 1.0));

                xs.add(
                        new Product(
                                gc.newParameter(-1.0, 1.0),
                                new Cosine(
                                        new Product(
                                                gc.newParameter(Math.PI * (double) (i + 1)),
                                                new Sum(ys.toArray(new DiffableFunctionSource[ys.size()]))
                                        )
                                )
                        )
                );

            }

            xs.add(gc.newParameter((double) 0, (double) 0));
            DiffableFunctionSource f = new Sum(
                    gc.newParameter(0.5, 0.5),
                    new Product(
                            gc.newParameter(0.01, 0.01),
                            new Sum(xs.toArray(new DiffableFunctionSource[xs.size()]))
                    )
            );

            gc.setDiffableFunctionSource(f);
            return gc;
        };
    }

    public static ParameterizedFunctionGenerator generateGradientFourierBasis(
            ApproxParameters approxParameters,
            int numFeatures) {
        return numInputVectorElements -> new OutputNormalizer(
                new InputNormalizer(
                        new GradientFitter(
                                approxParameters,
                                new DiffableFunctionMarshaller(
                                        Generator.generateFourierBasis(numFeatures),
                                        numInputVectorElements
                                )
                        )
                )
        );
    }

    public static ParameterizedFunctionGenerator generateGradientFFNN(
            ActivationFunctionFactory hiddenLayer,
            ActivationFunctionFactory outputLayer,
            ApproxParameters approxParameters,
            int numFeatures) {
        return numInputVectorElements -> new OutputNormalizer(
                new InputNormalizer(
                        new GradientFitter(
                                approxParameters,
                                new DiffableFunctionMarshaller(
                                        Generator.generateFFNN(
                                                hiddenLayer,
                                                outputLayer,
                                                numFeatures
                                        ),
                                        numInputVectorElements
                                )
                        )
                )
        );
    }

    public static ParameterizedFunctionGenerator generateCNFFunction(
            int numInputBits,
            int numOutputBits
    ) {
        return numInputVectorElements -> new OutputNormalizer(
                new InputNormalizer(
                        new CNFBooleanFunction(
                                numInputBits,
                                numOutputBits,
                                numInputVectorElements
                        )
                )
        );
    }
}
