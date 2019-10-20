package jcog.test.predict;

import jcog.learn.lstm.AbstractTraining;
import jcog.learn.lstm.ExpectedVsActual;

import java.util.Arrays;
import java.util.Random;
import java.util.function.Consumer;

public class DistractedSequenceRecall extends AbstractTraining {
	final int length;

	public DistractedSequenceRecall(Random r, int inputs, int outputs, int length, int batches) {
		super(r, inputs, outputs);

		this.length = length;
		this.batches = batches;
	}

	@Override
	protected void interact(Consumer<ExpectedVsActual> experience) {


		for (var i = 0; i < this.batches; i++) {


			var target1 = random.nextInt(outputs);
			var target2 = random.nextInt(outputs);
			var seq = new int[10];
			var count = 0;
			var bound = length;
            for (var t1 = 0; t1 < bound; t1++) {
				var i1 = random.nextInt(outputs) + outputs;
                if (seq.length == count) seq = Arrays.copyOf(seq, count * 2);
                seq[count++] = i1;
            }
            seq = Arrays.copyOfRange(seq, 0, count);
			var loc1 = random.nextInt(length);
			var loc2 = random.nextInt(length);
			while (loc1 == loc2)
				loc2 = random.nextInt(length);
			if (loc1 > loc2) {
				var temp = loc1;
				loc1 = loc2;
				loc2 = temp;
			}
			seq[loc1] = target1;
			seq[loc2] = target2;

			for (var t = 0; t < seq.length; t++) {
				var input = new double[inputs];
				input[seq[t]] = 1.0;

				var inter = new ExpectedVsActual();


				inter.actual = input;
				experience.accept(inter);

			}


			var input1 = new double[inputs];
			input1[8] = 1.0;
			var target_output1 = new double[outputs];
			target_output1[target1] = 1.0;
			experience.accept( ExpectedVsActual.the(input1, target_output1) );

			var input2 = new double[inputs];
			input2[9] = 1.0;
			var target_output2 = new double[outputs];
			target_output2[target2] = 1.0;
			experience.accept( ExpectedVsActual.the(input2, target_output2) );

		}

	}
}