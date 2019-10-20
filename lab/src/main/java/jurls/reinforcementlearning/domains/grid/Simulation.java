/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package jurls.reinforcementlearning.domains.grid;

import javax.swing.*;

/**
 *
 * @author me
 */
public class Simulation {

    public final Agent agent;

    private JFrame jf;

    public static boolean DISPLAY = true;

    long displayPeriodMS = 1000;

    private double rewardTotal;
    
    long cycleDelayMS;
    long lastDisplay = -1;
    long lastCycleTime;
    
    /*
    Run BECCA with world.  

    If restore=True, this method loads a saved agent if it can find one.
    Otherwise it creates a new one. It connects the agent and
    the world together and runs them for as long as the 
    world dictates.

    To profile BECCA's performance with world, manually setAt
    profile_flag in the top level script environment to True.

    */
    
    public void displayAgent(Agent a) {





        
    }

    public Simulation(Class<? extends Agent> agentClass, World world) throws Exception {
        this(agentClass, world, 0);
    }
            
    public void init(Agent a) {
        
    }
    
    public Simulation(Class<? extends Agent> agentClass, World world, long cycleDelayMS) throws Exception {
        this(agentClass.newInstance(), world, cycleDelayMS);
    }
    
    public Simulation(Agent agent, World world, long cycleDelayMS) {
        this.agent = agent;
        this.cycleDelayMS = cycleDelayMS;
        
        agent.init(world);

        init(agent);
        
        if (DISPLAY) {
            displayAgent(agent);
        }
        boolean displayRewardChart = DISPLAY;
        if (displayRewardChart) {
































        }
        
        /*if restore:
            agent = agent.restore()*/
        
        /*
        # If configured to do so, the world sets some BECCA parameters to 
        # modify its behavior. This is a development hack, and 
        # should eventually be removed as BECCA matures and settles on 
        # a good, general purpose set of parameters.
        world.set_agent_parameters(agent)        

        action = np.zeros((world.num_actions,1))
        */

        lastCycleTime = System.nanoTime();
        int cycles = 0;

        int time = 0;
        while (world.isActive()) {
            /*    
            # Repeat the loop through the duration of the existence of the world 
                sensor, reward = world.step(action)
                world.visualize(agent)
                action = agent.step(sensor, reward)
            return agent.report_performance()
            */

            double reward = world.step(agent.getAction(), agent.getSensor());
            rewardTotal+= reward;
            
            agent.step(reward);

            long now = System.currentTimeMillis();
            if ((lastDisplay == -1) || (now - lastDisplay > displayPeriodMS)) {
                
                if (jf!=null) jf.setTitle("Reward: " + reward + ", @" + time);
                lastDisplay = System.currentTimeMillis();

                long n = System.nanoTime();

                double cycleTime = ((((double)n) - ((double)lastCycleTime))/1000000000.0);
                double fps = cycles/cycleTime;
                System.out.println(time + " (" + fps + " cycles/sec)" + " " + cycleTime + "s");



                
                lastCycleTime = n;
                cycles = 0;            
            }
            
            if (cycleDelayMS > 0) {
                try {
                    Thread.sleep(cycleDelayMS);
                } catch (InterruptedException ex) {
                }
            }
            
            cycles++;            
            time++;
        }
        
        System.out.println("Simulation Finished.");
                          
    }
}
