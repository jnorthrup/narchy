package nars.ca;




public class RuleNeumBin {
	public static final int MAX_STATES = 3;
	public int iClo; 
	public int[][][][][] states = new int[MAX_STATES][MAX_STATES][MAX_STATES][MAX_STATES][MAX_STATES];

	
	public RuleNeumBin() {
		ResetToDefaults();
	}

	
	
	public void ResetToDefaults() {
		iClo = 3; 
		for (var i = 0; i < MAX_STATES; i++)
			for (var j = 0; j < MAX_STATES; j++)
				for (var k = 0; k < MAX_STATES; k++)
					for (var l = 0; l < MAX_STATES; l++)
						for (var m = 0; m < MAX_STATES; m++)
							states[i][j][k][l][m] = 0;
	}

	
	
	public void InitFromString(String sStr) {
        ResetToDefaults();

		var iPos = 0;
        iClo = Integer.valueOf(sStr.substring(iPos, iPos + 1));
		iPos++;
		if (iClo < 2)
			iClo = 2;
		if (iClo > MAX_STATES)
			iClo = MAX_STATES;
		for (var i = 0; i < iClo; i++)
			for (var j = 0; j < iClo; j++)
				for (var k = 0; k < iClo; k++)
					for (var l = 0; l < iClo; l++)
						for (var m = 0; m < iClo; m++) {
							var sOneChar = sStr.substring(iPos, iPos + 1);
                            iPos++;
                            int iStt = Integer.valueOf(sOneChar);
                            if (iStt < 0)
								iStt = 0;
							if (iStt >= MAX_STATES)
								iStt = MAX_STATES - 1;
							states[i][j][k][l][m] = iStt;
						}

		Validate(); 
	}

	
	
	public void InitFromPrm(int i_Clo, int[][][][][] sttAry) {
		iClo = i_Clo;
		states = sttAry;

		Validate(); 
	}

	
	
	
	public String GetAsString() {


        Validate();


		var sBff = String.valueOf(iClo);
		for (var i = 0; i < iClo; i++)
			for (var j = 0; j < iClo; j++)
				for (var k = 0; k < iClo; k++)
					for (var l = 0; l < iClo; l++)
						for (var m = 0; m < iClo; m++) {
                            sBff += states[i][j][k][l][m];
						}

		return sBff;
	}

	
	
	
	public void Validate() {
		int i, iMax;

		if (iClo < 2)
			iClo = 2;
		else if (iClo > MAX_STATES)
			iClo = MAX_STATES;
	}

	
	
	public int OnePass(int sizX, int sizY, boolean isWrap, int ColoringMethod,
			short[][] crrState, short[][] tmpState) {
		var modCnt = 0;
		int iCnt;
		var lurd = new int[4];
		var iCntAry = new int[iClo];
		int iTmp;

        for (var i = 0; i < sizX; ++i) {
			
			lurd[0] = (i > 0) ? i - 1 : (isWrap) ? sizX - 1 : sizX;
			lurd[2] = (i < sizX - 1) ? i + 1 : (isWrap) ? 0 : sizX;

			for (var j = 0; j < sizY; ++j) {
				
				lurd[1] = (j > 0) ? j - 1 : (isWrap) ? sizY - 1 : sizY;
				lurd[3] = (j < sizY - 1) ? j + 1 : (isWrap) ? 0 : sizY;
				var bOldVal = crrState[i][j];
                bOldVal = (short) ((bOldVal < 0) ? 0 : (bOldVal >= iClo)
						? (iClo - 1)
						: bOldVal);

                int l = crrState[lurd[0]][j];
                int u = crrState[i][lurd[1]];
                int r = crrState[lurd[2]][j];
                int d = crrState[i][lurd[3]];
                l = (l < 0) ? 0 : (l >= iClo) ? (iClo - 1) : l;
				u = (u < 0) ? 0 : (u >= iClo) ? (iClo - 1) : u;
				r = (r < 0) ? 0 : (r >= iClo) ? (iClo - 1) : r;
				d = (d < 0) ? 0 : (d >= iClo) ? (iClo - 1) : d;
				var bNewVal = (short) states[bOldVal][u][r][d][l];

                tmpState[i][j] = bNewVal;
				if (bNewVal != bOldVal) {
					modCnt++; 
				}
			}
		}

		return modCnt;
	}
	
}