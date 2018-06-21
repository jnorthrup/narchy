package jcog.grammar.parse.examples.pretty;

import jcog.grammar.parse.Repetition;
import jcog.grammar.parse.Seq;
import jcog.grammar.parse.tokens.TokenAssembly;
import jcog.grammar.parse.tokens.Word;

import java.util.Enumeration;

/**
 * Show that the pretty printer will find all the parses that
 * result from applying the parser <code>Word* Word*</code>
 * against a string with four words.
 * 
 * @author Steven J. Metsker
 * 
 * @version 1.0 
 */
public class ShowPrettyRepetitions {
	/**
	 * Show that the pretty printer will find all the parses that
	 * result from applying the parser <code>Word* Word*</code>
	 * against a string with four words.
	 */
	public static void main(String[] args) {
		PrettyParser p = new PrettyParser(seq());
		p.setShowLabels(true);
		TokenAssembly ta = new TokenAssembly("belfast cork dublin limerick");
		Enumeration e = p.parseTrees(ta).elements();
		while (e.hasMoreElements()) {
			System.out.println("The input parses as:");
			System.out.println("---------------------------");
			System.out.println(e.nextElement());
		}
	}

	/**
	 * The parser to try:
	 *
	 * <blockquote><pre> 
	 *     seq  = rep1 rep2;
	 *     rep1 = Word*;
	 *     rep2 = Word*;
	 * </pre></blockquote>
	 * 
	 */
	private static Seq seq() {
		Seq seq = new Seq("<seq>");
		seq.get(new Repetition(new Word(), "<rep1>"));
		seq.get(new Repetition(new Word(), "<rep2>"));
		return seq;
	}
}
