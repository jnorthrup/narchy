package jcog.grammar.parse.examples.tokens;

import jcog.grammar.parse.tokens.ITokenizer;
import jcog.grammar.parse.tokens.Tokenizer;

import java.io.IOException;

/**
 * This class shows a Tokenizer consuming a word.
 * 
 * @author Steven J. Metsker
 *
 * @version 1.0 
 */
public class ShowWord {
	/**
	 * Just an example.
	 */
	public static void main(String[] args) throws IOException {
		ITokenizer t = new Tokenizer("A65 B66");
		System.out.println(t.nextToken());
		System.out.println(t.nextToken());
	}
}
