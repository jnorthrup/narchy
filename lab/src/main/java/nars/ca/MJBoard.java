package nars.ca;




import java.awt.*;
import java.awt.image.MemoryImageSource;
import java.util.StringTokenizer;




class MJBoard extends Panel implements Runnable {
	public static final int MAX_X = 800; 
	public static final int MAX_Y = 600;
	public static final int MAX_CLO = 255; 
	public static final int MAX_CELLSIZE = 32; 
	public Thread caThread;
	public final MJPalette mjPal; 
	private final boolean InitDone;
	private final MJCellUI mjUI; 
	public int AnimDelay = 100; 
	public int RefreshStep = 1; 
	public int CrrGame; 
	public int GameType = MJRules.GAMTYP_2D; 
	public String RuleName; 
	public String RuleDef; 
	public int Cycle; 
	public int StatesCount = 2; 
	public int Population; 
	public final int[] Populations = new int[MAX_CLO + 1]; 
	
	private int lastX;
    private int lastY;
	public boolean IsRunning; 
	public boolean DrawGrid = true; 
	public int ColoringMethod = 1; 
	public int CrrState = 1; 

	
	private final Panel pnlBotm; 
	private final Scrollbar hSbar;
    private final Scrollbar vSbar; 
	private static final int sbarWidth = 16;
	private final Button btnZoomIn = new Button("+");
	private final Button btnZoomOut = new Button("-");
	private final Button btnFit = new Button("F");

	
	public final Point UnivSize; 
	public int CellSize = 5; 
	private final short[][] crrState; 
	private final short[][] tmpState; 
	private final short[][] bakState; 
	private final Point ViewOrg; 
	private final Point ViewSize; 
	private final Point CellsInView; 
	private final Point LastPanelSize;

	
	private int[] screen;
	private Image offImg; 

	
	private MemoryImageSource offSrs;
	private int OfsX;
	private int OfsY;

	
	public boolean WrapAtEdges = true; 
	public int i1DLastRow; 
	public final RuleGene RGene;
	public final RuleLife RLife;
	public final RuleVote RVote;
	public final RuleCyclic RCyclic;
	public final RuleWLife RWLife;
	public final Rule1DTotal R1DTo;
	public final Rule1DBin R1DBin;
	public final RuleNeumBin RNeumBin;
	public final RuleGenBin RGenBin;
	public final RuleRTab RRtab;
	public final RuleLgtL RLgtL;
	public final RuleMarg RMarg;
	public final RuleUser RUser;

	
	public final MJDiversities Div; 

	

	
	
	MJBoard(MJCellUI mui) {
		int i;

		mjUI = mui; 
		mjPal = new MJPalette();
		CrrGame = MJRules.GAME_LIFE; 
		crrState = new short[MAX_X + 1][MAX_Y + 1];
		tmpState = new short[MAX_X + 1][MAX_Y + 1];
		bakState = new short[MAX_X + 1][MAX_Y + 1];
		ViewOrg = new Point(20, 20); 
		UnivSize = new Point(0, 0); 
		ViewSize = new Point(0, 0); 
		CellsInView = new Point(0, 0); 
		LastPanelSize = new Point(0, 0); 

		setLayout(new BorderLayout(0, 0));
		pnlBotm = new Panel();
		add("South", pnlBotm);

		pnlBotm.setLayout(new GridLayout(1, 4)); 
		pnlBotm.add(btnZoomIn);
		pnlBotm.add(btnZoomOut);
		hSbar = new Scrollbar(Scrollbar.HORIZONTAL);
		pnlBotm.add(hSbar);
		pnlBotm.add(btnFit);
		vSbar = new Scrollbar(Scrollbar.VERTICAL);
		add("East", vSbar);

		InitBoard(160, 120, 5);
		InitDone = true;

		
		i1DLastRow = 0; 
		RGene = new RuleGene();
		RLife = new RuleLife();
		RVote = new RuleVote();
		RCyclic = new RuleCyclic();
		R1DTo = new Rule1DTotal();
		R1DBin = new Rule1DBin();
		RNeumBin = new RuleNeumBin();
		RGenBin = new RuleGenBin();
		RRtab = new RuleRTab();
		RWLife = new RuleWLife();
		RLgtL = new RuleLgtL();
		RMarg = new RuleMarg();
		RUser = new RuleUser();

		
		Div = new MJDiversities(); 
	}

	
	
	public void start() {
		mjUI.btnRunStop.setLabel("STOP");
		mjUI.itmRunStop.setLabel("Stop  (Enter)");
		if (caThread == null) {
			caThread = new Thread(this, "MJCell");
			caThread.setPriority(Thread.NORM_PRIORITY);
			caThread.start();
		}
	}

	
	
	public void stop() {
		mjUI.btnRunStop.setLabel("START");
		mjUI.itmRunStop.setLabel("Start  (Enter)");
		caThread = null;
	}

	
	
	@Override
	public void run() {
		var thisThread = Thread.currentThread();
		while (thisThread == caThread) {

			var doRedraw = false;
			if (OneCycle() > 0) 
			{
				switch (RefreshStep) {
				case 1: 
					doRedraw = true; 
					break;

				case -1: 
					if (GameType == MJRules.GAMTYP_1D) {
						if (i1DLastRow % UnivSize.y == 0) 
							doRedraw = true;
					} else {
						if (Cycle % UnivSize.y == 0) 
							doRedraw = true;
					}
					break;
				default: 
					if (Cycle % RefreshStep == 0)
						doRedraw = true;
					break;
				}
			} else {
				stop();
			}

			mjUI.UpdateUI(); 
			if (doRedraw) {
				RedrawBoard(false);


				var iDelay = AnimDelay;
				try {
					Thread.sleep(iDelay);
				} catch (InterruptedException e) {
				}
			}
		}
		mjUI.UpdateUI(); 
		if (RefreshStep != 1)
			RedrawBoard(false); 
	}

	
	
	public void SingleStep() {
		int iCnt;
		if (RefreshStep > 0) {
			iCnt = RefreshStep;
		} else 
		{
			iCnt = UnivSize.y - i1DLastRow; 
			if (iCnt <= 0)
				iCnt = UnivSize.y; 
		}

		stop();
		for (var i = 1; i <= iCnt; i++) {
			if (OneCycle() == 0) 
				break;
		}
		RedrawBoard(false); 
		mjUI.UpdateUI(); 
	}

	
	
	
	int OneCycle() {
		var modCnt = 0;

		try {
			
			if (Div.m_Enabled)
				Div.Perform(true, this); 

			switch (CrrGame) {
			case MJRules.GAME_LIFE: 
				modCnt = RLife.OnePass(UnivSize.x, UnivSize.y, WrapAtEdges,
						ColoringMethod, crrState, tmpState, this);
				break;

			case MJRules.GAME_VOTE: 
				modCnt = RVote.OnePass(UnivSize.x, UnivSize.y, WrapAtEdges,
						ColoringMethod, crrState, tmpState, this);
				break;

			case MJRules.GAME_WLIF: 
				modCnt = RWLife.OnePass(UnivSize.x, UnivSize.y, WrapAtEdges,
						ColoringMethod, crrState, tmpState, this);
				break;

			case MJRules.GAME_GENE: 
				modCnt = RGene.OnePass(UnivSize.x, UnivSize.y, WrapAtEdges,
						ColoringMethod, crrState, tmpState, this);
				break;

			case MJRules.GAME_RTBL: 
				modCnt = RRtab.OnePass(UnivSize.x, UnivSize.y, WrapAtEdges,
						ColoringMethod, crrState, tmpState, this);
				break;

			case MJRules.GAME_CYCL: 
				modCnt = RCyclic.OnePass(UnivSize.x, UnivSize.y, WrapAtEdges,
						ColoringMethod, crrState, tmpState);
				break;

			case MJRules.GAME_1DTO: 
				modCnt = R1DTo.OnePass(UnivSize.x, UnivSize.y, WrapAtEdges,
						ColoringMethod, crrState, tmpState, this);
				break;

			case MJRules.GAME_1DBI: 
				modCnt = R1DBin.OnePass(UnivSize.x, UnivSize.y, WrapAtEdges,
						ColoringMethod, crrState, tmpState, this);
				break;

			case MJRules.GAME_NMBI: 
				modCnt = RNeumBin.OnePass(UnivSize.x, UnivSize.y, WrapAtEdges,
						ColoringMethod, crrState, tmpState);
				break;

			case MJRules.GAME_GEBI: 
				modCnt = RGenBin.OnePass(UnivSize.x, UnivSize.y, WrapAtEdges,
						ColoringMethod, crrState, tmpState, this);
				break;

			case MJRules.GAME_LGTL: 
				modCnt = RLgtL.OnePass(UnivSize.x, UnivSize.y, WrapAtEdges,
						ColoringMethod, crrState, tmpState, this);
				break;

			case MJRules.GAME_MARG: 
				modCnt = RMarg.OnePass(UnivSize.x, UnivSize.y, WrapAtEdges,
						ColoringMethod, crrState, tmpState, this);
				break;

			case MJRules.GAME_USER: 
				modCnt = RUser.OnePass(UnivSize.x, UnivSize.y, WrapAtEdges,
						ColoringMethod, crrState, tmpState);
				break;
			}


			int i;
			if (GameType == MJRules.GAMTYP_2D)
			{
				for (i = 0; i < UnivSize.x; i++) {
					for (var j = 0; j < UnivSize.y; j++) {
						SetCell(i, j, tmpState[i][j]);
					}
				}
			} else 
			{
				for (i = 0; i < UnivSize.x; i++) {
					SetCell(i, i1DLastRow, tmpState[i][i1DLastRow]);
				}
			}

			
			if (Div.m_Enabled)
				Div.Perform(false, this); 
		} catch (Exception exc) {
        }
		if (modCnt > 0)
			Cycle++; 

		return modCnt;
	} 

	
	
	
	public final void SetCell(int x, int y, short bState) {
		if ((x >= 0) && (y >= 0) && (x < UnivSize.x) && (y < UnivSize.y)) {
			if (crrState[x][y] != bState) {
				Populations[crrState[x][y]]--;
				Populations[bState]++;
				if (0 == crrState[x][y])
					Population++;
				else if (0 == bState)
					Population--;
				crrState[x][y] = bState;
			}
		}
	}

	
	
	public final void SetCell(CACell cell) {
		if ((cell.x >= 0) && (cell.y >= 0) && (cell.x < UnivSize.x)
				&& (cell.y < UnivSize.y)) {
			if (crrState[cell.x][cell.y] != cell.state) {
				Populations[crrState[cell.x][cell.y]]--;
				Populations[cell.state]++;
				if (0 == crrState[cell.x][cell.y])
					Population++;
				else if (0 == cell.state)
					Population--;
				crrState[cell.x][cell.y] = cell.state;
			}
		}
	}

	
	
	public short GetCell(int x, int y) {
		if ((x >= 0) && (y >= 0) && (x < UnivSize.x) && (y < UnivSize.y)) {
			return crrState[x][y];
		}
		return 0;
	}

	
	
	public void SetBoardSize(int sizX, int sizY) {
		InitBoard(sizX, sizY, CellSize);
	}

	
	
	public void InitBoard(int sizX, int sizY, int cellSiz) {
		var fOldRun = caThread != null;

		if (sizX > MAX_X)
			sizX = MAX_X;
		if (sizY > MAX_Y)
			sizY = MAX_Y;
		if (sizX < 10)
			sizX = 10;
		if (sizY < 10)
			sizY = 10;
		
		stop();
		try {
			Thread.sleep(100);
		} catch (InterruptedException e) {
		}

		var fNewSize = false;
		if ((sizX != UnivSize.x) || (sizY != UnivSize.y)) {
			UnivSize.x = sizX;
			UnivSize.y = sizY;


			int j;
			int i;
			for (i = UnivSize.x; i <= MAX_X; i++)
				for (j = 0; j <= MAX_Y; j++)
					crrState[i][j] = 0;

			for (i = 0; i <= UnivSize.x; i++)
				for (j = UnivSize.y; j <= MAX_Y; j++)
					crrState[i][j] = 0;

			fNewSize = true;
			UpdatePopulation();
			Cycle = 0; 
		}
		CellSize = cellSiz;

		if (RecalcLayout()) 
		{
			if (fNewSize) 
			{
				CenterBoard();
			}

			
			screen = new int[ViewSize.x * ViewSize.y]; 
			offSrs = new MemoryImageSource(ViewSize.x, ViewSize.y, screen, 0,
					ViewSize.x);
			offSrs.setAnimated(true);
			offImg = createImage(offSrs);
			
			

			
			UpdateScrollbars(); 
			
			setBackground(Color.gray);
			hide();
			show(); 
			ValidateBoard(); 
			RedrawBoard(true); 
		}
		if (InitDone) {
			mjUI.UpdateGridUI(); 
			mjUI.UpdateUI(); 
		}
		if (fOldRun)
			start();
	}

	
	
	public boolean RecalcLayout() {
		var wdt = getSize().width;
		var hgt = getSize().height;
		LastPanelSize.x = wdt;
		LastPanelSize.y = hgt;

		
		pnlBotm.reshape(0, hgt - sbarWidth, wdt, sbarWidth);
		btnZoomIn.reshape(0, 0, sbarWidth, sbarWidth);
		btnZoomOut.reshape(sbarWidth + 1, 0, sbarWidth, sbarWidth);
		var iTmp = btnZoomIn.getSize().width + btnZoomOut.getSize().width;
		hSbar.reshape(iTmp + 1, 0, wdt - sbarWidth - iTmp - 1, sbarWidth);
		btnFit.reshape(wdt - sbarWidth, 0, sbarWidth, sbarWidth);
		vSbar.reshape(wdt - sbarWidth, 0, sbarWidth, hgt - sbarWidth);

		
		wdt -= sbarWidth;
		hgt -= sbarWidth;
		var retVal = true;
		if ((wdt > CellSize) && (hgt > CellSize)) {
			if (wdt >= UnivSize.x * CellSize) 
			{
				wdt = UnivSize.x * CellSize;
				ViewOrg.x = 0;
				OfsX = (LastPanelSize.x - sbarWidth - wdt) / 2;
			} else {
				OfsX = 0;
			}
			if (hgt >= UnivSize.y * CellSize) 
			{
				hgt = UnivSize.y * CellSize;
				ViewOrg.y = 0;
				OfsY = (LastPanelSize.y - sbarWidth - hgt) / 2;
			} else {
				OfsY = 0;
			}
			ViewSize.x = wdt - (wdt % CellSize);
			ViewSize.y = hgt - (hgt % CellSize);
			CellsInView.x = wdt / CellSize; 
			CellsInView.y = hgt / CellSize;
			
			
			
			
			
			
		} else 
		{
			ViewSize.x = 0;
			ViewSize.y = 0;
			retVal = false;
		}
		return retVal;
	}

	
	
	public void MakeBackup() {
		for (var i = 0; i < UnivSize.x; i++) {
            System.arraycopy(crrState[i], 0, bakState[i], 0, UnivSize.y);
		}
		mjUI.itmRewind.setEnabled(true);
	}

	
	
	public void RestoreBackup() {
		stop();
		for (var i = 0; i < UnivSize.x; i++) {
            System.arraycopy(bakState[i], 0, crrState[i], 0, UnivSize.y);
		}
		Cycle = 0; 
		UpdatePopulation();
		RedrawBoard(true); 
	}

	
	
	private void UpdatePopulation() {


		Population = 0;
		for (var i = 0; i <= MAX_CLO; i++)
			Populations[i] = 0;
		
		for (var iCol = 0; iCol < UnivSize.x; iCol++) {
			for (var iRow = 0; iRow < UnivSize.y; iRow++) {
				var bVal = GetCell(iCol, iRow);
				Populations[bVal]++;
				if (bVal != 0) {
					Population++;
				}
			}
		}
	}

	
	
	private void UpdateScrollbars() {
		
		hSbar.setValues(ViewOrg.x, CellsInView.x, 0, UnivSize.x);
		vSbar.setValues(ViewOrg.y, CellsInView.y, 0, UnivSize.y);
	}

	
	
	public void ValidateBoard() {
		if (ViewOrg.x > UnivSize.x - CellsInView.x)
			ViewOrg.x = UnivSize.x - CellsInView.x;
		if (ViewOrg.y > UnivSize.y - CellsInView.y)
			ViewOrg.y = UnivSize.y - CellsInView.y;
		if (ViewOrg.x < 0)
			ViewOrg.x = 0;
		if (ViewOrg.y < 0)
			ViewOrg.y = 0;
	}

	
	
	public void CenterBoard() {
		var ctrPnt = new Point(0, 0);
		ctrPnt.x = UnivSize.x / 2;
		ctrPnt.y = UnivSize.y / 2;
		ViewOrg.x = ctrPnt.x - (CellsInView.x / 2);
		ViewOrg.y = GameType == MJRules.GAMTYP_2D ? ctrPnt.y - (CellsInView.y / 2) : i1DLastRow;
		ValidateBoard(); 
	}

	
	
	public void Pan(int dx, int dy) {
		ViewOrg.x += dx;
		ViewOrg.y += dy;
		ValidateBoard(); 
		RedrawBoard(true); 
		UpdateScrollbars(); 
	}

	
	
	public Rectangle CalcPatternRect() {
		var rct = new Rectangle(MAX_X, MAX_Y, 0, 0);

		for (var iCol = 0; iCol < UnivSize.x; iCol++)
			for (var iRow = 0; iRow < UnivSize.y; iRow++)
				if (GetCell(iCol, iRow) != 0) {
					if (rct.x > iCol)
						rct.x = iCol;
					if (rct.x + rct.width - 1 < iCol)
						rct.width = iCol - rct.x + 1;
					if (rct.y > iRow)
						rct.y = iRow;
					if (rct.y + rct.height - 1 < iRow)
						rct.height = iRow - rct.y + 1;
				}
		return rct;
	}

	
	
	public void CenterPoint(int ix, int iy, boolean fRedraw) {
		var oldOrg = new Point(ViewOrg);

		CellsInView.x = (LastPanelSize.x - sbarWidth - 1) / CellSize; 
		
		
		CellsInView.y = (LastPanelSize.y - sbarWidth - 1) / CellSize;

		
		ViewOrg.x = ix - (CellsInView.x / 2);
		ViewOrg.y = iy - (CellsInView.y / 2);

		ValidateBoard(); 

		
		if ((fRedraw) && (oldOrg != ViewOrg)) {
			InitBoard(UnivSize.x, UnivSize.y, CellSize);
		}
	}

	
	
	
	public void Fit(boolean fRedraw) {
		var rct = new Rectangle();

		rct = CalcPatternRect();

		if (rct.width >= 0) 
		{
			double facX = LastPanelSize.x / rct.width;
			double facY = LastPanelSize.y / rct.height;

			var fac = facX < facY ? facX : facY;


			int iFac;
			if (fac >= 12)
				iFac = 11;
			else if (fac >= 10)
				iFac = 9;
			else if (fac >= 7)
				iFac = 7;
			else if (fac >= 5)
				iFac = 5;
			else if (fac >= 4)
				iFac = 4;
			else if (fac >= 3)
				iFac = 3;
			else if (fac >= 2)
				iFac = 2;
			else
				iFac = 1;

			CellSize = iFac;


			var iCtrX = rct.x + rct.width / 2;
			var iCtrY = rct.y + rct.height / 2;
			CenterPoint(iCtrX, iCtrY, fRedraw); 
		} else 
		{
			CellSize = 5; 
			CenterBoard();
			if (fRedraw) {
				InitBoard(UnivSize.x, UnivSize.y, CellSize);
			}
		}
        btnZoomIn.setEnabled(CellSize < MAX_CELLSIZE);
		btnZoomOut.setEnabled(CellSize > 1);
	}

	
	
	public void Zoom(boolean fIn) {
		var ctrPnt = new Point(0, 0);
		var orgCellSize = CellSize;
		ctrPnt.x = ViewOrg.x + CellsInView.x / 2;
		ctrPnt.y = ViewOrg.y + CellsInView.y / 2;
		if (fIn) {
			
			if (CellSize >= 20)
				CellSize += 4;
			else if (CellSize >= 11)
				CellSize += 3;
			else if (CellSize >= 5)
				CellSize += 2;
			else
				CellSize++;
			if (CellSize > MAX_CELLSIZE)
				CellSize = MAX_CELLSIZE;
		} else {
			
			if (CellSize > 20)
				CellSize -= 4;
			else if (CellSize > 11)
				CellSize -= 3;
			else if (CellSize > 5)
				CellSize -= 2;
			else
				CellSize--;
			if (CellSize < 1)
				CellSize = 1;
		}
		btnZoomIn.setEnabled(CellSize < MAX_CELLSIZE);
		btnZoomOut.setEnabled(CellSize > 1);
		if (CellSize != orgCellSize) {
			RecalcLayout();
			ViewOrg.x = ctrPnt.x - (CellsInView.x / 2);
			ViewOrg.y = ctrPnt.y - (CellsInView.y / 2);
			InitBoard(UnivSize.x, UnivSize.y, CellSize);
		}
	}

	
	
	public void CellsBigger() {
		Zoom(true);
	}

	
	
	public void CellsSmaller() {
		Zoom(false);
	}

	
	
	@Override
	public boolean handleEvent(Event e) {
		if ((e.target == hSbar) || (e.target == vSbar)) 
		{
			ViewOrg.x = hSbar.getValue();
			ViewOrg.y = vSbar.getValue();
			Pan(0, 0); 
			return true;
		}
		return super.handleEvent(e);
	}

	
	
	@Override
	public boolean action(Event e, Object arg) {
		if (e.target == btnZoomIn) 
		{
			CellsBigger();
		} else if (e.target == btnZoomOut) 
		{
			CellsSmaller();
		} else if (e.target == btnFit) 
		{
			Fit(true);
		}
		return true;
	}

	
	
	@Override
	public void paint(Graphics g) {
		if ((LastPanelSize.x != getSize().width)
				|| (LastPanelSize.y != getSize().height)) {
			InitBoard(UnivSize.x, UnivSize.y, CellSize); 
			
		}
		if (ViewSize.x > 0) {
			offSrs.newPixels();
			g.drawImage(offImg, OfsX, OfsY, null);
		}
	}

	
	
	@Override
	public void update(Graphics g) {
		paint(g);
	}

	
	
	public void RedrawBoard(boolean fDrawAll) {
		var dx = CellSize * CellsInView.x;
		var fDrawGrid = (DrawGrid && (CellSize > 4));
		int iMinY, iMaxY; 

		if (fDrawAll || (GameType == MJRules.GAMTYP_2D)) {
			iMinY = 0;
			iMaxY = CellsInView.y;
		} else {
			iMinY = i1DLastRow - ViewOrg.y; 
			iMaxY = iMinY + 1;
		}

		try {
			for (var i = 0; i < CellsInView.x; i++) {
				var ixCellSize = i * CellSize;
				for (var j = iMinY; j < iMaxY; j++) {
					var newClo = mjPal.Palette[crrState[ViewOrg.x + i][ViewOrg.y
							+ j]];

					if ((fDrawAll)
							|| (screen[(j * CellSize) * dx + i * CellSize] != newClo)) {
						int iTmpCol;
						switch (CellSize) {
						case 1:
							screen[j * dx + ixCellSize] = newClo;
							break;
						case 2:
							screen[iTmpCol = (j * CellSize + 0) * dx
									+ ixCellSize] = newClo;
							screen[++iTmpCol] = newClo;
							screen[iTmpCol = (j * CellSize + 1) * dx
									+ ixCellSize] = newClo;
							screen[++iTmpCol] = newClo;
							break;
						default:
							for (var ic = 0; ic < CellSize; ic++) {
								iTmpCol = (j * CellSize + ic) * dx + ixCellSize;
								for (var jc = 0; jc < CellSize; jc++) {
									if (fDrawGrid
											&& ((ic == CellSize - 1) || (jc == CellSize - 1))) {
										screen[iTmpCol++] = mjPal.GridColor[ic == CellSize - 1 && (ViewOrg.y + j) % 5 == 0
												|| jc == CellSize - 1 && (ViewOrg.x + i) % 5 == 0 ? 1 : 0];
										
										
									} else {
										screen[iTmpCol++] = newClo;
									}
								}
							}
							break;
						}
					}
				}
			}
			repaint(); 
		} catch (Exception exc) {
        }
	}

	
	
	public void setAnimDelay(int newDelay) {
		if (newDelay < 0)
			newDelay = 0;
		if (newDelay > 1000)
			newDelay = 1000;
		AnimDelay = newDelay;
	}

	
	
	public void SetRule(int iGame, String sRuleNam, String sRuleDef) {
		sRuleDef = sRuleDef.trim();
		if (sRuleDef.isEmpty())
			return;

		GameType = MJRules.GAMTYP_2D; 

		
		StringTokenizer st;
		String sTok;
		char cChar;
		int i, iNum = 1;
		int iCharVal;

		CrrGame = iGame;
		switch (CrrGame) {
		case MJRules.GAME_LIFE: 
			RLife.InitFromString(sRuleDef);
			sRuleDef = RLife.GetAsString(); 
			
			break;

		case MJRules.GAME_VOTE: 
			RVote.InitFromString(sRuleDef);
			sRuleDef = RVote.GetAsString(); 
			
			break;

		case MJRules.GAME_GENE: 
			RGene.InitFromString(sRuleDef);
			sRuleDef = RGene.GetAsString(); 
			
			SetStatesCount(RGene.iClo);
			break;

		case MJRules.GAME_WLIF: 
			RWLife.InitFromString(sRuleDef);
			sRuleDef = RWLife.GetAsString(); 
			
			if (RWLife.isHist)
				SetStatesCount(RWLife.iClo);
			break;

		case MJRules.GAME_RTBL: 
			RRtab.InitFromString(sRuleDef);
			sRuleDef = RRtab.GetAsString(); 
			
			SetStatesCount(RRtab.iClo);
			break;

		case MJRules.GAME_CYCL: 
			RCyclic.InitFromString(sRuleDef);
			sRuleDef = RCyclic.GetAsString(); 
			
			SetStatesCount(RCyclic.iClo);
			break;

		case MJRules.GAME_1DTO: 
			GameType = MJRules.GAMTYP_1D; 
			R1DTo.InitFromString(sRuleDef);
			sRuleDef = R1DTo.GetAsString(); 
			
			if (R1DTo.isHist)
				SetStatesCount(R1DTo.iClo);
			break;

		case MJRules.GAME_1DBI: 
			GameType = MJRules.GAMTYP_1D; 
			R1DBin.InitFromString(sRuleDef);
			sRuleDef = R1DBin.GetAsString(); 
			
			break;

		case MJRules.GAME_NMBI:
			RNeumBin.InitFromString(sRuleDef);
			sRuleDef = RNeumBin.GetAsString(); 
			
			SetStatesCount(RNeumBin.iClo);
			break;

		case MJRules.GAME_GEBI: 
			RGenBin.InitFromString(sRuleDef);
			sRuleDef = RGenBin.GetAsString(); 
			
			SetStatesCount(RGenBin.iClo);
			break;

		case MJRules.GAME_LGTL:
			RLgtL.InitFromString(sRuleDef);
			sRuleDef = RLgtL.GetAsString(); 
			
			if (RLgtL.isHist)
				SetStatesCount(RLgtL.iClo);
			break;

		case MJRules.GAME_MARG:
			RMarg.InitFromString(sRuleDef);
			sRuleDef = RMarg.GetAsString(); 
			
			if (RMarg.isHist)
				SetStatesCount(RMarg.iClo);
			break;

		case MJRules.GAME_USER:
			RUser.InitFromString(sRuleDef);
			sRuleDef = RUser.GetAsString(); 
			
			SetStatesCount(RUser.iClo);
			break;
		}
		RuleName = sRuleNam; 
		RuleDef = sRuleDef; 
		mjUI.UpdateColorsUI();
	} 

	
	
	@Override
	public boolean mouseDown(Event p1, int p2, int p3) {
		if ((p2 >= OfsX) && (p3 >= OfsY)
				&& (p2 <= OfsX + UnivSize.x * CellSize)
				&& (p3 <= OfsY + UnivSize.y * CellSize)) {
			lastX = (p2 - OfsX) / CellSize + ViewOrg.x;
			lastY = (p3 - OfsY) / CellSize + ViewOrg.y;

			
			
			
			
			SetCell(lastX, lastY, (short) CrrState); 
			RedrawBoard(false); 
			mjUI.UpdateUI(); 
			return true;
		} else {
			return super.mouseDown(p1, p2, p3);
		}
	}

	
	
	@Override
	public boolean mouseDrag(Event p1, int p2, int p3) {
		if ((p2 >= OfsX) && (p3 >= OfsY)
				&& (p2 <= OfsX + UnivSize.x * CellSize)
				&& (p3 <= OfsY + UnivSize.y * CellSize)) {
			var x = (p2 - OfsX) / CellSize + ViewOrg.x;
			var y = (p3 - OfsY) / CellSize + ViewOrg.y;
			DrawLine(lastX, lastY, x, y);
			lastX = x;
			lastY = y;
			RedrawBoard(false); 
			mjUI.UpdateUI(); 
			return true;
		} else {
			return super.mouseDrag(p1, p2, p3);
		}
	}

	
	
	public void DrawLine(int x1, int y1, int x2, int y2) {
		int shortDiff, longDiff;
		boolean across;

		var xDiff = Math.abs(x2 - x1);
		var yDiff = Math.abs(y2 - y1);

		if (xDiff > yDiff) {
			across = true;
			shortDiff = yDiff;
			longDiff = xDiff;
		} else {
			across = false;
			shortDiff = xDiff;
			longDiff = yDiff;
		}

		var xRight = x2 > x1 ? 1 : -1;
		var yDown = y2 > y1 ? 1 : -1;

		var j = 0;
		var wrap = 0;

		for (var i = 0; i < longDiff; i++) {
			int y;
			int x;
			if (across) {
				x = x1 + (i * xRight);
				y = y1 + (j * yDown);
			} else {
				x = x1 + (j * xRight);
				y = y1 + (i * yDown);
			}

			SetCell(x, y, (short) CrrState); 
			wrap += shortDiff;
			if (wrap >= longDiff) {
				j++;
				wrap %= longDiff;
			}
		}
		SetCell(x2, y2, (short) CrrState); 
	}

	
	
	public void RandomizeOneCell(int x, int y, double maxVal) {

		if (mjUI.chkMon.getState()) 
		{
			if (Math.random() <= maxVal)
				SetCell(x, y, (short) CrrState); 
		} else 
		{
			short newStt;
			if (mjUI.chkUni.getState())
			{
				newStt = (short) (Math.ceil(Math.random() * StatesCount) - 1);
				SetCell(x, y, newStt); 
			} else 
			{
				if (Math.random() <= maxVal) {
					newStt = (short) Math.ceil(Math.random()
							* (StatesCount - 1));
					SetCell(x, y, newStt); 
				}
			}
		}
	}

	
	public static final int RAND_ALL = 1;

	public static final int RAND_VIEW = 2;

	
	
	public void Randomize(String sHow, int what) {
		var fOldRun = (caThread != null);
		stop();
		try {
			Thread.sleep(300);
		} catch (InterruptedException e) {
		}

		int maxY;
		int minY;
		int maxX;
		int minX;
		switch (what) {
		case RAND_VIEW:
			minX = ViewOrg.x;
			maxX = ViewOrg.x + CellsInView.x - 1;
			minY = ViewOrg.y;
			maxY = ViewOrg.y + CellsInView.y - 1;
			break;
		case RAND_ALL:
		default:
			minX = 0;
			maxX = UnivSize.x - 1;
			minY = 0;
			maxY = UnivSize.y - 1;
			break;
		}
		sHow = sHow.substring(0, sHow.length() - 1);
		var i = Integer.parseInt(sHow.trim());
		var maxVal = i / 100.0;
		if (!mjUI.chkAdd.getState()) 
		{
			Clear(false);
		}

		if (GameType == MJRules.GAMTYP_2D) {
			for (i = minX; i <= maxX; i++)
				for (var j = minY; j <= maxY; j++)
					RandomizeOneCell(i, j, maxVal);
		} else 
		{
			for (i = minX; i <= maxX; i++)
				RandomizeOneCell(i, i1DLastRow, maxVal);
		}
		mjUI.vDescr.clear(); 
		RedrawBoard(true); 
		MakeBackup(); 
		if (fOldRun)
			start();
		Cycle = 0; 
	}

	
	
	public void Seed(String sHow) {
		var fOldRun = caThread != null;
		stop();
		try {
			Thread.sleep(300);
		} catch (InterruptedException e) {
		}

		if (!mjUI.chkAdd.getState()) 
		{
			Clear(false);
		}

		var ctrX = UnivSize.x / 2;
		var ctrY = UnivSize.y / 2;
		int j;
		int i;
		if (sHow.startsWith("BLK"))
		{
			sHow = sHow.substring(3).trim();
			var iPos = sHow.indexOf('x');
			if (iPos >= 0) {
				int dx = Integer.valueOf(sHow.substring(0, iPos));
				int dy = Integer.valueOf(
                        sHow.substring(iPos + 1));
				if (GameType == MJRules.GAMTYP_2D) {
					for (i = 0; i < dx; i++)
						for (j = 0; j < dy; j++)
							SetCell(ctrX - (dx / 2) + i, ctrY - (dy / 2) + j,
									(short) CrrState); 
				} else 
				{
					for (i = 0; i < dx; i++)
						SetCell(ctrX - (dx / 2) + i, i1DLastRow,
								(short) CrrState); 
				}
			}
		} else if (sHow.startsWith("FRM")) 
		{
			sHow = sHow.substring(3).trim();
			var iPos = sHow.indexOf('x');
			if (iPos >= 0) {
				int dx = Integer.valueOf(sHow.substring(0, iPos));
				int dy = Integer.valueOf(
                        sHow.substring(iPos + 1));
				if (GameType == MJRules.GAMTYP_2D) {
					for (i = 0; i < dx; i++) {
						SetCell(ctrX - (dx / 2) + i, ctrY - (dy / 2),
								(short) CrrState); 
						SetCell(ctrX - (dx / 2) + i, ctrY - (dy / 2) + dy - 1,
								(short) CrrState); 
					}
					for (j = 1; j < dy - 1; j++) {
						SetCell(ctrX - (dx / 2), ctrY - (dy / 2) + j,
								(short) CrrState); 
						SetCell(ctrX - (dx / 2) + dx - 1, ctrY - (dy / 2) + j,
								(short) CrrState); 
					}
				} else 
				{
					SetCell(ctrX - (dx / 2), i1DLastRow, (short) CrrState); 
					
					
					SetCell(ctrX + (dx / 2) - 1, i1DLastRow, (short) CrrState); 
					
					
				}
			}
		}
		CenterBoard(); 
		mjUI.vDescr.clear(); 
		RedrawBoard(true); 
		MakeBackup(); 
		UpdateScrollbars(); 
		if (fOldRun)
			start();
		Cycle = 0; 
	}

	
	
	public void Clear(boolean fRedraw) {
		stop();
		try {
			Thread.sleep(300);
		} catch (InterruptedException e) {
		}
		int i;
		for (i = 0; i <= MAX_X; ++i)
			for (var j = 0; j <= MAX_Y; ++j)
				SetCell(i, j, (short) 0);

		
		Population = 0;
		Populations[0] = UnivSize.x * UnivSize.y; 
		for (i = 1; i <= MAX_CLO; i++)
			Populations[i] = 0;
		Cycle = 0; 
		i1DLastRow = 0; 
		if (fRedraw)
			RedrawBoard(true); 
	}

	
	
	public void SetStatesCount(int iSttCnt) {
		if ((iSttCnt >= 2) && (iSttCnt <= MAX_CLO + 1)) {
			StatesCount = iSttCnt;
			
			mjPal.ActivatePalette(mjPal.PalName, StatesCount);
			mjUI.UpdateColorsUI();
		}
	}

	
	
	public void SetCrrState(int iCrrState) {
		if ((iCrrState >= 0) && (iCrrState < StatesCount)) {
			CrrState = iCrrState;
			mjUI.UpdateColorsUI();
		}
	}
	
	
}