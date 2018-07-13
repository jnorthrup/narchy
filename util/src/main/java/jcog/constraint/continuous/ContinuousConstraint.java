package jcog.constraint.continuous;

import jcog.data.list.FasterList;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Created by alex on 30/01/15.
 */
public class ContinuousConstraint {

    public final Expression expression;
    private double strength;
    public final RelationalOperator op;

    public ContinuousConstraint(Expression expr, RelationalOperator op) {
        this(expr, op, Strength.REQUIRED);
    }

    public ContinuousConstraint(Expression expr, RelationalOperator op, double strength) {
        this.expression = reduce(expr);
        this.op = op;
        this.strength = Strength.clip(strength);
    }

    public ContinuousConstraint(ContinuousConstraint other, double strength) {
        this(other.expression, other.op, strength);
    }

    private static Expression reduce(Expression expr){

        Map<DoubleVar, Double> vars = new LinkedHashMap<>(expr.terms.size());
        for(DoubleTerm term: expr.terms){
            vars.merge(term.var, term.coefficient, (vv, val)-> val + vv);
        }

        List<DoubleTerm> reducedTerms = new FasterList<>(vars.size());
        for(Map.Entry<DoubleVar, Double> variableDoubleEntry : vars.entrySet()){
            reducedTerms.add(new DoubleTerm(variableDoubleEntry.getKey(), variableDoubleEntry.getValue()));
        }

        return new Expression(reducedTerms, expr.getConstant());
    }

    public double getStrength() {
        return strength;
    }

    public ContinuousConstraint setStrength(double strength) {
        this.strength = strength;
        return this;
    }

    @Override
    public String toString() {
        return "expression: (" + expression + ") strength: " + strength + " operator: " + op;
    }

}
