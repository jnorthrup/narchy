package jurls.reinforcementlearning.domains.grid;


/*
    Simplest one-dimensional grid task
    
    In this task, the agent's goal is to activate the same pattern as
    it sees in its sensor field.
*/
public class Grid1DDiscrete implements World {
    private final int size;

    private final double REWARD_MAGNITUDE;
    private final double ENERGY_COST_FACTOR;
    private final double MATCH_REWARD_FACTOR;

    private double focusPosition;
        
    private double[] action;

    private final int totalTime;    
    private int time;
    

    public Grid1DDiscrete(int size, int totalTime) {
        this.time = 1;
        this.size = size;
        var VISUALIZE_PERIOD = Math.pow(10, 4);
        this.ENERGY_COST_FACTOR = 0.5;
        this.MATCH_REWARD_FACTOR = size*1.1;
        this.REWARD_MAGNITUDE = 1;
        
        this.focusPosition = size/2;
        this.totalTime = totalTime;
    }

    
    @Override    public String getName()    {     return "Grid1D";    }
    @Override    public int getNumSensors() {     return size;    }
    @Override    public int getNumActions() {     return size;    }
    @Override    public boolean isActive()  {     return time < totalTime;   }

    double[] action2 = null;
    
    @Override
    public double step(double[] action, double[] sensor) {

        time++;
        
        this.action = action;
        
        
                  
        focusPosition++;
        
        
        
        if (focusPosition > size) focusPosition = 0;
        if (focusPosition < 0) focusPosition = size + focusPosition;
        
        /*        
        # Assign basic_feature_input elements as binary. 
        # Represent the presence or absence of the current position in the bin.
        */

        
        /*if (action2 == null) action2 = new double[action.length];
        for (int i = 0; i < action2.length; i++) {
            action2[i] = action[i];
            if (i > 0) action2[i] += 0.5 * action[i-1];
            if (i < action2.length-1) action2[i] += 0.5 * action[i+1];
        } */           
                  
        double match = 0;        
        double energyCost = 0;
        for (var i = 0; i < size; i++) {
            match += Math.abs( sensor[i] * action[i] );
            energyCost += action[i];
        }


        var reward = REWARD_MAGNITUDE * ((MATCH_REWARD_FACTOR * match) - (energyCost * ENERGY_COST_FACTOR));
        
        
        
        for (var i = 0; i < size; i++) {
            final var exp = 3.0;
            if (i == focusPosition)
                sensor[i] = 1.0;
            else
                sensor[i] = 0.0;            
        }
        /*
        
        for (int i = 0; i < size; i++) {
            sensor[i] = (max-min)*(sensor[i] - min);
        }
        */        
                
        return reward;        
    }
        

    @Override
    public String toString() {
        var s = "";
        for (var i = 0; i < size; i++) {
            char c;
            if (i == (int)focusPosition)
                c = 'O';
            else
                c = '.';
            s += c;
        }
        s += "\n";
        for (var i = 0; i < size; i++) {
            char c;
            if (action[i] > 0)
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

}
