package jcog.grammar.parse.tokens;

import java.io.IOException;
import java.io.PushbackReader;
import java.math.BigDecimal;

/**
 * This state will either delegate to a comment-handling state, or return a
 * token with just a slash in it.
 * 
 * @author Steven J. Metsker
 * 
 * @version 1.0
 */
public class SlashState extends TokenizerState {

	private final SlashStarState slashStarState = new SlashStarState();

	private final SlashSlashState slashSlashState = new SlashSlashState();

	/**
	 * Either delegate to a comment-handling state, or return a token with just
	 * a slash in it.
	 * 
	 * @return either just a slash token, or the results of delegating to a
	 *         comment-handling state
	 */
	@Override
	public Token nextToken(PushbackReader r, int theSlash, Tokenizer t) throws IOException {

        int c = r.read();
		if (c == (int) '*') {
			return slashStarState.nextToken(r, (int) '*', t);
		}
		if (c == (int) '/') {
			return slashSlashState.nextToken(r, (int) '/', t);
		}
		if (c >= 0) {
			r.unread(c);
		}
		return new Token(Token.TT_SYMBOL, "/", BigDecimal.ZERO);
	}
}
