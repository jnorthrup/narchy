/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package jurls.reinforcementlearning.domains;

import javax.swing.*;
import java.awt.*;

/**
 *
 * @author thorsten
 */
public class PhysicsRenderer extends javax.swing.JPanel {

    public Physics2D physics2D;

    /**
     * Creates new form PhysicsRenderer
     */
    public PhysicsRenderer() {
        initComponents();
    }

    Stroke st = new BasicStroke(4);

    @Override
    protected void paintComponent(Graphics g) {
        g.setColor(new Color(0, 0, 0, 0.25f));
        g.fillRect(0, 0, getWidth(), getHeight());

        Graphics2D g2 = (Graphics2D)g;
        g2.setStroke(st);
        if (physics2D != null) {
            g.setColor(Color.magenta);
            g.drawLine(0, (int) physics2D.floor, getWidth(), (int) physics2D.floor);

            g.setColor(Color.orange);
            for (Connection c : physics2D.connections) {
                g.drawLine((int) c.p1.x, (int) c.p1.y, (int) c.p2.x, (int) c.p2.y);
            }

            g.setColor(Color.green);
            for (Point p : physics2D.points) {
                g.drawArc((int) p.x - 10, (int) p.y - 10, 20, 20, 0, 360);
            }
        }
    }

    /**
     * This method is called from within the constructor to initialize the form.
     * WARNING: Do NOT modify this code. The content of this method is always
     * regenerated by the Form Editor.
     */
    @SuppressWarnings("unchecked")
    
    private void initComponents() {

        GroupLayout layout = new javax.swing.GroupLayout(this);
        this.setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGap(0, 400, Short.MAX_VALUE)
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGap(0, 300, Short.MAX_VALUE)
        );
    }


    
    
}
