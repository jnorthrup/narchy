/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package jurls;




import jurls.core.LearnerAndActor;
import jurls.core.approximation.ApproxParameters;
import jurls.core.reinforcementlearning.RLAgent;
import jurls.core.reinforcementlearning.RLParameters;
import jurls.core.utils.MatrixImage;
import jurls.examples.menu.AgentMenu;
import jurls.examples.menu.ObjectListMenu;
import jurls.examples.menu.RLMenu;
import jurls.reinforcementlearning.domains.PoleBalancing2D;
import jurls.reinforcementlearning.domains.RLEnvironment;
import jurls.reinforcementlearning.domains.follow.Follow1D;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;



/**
 *
 * @author thorsten
 */
public class RLDemo extends javax.swing.JFrame {


    public class DomainMenu extends RLMenu {

        private final JRadioButtonMenuItem follow1D = new JRadioButtonMenuItem(new MyAction("Follow 1D"));
        private final JRadioButtonMenuItem poleBalancing = new JRadioButtonMenuItem(new MyAction("Pole Balancing"));
        private final JRadioButtonMenuItem wanderBot = new JRadioButtonMenuItem(new MyAction("Wander Bot"));
        

        public DomainMenu() {
            super("Domain");

            ButtonGroup bg = new ButtonGroup();
            bg.add(poleBalancing);
            bg.add(wanderBot);
            bg.add(follow1D);
            

            add(follow1D);
            add(poleBalancing);
            add(wanderBot);
            

            follow1D.setSelected(true);
        }

        public RLEnvironment getDomain() {
            if (poleBalancing.isSelected()) {
                return new PoleBalancing2D();
            }





            if (follow1D.isSelected()) {
                return new Follow1D();
            }

            /*if (martialArts.isSelected()) {
                return new MartialArts();
            }*/

            return null;
        }
    }

    private LearnerAndActor agent;
    private RLEnvironment rLDomain;

    final int updatePeriodMS = 0;
    private int numIterationsPerLoop = 1;
    private final ApproxParameters approxParameters = new ApproxParameters(0.01, 0.1);
    private final RLParameters rLParameters = new RLParameters(0.01, 0.9, 0.9, 0);
    private int numPhysicsIterations = 0;
    private final AgentMenu agentMenu = new AgentMenu("", 1);
    private final DomainMenu domainMenu = new DomainMenu();
    private final ObjectListMenu iterationsMenu = new ObjectListMenu(
            "No. Iterations", 0, 1, 2, 5, 10, 50, 100, 500, 1000, 5000
    );
    private int action = 0;
    boolean visualize = true;

    public final MatrixImage parameterChart = new MatrixImage(50, 50);

    private final Action menuAction = new AbstractAction() {

        @Override
        public void actionPerformed(ActionEvent e) {
            synchronized (RLDemo.this) {
                timer.stop();

                rLDomain = domainMenu.getDomain();
                if (agent != null) {
                    agent.stop();
                }
                agent = agentMenu.getAgent(
                        rLDomain.numActions(),
                        rLDomain.observe(), approxParameters, rLParameters
                );

                parameterChart.removeAll();

                renderPanel.removeAll();
                renderPanel.add(rLDomain.component());

                action = 0;
                numPhysicsIterations = 0;

                timer.start();
            }
        }
    };
    private final Timer timer = new Timer(updatePeriodMS, new ActionListener() {
        @Override
        public void actionPerformed(ActionEvent e) {

                updateParameters();

                for (int i = 0; i < numIterationsPerLoop; ++i) {
                    double[] previousState = rLDomain.observe();
                    rLDomain.takeAction(action);
                    rLDomain.frame();
                    double[] nextState = rLDomain.observe();

                    action = agent.learnAndAction(
                            nextState,
                            rLDomain.getReward(),
                            previousState,
                            action
                    );
                    numPhysicsIterations++;
                }

                jTextArea1.setText(agent.getDebugString(0));
                debugLabel.setText("@" + numPhysicsIterations);

                if (visualize) {
                    final double r = rLDomain.getReward();









                    if (agent instanceof RLAgent) {
                        double[] d = ((RLAgent)agent).getStateNormalized();
                        int numParam = d.length;
                        final int cw = (int) Math.ceil(Math.sqrt(numParam));
                        final int ch = numParam / cw;

                        parameterChart.draw(agent.getFunction());
                        













                    }

                }

                rLDomain.component().repaint();

        }
    });

    /**
     * Creates new form RLDemo
     */
    public RLDemo() {
        initComponents();

        timer.setCoalesce(true);

        jMenuBar1.add(agentMenu);
        agentMenu.addActionListener(menuAction);
        jMenuBar1.add(domainMenu);
        domainMenu.addActionListener(menuAction);
        jMenuBar1.add(iterationsMenu);

        iterationsMenu.addActionListener((ActionEvent e) -> {
            numIterationsPerLoop = (int) iterationsMenu.getObject();
        });
        iterationsMenu.notifyListeners();

        updateParameters();

        menuAction.actionPerformed(null);

        timer.start();
    }

    protected void updateParameters() {






    }

    /**
     * This method is called from within the constructor to initialize the form.
     * WARNING: Do NOT modify this code. The content of this method is always
     * regenerated by the Form Editor.
     */
    @SuppressWarnings("unchecked")

    private void initComponents() {

        buttonGroup1 = new javax.swing.ButtonGroup();
        approximatorButtonGroup = new javax.swing.ButtonGroup();
        actionSelectorButtonGroup = new javax.swing.ButtonGroup();
        numFeaturesButtonGroup = new javax.swing.ButtonGroup();
        agentButtonGroup = new javax.swing.ButtonGroup();
        jTabbedPane1 = new javax.swing.JTabbedPane();
        jPanel3 = new javax.swing.JPanel();

        parametersPanel = new javax.swing.JPanel();
        jLabel1 = new javax.swing.JLabel();
        jLabel2 = new javax.swing.JLabel();





        jLabel9 = new javax.swing.JLabel();
        jLabel10 = new javax.swing.JLabel();
        jLabel3 = new javax.swing.JLabel();

        jLabel4 = new javax.swing.JLabel();
        debugLabel = new javax.swing.JLabel();
        renderPanel = new javax.swing.JPanel();
        jPanel4 = new javax.swing.JPanel();
        jPanel5 = new javax.swing.JPanel();
        jScrollPane2 = new javax.swing.JScrollPane();
        jTextArea2 = new javax.swing.JTextArea();
        jPanel6 = new javax.swing.JPanel();
        jScrollPane3 = new javax.swing.JScrollPane();
        jTextArea3 = new javax.swing.JTextArea();
        jTextArea1 = new javax.swing.JTextArea();
        jMenuBar1 = new javax.swing.JMenuBar();

        setDefaultCloseOperation(javax.swing.WindowConstants.DISPOSE_ON_CLOSE);

        jTabbedPane1.setPreferredSize(new java.awt.Dimension(1000, 800));

        jPanel3.setLayout(new java.awt.BorderLayout());


        parametersPanel.setLayout(new GridLayout(0,2));












        jPanel3.add(parametersPanel, java.awt.BorderLayout.SOUTH);

        debugLabel.setText("jLabel5");
        jPanel3.add(debugLabel, java.awt.BorderLayout.NORTH);

        renderPanel.setLayout(new java.awt.GridLayout(1, 0));
        jPanel3.add(renderPanel, java.awt.BorderLayout.CENTER);

        jTabbedPane1.addTab("Demo", jPanel3);

        jPanel4.setLayout(new java.awt.BorderLayout());
        jTabbedPane1.addTab("Debug 1", jPanel4);

        jPanel5.setLayout(new java.awt.BorderLayout());

        jTextArea2.setColumns(20);
        jTextArea2.setRows(5);
        jScrollPane2.setViewportView(jTextArea2);

        jPanel5.add(jScrollPane2, java.awt.BorderLayout.CENTER);


        jTabbedPane1.addTab("Debug 2", jPanel5);

        jPanel6.setLayout(new java.awt.BorderLayout());

        jTextArea3.setColumns(20);
        jTextArea3.setRows(5);
        jScrollPane3.setViewportView(jTextArea3);

        jPanel6.add(jScrollPane3, java.awt.BorderLayout.CENTER);

        jTabbedPane1.addTab("Debug 3", jPanel6);


        jTextArea1.setColumns(20);
        jTextArea1.setRows(5);


        JPanel outputPanel = new JPanel(new GridLayout(0, 1));
        outputPanel.add(new JScrollPane(jTextArea1));
        outputPanel.add(parameterChart);
        


        getContentPane().add(jTabbedPane1, java.awt.BorderLayout.CENTER);
        getContentPane().add(outputPanel, java.awt.BorderLayout.WEST);
        setJMenuBar(jMenuBar1);

        pack();
    }





















    @Override
    public void dispose() {
        timer.stop();
        if (agent != null) {
            agent.stop();
        }
        super.dispose();
    }

    /**
     * @param args the command line arguments
     */
    public static void main(String[] args) {


        /* Create and display the form */
        java.awt.EventQueue.invokeLater(() -> new RLDemo().setVisible(true));
    }

    
    private javax.swing.ButtonGroup actionSelectorButtonGroup;
    private javax.swing.ButtonGroup agentButtonGroup;
    
    private javax.swing.ButtonGroup approximatorButtonGroup;
    private javax.swing.ButtonGroup buttonGroup1;
    private javax.swing.JLabel debugLabel;
    
    private javax.swing.JLabel jLabel1;
    private javax.swing.JLabel jLabel10;
    private javax.swing.JLabel jLabel2;
    private javax.swing.JLabel jLabel3;
    private javax.swing.JLabel jLabel4;
    private javax.swing.JLabel jLabel9;
    private javax.swing.JMenuBar jMenuBar1;
    
    
    private javax.swing.JPanel jPanel3;
    private javax.swing.JPanel jPanel4;
    private javax.swing.JPanel jPanel5;
    private javax.swing.JPanel jPanel6;
    private javax.swing.JPanel parametersPanel;
    private javax.swing.JScrollPane jScrollPane2;
    private javax.swing.JScrollPane jScrollPane3;
    private javax.swing.JTabbedPane jTabbedPane1;
    private javax.swing.JTextArea jTextArea1;
    private javax.swing.JTextArea jTextArea2;
    private javax.swing.JTextArea jTextArea3;
    
    private javax.swing.ButtonGroup numFeaturesButtonGroup;
    
    
    private javax.swing.JPanel renderPanel;
    
}
