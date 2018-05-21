// Copyright 2015-2016 Stanford University
//
// Licensed under the Apache License, Version 2.0 (the "License");
// you may not use this file except in compliance with the License.
// You may obtain a copy of the License at
//
//     http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing, software
// distributed under the License is distributed on an "AS IS" BASIS,
// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// See the License for the specific language governing permissions and
// limitations under the License.

package jcog.grammar;


import jcog.grammar.synthesize.GrammarFuzzer;
import jcog.grammar.synthesize.GrammarSynthesis;
import jcog.grammar.synthesize.util.GrammarUtils;
import jcog.grammar.synthesize.util.Log;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.List;
import java.util.Random;
import java.util.Stack;
import java.util.function.Predicate;

import static org.junit.jupiter.api.Assertions.assertEquals;


public class ParensTest {

	@Test
	public void test1() {

		//Log.init(true);

		// input: query oracle
		Predicate<String> oracle = query -> {
			Stack<Character> stack = new Stack<>();
			for (int i = 0; i < query.length(); i++) {
				char c = query.charAt(i);
				if (c == '(' || c == '[' || c == '{') {
					// handle open parentheses
					stack.push(c);
				} else if (c == ')' || c == ']' || c == '}') {
					// handle closed parentheses
					if (stack.isEmpty()) {
						return false;
					}
					char d = stack.pop();
					if ((d == '(' && c != ')') || (d == '[' && c != ']') || (d == '{' && c != '}')) {
						return false;
					}
				} else {
					return false;
				}
			}
			return stack.isEmpty();
		};

		// input: (positive) training examples
		List<String> examples = Arrays.asList("{([][])([][])}{[()()][()()]}");

		// learn grammar
		GrammarUtils.Grammar grammar = GrammarSynthesis.learn(examples, oracle);

		// fuzz using grammar
		Iterable<String> samples = new GrammarFuzzer.GrammarMutationSampler(grammar, new GrammarFuzzer.SampleParameters(new double[]{
				0.2, 0.2, 0.2, 0.4}, // (multinomial) distribution of repetitions
				0.8,                                          // probability of using recursive production
				0.1,                                          // probability of a uniformly random character (vs. a special character)
				100),
                1000, 20, new Random(0));

		int pass = 0;
		int count = 0;
        int numSamples = 10;
		for(String sample : samples) {
			Log.info("SAMPLE: " + sample);
			if(oracle.test(sample)) {
				Log.info("PASS");
				pass++;
			} else {
				Log.info("FAIL");
			}
			Log.info("");
			count++;
			if(count >= numSamples) {
				break;
			}
		}
		Log.info("PASS RATE: " + (float)pass/numSamples);

		assertEquals(pass, numSamples);
	}
}