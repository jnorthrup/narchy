package jurls.reinforcementlearning.domains.grid;

/*
    One-dimensional grid task
    
    In this task, the agent steps forward and backward along 
    a nine-position line. The fourth position is rewarded and 
    the ninth position is punished. There is also a slight 
    punishment for effort expended in trying to move, 
    i.e. taking actions. This is intended to be a simple-as-possible 
    task for troubleshooting BECCA. See Chapter 4 of the 
    Users Guide for details.
    Optimal performance is a reward of about 90 per time step.
*/
public class Grid1D implements World {
    private final int size;
    private final double REWARD_MAGNITUDE;
    private final double ENERGY_COST;
    private final double JUMP_FRACTION;
    private double worldState;
    private int simpleState;
    private double energy;
    private double[] action;
    private final int totalTime;
    
    private int time;
    private final double noise;
    private final double cycleSkew;

    public Grid1D(int size, int totalTime, double noise, double cycleSkew) {
        this.time = 0;
        this.size = size;
        double VISUALIZE_PERIOD = Math.pow(10.0, 4.0);
        this.REWARD_MAGNITUDE = 100.0;
        this.ENERGY_COST =  this.REWARD_MAGNITUDE / 100.0;
        this.JUMP_FRACTION = 0.0;        
        this.noise = noise;
        this.cycleSkew = cycleSkew;
        
        
        
        
        this.worldState = 0.0;
        this.simpleState = 0;
        this.totalTime = totalTime;
        /*
        this.display_state = True;
        */
    }
    
    @Override    public String getName()    {     return "Grid1D";    }
    @Override    public int getNumSensors() {     return size;    }
    @Override    public int getNumActions() {     return size;    }
    @Override    public boolean isActive()  {     return time < totalTime;   }

    @Override
    public double step(double[] action, double[] sensor) {

        time++;
        
        this.action = action;


        double stepSize = (    action[0] +
                2.0 * action[1] +
                3.0 * action[2] +
                4.0 * action[3] -
                                action[4] -
                2.0 * action[5] -
                3.0 * action[6] -
                4.0 * action[7]);

        
        energy=(action[0] +
                2.0 * action[1] +
                3.0 * action[2] +
                4.0 * action[3] +
                     action[4] +
                2.0 * action[5] +
                3.0 * action[6] +
                4.0 * action[7]);

        worldState += stepSize;
                
        
        if (Math.random() < JUMP_FRACTION) {
            worldState = (double) size * Math.random();
        }
        else {
            worldState += cycleSkew;
        }
        
        
        worldState -= (double) size * Math.floor( worldState / ((double)size) );
        simpleState = (int)Math.floor(worldState);
        if (simpleState == 9) simpleState = 0;
        
        /*        
        # Assign basic_feature_input elements as binary. 
        # Represent the presence or absence of the current position in the bin.
        */
        
        for (int i = 0; i < sensor.length; i++) {
            sensor[i] = (Math.random()*noise);
        }            
        sensor[simpleState] = 1.0 - (Math.random()*noise);
        
        
        
        return getReward(sensor);
    }
    
    public double getReward(double[] sensor) {

        double reward = 0.;
        reward -= sensor[8] * REWARD_MAGNITUDE;
        reward += sensor[3] * REWARD_MAGNITUDE;
        
        
        reward -= energy  * ENERGY_COST;
        reward = Math.max(reward, -REWARD_MAGNITUDE);
        
        return reward;    
    }
    

    @Override
    public String toString() {
        String s = "";
        for (int i = 0; i < size; i++) {
            char c;
            if (i == simpleState)
                c = 'O';
            else
                c = '.';
            s += c;
        }
        s += "\n";
        for (int i = 0; i < size; i++) {
            char c;
            if (action[i] > (double) 0)
                c = 'X';
            else
                c = '.';
            s += c;
        }
        s += "\n";
        return s;
    }
    
    /*
    def visualize(self, agent):
        """ Show what's going on in the world """
        if (this.display_state):
            state_image = ['.'] * (this.num_sensors + this.num_actions + 2)
            state_image[this.simple_state] = 'O'
            state_image[this.num_sensors:this.num_sensors + 2] = '||'
            action_index = np.where(this.action > 0.1)[0]
            if action_index.size > 0:
                for i in range(action_index.size):
                    state_image[this.num_sensors + 2 + action_index[i]] = 'x'
            print(''.join(state_image))
            
        if (this.timestep % this.VISUALIZE_PERIOD) == 0:
            print("world age is %s timesteps " % this.timestep)    
    */

    /*
    
    def set_agent_parameters(self, agent):
    """ Turn a few of the knobs to adjust BECCA for this world """
    # Prevent the agent from forming any groups
    #agent.reward_min = -100.
    #agent.reward_max = 100.
    pass
     */
    
    public static void main(String[] args) {
        
    }
}
