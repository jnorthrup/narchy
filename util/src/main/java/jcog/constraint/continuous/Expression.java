package jcog.constraint.continuous;

import jcog.data.list.FasterList;

import java.util.List;

/**
 * Created by alex on 30/01/15.
 */
public class Expression {

    public final List<DoubleTerm> terms;

    private final double constant;

    public Expression() {
        this(0);
    }

    public Expression(double constant) {
        this.constant = constant;
        this.terms = new FasterList<>();
    }

    public Expression(DoubleTerm term, double constant) {
        this.terms = new FasterList<>();
        terms.add(term);
        this.constant = constant;
    }

    public Expression(DoubleTerm term) {
        this (term, 0.0);
    }

    public Expression(List<DoubleTerm> terms, double constant) {
        this.terms = terms;
        this.constant = constant;
    }

    public Expression(List<DoubleTerm> terms) {
        this(terms, 0);
    }

    public double getConstant() {
        return constant;
    }


    public double getValue() {
        double result = this.constant;

        for (int i = 0, termsSize = terms.size(); i < termsSize; i++) {
            result += terms.get(i).value();
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
            for (DoubleTerm term: terms) {
                sb.append('(').append(term).append(')');
            }
            sb.append("] ");
        }
        return sb.toString();
    }

}

