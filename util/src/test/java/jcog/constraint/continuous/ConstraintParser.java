package jcog.constraint.continuous;

import jcog.constraint.continuous.exceptions.NonlinearExpressionException;

import java.util.ArrayList;
import java.util.List;
import java.util.Stack;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Created by alex on 25/09/2014.
 */
public class ConstraintParser {

    private static final Pattern pattern = Pattern.compile("\\s*(.*?)\\s*(<=|==|>=|[GL]?EQ)\\s*(.*?)\\s*(!(required|strong|medium|weak))?");

    private final static String OPS = "-+/*^";

    public interface CassowaryVariableResolver {

        DoubleVar resolveVariable(String variableName);
        Expression resolveConstant(String name);
    }

    public static ContinuousConstraint parse(String constraintString, CassowaryVariableResolver variableResolver) throws NonlinearExpressionException {

        Matcher matcher = pattern.matcher(constraintString);
        matcher.find();
        if (matcher.matches()) {
            DoubleVar variable = variableResolver.resolveVariable(matcher.group(1));
            ScalarComparison operator = parseOperator(matcher.group(2));
            Expression expression = resolveExpression(matcher.group(3), variableResolver);
            double strength = parseStrength(matcher.group(4));

            return new ContinuousConstraint(C.subtract(variable, expression), operator, strength);
        } else {
            throw new RuntimeException("could not parse " +   constraintString);
        }
    }

    private static ScalarComparison parseOperator(String operatorString) {

        ScalarComparison operator = null;
        if ("EQ".equals(operatorString) || "==".equals(operatorString)) {
            operator = ScalarComparison.Equal;
        } else if ("GEQ".equals(operatorString) || ">=".equals(operatorString)) {
            operator = ScalarComparison.GreaterThanOrEqual;
        } else if ("LEQ".equals(operatorString) || "<=".equals(operatorString)) {
            operator = ScalarComparison.LessThanOrEqual;
        }
        return operator;
    }

    private static double parseStrength(String strengthString) {

        double strength =  Strength.REQUIRED;
        if ("!required".equals(strengthString)) {
            strength = Strength.REQUIRED;
        } else if ("!strong".equals(strengthString)) {
            strength = Strength.STRONG;
        } else if ("!medium".equals(strengthString)) {
            strength = Strength.MEDIUM;
        } else if ("!weak".equals(strengthString)) {
            strength = Strength.WEAK;
        }
        return strength;
    }

    private static Expression resolveExpression(String expressionString, CassowaryVariableResolver variableResolver) throws NonlinearExpressionException {

        List<String> postFixExpression = infixToPostfix(tokenizeExpression(expressionString));

        Stack<Expression> expressionStack = new Stack<>();

        for (String expression : postFixExpression) {
            if ("+".equals(expression)) {
                expressionStack.push(C.add(expressionStack.pop(), (expressionStack.pop())));
            } else if ("-".equals(expression)) {
                Expression a = expressionStack.pop();
                Expression b = expressionStack.pop();
                
                expressionStack.push(C.subtract(b, a));
            } else if ("/".equals(expression)) {
                Expression denominator = expressionStack.pop();
                Expression numerator = expressionStack.pop();
                expressionStack.push(C.divide(numerator, denominator));
            } else if ("*".equals(expression)) {
                expressionStack.push(C.multiply(expressionStack.pop(), (expressionStack.pop())));
            } else {
                Expression linearExpression =  variableResolver.resolveConstant(expression);
                if (linearExpression == null) {
                    linearExpression = new Expression(new DoubleTerm(variableResolver.resolveVariable(expression)));
                }
                expressionStack.push(linearExpression);
            }
        }

        return expressionStack.pop();
    }

    private static List<String> infixToPostfix(List<String> tokenList) {

        Stack<Integer> s = new Stack<>();

        List<String> postFix = new ArrayList<>();
        for (String token : tokenList) {
            char c = token.charAt(0);
            int idx = OPS.indexOf(c);
            if (idx != -1 && token.length() == 1) {
                if (s.isEmpty())
                    s.push(idx);
                else {
                    while (!s.isEmpty()) {
                        int prec2 = s.peek() / 2;
                        int prec1 = idx / 2;
                        if (prec2 > prec1 || (prec2 == prec1 && c != '^'))
                            postFix.add(Character.toString(OPS.charAt(s.pop())));
                        else break;
                    }
                    s.push(idx);
                }
            } else if (c == '(') {
                s.push(-2);
            } else if (c == ')') {
                while (s.peek() != -2)
                    postFix.add(Character.toString(OPS.charAt(s.pop())));
                s.pop();
            } else {
                postFix.add(token);
            }
        }
        while (!s.isEmpty())
            postFix.add(Character.toString(OPS.charAt(s.pop())));
        return postFix;
    }

    private static List<String> tokenizeExpression(String expressionString) {
        ArrayList<String> tokenList = new ArrayList<>();

        StringBuilder stringBuilder = new StringBuilder();
        int i;
        for (i = 0; i < expressionString.length(); i++) {
            char c = expressionString.charAt(i);
            switch (c) {
                case '+':
                case '-':
                case '*':
                case '/':
                case '(':
                case ')':
                    if (stringBuilder.length() > 0) {
                        tokenList.add(stringBuilder.toString());
                        stringBuilder.setLength(0);
                    }
                    tokenList.add(Character.toString(c));
                    break;
                case ' ':
                    
                    break;
                default:
                    stringBuilder.append(c);
            }

        }
        if (stringBuilder.length() > 0) {
            tokenList.add(stringBuilder.toString());
        }

        return tokenList;
    }

}
