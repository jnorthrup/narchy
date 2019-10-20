package nars.ca;




import java.util.List;
import java.util.StringTokenizer;
import java.util.Vector;

public class MJOpen {
	private final MJCellUI mjUI;
	private final MJBoard mjb;

	private List<CACell> m_vCells; 
	private List<String> m_vDescr; 
	private List<String> m_vDiv; 

	private String m_sGame; 
	private String m_sRules; 
	private int m_rectMinX;
    private int m_rectMaxX;
    private int m_rectMinY;
    private int m_rectMaxY;
	private int m_Speed; 
	private int m_BSizeX;
    private int m_BSizeY;
	private int m_Wrap; 
	private int m_CColors; 
	private int m_Coloring; 
	private String m_sPalette; 
	private double m_dMclVsn; 

	
	
	public MJOpen(MJCellUI cmjui, MJBoard cmjb) {
		mjUI = cmjui;
		mjb = cmjb;
	}

	
	
	@SuppressWarnings("HardcodedFileSeparator")
	public boolean OpenFile(String sFileName) {

		m_vCells = new Vector();
		m_vDescr = new Vector();
		m_vDiv = new Vector(); 

		m_sRules = "";
		m_sGame = MJRules.GAME_LIFE_Name; 
		m_Speed = -1; 
		m_BSizeX = -1; 
		m_BSizeY = m_BSizeX;
		m_Wrap = -1; 
		m_CColors = -1; 
		m_Coloring = -1; 
		m_sPalette = ""; 
		m_dMclVsn = 0; 

		sFileName = CorrectFileName(sFileName);
		var sFilePath = mjUI.sBaseURL + "p/" + sFileName;


		var mjT = new MJTools();
		var vLines = new Vector();
		var fOk = false;
		if (MJTools.LoadTextFile(sFilePath, vLines))
		{
			if (!vLines.isEmpty()) 
			{

				var i = 0;
				var sFirstLine = "";
				while (i < vLines.size()) {
					var sBff = (String) vLines.elementAt(i);
					if ((sBff.isEmpty()) || (sBff.startsWith("#C"))
							|| (sBff.startsWith("#D"))
							|| (sBff.length() > 0 && sBff.charAt(0) == '!')) {
						i++; 
					} else {
						sFirstLine = sBff;
						i = vLines.size(); 
					}
				}

				
				if (sFirstLine.startsWith("#Life 1.05") || sFirstLine.startsWith("#Life 1.02") || sFirstLine.startsWith("#P") || sFirstLine.startsWith("#MCLife"))
					fOk = ReadLife105(vLines);
				else if (sFirstLine.startsWith("#MCell") || sFirstLine.startsWith("#Life 1.06"))
					fOk = ReadMCL(vLines);
				else if (sFirstLine.startsWith("#Life 1.05b") || sFirstLine.length() > 0 && sFirstLine.charAt(0) == 'x')
					fOk = ReadLife106(vLines);

				if (!fOk)
					fOk = ReadLife105(vLines);
				if (!fOk)
					fOk = ReadLife106(vLines);
				if (!fOk)
					fOk = ReadMCL(vLines);
				if (!fOk)
					fOk = ReadRLE(vLines);

				if (fOk) 
				{
					if (!m_vCells.isEmpty()) 
						AddPattern();
				} else {
					System.out
							.println("Unrecognized file format: " + sFilePath);
				}
			} else {
				System.out.println("Empty pattern file: " + sFilePath);
			}
		}

		return fOk;
	}

	
	
	private static String CorrectFileName(String sFileName) {
		var sNew = sFileName.replace(' ', '_');
		sNew = sNew.replace('\'', '_');
		return sNew;
	}

	
	
	private void AddPattern() {
		int minX, maxX, minY, maxY;
		int state;
		var cell = new CACell();

		
		mjUI.vDescr.clear();
		mjUI.vDescr = m_vDescr;

		
		CalcMinRectangle();
		var sizeX = m_rectMaxX - m_rectMinX;
		var sizeY = m_rectMaxY - m_rectMinY;
		if ((m_BSizeX > 0) && (m_BSizeY > 0)) 
		{
			mjb.SetBoardSize(m_BSizeX, m_BSizeY);
		} else 
		{
			mjb.InitBoard(sizeX + 120, sizeY + 120, mjb.CellSize);
		}

		
		mjb.Div.m_Enabled = false;
		int i;
		for (i = 0; i < m_vDiv.size(); i++)
			mjb.Div.ItemFromString(m_vDiv.get(i),
					mjb.UnivSize.x, mjb.UnivSize.y);

		
		mjb.Clear(false);
		var dx = mjb.UnivSize.x / 2 - (m_rectMaxX + m_rectMinX) / 2 - 1;
		var dy = mjb.GameType == MJRules.GAMTYP_2D ? mjb.UnivSize.y / 2 - (m_rectMaxY + m_rectMinY) / 2 - 1 : 0;
		for (i = 0; i < m_vCells.size(); i++) {
			cell = m_vCells.get(i);
			var x = cell.x + dx;
			var y = cell.y + dy;
			mjb.SetCell(x, y, cell.state);
		}

		
		if (m_Wrap >= 0)
			mjUI.SetWrapping(m_Wrap != 0);

		
		if (m_CColors >= 2)
			mjb.SetStatesCount(m_CColors);

		
		if (m_Coloring > 0)
			mjUI.SetColoringMethod(m_Coloring);

		
		if (!m_sPalette.isEmpty())
			mjUI.SetColorPalette(m_sPalette);

		
		if (!m_sRules.isEmpty()) {
			mjb.SetRule(MJRules.GetGameIndex(m_sGame), "", m_sRules);
		}

		mjb.RedrawBoard(true);
		mjUI.UpdateUI();
		mjUI.UpdateColorsUI();
		mjb.MakeBackup(); 
		
	}

	
	
	private void CalcMinRectangle() {
		var cell = new CACell();
		m_rectMinX = m_rectMinY = 999999;
		m_rectMaxX = m_rectMaxY = -999999;

		for (var m_vCell : m_vCells) {
			cell = m_vCell;

			if (m_rectMinX > cell.x)
				m_rectMinX = cell.x;
			if (m_rectMaxX < cell.x)
				m_rectMaxX = cell.x;
			if (m_rectMinY > cell.y)
				m_rectMinY = cell.y;
			if (m_rectMaxY < cell.y)
				m_rectMaxY = cell.y;
		}
	}

	
	
	
	
	private boolean ReadLife105(Vector vLines) {

		iBlkX = 0;
		iBlkY = 0;
		iRow105 = 0;


		var fOk = false;
		for (var i = 0; i < vLines.size(); i++) {
			var bff = (String) vLines.elementAt(i);
			if (ProcessOneLIF105Line(bff))
				fOk = true; 
		}

		return fOk;
	}

	
	
	
	int iRow105;
	int iBlkX;
    int iBlkY;

	@SuppressWarnings("HardcodedFileSeparator")
	boolean ProcessOneLIF105Line(String bff) {
		int iPos;

		bff = bff.trim();

		var fOk = false;
		if (!bff.isEmpty()) {
			
			if ((bff.charAt(0) == '#') || (bff.charAt(0) == '!')
					|| (bff.charAt(0) == '/')) {

				String sTok;
				if (bff.startsWith("#P"))
				{

					var st = new StringTokenizer(bff);
					st.nextToken(); 
					iRow105 = 0;
					if (st.hasMoreTokens())
						iBlkX = Integer.parseInt(st.nextToken());
					if (st.hasMoreTokens())
						iBlkY = Integer.parseInt(st.nextToken());
				} else if (bff.startsWith("#N")) 
				{
					m_sRules = "23/3"; 
				} else if (bff.startsWith("#R")) 
				{

					var st = new StringTokenizer(bff);
					st.nextToken(); 
					if (st.hasMoreTokens())
						m_sRules = st.nextToken();
				} else if (bff.startsWith("#S")) 
				{

					var st = new StringTokenizer(bff);
					st.nextToken(); 
					if (st.hasMoreTokens())
						m_Speed = Integer.parseInt(st.nextToken());
				} else if (bff.startsWith("#D") || bff.startsWith("#C")) 
				{
					sTok = bff.substring(2);
					if (!sTok.isEmpty()) 
						if (sTok.charAt(0) == ' ')
							sTok = sTok.substring(1);
					m_vDescr.add(sTok); 
				} else if (bff.length() > 0 && bff.charAt(0) == '!') 
				{
					sTok = bff.substring(1);
					if (!sTok.isEmpty()) 
						if (sTok.charAt(0) == ' ')
							sTok = sTok.substring(1);
					m_vDescr.add(sTok); 
				}
			} else 
			{
				var iCol = 0;
				var iNum = 0;
				for (var i = 0; i < bff.length(); i++) {
					if ((bff.charAt(i) >= '0') && (bff.charAt(i) <= '9')) {
						iNum = iNum * 10 + (bff.charAt(i) - '0');
					} else {
						if (iNum == 0)
							iNum = 1;
						
						if ((bff.charAt(i) == '*') || (bff.charAt(i) == 'o')
								|| (bff.charAt(i) == 'O')) {
							for (var j = 0; j <= iNum - 1; j++)
								m_vCells.add(new CACell(
                                        iCol + j + iBlkX, iRow105 + iBlkY,
                                        (short) 1));
							fOk = true;
                            iCol += iNum;
						} else 
						{
                            iCol += iNum;
						}
						iNum = 0;
					}
				} 
				iRow105++;
			}
		}
		return fOk;
	}

	
	
	
	private int iIniColMCL;
	private int iColMCL;
    private int iRowMCL;
	private int iNumMCL; 

	private boolean ReadMCL(Vector vLines) {

		iColMCL = 0;
		iRowMCL = 0;
		iNumMCL = 0;
		iIniColMCL = 0;


		var bff = (String) vLines.elementAt(0);
		if (bff.startsWith("#MCLife ")) 
		{
			bff = bff.substring(8);
			m_dMclVsn = Double.valueOf(bff);
		}
		if (bff.startsWith("#MCell ")) 
		{
			bff = bff.substring(7);
			m_dMclVsn = Double.valueOf(bff);
		}

		
		m_sRules = "";
		var fOk = false;
		for (var i = 0; i < vLines.size(); i++) {
			bff = (String) vLines.elementAt(i);
			if (ProcessOneMCLLine(bff))
				fOk = true; 
		}

		return fOk;
	}

	
	
	
	
	@SuppressWarnings("HardcodedFileSeparator")
	private boolean ProcessOneMCLLine(String bff) {

		bff = bff.trim();

		var fOk = false;
		if (!bff.isEmpty()) {

			String sTok;
			if (bff.startsWith("#RULE"))
			{
				sTok = bff.substring(5);

                m_sRules += sTok.trim();
				fOk = true; 
			} else if (bff.startsWith("#GAME")) 
			{
				m_sGame = bff.substring(5).trim();
				fOk = true; 
			} else if (bff.startsWith("#DIV")) 
			{
				sTok = bff.substring(4).trim();
				m_vDiv.add(sTok);
			} else if (bff.startsWith("#D")) 
			{
				sTok = bff.substring(2);
				if (!sTok.isEmpty()) 
					if (sTok.charAt(0) == ' ')
						sTok = sTok.substring(1);
				m_vDescr.add(sTok); 
			} else if (bff.startsWith("#BOARD")) 
			{
				sTok = bff.substring(6).trim();

				var st = new StringTokenizer(sTok, "x", false);
				if (st.hasMoreTokens()) {
					var sTmp = st.nextToken();
					m_BSizeX = Integer.valueOf(sTmp);
					if (st.hasMoreTokens()) {
						sTmp = st.nextToken();
						m_BSizeY = Integer.valueOf(sTmp);
					}
				}
			} else if (bff.startsWith("#SPEED")) 
			{
				sTok = bff.substring(6).trim();
				m_Speed = Integer.valueOf(sTok);
			} else if (bff.startsWith("#WRAP")) 
			{
				sTok = bff.substring(5).trim();
				m_Wrap = Integer.valueOf(sTok);
			} else if (bff.startsWith("#CCOLORS")) 
			{
				sTok = bff.substring(8).trim();
				m_CColors = Integer.valueOf(sTok);
			} else if (bff.startsWith("#COLORING")) 
			{
				sTok = bff.substring(9).trim();
				m_Coloring = Integer.valueOf(sTok);
			} else if (bff.startsWith("#PALETTE")) 
			{
				sTok = bff.substring(8).trim();
				m_sPalette = sTok;
			} else if (bff.startsWith("#L")) 
			{
				bff = bff.substring(2).trim();
				var iAdd = 0;
				for (var i = 0; i < bff.length(); i++) {
					if ((bff.charAt(i) >= '0') && (bff.charAt(i) <= '9')) {
						iNumMCL = iNumMCL * 10 + (bff.charAt(i) - '0');
					} else {
						if (iNumMCL == 0)
							iNumMCL = 1;
						switch (bff.charAt(i)) {
						case '$':
                            iRowMCL += iNumMCL;
							iColMCL = iIniColMCL;
							iNumMCL = 0;
							break;

						case '.':
                            iColMCL += iNumMCL;
							iNumMCL = 0;
							break;

						default:
							if ((bff.charAt(i) >= 'a')
									&& (bff.charAt(i) <= 'j')) {
								
								iAdd = (bff.charAt(i) - 'a' + 1) * 24;
							} else if ((bff.charAt(i) >= 'A')
									&& (bff.charAt(i) <= 'X')) {
								
								for (var j = 0; j < iNumMCL; j++) {
									m_vCells
											.add(new CACell(
                                                    iColMCL + j,
                                                    iRowMCL,
                                                    (short) (bff.charAt(i) - 'A' + 1 + iAdd)));
								}
                                iColMCL += iNumMCL;
								fOk = true; 
								iAdd = 0;
								iNumMCL = 0;
							} else {
								iNumMCL = 0;
							}
							break;
						}
					}
				}
			} 
			else if (bff.startsWith("#N")) 
			{
				m_sGame = MJRules.GAME_LIFE_Name;
				m_sRules = "23/3"; 
			}
		} 

		return fOk;
	}

	
	
	
	private boolean fEndFlg; 
	private boolean fXYFound; 
	private int iCol;
    private int iRow;
    private int iniCol;
	private int iNum; 

	private boolean ReadRLE(Vector vLines) {

		iCol = 0;
		iRow = 0;
		iNum = 0;
		iniCol = 0;
		fEndFlg = false;
		fXYFound = false;


		var fOk = false;
		for (var i = 0; i < vLines.size(); i++) {
			var bff = (String) vLines.elementAt(i);
			if (ProcessOneRLELine(bff))
				fOk = true; 
		}
		return fOk;
	}

	
	
	
	private boolean ProcessOneRLELine(String bff) {

		bff = bff.trim();

		var fOk = false;
		if (bff.startsWith("#D") || bff.startsWith("#C"))
		{
			var sTok = bff.substring(2);
			if (!sTok.isEmpty()) 
				if (sTok.charAt(0) == ' ')
					sTok = sTok.substring(1);
			m_vDescr.add(sTok); 
		} else {
			if (fEndFlg) 
			{
				m_vDescr.add(bff); 
			} else {
				if (!bff.isEmpty()) {
					if ((!fXYFound) && bff.length() > 0 && bff.charAt(0) == 'x') 
					{
						fXYFound = true;
						fOk = true;


						var stcomma = new StringTokenizer(bff, ",");
						while (stcomma.hasMoreTokens()) {
							var t = stcomma.nextToken();

							var stequal = new StringTokenizer(t,
									"= ");
							var tokenType = stequal.nextToken();
							var tokenValue = stequal.nextToken();

                            switch (tokenType) {
                                case "x":

                                    iniCol = iCol = -(Math.abs(Integer
                                            .parseInt(tokenValue)) / 2);
                                    break;
                                case "y":

                                    iRow = -(Math.abs(Integer.parseInt(tokenValue)) / 2);
                                    break;
                                case "rule":
                                case "rules":
                                    m_sRules = tokenValue;
                                    break;
                                case "skip":
                                    
                                    break;
                                case "fps":
                                    m_Speed = Integer.parseInt(tokenValue);
                                    break;
                            }
						}
					} else 
					{
						for (var i = 0; (i < bff.length()) && (!fEndFlg); i++) {
							if ((bff.charAt(i) >= '0')
									&& (bff.charAt(i) <= '9')) {
								iNum = iNum * 10 + (bff.charAt(i) - '0');
							} else {
								if (iNum == 0)
									iNum = 1;
								switch (bff.charAt(i)) {
								case '$':
                                    iRow += iNum;
									iCol = iniCol;
									break;

								case 'b':
								case 'B':
								case '.':
                                    iCol += iNum;
									break;

								case '!': 
									fEndFlg = true;
									break;

								default: 
									if (((bff.charAt(i) >= 'a') && (bff
											.charAt(i) <= 'z'))
											|| ((bff.charAt(i) >= 'A') && (bff
													.charAt(i) <= 'Z'))) {
										int iTmp;
										switch (bff.charAt(i)) {
										case 'x':
										case 'X':
											iTmp = 2;
											break;

										case 'y':
										case 'Y':
											iTmp = 3;
											break;

										case 'z':
										case 'Z':
											iTmp = 4;
											break;

										default:
											iTmp = 1;
											break;
										}
										for (var j = 0; j <= iNum - 1; j++)
											m_vCells.add(new CACell(iCol
                                                    + j, iRow, (short) iTmp));

                                        iCol += iNum;
										fOk = true; 
									}
									break;
								}
								iNum = 0;
							} 
						} 
					} 
				} 
			} 
		}
		return fOk;
	}

	
	
	
	
	boolean ReadLife106(Vector vLines) {

		iCol = 0;
		iRow = 0;
		iNum = 0;
		iniCol = 0;
		fEndFlg = false;
		fXYFound = false;


		var fOk = false;
		for (var i = 0; i < vLines.size(); i++) {
			var bff = (String) vLines.elementAt(i);
			if (ProcessOneLIF106Line(bff))
				fOk = true; 
		}

		return fOk;
	}

	
	
	
	@SuppressWarnings("HardcodedFileSeparator")
	boolean ProcessOneLIF106Line(String bff) {
		int iPos;

		bff = bff.trim();

		var fOk = false;
		if (!bff.isEmpty()) {
			
			if ((bff.charAt(0) == '#') || (bff.charAt(0) == '/')
					|| (bff.charAt(0) == '!')) {

				String sTok;
				if (bff.startsWith("#N"))
				{
					m_sRules = "23/3"; 
				} else if (bff.startsWith("#R")) 
				{

					var st = new StringTokenizer(bff);
					st.nextToken(); 
					if (st.hasMoreTokens())
						m_sRules = st.nextToken();
				} else if (bff.startsWith("#S")) 
				{

					var st = new StringTokenizer(bff);
					st.nextToken(); 
					if (st.hasMoreTokens())
						m_Speed = Integer.parseInt(st.nextToken());
				} else if (bff.startsWith("#D") || bff.startsWith("#C")) {
					
					sTok = bff.substring(2);
					if (!sTok.isEmpty()) 
						if (sTok.charAt(0) == ' ')
							sTok = sTok.substring(1);
					m_vDescr.add(sTok); 
				} else if (bff.length() > 0 && bff.charAt(0) == '!') {
					
					sTok = bff.substring(1);
					if (!sTok.isEmpty()) 
						if (sTok.charAt(0) == ' ')
							sTok = sTok.substring(1);
					m_vDescr.add(sTok); 
				}
			} else 
			{

				var st = new StringTokenizer(bff);
				if (st.hasMoreTokens()) {
					var iCol = Integer.parseInt(st.nextToken());
					if (st.hasMoreTokens()) {
						var iRow = Integer.parseInt(st.nextToken());
						m_vCells.add(new CACell(iCol, iRow, (short) 1));
						fOk = true;
					}
				}
			}
		}
		return fOk;
	}
	

}