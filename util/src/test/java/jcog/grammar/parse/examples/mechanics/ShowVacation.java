package jcog.grammar.parse.examples.mechanics;

import jcog.grammar.parse.Alternation;
import jcog.grammar.parse.Assembly;
import jcog.grammar.parse.Parser;
import jcog.grammar.parse.Sequence;
import jcog.grammar.parse.tokens.Literal;
import jcog.grammar.parse.tokens.TokenAssembly;

import java.util.HashSet;
import java.util.Set;

/*
 * Copyright (c) 1999 Steven J. Metsker. All Rights Reserved.
 * 
 * Steve Metsker makes no representations or warranties about
 * the fitness of this software for any particular purpose, 
 * including the implied warranty of merchantability.
 */

/**
 * This class uses a <code>VerboseRepetition</code> to show the progress a
 * repetition makes during matching.
 * 
 * @author Steven J. Metsker
 * 
 * @version 1.0
 */
public class ShowVacation {
	/**
	 * Using a <code>VerboseRepetition</code>, show the progress a repetition
	 * makes during matching.
	 */
	public static void main(String args[]) {

		Parser prepare = new Alternation().add(new Literal("plan").discard()).add(new Literal("shop").discard()).add(new Literal("pack").discard());

		Parser enjoy = new Alternation().add(new Literal("swim").discard()).add(new Literal("hike").discard()).add(new Literal("relax").discard());

		Parser vacation = new Sequence().add(new VerboseRepetition(prepare)).add(new VerboseRepetition(enjoy));

		Set<Assembly> v = new HashSet<Assembly>();
		v.add(new TokenAssembly("plan pack hike relax"));

		vacation.match(v);
	}
}
