package jcog.learn.lstm.test;

import jcog.learn.lstm.DistractedSequenceRecall;
import jcog.learn.lstm.SimpleLSTM;
import jcog.math.random.XorShift128PlusRandom;
import org.junit.jupiter.api.Test;

import java.util.Random;

import static org.junit.jupiter.api.Assertions.assertTrue;

public class LSTMTest {

	@Test
	public void testLSTM1() {
		
		//System.out.println("Test of SimpleLSTM\n");
		
		Random r = new XorShift128PlusRandom(1234);

		DistractedSequenceRecall task = new DistractedSequenceRecall(r,
				12, 3, 22, 1000);

		int cell_blocks = 4;
		//double learningRate = 0.05;
		SimpleLSTM slstm = task.lstm(cell_blocks);

		int epochs = 150;
		double error = 0;
		for (int epoch = 0; epoch < epochs; epoch++) {
			double fit = task.scoreSupervised(slstm, 0.1f);
			error = 1 - fit;
			if (epoch % 10 == 0)
				System.out.println("["+epoch+"] error = " + error);
		}
		//System.out.println("done.");
		assertTrue(error < 0.01f);
	}

}
