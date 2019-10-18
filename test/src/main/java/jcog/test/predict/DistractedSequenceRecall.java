package jcog.test.predict;

import jcog.learn.lstm.AbstractTraining;
import jcog.learn.lstm.ExpectedVsActual;

import java.util.Arrays;
import java.util.Random;
import java.util.function.Consumer;
import java.util.stream.IntStream;

public class DistractedSequenceRecall extends AbstractTraining {
	final int length;

	public DistractedSequenceRecall(Random r, int inputs, int outputs, int length, int batches) {
		super(r, inputs, outputs);

		this.length = length;
		this.batches = batches;
	}

	@Override
	protected void interact(Consumer<ExpectedVsActual> experience) {


		for (int i = 0; i < this.batches; i++) {


            int target1 = random.nextInt(outputs);
			int target2 = random.nextInt(outputs);
            int[] seq = new int[10];
            int count = 0;
            int bound = length;
            for (int t1 = 0; t1 < bound; t1++) {
                int i1 = random.nextInt(outputs) + outputs;
                if (seq.length == count) seq = Arrays.copyOf(seq, count * 2);
                seq[count++] = i1;
            }
            seq = Arrays.copyOfRange(seq, 0, count);
            int loc1 = random.nextInt(length);
			int loc2 = random.nextInt(length);
			while (loc1 == loc2)
				loc2 = random.nextInt(length);
			if (loc1 > loc2) {
				int temp = loc1;
				loc1 = loc2;
				loc2 = temp;
			}
			seq[loc1] = target1;
			seq[loc2] = target2;

			for (int t = 0; t < seq.length; t++) {
				double[] input = new double[inputs];
				input[seq[t]] = 1.0;

				ExpectedVsActual inter = new ExpectedVsActual();


				inter.actual = input;
				experience.accept(inter);

			}

			
			double[] input1 = new double[inputs];
			input1[8] = 1.0;
			double[] target_output1 = new double[outputs];
			target_output1[target1] = 1.0;
			experience.accept( ExpectedVsActual.the(input1, target_output1) );

			double[] input2 = new double[inputs];
			input2[9] = 1.0;
			double[] target_output2 = new double[outputs];
			target_output2[target2] = 1.0;
			experience.accept( ExpectedVsActual.the(input2, target_output2) );

		}

	}
}