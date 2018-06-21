package jcog.grammar.parse.examples.query;





import jcog.grammar.parse.examples.utensil.SwingUtensil;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;

/**
 * This is a simple user environment (UE) for queries 
 * written in the Jaql language. This UE applies Jaql 
 * queries to the ChipSource axiom source.
 * <p>
 * This class contains just the methods that create Swing 
 * components, and uses a "mediator" to control how the 
 * components interact.
 *
 * @author Steven J. Metsker
 *
 * @version 1.0
 *
 * @see JaqlMediator
 */

public class JaqlUe {
	private JaqlMediator mediator;
	private JButton goButton;
	private JButton clearButton;
	private JTextArea metadataArea;
	private JTextArea queryArea;
	private JTextArea resultArea;
	private static int PREFERREDWIDTH = 600;

	/*
	 * The panel for the "Go!" and "Clear" buttons.
	 */
    private JPanel buttonPanel() {
		JPanel p = new JPanel();
		p.setLayout(new BorderLayout());
		p.add(goButton(), "North");
		p.add(clearButton(), "South");
		p.setBorder(new EmptyBorder(10, 6, 5, 6));
		return p;
	}

	/*
	 * The "Clear" button.
	 */
    private JButton clearButton() {
		if (clearButton == null) {
			clearButton = new JButton("Clear");
			clearButton.addActionListener(mediator());
			clearButton.setFont(SwingUtensil.ideFont());
		}
		return clearButton;
	}

	/*
	 * The "Go!" button. This method also establishes "Ctrl-G"
	 * as a shortcut for pressing the button.
	 */
    private JButton goButton() {
		if (goButton == null) {
			goButton = new JButton("Go!");
			goButton.addActionListener(mediator());
			goButton.setFont(SwingUtensil.ideFont());

			
			KeyStroke ctrlg = KeyStroke.getKeyStroke(KeyEvent.VK_G, InputEvent.CTRL_MASK);

			goButton.registerKeyboardAction(mediator(), ctrlg, JComponent.WHEN_IN_FOCUSED_WINDOW);
		}
		return goButton;
	}

	/*
	 * The split pane that contains the query area and the 
	 * result area.
	 */
    private JSplitPane ioPane() {
		Dimension min = new Dimension(PREFERREDWIDTH, 80);
		Dimension pref = new Dimension(PREFERREDWIDTH, 180);

		JPanel q = SwingUtensil.textPanel("Query", queryArea(), pref, min);
		JPanel r = SwingUtensil.textPanel("Results", resultArea(), pref, min);

		JSplitPane jsp = new JSplitPane(JSplitPane.VERTICAL_SPLIT, false, q, r);
		jsp.setDividerSize(3);
		return jsp;
	}

	/**
	 * Launch the interactive development environment.
	 */
	public static void main(String[] args) {
		SwingUtensil.launch(new JaqlUe().mainPanel(), "Jaql and Chips");
	}

	/*
	 * The main panel, which contains all components.
	 */
    private JPanel mainPanel() {
		JPanel p = new JPanel();
		p.setLayout(new BorderLayout());
		p.add(upperPanel(), "Center");
		p.add(metadataPanel(), "South");
		return p;
	}

	/*
	 * The object that controls the component interactions.
	 */
    private JaqlMediator mediator() {
		if (mediator == null) {
			mediator = new JaqlMediator();
			mediator.initialize(goButton(), clearButton(), queryArea(), resultArea(), metadataArea());
		}
		return mediator;
	}

	/*
	 * The metadata text area.
	 */
    private JTextArea metadataArea() {
		if (metadataArea == null) {
			metadataArea = SwingUtensil.ideTextArea();
			ChipSource cs = new ChipSource();
			metadataArea.append(ChipSource.queryChip() + "\n");
			metadataArea.append(ChipSource.queryCustomer() + "\n");
			metadataArea.append(ChipSource.queryOrder() + "\n");
		}
		return metadataArea;
	}

	/*
	 * The panel that contains the metadata text area.
	 */
    private JPanel metadataPanel() {
		return SwingUtensil.textPanel("Metadata", metadataArea(), new Dimension(PREFERREDWIDTH, 120), new Dimension(PREFERREDWIDTH, 80));
	}

	/*
	 * The input text area.
	 */
    private JTextArea queryArea() {
		if (queryArea == null) {
			queryArea = SwingUtensil.ideTextArea();
			queryArea.setText("select ChipName, PricePerBag from Chip \n" + "where Oil != \"Sunflower\"");
		}
		return queryArea;
	}

	/*
	 * The output text area.
	 */
    private JTextArea resultArea() {
		if (resultArea == null) {
			resultArea = SwingUtensil.ideTextArea();
		}
		return resultArea;
	}

	/*
	 * The panel that contains the query area, the result area 
	 * and the buttons.
	 */
    private JPanel upperPanel() {
		JPanel p = new JPanel();
		p.setLayout(new BorderLayout());
		p.add(ioPane(), "Center");
		p.add(buttonPanel(), "East");
		return p;
	}
}
