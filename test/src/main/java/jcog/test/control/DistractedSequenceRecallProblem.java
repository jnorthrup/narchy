/*
 * Copyright (c) 2017.
 *
 * This file is part of Project AGI. <http://agi.io>
 *
 * Project AGI is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Project AGI is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Project AGI.  If not, see <http://www.gnu.org/licenses/>.
 */

package jcog.test.control;


import jcog.Texts;
import jcog.learn.Agent;
import jcog.learn.ql.HaiQae;
import jcog.random.XoRoShiRo128PlusRandom;
import jcog.signal.tensor.ArrayTensor;
import jcog.test.AbstractAgentTest;
import org.apache.commons.math3.stat.descriptive.DescriptiveStatistics;
import org.eclipse.collections.api.block.function.primitive.IntIntToObjectFunction;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Random;

/**
 * From Monner & Reggia 2012, "A generalized LSTM-like training algorithm for
 * second-order recurrent neural networks"
 *
 * 5.1. Distracted Sequence Recall on the standard architecture
 * In the first set of experiments, we trained different neural networks on a
 * task we call the Distracted Sequence Recall task. This task is our variation of
 * the “temporal order” task, which is arguably the most challenging task demon-
 * strated by Hochreiter & Schmidhuber (1997). The Distracted Sequence Recall
 * task involves 10 symbols, each represented locally by a single active unit in an
 * input layer of 10 units: 4 target symbols, which must be recognized and remem-
 * bered by the network, 4 distractor symbols, which never need to be remembered,
 * and 2 prompt symbols which direct the network to give an answer. A single trial
 * consists of a presentation of a temporal sequence of 24 input symbols. The first
 * 22 consist of 2 randomly chosen target symbols and 20 randomly chosen dis-
 * tractor symbols, all in random order; the remaining two symbols are the two
 * prompts, which direct the network to produce the first and second target in the
 * sequence, in order, regardless of when they occurred. Note that the targets may
 * appear at any point in the sequence, so the network cannot rely on their tempo-
 * ral position as a cue; rather, the network must recognize the symbols as targets
 * and preferentially save them, along with temporal order information, in order
 * to produce the correct output sequence. The network is trained to produce no
 * output for all symbols except the prompts, and for each prompt symbol the
 * network must produce the output symbol which corresponds to the appropriate
 * target from the sequence.
 * The major difference between the “temporal order” task and our Distracted
 * Sequence Recall task is as follows. In the former, the network is required to
 * activate one of 16 output units, each of which represents a possible ordered se-
 * quence of both target symbols. In contrast, the latter task requires the network
 * to activate one of only 4 output units, each representing a single target symbol;
 * the network must activate the correct output unit for each of the targets, in the
 * same order they were observed. Requiring the network to produce outputs in
 * sequence adds a layer of difficulty; however, extra generalization power may be
 * imparted by the fact that the network is now using the same output weights
 * to indicate the presence of a target, regardless of its position in the sequence.
 * Because the “temporal order” task was found to be unsolvable by gradient train-
 * ing methods other than LSTM (Hochreiter & Schmidhuber, 1997), we do not
 * include methods other than LSTM and LSTM-g in the comparisons.
 * @author dave
 */
public class DistractedSequenceRecallProblem extends AbstractAgentTest {

    //    OBSERVATIONS 10 symbols = 10 inputs, one exclusively active each time
    //    TARGETS 4 target symbols - must be recognized and remembered
    //            4 distractor symbols
    //    PROMPTS 2 prompt symbols - network should give answer.
    //
    //    Each trial:
    //        - presentation of 24 inputs in sequence
    //        - First 22 consist of 2 random target symbols and 20 random distractors
    //        - Last 2 are prompts, requiring the network to produce the 1st and 2nd
    //          target pattern in the sequence, regardless of when they occurred.
    //        - No output allowed except when prompted.
    public final Random rng = new XoRoShiRo128PlusRandom(1);
    public boolean loop = true;

    public int epoch = 0;
    public int seq = 0;
    public int seqLen = 0;
    public int distractors = 0;
    public int targets = 0; //numActions
    public int prompts = 0;

    public float reward = 0;

    public ArrayTensor sequenceState;
    public ArrayTensor sequenceActions;
    public ArrayTensor state;
    public ArrayTensor actions;
    public ArrayTensor idealActions;

    @Override
    protected void test(IntIntToObjectFunction<Agent> agentBuilder) {
        int sequenceLength =
                //24;
                8;

        int targets =
                //4;
                2;
        int prompts = 2;
        int distractors =
                //4;
                2;
        seqLen = sequenceLength;
        this.targets = targets; // targets all all bits up to _targets
        this.prompts = prompts;
        this.distractors = distractors;

        int observations = numInputs();

        sequenceState = new ArrayTensor( new int[] { observations, seqLen } );
        sequenceActions = new ArrayTensor(new int[] { this.targets, seqLen });
        state = new ArrayTensor( observations );
        actions = new ArrayTensor(this.targets);
        idealActions = new ArrayTensor(this.targets);

        reset();

        Agent a = agentBuilder.value(observations, targets);

        int windowLen = 5000;
        DescriptiveStatistics rewardStat = new DescriptiveStatistics(windowLen);
        for (int i = 0; i < 5000000; i++) {

            update(a);

            rewardStat.addValue(reward);

            if (i % windowLen == 0) {
                System.out.println(Texts.n4(rewardStat.getMean()));
                //print();
            }

        }
    }



    public int numInputs() {
        return targets + prompts + distractors;
    }


    public void reset() {
        // randomly generate a trial
        seq = 0;
        sequenceState.fill( 0.f );
        sequenceActions.fill( 0.f );

        ArrayList< Integer > targetBits = new ArrayList< Integer >();
        ArrayList< Integer > targetTimes = new ArrayList< Integer >();
//        HashSet< Integer > usedTargetIndices = new HashSet< Integer >();

        int observations = numInputs();
        int length = seqLen - prompts;

        // pick targets for each prompt in a trial:
        // Need a 2-D pick:
        for(int t = 0; t < prompts; ++t ) {
            int targetBit = rng.nextInt(targets); // allow duplicate targets (unspecified whether this is the case)

            int targetTime = 0;

            do {
                targetTime = rng.nextInt( length );//targetIndexRange );
            }
            while( targetTimes.contains( targetTime ) );  // must be dissimilar

            targetBits.add( targetBit );
            targetTimes.add( targetTime );
        }

        // now sort by target times.
        Collections.sort( targetTimes );

        // set the target inputs:
        for(int t = 0; t < prompts; ++t ) {
            int targetBit  = targetBits.get( t );
            int targetTime = targetTimes.get( t );
            sequenceState.set(1f, targetTime, targetBit ); // these inputs are the targets
            // zero output expected during target setting
            int promptBit = targets +t;
            int promptTime = length + t;
            sequenceState.set(1f, promptTime, promptBit); // these inputs are the targets
            sequenceActions.set(1f, promptTime, targetBit); // ideal output bit is the target value
        }

        // now set some random distractor bits:
        // " A single trial
        // consists of a presentation of a temporal sequence of 24 input symbols. The first
        // 22 consist of 2 randomly chosen target symbols and 20 randomly chosen dis-
        // tractor symbols, all in random order; the remaining two symbols are the two
        // prompts, which direct the network to produce the first and second target in the
        // sequence "
        for( int t = 0; t < length; ++t ) {
            // don't set a bit when there's an target given:
            boolean hasTargetBit = false;
            for(int t2 = 0; t2 < prompts; ++t2 ) {
                int targetTime = targetTimes.get( t2 );
                if( targetTime == t ) {
                    hasTargetBit = true;
                    break;
                }
            }
            if( hasTargetBit ) {
                continue;
            }

            // else pick a random distractor bit:
            int distractorBit = targets + prompts + rng.nextInt(distractors);
            sequenceState.set(1f, t , distractorBit); // these inputs are the targets
        }

        //print();
    }

    int getNbrTargets() {
        return targets;
    }

    int getNbrPrompts() {
        return prompts;
    }

    int getNbrDistractors() {
        return distractors;
    }

    public void print() {

        // print column titles
        int observations = numInputs();
        int length = observations;

        for( int c = 0; c < length; ++c ) {
            if( c < 10 ) {
                System.err.print( "0" );
            }
            System.err.print( c );
            System.err.print( " " );
        }

        System.err.print( "| " );

        for( int c = 0; c < length; ++c ) {
            if( c < 10 ) {
                System.err.print( "0" );
            }
            System.err.print( c );
            System.err.print( " " );
        }

        System.err.println();

        // Row 2: Bit assignments
        length = targets;
        for( int c = 0; c < length; ++c ) {
            System.err.print( "T  " );
        }

        length = prompts;
        for( int c = 0; c < length; ++c ) {
            System.err.print( "P  " );
        }

        length = distractors;
        for( int c = 0; c < length; ++c ) {
            System.err.print( "D  " );
        }

        System.err.print( "| " );

        length = targets;
        for( int c = 0; c < length; ++c ) {
            System.err.print( "T  " );
        }

        length = prompts;
        for( int c = 0; c < length; ++c ) {
            System.err.print( "P  " );
        }

        length = distractors;
        for( int c = 0; c < length; ++c ) {
            System.err.print( "D  " );
        }

        System.err.println();

        // ok now print the actual inputs and outputs
        for(int sequence = 0; sequence < seqLen; ++sequence ) {

            for( int c = 0; c < observations; ++c ) {
                String s = "   ";
                float r = sequenceState.get(sequence, c);
                if( r > 0.f ) {
                    if( c < ( targets + prompts) ) {
                        s = "*  ";
                    }
                    else {
                        s = "+  ";
                    }
                }

                System.err.print( s );
            }

            System.err.print( "| " );

            for(int c = 0; c < targets; ++c ) {
//                String s = "   ";
                float r = sequenceActions.get(sequence, c);
                String ideal = "";
//                if( r > 0.f ) {
//                    if( c < ( _targets + _prompts ) ) {
//                        s = "*  ";
//                    }
//                    else {
//                        s = "+  ";
//                    }

                    r = Math.min( 0.999f, Math.max( 0f, r ) );
                    int n = (int)( r * 100f ); // 0..99
                    String val = String.valueOf( n );
                    while( val.length() < 3 ) val = " " + val;
                    ideal += val;
//                }
                System.err.print( ideal );
//                System.err.print( s );
            }

            String seq = String.valueOf( sequence );
            while( seq.length() < 3 ) seq = " " + seq;
            System.err.print( "-" + seq + "-" );

            if( sequence == this.seq) {
                String agent = "";
                for(int c = 0; c < targets; ++c ) {
//                    float r = _idealActions._values[ c ];
                    float r = actions.get(c);
                    r = Math.min( 0.999f, Math.max( 0f, r ) );
                    int n = (int)( r * 100f ); // 0..99
                    String val = String.valueOf( n );
                    while( val.length() < 3 ) val = " " + val;
                    agent += val;
                }

                System.err.print( agent );
                System.err.print( " <---" );
            }

            System.err.println();
        }
    }

    public boolean complete() {
        return seq >= seqLen;
    }


    protected void updateState() {
        // generate state:
        int observations = numInputs();
        for( int i = 0; i < observations; ++i ) {
            state.setAt(sequenceState.get(seq, i), i);
        }
    }

    protected void updateIdealActions() {
        // generate ideal actions
        for(int i = 0; i < targets; ++i ) {
            idealActions.setAt(sequenceActions.get(seq, i), i);
        }
    }

    public float getRandomExpectedReward() {
        float expectedReward = 1f / (float) targets; // will be correct 25% of the time if 4 targets
        expectedReward *= (float) prompts; // happens this number of times
        expectedReward /= (float) seqLen;
        //float t = ( _sequenceLength - _prompts ) + expectedReward; // ie 22 * 1, +
        //t /= (float)_sequenceLength; // unit value
        return expectedReward;
    }

    protected void updateReward() {
        // e.g. len 10, 2 prompts, 10-2=8,9 are the test ones
        int testStart = seqLen - prompts;

        if( seq < testStart ) {
            reward = 0f;
            return; // don't care.
        }

        float maxError = 0f;

//        System.err.println( "REWARD @ seq: " + _sequence );
//        for( int i = 0; i < _targets; ++i ) {
//            float ideal = _idealActions._values[ i ];
//            float actual = _actions._values[ i ];
//            float diff = Math.abs( ideal - actual );
//            System.err.println( "T: " + i + " Ideal: " + ideal + " Actual: " + actual + " Err: " + diff );
//        }

        for(int i = 0; i < targets; ++i ) {
            float ideal = idealActions.get(i);
            float actual = actions.get(i);
            float diff = Math.abs( ideal - actual );
            maxError = Math.max( diff, maxError );
        }

        reward =
                //1f - maxError;                // error = 0, reward = 1
                2 * (-0.5f + (1f - maxError)); // error = -1, reward = 1

//        if( _reward > 0.5f ) {
//            int g = 0;
//            g++;
//        }
    }

    public void update(Agent a) {

        updateReward();

        int nextAction = a.act(reward, state.data);
        actions.fill(0);
        actions.setAt(1f, nextAction);

        ++seq;

        if( complete() ) {
            ++epoch;
            if(loop) {
                reset();
            }
        }

        updateState(); // expose next state
        updateIdealActions();


    }

    public static void main(String[] args) {
        new DistractedSequenceRecallProblem().test(
            //DQN2::new
            HaiQae::new
        );
    }

}