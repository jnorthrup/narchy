package jcog.grammar.parse.examples.logic;




import jcog.grammar.parse.examples.utensil.SwingUtensil;

import javax.swing.*;
import java.awt.*;

/**
 * This class provides an interactive development environment
 * for Logikus.
 * <p>
 * This class contains just the Swing components, and
 * delegates responsibility for the interaction of these
 * components to a LogikusMediator object. 
 * 
 * @author Steven J. Metsker
 *
 * @version 1.0 
 */

public class LogikusIde {
	private LogikusMediator mediator;

	private JTextArea programArea;
	private JTextArea resultsArea;
	private JTextArea queryArea;

	private JButton proveNextButton;
	private JButton proveRestButton;
	private JButton haltButton;
	private JButton clearProgramButton;
	private JButton clearResultsButton;

	/*
	 * Creates and returns the box that contains all of the IDE's
	 * buttons.
	 */
    private Box buttonBox() {
		Box b = Box.createHorizontalBox();
		b.add(proveButtonPanel());
		b.add(Box.createHorizontalGlue());
		b.add(clearButtonPanel());
		return b;
	}

	/*
	 * Creates and returns the panel that contains the IDE's
	 * clear buttons.
	 */
    private JPanel clearButtonPanel() {
		JPanel p = new JPanel();
		p.setBorder(SwingUtensil.ideTitledBorder("Clear"));
		p.add(clearProgramButton());
		p.add(clearResultsButton());
		return p;
	}

	/*
	 * The button that clears the results area.
	 */
    private JButton clearProgramButton() {
		if (clearProgramButton == null) {
			clearProgramButton = new JButton("Program");
			clearProgramButton.addActionListener(mediator());
			clearProgramButton.setFont(SwingUtensil.ideFont());
		}
		return clearProgramButton;
	}

	/*
	 * The button that clears the results area.
	 */
    private JButton clearResultsButton() {
		if (clearResultsButton == null) {
			clearResultsButton = new JButton("Results");
			clearResultsButton.addActionListener(mediator());
			clearResultsButton.setFont(SwingUtensil.ideFont());
		}
		return clearResultsButton;
	}

	/*
	 * The button that halts the proof thread.
	 */
    private JButton haltButton() {
		if (haltButton == null) {
			haltButton = new JButton("Halt");
			haltButton.setEnabled(false);
			haltButton.addActionListener(mediator());
			haltButton.setFont(SwingUtensil.ideFont());
		}
		return haltButton;
	}

	/**
	 * Launch a Logikus interactive development environment.
	 */
	public static void main(String[] args) {
		SwingUtensil.launch(new LogikusIde().mainPanel(), "Logikus");
	}

	/*
	 * Builds and returns the panel that contains all the
	 * components of the IDE.
	 */
    private JPanel mainPanel() {
		JPanel p = SwingUtensil.textPanel("Program", programArea(), new Dimension(600, 300), new Dimension(600, 150));

		JPanel q = SwingUtensil.textPanel("Query", queryArea(), new Dimension(600, 60), new Dimension(600, 60));

		JPanel r = SwingUtensil.textPanel("Results", resultsArea(), new Dimension(600, 150), new Dimension(600, 75));

		JSplitPane inner = new JSplitPane(JSplitPane.VERTICAL_SPLIT, false, q, r);
		JSplitPane outer = new JSplitPane(JSplitPane.VERTICAL_SPLIT, false, p, inner);

		inner.setDividerSize(3);
		outer.setDividerSize(3);

		JPanel whole = new JPanel();
		whole.setLayout(new BorderLayout());
		whole.add(outer, "Center");
		whole.add(buttonBox(), "South");
		return whole;
	}

	/*
	 * The object that controls or "mediates" the interaction
	 * of this development environment's Swing components.
	 */
    private LogikusMediator mediator() {
		if (mediator == null) {
			mediator = new LogikusMediator();
			mediator.initialize(proveNextButton(), proveRestButton(), haltButton(), clearProgramButton(), clearResultsButton(), programArea(), resultsArea(), queryArea());
		}
		return mediator;
	}

	/*
	 * The program text area.
	 */
    private JTextArea programArea() {
		if (programArea == null) {
			programArea = SwingUtensil.ideTextArea();
		}
		return programArea;
	}

	/*
	 * Creates and returns the panel that contains the IDE's
	 * proof buttons.
	 */
    private JPanel proveButtonPanel() {
		JPanel p = new JPanel();
		p.setBorder(SwingUtensil.ideTitledBorder("Prove"));
		p.add(proveNextButton());
		p.add(proveRestButton());
		p.add(haltButton());
		return p;
	}

	/*
	 * The button that starts the proof thread.
	 */
    private JButton proveNextButton() {
		if (proveNextButton == null) {
			proveNextButton = new JButton("Next");
			proveNextButton.addActionListener(mediator());
			proveNextButton.setFont(SwingUtensil.ideFont());
		}
		return proveNextButton;
	}

	/*
	 * The button that starts the proof thread, asking it to
	 * find all remaining proofs.
	 */
    private JButton proveRestButton() {
		if (proveRestButton == null) {
			proveRestButton = new JButton("Rest");
			proveRestButton.addActionListener(mediator());
			proveRestButton.setFont(SwingUtensil.ideFont());
		}
		return proveRestButton;
	}

	/*
	 * The query text area.
	 */
    private JTextArea queryArea() {
		if (queryArea == null) {
			queryArea = SwingUtensil.ideTextArea();
		}
		return queryArea;
	}

	/*
	 * The results text area.
	 */
    private JTextArea resultsArea() {
		if (resultsArea == null) {
			resultsArea = SwingUtensil.ideTextArea();
		}
		return resultsArea;
	}
}
