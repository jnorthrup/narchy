package java4k.pinball4k.editor;

import javax.swing.*;
import java.awt.*;

public class QuadBezier extends JPanel {
	
	@Override
    public void paintComponent(Graphics g) {
		float[] xs = {100, 200, 200, 200, 300};
		float[] ys = {100, 100, 200, 150, 200};
		
		for (var curveIdx = 0; curveIdx < (xs.length-1)/2; curveIdx++) {
			var idx = curveIdx * 2;
			var x0 = xs[idx];
			var y0 = ys[idx];
			var x1 = xs[idx+1];
			var y1 = ys[idx+1];
			var x2 = xs[idx+2];
			var y2 = ys[idx+2];

			var prevx = x0;
			var prevy = y0;
			for (float t=0; t<=1; t+=0.01f) {
				var t2 = t*t;
				var tinv = 1-t;
				var tinv2 = tinv*tinv;

				var x = tinv2 * x0 + 2*t*tinv*x1 + t2*x2;
				var y = tinv2 * y0 + 2*t*tinv*y1 + t2*y2;
				g.drawLine((int) prevx, (int) prevy, (int) x, (int) y);
				prevx = x;
				prevy = y;
			}
		}
	}
	

	public static void main(String[] args) {
		var frame = new JFrame();
		frame.setSize(640, 480);
		frame.setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
		frame.getContentPane().add(new QuadBezier(), BorderLayout.CENTER);
		frame.setVisible(true);
	}
}
