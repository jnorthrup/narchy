package java4k.pinball4k.editor;

import javax.swing.*;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;

/**
 * A panel where the user can edit the level object properties.
 * @author tombr
 */
public class GroupUI extends JPanel implements ActionListener, ListSelectionListener {

	private GroupList groups;
	private final LevelPanel levelPanel;
	private final JList myList;
	private final JButton upBtn = new JButton("Up");
	private final JButton downBtn = new JButton("Down");
	private final JButton deleteBtn = new JButton("Delete");
	
	
	public GroupUI(GroupList groups, LevelPanel levelPanel) {
		this.groups = groups;
		this.levelPanel = levelPanel;
		
		myList = new JList(groups);
		myList.setCellRenderer(new GroupsCellRenderer());
		myList.addListSelectionListener(this);
		
		deleteBtn.addActionListener(this);
		upBtn.addActionListener(this);
		downBtn.addActionListener(this);

		var buttonPnl = new JPanel();
		buttonPnl.add(upBtn);
		buttonPnl.add(downBtn);
		buttonPnl.add(deleteBtn);
		
		setLayout(new BorderLayout());
		add(new JLabel("Groups:"), BorderLayout.NORTH);
		add(new JScrollPane(myList), BorderLayout.CENTER);
		add(buttonPnl, BorderLayout.SOUTH);
	}
	
	public void levelChanged() {
		this.groups = levelPanel.level.groups;
		myList.setModel(groups);
	}
	
	@Override
    public void actionPerformed(ActionEvent e) {
		if (e.getSource() == deleteBtn) {
			groups.remove((ArrayList<LevelObject>) myList.getSelectedValue());
		} else if (e.getSource() == upBtn) {
			var selectedIdx = myList.getSelectedIndex();
			if (selectedIdx > 0) {
				groups.move(myList.getSelectedIndex(), true);
				myList.setSelectedIndex(selectedIdx - 1);
			}
		} else if (e.getSource() == downBtn) {
			var selectedIdx = myList.getSelectedIndex();
			if (selectedIdx >= 0 && selectedIdx < groups.getSize() - 1) {
				groups.move(myList.getSelectedIndex(), false);
				myList.setSelectedIndex(selectedIdx + 1);
			}
		}
	}

	class GroupsCellRenderer extends JLabel implements ListCellRenderer {
		@Override
        public Component getListCellRendererComponent(
	    	       JList list,              
	    	       Object value,            
	    	       int index,               
	    	       boolean isSelected,      
	    	       boolean cellHasFocus)    
		 {
			 var s = value.toString();
		     setText(s);
		     
		     if (isSelected) {
		         setBackground(list.getSelectionBackground());
		         setForeground(list.getSelectionForeground());
		     } else {
		         setBackground(list.getBackground());
		         setForeground(list.getForeground());
		     }
		     setEnabled(list.isEnabled());
		     setFont(list.getFont());
		     setOpaque(true);
		     return this;
		 }	
	}

	/**
	 * Implements ListSelectionListener.
	 */
	@Override
    public void valueChanged(ListSelectionEvent e) {
		if (myList.getSelectedValue() != null) {
			var group = (ArrayList<LevelObject>) myList.getSelectedValue();
			levelPanel.setSelection(group);
		}
		
	}
}
