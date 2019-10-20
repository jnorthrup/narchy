package nars.ca;




import java.util.StringTokenizer;

public class RuleWLife {
	public static final int IMAXWLIFEVAL = 256;
	public static final int IMAXWLIFERUL = 8 * IMAXWLIFEVAL;
	public int iClo; 
	public final int[] wgtAry = new int[10]; 
	public final boolean[] rulesS = new boolean[IMAXWLIFERUL + 1]; 
																	
	public final boolean[] rulesB = new boolean[IMAXWLIFERUL + 1]; 
																	
	public boolean isHist; 

	
	public RuleWLife() {
		ResetToDefaults();
	}

	
	
	public void ResetToDefaults() {
		int i;

		for (i = 1; i <= 9; i++)
			wgtAry[i] = 1;
		wgtAry[5] = 0; 
		isHist = true; 

		for (i = 0; i < IMAXWLIFERUL; i++) {
			rulesS[i] = false;
			rulesB[i] = false;
		}
	}

	
	
	
	public void InitFromString(String sStr) {

        ResetToDefaults();

		var st = new StringTokenizer(sStr, " ,", true);
		while (st.hasMoreTokens()) {
			var sTok = st.nextToken().toUpperCase();


            int i;
            if (sTok.startsWith("NW"))
				wgtAry[1] = Integer.valueOf(sTok.substring(2));
			else if (sTok.startsWith("NN"))
				wgtAry[2] = Integer.valueOf(sTok.substring(2));
			else if (sTok.startsWith("NE"))
				wgtAry[3] = Integer.valueOf(sTok.substring(2));

			else if (sTok.startsWith("WW"))
				wgtAry[4] = Integer.valueOf(sTok.substring(2));
			else if (sTok.startsWith("ME"))
				wgtAry[5] = Integer.valueOf(sTok.substring(2));
			else if (sTok.startsWith("EE"))
				wgtAry[6] = Integer.valueOf(sTok.substring(2));

			else if (sTok.startsWith("SW"))
				wgtAry[7] = Integer.valueOf(sTok.substring(2));
			else if (sTok.startsWith("SS"))
				wgtAry[8] = Integer.valueOf(sTok.substring(2));
			else if (sTok.startsWith("SE"))
				wgtAry[9] = Integer.valueOf(sTok.substring(2));

			else if (sTok.startsWith("HI")) {
				i = Integer.valueOf(sTok.substring(2));
				if (i >= 3) {
					isHist = true; 
					iClo = i;
				} else
					isHist = false; 
			} else if (sTok.startsWith("RS")) 
			{
				i = Integer.valueOf(sTok.substring(2));
				if ((i >= 0) && (i <= IMAXWLIFERUL))
					rulesS[i] = true;
			} else if (sTok.startsWith("RB")) 
			{
				i = Integer.valueOf(sTok.substring(2));
				if ((i > 0) && (i <= IMAXWLIFERUL))
					rulesB[i] = true;
			}
		}

		Validate(); 
	}

	
	
	public String GetAsString() {


        Validate();

		var ih = isHist ? iClo : 0;

		var sBff = "NW" + wgtAry[1] + ",NN" + wgtAry[2] + ",NE" + wgtAry[3] + ",WW"
                + wgtAry[4] + ",ME" + wgtAry[5] + ",EE" + wgtAry[6] + ",SW"
                + wgtAry[7] + ",SS" + wgtAry[8] + ",SE" + wgtAry[9] + ",HI"
                + ih;

        int i;
        for (i = 0; i < IMAXWLIFERUL; i++)
			if (rulesS[i])
				sBff = sBff + ",RS" + i;

		for (i = 0; i < IMAXWLIFERUL; i++)
			if (rulesB[i])
				sBff = sBff + ",RB" + i;

		return sBff;
	}

	
	
	
	public void Validate() {
		if (iClo < 2)
			iClo = 2;
		else if (iClo > MJBoard.MAX_CLO)
			iClo = MJBoard.MAX_CLO;
	}

	
	
	public int OnePass(int sizX, int sizY, boolean isWrap, int ColoringMethod,
			short[][] crrState, short[][] tmpState, MJBoard mjb) {
		var modCnt = 0;
		var lurd = new int[4];

		for (var i = 0; i < sizX; ++i) {
			
			lurd[0] = (i > 0) ? i - 1 : (isWrap) ? sizX - 1 : sizX;
			lurd[2] = (i < sizX - 1) ? i + 1 : (isWrap) ? 0 : sizX;
			for (var j = 0; j < sizY; ++j) {
				
				lurd[1] = (j > 0) ? j - 1 : (isWrap) ? sizY - 1 : sizY;
				lurd[3] = (j < sizY - 1) ? j + 1 : (isWrap) ? 0 : sizY;
				var bOldVal = crrState[i][j];
				var bNewVal = bOldVal;

				var iCnt = 0;
                if (isHist)
				{
					if (bOldVal <= 1) 
					{
						if (crrState[lurd[0]][lurd[1]] == 1)
							iCnt += wgtAry[1];
						if (crrState[i][lurd[1]] == 1)
							iCnt += wgtAry[2];
						if (crrState[lurd[2]][lurd[1]] == 1)
							iCnt += wgtAry[3];
						if (crrState[lurd[0]][j] == 1)
							iCnt += wgtAry[4];
						if (crrState[i][j] == 1)
							iCnt += wgtAry[5];
						if (crrState[lurd[2]][j] == 1)
							iCnt += wgtAry[6];
						if (crrState[lurd[0]][lurd[3]] == 1)
							iCnt += wgtAry[7];
						if (crrState[i][lurd[3]] == 1)
							iCnt += wgtAry[8];
						if (crrState[lurd[2]][lurd[3]] == 1)
							iCnt += wgtAry[9];
						if (iCnt < 0)
							iCnt = 0; 

						
						if (bOldVal == 0) 
						{
							if (rulesB[iCnt]) 
								bNewVal = 1; 
						} else 
						{
							if (rulesS[iCnt]) 
							{
								bNewVal = 1;
							} else 
							{
								bNewVal = bOldVal < (iClo - 1)
										? (short) (bOldVal + 1)
										: 0;
							}
						}
					} else 
					{
						bNewVal = bOldVal < (iClo - 1)
								? (short) (bOldVal + 1)
								: 0;
					}
				} else 
				{
					if (crrState[lurd[0]][lurd[1]] != 0)
						iCnt += wgtAry[1];
					if (crrState[i][lurd[1]] != 0)
						iCnt += wgtAry[2];
					if (crrState[lurd[2]][lurd[1]] != 0)
						iCnt += wgtAry[3];
					if (crrState[lurd[0]][j] != 0)
						iCnt += wgtAry[4];
					if (crrState[i][j] != 0)
						iCnt += wgtAry[5];
					if (crrState[lurd[2]][j] != 0)
						iCnt += wgtAry[6];
					if (crrState[lurd[0]][lurd[3]] != 0)
						iCnt += wgtAry[7];
					if (crrState[i][lurd[3]] != 0)
						iCnt += wgtAry[8];
					if (crrState[lurd[2]][lurd[3]] != 0)
						iCnt += wgtAry[9];
					if (iCnt < 0)
						iCnt = 0; 

					
					if (bOldVal == 0) 
					{
						if (rulesB[iCnt]) 
							bNewVal = ColoringMethod == 1
									? 1
									: (short) (mjb.Cycle
											% (mjb.StatesCount - 1) + 1);
					} else 
					{
						if (rulesS[iCnt]) 
						{
							if (ColoringMethod == 1) 
							{
								bNewVal = (short) (bOldVal < mjb.StatesCount - 1 ? bOldVal + 1 : mjb.StatesCount - 1);
							} else {
								
							}
						} else
							bNewVal = 0; 
					}
				}

				tmpState[i][j] = bNewVal;
				if (bNewVal != bOldVal) {
					modCnt++; 
				}
			} 
		} 

		return modCnt;
	}
	
}