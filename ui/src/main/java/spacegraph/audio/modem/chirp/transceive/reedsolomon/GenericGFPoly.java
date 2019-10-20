/*
 * Copyright 2007 ZXing authors
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package spacegraph.audio.modem.chirp.transceive.reedsolomon;

import java.util.Arrays;

/**
 * <p>Represents a polynomial whose coefficients are elements of a GF.
 * Instances of this class are immutable.</p>
 *
 * <p>Much credit is due to William Rucklidge since portions of this code are an indirect
 * port of his C++ Reed-Solomon implementation.</p>
 *
 * @author Sean Owen
 */
final class GenericGFPoly {

    private final GenericGF field;
    private final int[] coefficients;

    /**
     * @param field        the {@link GenericGF} instance representing the field to use
     *                     to perform computations
     * @param coefficients coefficients as ints representing elements of GF(size), arranged
     *                     from most significant (highest-power term) coefficient to least significant
     * @throws IllegalArgumentException if argument is null or empty,
     *                                  or if leading coefficient is 0 and this is not a
     *                                  constant polynomial (that is, it is not the monomial "0")
     */
    GenericGFPoly(GenericGF field, int[] coefficients) {
        if (coefficients.length == 0) {
            throw new IllegalArgumentException();
        }
        this.field = field;
        var coefficientsLength = coefficients.length;
        if (coefficientsLength > 1 && coefficients[0] == 0) {
            // Leading term must be non-zero for anything except the constant polynomial "0"
            var firstNonZero = 1;
            while (firstNonZero < coefficientsLength && coefficients[firstNonZero] == 0) {
                firstNonZero++;
            }
            if (firstNonZero == coefficientsLength) {
                this.coefficients = new int[]{0};
            } else {
                this.coefficients = new int[coefficientsLength - firstNonZero];
                System.arraycopy(coefficients,
                        firstNonZero,
                        this.coefficients,
                        0,
                        this.coefficients.length);
            }
        } else {
            this.coefficients = coefficients;
        }
    }

    int[] getCoefficients() {
        return coefficients;
    }

    /**
     * @return degree of this polynomial
     */
    int getDegree() {
        return coefficients.length - 1;
    }

    /**
     * @return true iff this polynomial is the monomial "0"
     */
    boolean isZero() {
        return coefficients[0] == 0;
    }

    /**
     * @return coefficient of x^degree term in this polynomial
     */
    int getCoefficient(int degree) {
        return coefficients[coefficients.length - 1 - degree];
    }

    /**
     * @return evaluation of this polynomial at a given point
     */
    int evaluateAt(int a) {
        if (a == 0) {
            // Just return the x^0 coefficient
            return getCoefficient(0);
        }
        if (a == 1) {
            // Just the sum of the coefficients
            var result = 0;
            for (var coefficient : coefficients) {
                result = GenericGF.addOrSubtract(result, coefficient);
            }
            return result;
        }
        var result = coefficients[0];
        var size = coefficients.length;
        for (var i = 1; i < size; i++) {
            result = GenericGF.addOrSubtract(field.multiply(a, result), coefficients[i]);
        }
        return result;
    }

    GenericGFPoly addOrSubtract(GenericGFPoly other) {
        if (field != other.field) {
            throw new IllegalArgumentException("GenericGFPolys do not have same GenericGF field");
        }
        if (isZero()) {
            return other;
        }
        if (other.isZero()) {
            return this;
        }

        var smallerCoefficients = this.coefficients;
        var largerCoefficients = other.coefficients;
        if (smallerCoefficients.length > largerCoefficients.length) {
            var temp = smallerCoefficients;
            smallerCoefficients = largerCoefficients;
            largerCoefficients = temp;
        }
        var sumDiff = new int[largerCoefficients.length];
        var lengthDiff = largerCoefficients.length - smallerCoefficients.length;
        // Copy high-order terms only found in higher-degree polynomial's coefficients
        System.arraycopy(largerCoefficients, 0, sumDiff, 0, lengthDiff);

        for (var i = lengthDiff; i < largerCoefficients.length; i++) {
            sumDiff[i] = GenericGF.addOrSubtract(smallerCoefficients[i - lengthDiff], largerCoefficients[i]);
        }

        return new GenericGFPoly(field, sumDiff);
    }

    GenericGFPoly multiply(GenericGFPoly other) {
        if (field != other.field) {
            throw new IllegalArgumentException("GenericGFPolys do not have same GenericGF field");
        }
        if (isZero() || other.isZero()) {
            return field.getZero();
        }
        var aCoefficients = this.coefficients;
        var aLength = aCoefficients.length;
        var bCoefficients = other.coefficients;
        var bLength = bCoefficients.length;
        var product = new int[aLength + bLength - 1];
        for (var i = 0; i < aLength; i++) {
            var aCoeff = aCoefficients[i];
            for (var j = 0; j < bLength; j++) {
                product[i + j] = GenericGF.addOrSubtract(product[i + j],
                        field.multiply(aCoeff, bCoefficients[j]));
            }
        }
        return new GenericGFPoly(field, product);
    }

    GenericGFPoly multiply(int scalar) {
        if (scalar == 0) {
            return field.getZero();
        }
        if (scalar == 1) {
            return this;
        }
        var size = coefficients.length;
        var product = new int[10];
        var count = 0;
        for (var coefficient : coefficients) {
            var multiply = field.multiply(coefficient, scalar);
            if (product.length == count) product = Arrays.copyOf(product, count * 2);
            product[count++] = multiply;
        }
        product = Arrays.copyOfRange(product, 0, count);
        return new GenericGFPoly(field, product);
    }

    GenericGFPoly multiplyByMonomial(int degree, int coefficient) {
        if (degree < 0) {
            throw new IllegalArgumentException();
        }
        if (coefficient == 0) {
            return field.getZero();
        }
        var size = coefficients.length;
        var product = new int[size + degree];
        for (var i = 0; i < size; i++) {
            product[i] = field.multiply(coefficients[i], coefficient);
        }
        return new GenericGFPoly(field, product);
    }

    GenericGFPoly[] divide(GenericGFPoly other) {
        if (field != other.field) {
            throw new IllegalArgumentException("GenericGFPolys do not have same GenericGF field");
        }
        if (other.isZero()) {
            throw new IllegalArgumentException("Divide by 0");
        }

        var quotient = field.getZero();
        var remainder = this;

        var denominatorLeadingTerm = other.getCoefficient(other.getDegree());
        var inverseDenominatorLeadingTerm = field.inverse(denominatorLeadingTerm);

        while (remainder.getDegree() >= other.getDegree() && !remainder.isZero()) {
            var degreeDifference = remainder.getDegree() - other.getDegree();
            var scale = field.multiply(remainder.getCoefficient(remainder.getDegree()), inverseDenominatorLeadingTerm);
            var term = other.multiplyByMonomial(degreeDifference, scale);
            var iterationQuotient = field.buildMonomial(degreeDifference, scale);
            quotient = quotient.addOrSubtract(iterationQuotient);
            remainder = remainder.addOrSubtract(term);
        }

        return new GenericGFPoly[]{quotient, remainder};
    }

    @Override
    public String toString() {
        if (isZero()) {
            return "0";
        }
        var result = new StringBuilder(8 * getDegree());
        for (var degree = getDegree(); degree >= 0; degree--) {
            var coefficient = getCoefficient(degree);
            if (coefficient != 0) {
                if (coefficient < 0) {
                    if (degree == getDegree()) {
                        result.append('-');
                    } else {
                        result.append(" - ");
                    }
                    coefficient = -coefficient;
                } else {
                    if (result.length() > 0) {
                        result.append(" + ");
                    }
                }
                if (degree == 0 || coefficient != 1) {
                    var alphaPower = field.log(coefficient);
                    switch (alphaPower) {
                        case 0:
                            result.append('1');
                            break;
                        case 1:
                            result.append('a');
                            break;
                        default:
                            result.append("a^");
                            result.append(alphaPower);
                            break;
                    }
                }
                if (degree != 0) {
                    if (degree == 1) {
                        result.append('x');
                    } else {
                        result.append("x^");
                        result.append(degree);
                    }
                }
            }
        }
        return result.toString();
    }

}
