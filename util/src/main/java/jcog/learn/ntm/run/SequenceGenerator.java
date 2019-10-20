package jcog.learn.ntm.run;


import jcog.random.XorShift128PlusRandom;

import java.util.Random;

public class SequenceGenerator
{
    static final Random rng = new XorShift128PlusRandom(8L);

    public static TrainingSequence generateSequenceSawtooth(int length, int vectorSize) {
        length = length*2+2;

        var input = new double[length][vectorSize];
        var output = new double[length][vectorSize];

        var direction = rng.nextFloat() < 0.5;


        var j = (int)(rng.nextFloat() * 100);
        for (var i = 0; i < length; i++) {
            var index = j % vectorSize;

            input[i][index] = 1;
            var reflected = (vectorSize - 1) - index;
            output[i][reflected] = 1;

            if (direction)
                j++;
            else
                j--;

        }

        return new TrainingSequence(input, output);
    }

    public static TrainingSequence generateSequenceXOR(int length, int vectorSize) {


        var input = new double[length][vectorSize];
        var output = new double[length][vectorSize];


        var j = (int)(rng.nextFloat() * 153) % Math.max(1,vectorSize/2) + vectorSize/2;

        for (var i = 0; i < length; i++) {
            var index = ((j)^(i)) % vectorSize;
            

            if (i < vectorSize/2)
                input[i][i] = 1;
            input[i][j] = 1;
            output[i][index] = 1;


        }

        return new TrainingSequence(input, output);
    }

    public static TrainingSequence generateSequenceWTF(int length, int inputVectorSize) {
        var data = new double[length][inputVectorSize];
        for (var i = 0; i < length; i++)
        {

            for (var j = 0; j < inputVectorSize; j++)
            {
                data[i][j] = rng.nextInt(2);
            }
        }
        var sequenceLength = (length * 2) + 2;
        var vectorSize = inputVectorSize - 2;
        var input = new double[sequenceLength][inputVectorSize];

        for (var i = 0; i < sequenceLength; i++)
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
        var output = new double[sequenceLength][vectorSize];
        for (var i = 0; i < sequenceLength; i++)
        {
            
            if (i >= (length + 2))
            {
                System.arraycopy(data[i - (length + 2)], 0, output[i], 0, vectorSize);
            }
             
        }
        return new TrainingSequence(input, output);
    }

}


