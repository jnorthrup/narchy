/*
 * Copyright (C) 2015 Machine Learning Lab - University of Trieste, 
 * Italy (http:
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http:
 */
package jcog.grammar.evolve.tree.operator;

import jcog.grammar.evolve.tree.DescriptionContext;
import jcog.grammar.evolve.tree.Node;

/**
 *
 * @author MaleLabTs
 */
public class MatchZeroOrOne extends Quantifier {

    @Override
    public void describe(StringBuilder builder, DescriptionContext context, RegexFlavour flavour) {
        var tmp = new StringBuilder();
        var child = get(0);

        var index = context.incGroups();
        child.describe(tmp, context, flavour);
        var l = child.isEscaped() ? tmp.length() - 1 : tmp.length();
        var group = l > 1 && !child.isCharacterClass() && !(child instanceof Group) && !(child instanceof NonCapturingGroup);
        switch (flavour) {
            case JAVA:
                if (group) {
                    builder.append("(?:");
                    builder.append(tmp);
                    builder.append(')');
                } else {
                    builder.append(tmp);
                }
                builder.append("?+");
                break;
            default:
                builder.append("(?=(");
                if (group) {
                    builder.append("(?:");
                    builder.append(tmp);
                    builder.append(')');
                } else {
                    builder.append(tmp);
                }
                builder.append("?))\\").append(index);
                context.incExpansionGroups();
             
        }
    }

    @Override
    protected UnaryOperator buildCopy() {
        return new MatchZeroOrOne();
    }

}
