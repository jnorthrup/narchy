package nars.ca;




import java.util.StringTokenizer;

public class RuleRTab {
	public int iNghTyp; 
	public boolean fCtrCell; 
	public boolean fAll1Fire; 
	public int iClo; 
	public final int[][] table = new int[MJBoard.MAX_CLO + 1][10]; 
																	

	
	public RuleRTab() {
		ResetToDefaults();
	}

	
	
	public void ResetToDefaults() {
		int iS, iN;
		iNghTyp = MJRules.NGHTYP_MOOR; 
		fCtrCell = true; 
		fAll1Fire = false; 

		for (iS = 0; iS <= MJBoard.MAX_CLO; iS++)
			
			for (iN = 0; iN <= 9; iN++)
				
				table[iS][iN] = 0;
	}

	
	
	public void InitFromString(String sStr) {
		int i_Stt, i_Ngh, iNum, iTmp;
		
		StringTokenizer st;
		String sTok;
		ResetToDefaults();

		if (sStr.length() > 6) {
			iNum = 0;
			st = new StringTokenizer(sStr, " ,", true);
			while (st.hasMoreTokens()) {
				sTok = st.nextToken();
				if (sTok.compareTo(",") != 0) {
					iNum++;
					iTmp = Integer.valueOf(sTok);
					switch (iNum) {
						case 1 : 
							iNghTyp = iTmp == 2
									? MJRules.NGHTYP_NEUM
									: MJRules.NGHTYP_MOOR;
							break;
						case 2 : 
							fCtrCell = (iTmp == 1); 
							break;
						case 3 : 
							fAll1Fire = (iTmp == 1); 
														
							break;
						default : 
							if (iTmp < 0)
								iTmp = 0;
							if (iTmp > MJBoard.MAX_CLO)
								iTmp = MJBoard.MAX_CLO;
							i_Stt = (iNum - 4) / 10;
							i_Ngh = (iNum - 4) % 10;
							table[i_Stt][i_Ngh] = iTmp;
							iClo = i_Stt + 2;
							break;
					}
				}
			}
		}
		Validate(); 
	}

	
	
	public String GetAsString() {
		String sBff = "";

		
		Validate();

		
		sBff = iNghTyp == MJRules.NGHTYP_NEUM ? "2" : "1";

		sBff = sBff + (fCtrCell ? ",1" : ",0");

		sBff = sBff + (fAll1Fire ? ",1" : ",0");

		int i_Stt, i_Ngh, iTmp;
		for (i_Stt = 0; i_Stt <= MJBoard.MAX_CLO; i_Stt++) 
		{
			for (i_Ngh = 0; i_Ngh <= 9; i_Ngh++) 
			{
				iTmp = table[i_Stt][i_Ngh];
				if (iTmp < 0)
					iTmp = 0;
				if (iTmp > MJBoard.MAX_CLO)
					iTmp = MJBoard.MAX_CLO;
				sBff = sBff + ',' + iTmp;
			}
		}

		
		while ((sBff.length() > 2) && (sBff.endsWith(",0"))) {
			sBff = sBff.substring(0, sBff.length() - 3);
		}

		return sBff;
	}

	
	
	
	public void Validate() {
		table[0][0] = 0; 

		if (iClo < 2)
			iClo = 2;
		else if (iClo > MJBoard.MAX_CLO)
			iClo = MJBoard.MAX_CLO;
	}

	
	
	public int OnePass(int sizX, int sizY, boolean isWrap, int ColoringMethod,
			short[][] crrState, short[][] tmpState, MJBoard mjb) {
		short bOldVal, bNewVal;
		int modCnt = 0;
		int i, j, iCnt;
		int[] lurd = new int[4];
        boolean fMoore = (iNghTyp == MJRules.NGHTYP_MOOR);


        int rtMask = fAll1Fire ? 1 : 0xFF;

		for (i = 0; i < sizX; ++i) {
			
			lurd[0] = (i > 0) ? i - 1 : (isWrap) ? sizX - 1 : sizX;
			lurd[2] = (i < sizX - 1) ? i + 1 : (isWrap) ? 0 : sizX;
			for (j = 0; j < sizY; ++j) {
				
				lurd[1] = (j > 0) ? j - 1 : (isWrap) ? sizY - 1 : sizY;
				lurd[3] = (j < sizY - 1) ? j + 1 : (isWrap) ? 0 : sizY;
				bOldVal = crrState[i][j];

				iCnt = 0; 
				if (fMoore && ((crrState[lurd[0]][lurd[1]] & rtMask) == 1))
					iCnt++;
				if ((crrState[lurd[0]][j] & rtMask) == 1)
					iCnt++;
				if (fMoore && ((crrState[lurd[0]][lurd[3]] & rtMask) == 1))
					iCnt++;

				if ((crrState[i][lurd[1]] & rtMask) == 1)
					iCnt++;
				if (fCtrCell && ((crrState[i][j] & rtMask) == 1))
					iCnt++;
				if ((crrState[i][lurd[3]] & rtMask) == 1)
					iCnt++;

				if (fMoore && ((crrState[lurd[2]][lurd[1]] & rtMask) == 1))
					iCnt++;
				if ((crrState[lurd[2]][j] & rtMask) == 1)
					iCnt++;
				if (fMoore && ((crrState[lurd[2]][lurd[3]] & rtMask) == 1))
					iCnt++;

				bNewVal = (short) table[bOldVal][iCnt]; 

				tmpState[i][j] = bNewVal;
				if (bNewVal != bOldVal) {
					modCnt++; 
				}
			}
		}

		return modCnt;
	}
	
}