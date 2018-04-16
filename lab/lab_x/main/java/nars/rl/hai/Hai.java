package nars.rl.hai;

import nars.util.concept.random.XORShiftRandom;

import java.util.Random;

/** Original Q-Learning + SOM agent by patham9 */
public class Hai {

    final Random random;


    double Q[][]; //state, action
    double et[][];
    int nActions = 0, nStates = 0;
    int lastStateX = 0, lastStateY = 0, lastAction = 0;
    double Alpha = 0.1, Gamma = 0.8, Lambda = 0.1; //0.1 0.5 0.9
    Hsom som;

    public Hai(int nactions, int nstates, XORShiftRandom random) {
        this.random = random;
        nActions = nactions;
        nStates = nstates;
        Q = new double[nStates][nActions];
        et = new double[nStates][nActions];
    }



    /** interfaces with 2D SOM */
    @Deprecated int update(int StateX, int StateY, int states, double reward) {
        return update(StateY*states+stateX, reward);
    }

    int update(int state, double reward) {
        int maxk = 0;
        double maxval = -999999;
        for (int k = 0; k < nActions; k++) {
            if (Q[StateX][StateY][k] > maxval) {
                maxk = k;
                maxval = Q[StateX][StateY][k];
            }
        }
        
        int Action = 0;
        if (random.nextFloat() < Alpha) {
            Action = (int) random.nextFloat() * (nActions);
        } else {
            Action = maxk;
        }
        
        double DeltaQ = reward + Gamma * Q[StateX][StateY][Action] - 
                Q[lastStateX][lastStateY][lastAction];
        
        et[lastStateX][lastStateY][lastAction] += 1;
        
        for (int i = 0; i < nStates; i++) {
            for (int j = 0; j < nStates; j++) {
                for (int k = 0; k < nActions; k++) {
                    Q[i][j][k] += Alpha * DeltaQ * et[i][j][k];
                    et[i][j][k] = Gamma * Lambda * et[i][j][k];
                }
            }
        }

        lastStateX = StateX;
        lastStateY = StateY;
        lastAction = Action;
        return lastAction;
    }

    void SetParams(double[] Params) {
        Alpha = Params[0];
        Gamma = Params[1];
        Lambda = Params[2];
    }

    int UpdateSOM(double[] viewField, double reward) {
        som.learn(viewField);
        return update(som.winnerx, som.winnery, reward);
    }


//    int Quantify(double val, int quantsteps) {
//        double step = 1 / ((double) quantsteps);
//        double wander = 0.0;
//        int ind = -1;
//        while (wander <= val) {
//            wander += step;
//            ind++;
//        }
//        return ind;
//    }
}
