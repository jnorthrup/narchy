package nars.ca;




import java.util.StringTokenizer;

public class RuleCyclic {
	public int iClo; 
	public int iRng; 
	public int iThr; 
	public int iNgh; 
	public boolean fGH; 

	public static final int MAX_RANGE = 10;

	
	public RuleCyclic() {
		ResetToDefaults();
	}

	
	
	public void ResetToDefaults() {
		iClo = 3; 
		iRng = 1; 
		iThr = 3; 
		iNgh = MJRules.NGHTYP_MOOR; 
		fGH = false; 
	}

	
	
	@SuppressWarnings("HardcodedFileSeparator")
	public void InitFromString(String sStr) {

        ResetToDefaults();

		var st = new StringTokenizer(sStr, ",/", true);
		while (st.hasMoreTokens()) {
			var sTok = st.nextToken().toUpperCase();

            if (sTok.length() > 0 && sTok.charAt(0) == 'R')
				iRng = Integer.valueOf(sTok.substring(1));
			else if (sTok.length() > 0 && sTok.charAt(0) == 'T')
				iThr = Integer.valueOf(sTok.substring(1));
			else if (sTok.length() > 0 && sTok.charAt(0) == 'C')
				iClo = Integer.valueOf(sTok.substring(1));
			else if (sTok.startsWith("NM"))
				iNgh = MJRules.NGHTYP_MOOR;
			else if (sTok.startsWith("NN"))
				iNgh = MJRules.NGHTYP_NEUM;
			else if (sTok.startsWith("GH"))
				fGH = true;
		}
		Validate(); 
	}

	
	
	public void InitFromPrm(int i_Clo, int i_Rng, int i_Thr, int i_Ngh,
			boolean f_GH) {
		iClo = i_Clo;
		iRng = i_Rng;
		iThr = i_Thr;
		iNgh = i_Ngh;
		fGH = f_GH;

		Validate(); 
	}

	
	
	
	@SuppressWarnings("HardcodedFileSeparator")
	public String GetAsString() {


        Validate();


		var sBff = 'R' + String.valueOf(iRng) + "/T" + iThr + "/C"
                + iClo;

        sBff += (iNgh == MJRules.NGHTYP_NEUM ? "/NN" : "/NM");

		if (fGH)
            sBff += "/GH";

		return sBff;
	}

	
	
	
	public void Validate() {

        if (iClo < 2)
			iClo = 2;
		else if (iClo > MJBoard.MAX_CLO)
			iClo = MJBoard.MAX_CLO;

		if (iRng < 1)
			iRng = 1;
		else if (iRng > MAX_RANGE)
			iRng = MAX_RANGE;

		var iMax = 0;
		for (var i = 1; i <= iRng; i++)

            iMax += i * 8;

		if (iThr < 1)
			iThr = 1;
		else if (iThr > iMax)
			iThr = iMax;

		if (iNgh != MJRules.NGHTYP_NEUM)
			iNgh = MJRules.NGHTYP_MOOR; 
	}

	
	
	public int OnePass(int sizX, int sizY, boolean isWrap, int ColoringMethod,
					   short[][] crrState, short[][] tmpState) {
		var modCnt = 0;
		var xVector = new int[21];
		var yVector = new int[21];

		var fMoore = (iNgh == MJRules.NGHTYP_MOOR);

		for (var i = 0; i < sizX; i++) {
			for (var j = 0; j < sizY; j++) {
				
				
				xVector[10] = i;
				yVector[10] = j;
				for (var iTmp = 1; iTmp <= iRng; iTmp++) {
					var colL = i - iTmp;
                    xVector[10 - iTmp] = colL >= 0 ? colL : sizX + colL;

					var colR = i + iTmp;
                    xVector[10 + iTmp] = colR < sizX ? colR : colR - sizX;

					var rowT = j - iTmp;
                    yVector[10 - iTmp] = rowT >= 0 ? rowT : sizY + rowT;

					var rowB = j + iTmp;
                    yVector[10 + iTmp] = rowB < sizY ? rowB : rowB - sizY;
				}
				var bOldVal = crrState[i][j];
				var nxtStt = bOldVal >= (iClo - 1) ? 0 : (short) (bOldVal + 1);

                short bNewVal;
                if ((!fGH) || (bOldVal == 0)) {
					bNewVal = bOldVal; 
					if (bNewVal >= iClo)
						bNewVal = (short) (iClo - 1);

					var iCnt = 0;
                    for (var ic = 10 - iRng; ic <= 10 + iRng; ic++) {
						for (var ir = 10 - iRng; ir <= 10 + iRng; ir++) {
							if ((fMoore)
									|| ((Math.abs(ic - 10) + Math.abs(ir - 10)) <= iRng)) {
								if (crrState[xVector[ic]][yVector[ir]] == nxtStt) {
									iCnt++;
								}
							}
						}
					}
					if (iCnt >= iThr)
						bNewVal = nxtStt; 
				} else {
					bNewVal = nxtStt; 
				}

				tmpState[i][j] = bNewVal;
				if (bNewVal != bOldVal) 
				{
					modCnt++; 
				}
			} 
		} 

		return modCnt;
	}
}