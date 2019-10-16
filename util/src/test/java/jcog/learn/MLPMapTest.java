package jcog.learn;

import jcog.learn.ntm.control.SigmoidActivation;
import jcog.random.XoRoShiRo128PlusRandom;
import org.junit.jupiter.api.Test;

import java.util.Random;

class MLPMapTest {
	@Test void XOR_in_2_out_1() {

		float[][] train_in = {new float[]{0, 0}, new float[]{0, 1}, new float[]{1, 0}, new float[]{1, 1}};
		float[][] train_out = {new float[]{0}, new float[]{1}, new float[]{1}, new float[]{0}};

		int cases = train_out.length;

		Random r = //new Random();
			new XoRoShiRo128PlusRandom(1);

		MLPMap mlp = new MLPMap(2,
			new MLPMap.Layer(2, SigmoidActivation.the),
			new MLPMap.Layer(1,null)
		).randomize(r);
		//, new int[]{2, 1}, new Random(), true);

		float learningRate = 0.1f;

		int t;
		int time = 5000;
		for (t = 0; t < time; t++) {

			double err = 0;

			for (int i = 0; i < cases; i++) {
				int idx =
					//i;
					r.nextInt(cases);

				mlp.put(train_in[idx], train_out[idx], learningRate);
				err += mlp.errorSquared();
			}

			if ((t + 1) % 10 == 0) {
				System.out.println();
				for (int i = 0; i < cases; i++) {
					float[] u = train_in[i];
					System.out.printf("%d\t", t);
					System.out.printf("(%f err) %.1f, %.1f --> %.5f\n",
						err,
						u[0], u[1], mlp.get(u)[0]);
				}
			}

			if(err < 0.0001)
				break;
		}
		assert(t < time);
	}

}