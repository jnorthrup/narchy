package nars.ca;




import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.Vector;



public class MJFavourities extends Dialog implements ActionListener {
	private final Button btnLoad;
    private final Button btnCcl;
	private final Label lblPrompt;
	private final List LstFiles = new List(10, false);
	private final MJCellUI mjUI;

	
	
	MJFavourities(Frame frame, MJCellUI mjui) {
		super(frame, "Favourite patterns", false);
		mjUI = mjui;
		setLayout(new BorderLayout());
		lblPrompt = new Label("Select the pattern:");
		add("North", lblPrompt);
		add("Center", LstFiles);

		var pnlButtons = new Panel();
		pnlButtons.setLayout(new FlowLayout());
		pnlButtons.add(btnLoad = new Button(" Load "));
		btnLoad.addActionListener(this);
		pnlButtons.add(btnCcl = new Button("Cancel"));
		btnCcl.addActionListener(this);
		add("South", pnlButtons);

		var d = getToolkit().getScreenSize();
		setLocation(d.width / 4, d.height / 3);
		setSize(d.width / 6, d.height / 3);
		setVisible(false);
		InitList();
		pack();
	}

	
	
	@SuppressWarnings("HardcodedFileSeparator")
	public void InitList() {
        int iGame;

		LstFiles.clear();

		var vLines = new Vector();
		var mjT = new MJTools();
		if (MJTools.LoadResTextFile("fav.txt", vLines)) {
			for (var i = 0; i < vLines.size(); i++) {
				if (!((String) vLines.elementAt(i)).startsWith("//"))
					LstFiles.add((String) vLines.elementAt(i));
			}
		}
	}

	
	
	@SuppressWarnings("HardcodedFileSeparator")
	private void LoadCurrentPattern() {

        if (LstFiles.getSelectedIndex() >= 0) {
			var sItem = LstFiles.getSelectedItem();
			var whereSlash = sItem.lastIndexOf('/');
			var sPattName = "";
			var sRuleName = "";
			var sGameName = "";
            if (whereSlash > 0) {
				sPattName = sItem.substring(whereSlash + 1); 

				sItem = sItem.substring(0, whereSlash);
				var whereSep = sItem.lastIndexOf('|');
				if (whereSep > 0) {
					sGameName = sItem.substring(0, whereSep); 
					sRuleName = sItem.substring(whereSep + 1); 

					sGameName = MJRules.GetGameName(MJRules
							.GetGameIndex(sGameName));
					mjUI.ActivateGame(sGameName);
					mjUI.ActivateRule(sRuleName);
				}
			}

			if ((!sGameName.isEmpty()) && (!sRuleName.isEmpty())) {
				lblPrompt.setText("Please wait...");
				try {
					mjUI.mjo.OpenFile(sGameName + '/' + sRuleName + '/'
							+ sPattName);
					lblPrompt.setText("Select the pattern:");
				} catch (Exception exc) {
					lblPrompt.setText("Error loading pattern!");
				}
			}
		}
	}

	
	@Override
	public void actionPerformed(ActionEvent ae) {
		if (ae.getSource() == btnLoad) {
			LoadCurrentPattern();
		} else if (ae.getSource() == btnCcl) {
			setVisible(false);
		}
	}

	
	@Override
	public boolean action(Event evt, Object arg) {
		if (evt.target.equals(LstFiles)) {
			LoadCurrentPattern();
		} else
			return super.action(evt, arg);
		return true;
	}

	
	@Override
	public boolean handleEvent(Event evt) {
		if (evt.id == Event.WINDOW_DESTROY)
			setVisible(false);
		else
			return super.handleEvent(evt);
		return true;
	}
	

}