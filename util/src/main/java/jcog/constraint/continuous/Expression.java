package jcog.constraint.continuous;

import jcog.data.list.FasterList;

import java.util.List;
import java.util.function.DoubleSupplier;

/**
 * Created by alex on 30/01/15.
 */
public class Expression {

    public final List<DoubleTerm> terms;

    private final double constant;

    public Expression() {
        this((double) 0);
    }

    public Expression(double constant) {
        this.constant = constant;
        this.terms = new FasterList<>();
    }

    public Expression(DoubleSupplier term, double constant) {
        this.terms = new FasterList<>(1);
        terms.add(term instanceof DoubleTerm ? ((DoubleTerm)term) : new DoubleTerm(term));
        this.constant = constant;
    }

    public Expression(DoubleSupplier term) {
        this (term, 0.0);
    }

    public Expression(List<DoubleTerm> terms, double constant) {
        this.terms = terms;
        this.constant = constant;
    }

    public Expression(List<DoubleTerm> terms) {
        this(terms, (double) 0);
    }

    public double getConstant() {
        return constant;
    }


    public double getValue() {
        double result = this.constant;

        for (int i = 0, termsSize = terms.size(); i < termsSize; i++) {
            result += terms.get(i).getAsDouble();
        }
        return result;
    }

    public final boolean isConstant() {
        return terms.isEmpty();
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("isConstant: ").append(isConstant()).append(" constant: ").append(constant);
        if (!isConstant()) {
            sb.append(" terms: [");
            for (DoubleSupplier term: terms) {
                sb.append('(').append(term).append(')');
            }
            sb.append("] ");
        }
        return sb.toString();
    }

}

