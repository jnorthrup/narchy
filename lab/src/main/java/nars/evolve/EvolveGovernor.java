/*
 * Java Genetic Algorithm Library (@__identifier__@).
 * Copyright (c) @__year__@ Franz Wilhelmstötter
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 * Author:
 *    Franz Wilhelmstötter (franz.wilhelmstoetter@gmail.com)
 */
package nars.evolve;

import ch.qos.logback.classic.Level;
import io.jenetics.Genotype;
import io.jenetics.Mutator;
import io.jenetics.engine.*;
import io.jenetics.ext.SingleNodeCrossover;
import io.jenetics.ext.util.Tree;
import io.jenetics.ext.util.TreeNode;
import io.jenetics.prog.ProgramChromosome;
import io.jenetics.prog.ProgramGene;
import io.jenetics.prog.op.*;
import io.jenetics.util.ISeq;
import io.jenetics.util.RandomRegistry;
import jcog.Log;
import jcog.Util;
import nars.NAR;
import nars.NARS;
import nars.derive.Deriver;
import nars.derive.action.How;
import nars.test.impl.DeductiveMeshTest;

import java.util.function.Function;

/**
 * Symbolic regression involves finding a mathematical expression, in symbolic
 * form, that provides a good, best, or perfect fit between a given finite
 * sampling of values of the independent variables and the associated values of
 * the dependent variables. --- John R. Koza, Genetic Programming I
 *
 * @author <a href="mailto:franz.wilhelmstoetter@gmail.com">Franz Wilhelmstötter</a>
 * @version 5.0
 * @since 3.9
 */
public class EvolveGovernor {

	static {
		Log.root().setLevel(Level.WARN);
	}

	public static void main(String[] args) {
		Engine<ProgramGene<Double>, Double> engine = Engine
			.builder(new GovernorProblem())
			.maximizing()
			.alterers(
				new SingleNodeCrossover<>(0.1),
				new Mutator<>())
			.build();

		EvolutionResult<ProgramGene<Double>, Double> result = engine.stream()
			.limit(Limits.byFixedGeneration(100))
			.collect(EvolutionResult.toBestEvolutionResult());

		ProgramGene<Double> program = result.getBestPhenotype()
			.getGenotype()
			.getGene();

		TreeNode<Op<Double>> tree = program.toTreeNode();
		MathExpr.rewrite(tree);
		System.out.println("Generations: " + result.getTotalGenerations());
		System.out.println("Function:    " + new MathExpr(tree));


	}

	public static class GovernorProblem implements Problem<Tree<Op<Double>, ?>, ProgramGene<Double>, Double> {
		// Definition of the allowed operations.
		private static final ISeq<Op<Double>> ops =
			ISeq.of(MathOp.ADD, MathOp.SUB, MathOp.NEG, MathOp.MUL, MathOp.GT, MathOp.MIN, MathOp.MOD, MathOp.SIN);

		// Definition of the terminals.
		private static final ISeq<Op<Double>> terminals = ISeq.of(
			Var.of("clsHash"), Var.of("value"),
			//, Var.of("priPrev"),
			//Const.of(0.5), Const.of(1.0), Const.of(2.0), Const.of(3.0)
			EphemeralConst.of(() -> (double) RandomRegistry.getRandom().nextInt(4))
		);

		private final Codec<Tree<Op<Double>, ?>, ProgramGene<Double>> codec;

		{
			int depth = 3;
			codec = Codec.of(
				Genotype.of(ProgramChromosome.of(
					depth,
					(t) -> true,
					ops,
					terminals
				)),
				Genotype::getGene
			);
		}

		@Override
		public Function<Tree<Op<Double>, ?>, Double> fitness() {
			return (g) -> {
				NAR n = NARS.tmp(6);
				Deriver d = n.parts(Deriver.class).findFirst().get();
				DeductiveMeshTest t = new DeductiveMeshTest(n, new int[]{2, 1}, 100);

//				Op<Double> gov = g.getValue();
				TreeNode tree = ((ProgramGene) g).toTreeNode();

				Double[] govParm = new Double[2];
				n.onDur(() -> {
					for (How a : d.program.branch) {
						govParm[0] = Math.abs((double) a.getClass().hashCode()) / Integer.MAX_VALUE;
						govParm[1] = (double) a.why.value;
//						govParm[2] = (double) a.why.pri;
						Double pp = (Double) ((ProgramGene) g).eval(govParm);
						float p = pp.floatValue(); //<-
						if (!Float.isFinite(p)) p = 0;
						p = Util.clamp(p, 0.1f, 1f);
						a.why.pri(p);
					}
				});
				try {
					t.test.test();
				} catch (Throwable tt) {

				}
				float score = t.test.score;

				System.out.println(score + "\t" + tree);

				return (double) score;
			};
		}

		@Override
		public Codec<Tree<Op<Double>, ?>, ProgramGene<Double>> codec() {
			return codec;
		}
	}

}
