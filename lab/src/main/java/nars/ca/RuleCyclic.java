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

        StringTokenizer st = new StringTokenizer(sStr, ",/", true);
		while (st.hasMoreTokens()) {
            String sTok = st.nextToken().toUpperCase();

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


        String sBff = 'R' + String.valueOf(iRng) + "/T" + iThr + "/C"
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

        int iMax = 0;
		for (int i = 1; i <= iRng; i++)

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
        int modCnt = 0;
        int[] xVector = new int[21];
        int[] yVector = new int[21];

        boolean fMoore = (iNgh == MJRules.NGHTYP_MOOR);

		for (int i = 0; i < sizX; i++) {
			for (int j = 0; j < sizY; j++) {
				
				
				xVector[10] = i;
				yVector[10] = j;
				for (int iTmp = 1; iTmp <= iRng; iTmp++) {
                    int colL = i - iTmp;
                    xVector[10 - iTmp] = colL >= 0 ? colL : sizX + colL;

                    int colR = i + iTmp;
                    xVector[10 + iTmp] = colR < sizX ? colR : colR - sizX;

                    int rowT = j - iTmp;
                    yVector[10 - iTmp] = rowT >= 0 ? rowT : sizY + rowT;

                    int rowB = j + iTmp;
                    yVector[10 + iTmp] = rowB < sizY ? rowB : rowB - sizY;
				}
                short bOldVal = crrState[i][j];
                short nxtStt = bOldVal >= (iClo - 1) ? 0 : (short) (bOldVal + 1);

                short bNewVal;
                if ((!fGH) || (bOldVal == 0)) {
					bNewVal = bOldVal; 
					if (bNewVal >= iClo)
						bNewVal = (short) (iClo - 1);

                    int iCnt = 0;
                    for (int ic = 10 - iRng; ic <= 10 + iRng; ic++) {
						for (int ir = 10 - iRng; ir <= 10 + iRng; ir++) {
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