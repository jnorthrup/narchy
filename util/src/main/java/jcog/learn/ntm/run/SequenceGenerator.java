package jcog.learn.ntm.run;


import jcog.random.XorShift128PlusRandom;

import java.util.Random;

public class SequenceGenerator
{
    static final Random rng = new XorShift128PlusRandom(8L);

    public static TrainingSequence generateSequenceSawtooth(int length, int vectorSize) {
        length = length*2+2; 

        double[][] input = new double[length][vectorSize];
        double[][] output = new double[length][vectorSize];

        boolean direction = rng.nextFloat() < 0.5;


        int j = (int)(rng.nextFloat() * 100);
        for (int i = 0;i < length;i++) {
            int index = j % vectorSize;
            int reflected = (vectorSize-1) - index;

            input[i][index] = 1;
            output[i][reflected] = 1;

            if (direction)
                j++;
            else
                j--;

        }

        return new TrainingSequence(input, output);
    }

    public static TrainingSequence generateSequenceXOR(int vectorSize) {
        

        int length = (int) (vectorSize * (1.0 + rng.nextFloat()));

        double[][] input = new double[length][vectorSize];
        double[][] output = new double[length][vectorSize];


        int j = (int)(rng.nextFloat() * 153) % (vectorSize/2) + vectorSize/2;

        for (int i = 0; i < length; i++) {
            int index = ((j)^(i)) % vectorSize;
            

            if (i < vectorSize/2)
                input[i][i] = 1;
            input[i][j] = 1;
            output[i][index] = 1;


        }

        return new TrainingSequence(input, output);
    }

    public static TrainingSequence generateSequenceWTF(int length, int inputVectorSize) {
        double[][] data = new double[length][inputVectorSize];
        for (int i = 0;i < length;i++)
        {

            for (int j = 0;j < inputVectorSize;j++)
            {
                data[i][j] = rng.nextInt(2);
            }
        }
        int sequenceLength = (length * 2) + 2;
        int vectorSize = inputVectorSize - 2;
        double[][] input = new double[sequenceLength][inputVectorSize];

        for (int i = 0;i < sequenceLength;i++)
        {
            
            if (i == 0) {
                
                input[0][vectorSize] = 1.0;
            }
            else if (i <= length) {
                
                System.arraycopy(data[i - 1], 0, input[i], 0, vectorSize);
            }
            else if (i == (length + 1)) {
                
                input[i][vectorSize + 1] = 1.0;
            }
               
        }
        double[][] output = new double[sequenceLength][vectorSize];
        for (int i = 0;i < sequenceLength;i++)
        {
            
            if (i >= (length + 2))
            {
                System.arraycopy(data[i - (length + 2)], 0, output[i], 0, vectorSize);
            }
             
        }
        return new TrainingSequence(input, output);
    }

}


