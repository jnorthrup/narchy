package jcog.learn;

import jcog.Texts;
import jcog.Util;
import jcog.learn.ql.DQN2;
import jcog.learn.ql.HaiQ;
import jcog.learn.ql.HaiQae;
import jcog.random.XoRoShiRo128PlusRandom;
import org.eclipse.collections.api.block.function.primitive.IntIntToObjectFunction;
import org.eclipse.collections.impl.list.mutable.primitive.FloatArrayList;
import org.eclipse.collections.impl.map.mutable.primitive.IntIntHashMap;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.Random;

import static org.junit.jupiter.api.Assertions.assertTrue;

/** https://github.com/openai/gym/tree/master/gym/envs
 * https://github.com/jmacglashan/burlap/tree/master/src/main/java/burlap/domain/singleagent
 * */
abstract class AgentTest {


    @Test
    void testHaiQ() {
        test(HaiQ::new);
    }

    @Test
    void testDQN2() {
        test(DQN2::new);
    }

    @Test
    void testHaiQae() {
        test(HaiQae::new);
    }


    abstract protected void test(IntIntToObjectFunction<Agent> agentBuilder);

    public static class TestAgentObviousChoice extends AgentTest {

        @Override
        protected void test(IntIntToObjectFunction<Agent> agentBuilder) {

            Agent agent = agentBuilder.value(1, 2);
            assert (agent.inputs >= 1);
            assert (agent.actions == 2);

            final float minRatio = 2f;

            int cycles = 100;

            float nextReward = 0;
            IntIntHashMap acts = new IntIntHashMap();
            for (int i = 0; i < cycles; i++) {
                int action = agent.act(nextReward, new float[]{(float) Math.random()});

                acts.addToValue(action, 1);
                switch (action) {
                    case 0:
                        nextReward = -1.0f;
                        break;
                    case 1:
                        nextReward = +1.0f;
                        break;
                    default:
                        throw new UnsupportedOperationException();
                }
            }
            System.out.println(this.getClass().getSimpleName() + "\t" + agent.getClass() + " " + agent.summary() + "\n" + acts);
            assertTrue(acts.get(1) > acts.get(0));
            assertTrue(acts.get(1) / minRatio > acts.get(0));

        }
    }

    public static class TestAgentTwoToTwoBoolean extends AgentTest {


        @Override
        protected void test(IntIntToObjectFunction<Agent> agentBuilder) {

            Agent agent = agentBuilder.value(2, 2);

            int cycles = 500;

            Random rng = new XoRoShiRo128PlusRandom(1);
            IntIntHashMap acts = new IntIntHashMap();

            float nextReward = 0;
            float rewardSum = 0;
            for (int i = 0; i < cycles; i++) {

                boolean which = rng.nextBoolean();

                int action = agent.act(nextReward, new float[]{which ? 1 : 0, which ? 0 : 1});

                acts.addToValue(action, 1);

                nextReward = (which ? action == 1 : action == 0) ? +1 : -1;
                rewardSum += nextReward;
            }
            float rewardMean = rewardSum / cycles;
            System.out.println(agent.getClass() + " " + agent.summary() + "\n" + acts + " " + rewardMean);
            assertTrue(rewardMean > 0);
        }

    }

    /**
     *  Adapted from Example 6.6 (page 132) from Reinforcement Learning: An Introduction
     *     by Sutton and Barto:
     *     http://incompleteideas.net/book/the-book-2nd.html
     *
     *     The board is a 4x12 matrix, with (using Numpy matrix indexing):
     *         [3, 0] as the start at bottom-left
     *         [3, 11] as the goal at bottom-right
     *         [3, 1..10] as the cliff at bottom-center
     *     Each time step incurs -1 reward, and stepping into the cliff incurs -100 reward
     *     and a reset to the start. An episode terminates when the agent reaches the goal.
     *
     * https://github.com/openai/gym/blob/master/gym/envs/toy_text/cliffwalking.py */
    public static class CliffWalking extends AgentTest {

        static final int UP = 0, RIGHT = 1, DOWN = 2, LEFT = 3;

        //increasing the size of the map increases the complexity
        //final int shapeX = 4, shapeY = 12; //<- original
        //final int shapeX = 3, shapeY = 4; //small
        final int shapeX = 2, shapeY = 3; //minimum
        {
            assert(shapeX > 1);
            assert(shapeY > 2);
        }


        final int gx = shapeX-1, gy = shapeY-1;
        final int sx = shapeX-1, sy = 0;

        int x = sx, y = sy;

        private int index(int x, int y) {
            return shapeX * y + x;
        }

        final boolean[][] cliff = new boolean[shapeX][shapeY];


        @Override
        protected void test(IntIntToObjectFunction<Agent> agentBuilder) {
            Agent a = agentBuilder.value(shapeX * shapeY, 4);

            for (int y = 1; y < shapeY-1; y++)
                cliff[shapeX-1][y] = true;

            int cycles = 50000;
            float epReward = 0;
            float reward = 0;

            int goals = 0, deaths = 0;
            final float[] map = new float[shapeX * shapeY];

            FloatArrayList episodeReward = new FloatArrayList(2 * cycles/(shapeX*shapeY));
            for (int i = 0; i < cycles; i++) {

                Arrays.fill(map, 0);
                map[index(x,y)] = 1;
//                for (int mx = 0; mx < shapeX; mx++) {
//                    for (int my = 0; my < shapeY; my++) {
//                        map[index(mx, my)] =
//                    }
//                }

                int action = a.act(reward, map);

                switch (action) {
                    case LEFT:
                        x = Util.clamp(x-1, 0, shapeX-1);
                        break;
                    case RIGHT:
                        x = Util.clamp(x+1, 0, shapeX-1);
                        break;
                    case UP:
                        y = Util.clamp(y+1, 0, shapeY-1);
                        break;
                    case DOWN:
                        y = Util.clamp(y-1, 0, shapeY-1);
                        break;
                    default:
                        throw new RuntimeException();
                }


                boolean restart = false;
                if (cliff[x][y]) {
                    reward = -1;
                    deaths++;
                    restart = true;
                } else if (x == gx && y == gy){
                    reward =
                            0;
                            //+1;
                    goals++;
                    restart = true;
                } else {
                    reward = -0.01f;
                }

                epReward += reward;

                if (restart) {
                    episodeReward.add(epReward);
                    epReward = 0;
                    x = sx; y = sy;
                }
            }


            System.out.println(getClass().getSimpleName() + "\t" + a + "\t" +
                    Texts.n4(episodeReward.average()) + " mean episode reward (" + episodeReward.size() + " episodes)"
                    //Texts.n4(rewardSum/cycles) + " mean reward; "
                    + "\t" +
                    goals + " goals, " + deaths + " deaths");

//            episodeReward.forEachWithIndex((r, i) -> {
//               System.out.println(i + "\t" + Texts.n4(r));
//            });
            //System.out.println(SparkLine.renderFloats(episodeReward));

            //assertTrue(episodeReward.size() > ..)
            assertTrue(goals > 2 * deaths);
        }
    }

}