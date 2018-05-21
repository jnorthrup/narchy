package jcog.grammar.parse.tokens;

import jcog.grammar.parse.Parser;

import java.util.Set;

public class LowerCaseWord extends Word {
	@Override
	protected boolean qualifies(Object o) {
		if (!super.qualifies(o))
			return false;
		Token t = (Token) o;
		return Character.isLowerCase(t.sval().charAt(0));
	}

	@Override
	public String unvisitedString(Set<Parser> visited) {
		return "LowerCaseWord";
	}

	@Override
	protected char workOnCharForRandomExpansion(char c, int pos) {
		if (pos == 0)
			return Character.toLowerCase(c);
		return c;
	}

}
