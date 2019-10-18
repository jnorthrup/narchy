package nars.ca;






import java.util.Vector;

public class MJRules {
	public static final String S_USERRULE = "User rule";

	public static final int GAME_LIFE = 0; 
	public static final int GAME_GENE = 1; 
	public static final int GAME_WLIF = 2; 
	public static final int GAME_VOTE = 3; 
	public static final int GAME_RTBL = 4; 
	public static final int GAME_CYCL = 5; 
	public static final int GAME_1DTO = 6; 
	public static final int GAME_1DBI = 7; 
	public static final int GAME_NMBI = 8; 
	public static final int GAME_GEBI = 9; 
	public static final int GAME_LGTL = 10; 
	public static final int GAME_MARG = 11; 
	public static final int GAME_USER = 12; 
	public static final int GAME_SPEC = 13; 
	public static final int GAME_LAST = 13; 

	public static final int NGHTYP_MOOR = 1; 
	public static final int NGHTYP_NEUM = 2; 

	public static final int GAMTYP_1D = 1; 
	public static final int GAMTYP_2D = 2; 

	public static final String GAME_LIFE_Name = "Life";
	public static final String GAME_GENE_Name = "Generations";
	public static final String GAME_WLIF_Name = "Weighted Life";
	public static final String GAME_VOTE_Name = "Vote for Life";
	public static final String GAME_RTBL_Name = "Rules table";
	public static final String GAME_CYCL_Name = "Cyclic CA";
	public static final String GAME_1DTO_Name = "1-D totalistic";
	public static final String GAME_1DBI_Name = "1-D binary";
	public static final String GAME_NMBI_Name = "Neumann binary";
	public static final String GAME_GEBI_Name = "General binary";
	public static final String GAME_LGTL_Name = "Larger than Life";
	public static final String GAME_MARG_Name = "Margolus";
	public static final String GAME_USER_Name = "User DLL";
	public static final String GAME_SPEC_Name = "Special rules";

	public static final String GAME_LIFE_Abbr = "LIFE"; 
	public static final String GAME_GENE_Abbr = "GENE"; 
	public static final String GAME_WLIF_Abbr = "WLIF"; 
	public static final String GAME_VOTE_Abbr = "VOTE"; 
	public static final String GAME_RTBL_Abbr = "RTBL"; 
	public static final String GAME_CYCL_Abbr = "CYCL"; 
	public static final String GAME_1DTO_Abbr = "1DTO"; 
	public static final String GAME_1DBI_Abbr = "1DBI"; 
	public static final String GAME_NMBI_Abbr = "NMBI"; 
	public static final String GAME_GEBI_Abbr = "GEBI"; 
	public static final String GAME_LGTL_Abbr = "LGTL"; 
	public static final String GAME_MARG_Abbr = "MARG"; 
	public static final String GAME_USER_Abbr = "USER"; 
	public static final String GAME_SPEC_Abbr = "SPEC"; 

	
	public static final int NAM = 0; 
	public static final int RUL = 1; 

	public final Vector[] Rules = new Vector[GAME_LAST + 1]; 

	
	
	public MJRules() {
		for (int i = 0; i <= GAME_LAST; i++) {
			Rules[i] = new Vector();
		}
		AddRules();
	}

	
	
	@SuppressWarnings("HardcodedFileSeparator")
	private void AddRules() {

        Vector vLines = new Vector();
        MJTools mjT = new MJTools();
		if (MJTools.LoadResTextFile("rul.txt", vLines))
		{
            int iGame = -1;
            int i = -1;
            for (i = 0; i < vLines.size(); i++) {
                String sBff = ((String) vLines.elementAt(i)).trim();
                if ((!sBff.isEmpty())
						&& !((String) vLines.elementAt(i)).startsWith("//")) {
					if (sBff.length() > 0 && sBff.charAt(0) == '#') // next family of rules
					{
						iGame = GetGameIndex(sBff.substring(1));
					} else // next rule
					{
						if (iGame >= 0) {
							int whereSep = sBff.indexOf('|');
							if (whereSep > 0) {
                                String sNam = sBff.substring(0, whereSep);
								sNam = sNam.trim();
								String sDef = sBff.substring(whereSep + 1);
								sDef = sDef.trim();
								Rules[iGame].addElement(new CARule(sNam, sDef));
							}
						}
					}
				}
			}
		}
	}

	
	
	public static int GetGameIndex(String sGameName) {
		int iGame = -1;
		
		if ((sGameName.compareTo(GAME_GENE_Name) == 0) 
				|| (sGameName.compareTo(GAME_GENE_Abbr) == 0))
			iGame = GAME_GENE;
		else if ((sGameName.compareTo(GAME_LIFE_Name) == 0) 
				|| (sGameName.compareTo(GAME_LIFE_Abbr) == 0))
			iGame = GAME_LIFE;
		else if ((sGameName.compareTo(GAME_WLIF_Name) == 0) 
				|| (sGameName.compareTo(GAME_WLIF_Abbr) == 0))
			iGame = GAME_WLIF;
		else if ((sGameName.compareTo(GAME_VOTE_Name) == 0) 
				|| (sGameName.compareTo(GAME_VOTE_Abbr) == 0))
			iGame = GAME_VOTE;
		else if ((sGameName.compareTo(GAME_RTBL_Name) == 0) 
				|| (sGameName.compareTo(GAME_RTBL_Abbr) == 0))
			iGame = GAME_RTBL;
		else if ((sGameName.compareTo(GAME_CYCL_Name) == 0) 
				|| (sGameName.compareTo(GAME_CYCL_Abbr) == 0))
			iGame = GAME_CYCL;
		else if ((sGameName.compareTo(GAME_1DTO_Name) == 0) 
				|| (sGameName.compareTo(GAME_1DTO_Abbr) == 0))
			iGame = GAME_1DTO;
		else if ((sGameName.compareTo(GAME_1DBI_Name) == 0) 
				|| (sGameName.compareTo(GAME_1DBI_Abbr) == 0))
			iGame = GAME_1DBI;
		else if ((sGameName.compareTo(GAME_NMBI_Name) == 0) 
				|| (sGameName.compareTo(GAME_NMBI_Abbr) == 0))
			iGame = GAME_NMBI;
		else if ((sGameName.compareTo(GAME_GEBI_Name) == 0) 
				|| (sGameName.compareTo(GAME_GEBI_Abbr) == 0))
			iGame = GAME_GEBI;
		else if ((sGameName.compareTo(GAME_LGTL_Name) == 0) 
				|| (sGameName.compareTo(GAME_LGTL_Abbr) == 0))
			iGame = GAME_LGTL;
		else if ((sGameName.compareTo(GAME_MARG_Name) == 0) 
				|| (sGameName.compareTo(GAME_MARG_Abbr) == 0))
			iGame = GAME_MARG;
		else if ((sGameName.compareTo(GAME_USER_Name) == 0) 
				|| (sGameName.compareTo(GAME_USER_Abbr) == 0))
			iGame = GAME_USER;
		else if ((sGameName.compareTo(GAME_SPEC_Name) == 0) 
				|| (sGameName.compareTo(GAME_SPEC_Abbr) == 0))
			iGame = GAME_SPEC;
		return iGame;
	}

	
	
	public static String GetGameName(int iGame) {
		String sRetVal;
		switch (iGame) {
		case GAME_LIFE:
			sRetVal = GAME_LIFE_Name;
			break; 
		case GAME_GENE:
			sRetVal = GAME_GENE_Name;
			break; 
		case GAME_WLIF:
			sRetVal = GAME_WLIF_Name;
			break; 
		case GAME_VOTE:
			sRetVal = GAME_VOTE_Name;
			break; 
		case GAME_RTBL:
			sRetVal = GAME_RTBL_Name;
			break; 
		case GAME_CYCL:
			sRetVal = GAME_CYCL_Name;
			break; 
		case GAME_1DTO:
			sRetVal = GAME_1DTO_Name;
			break; 
		case GAME_1DBI:
			sRetVal = GAME_1DBI_Name;
			break; 
		case GAME_NMBI:
			sRetVal = GAME_NMBI_Name;
			break; 
		case GAME_GEBI:
			sRetVal = GAME_GEBI_Name;
			break; 
		case GAME_LGTL:
			sRetVal = GAME_LGTL_Name;
			break; 
		case GAME_MARG:
			sRetVal = GAME_MARG_Name;
			break; 
		case GAME_USER:
			sRetVal = GAME_USER_Name;
			break; 
		case GAME_SPEC:
			sRetVal = GAME_SPEC_Name;
			break; 
		default:
			sRetVal = "???";
			break;
		}
		return sRetVal;
	}

	
	
	public static boolean IsGameIdxValid(int iGame) {
		return (iGame >= GAME_LIFE) && (iGame <= GAME_LAST);
	}

	
	
	public String GetRuleDef(String sGameName, String sRuleName) {
		String sRuleDef = "";

        int iGame = GetGameIndex(sGameName);
		if (iGame >= 0) {
			for (int i = 0; i < Rules[iGame].size(); i++) {
				if (sRuleName
						.compareTo(((CARule) Rules[iGame].elementAt(i)).name) == 0) {
					sRuleDef = ((CARule) Rules[iGame].elementAt(i)).def;
					break;
				}
			}
		}

		return sRuleDef;
	}

	
	
	public String GetRuleName(String sGameName, String sRuleDef) {
		String sRuleName = "";

        int iGame = GetGameIndex(sGameName);
		if (iGame >= 0) {
			for (int i = 0; i < Rules[iGame].size(); i++) {
				if (sRuleDef
						.compareTo(((CARule) Rules[iGame].elementAt(i)).def) == 0) {
					sRuleName = ((CARule) Rules[iGame].elementAt(i)).name;
					break;
				}
			}
		}
		return sRuleName;
	}

	
	
	public static String CorrectRuleDef(String sGameName, String sRuleDef) {
		sRuleDef = sRuleDef.trim();
		int iGame = GetGameIndex(sGameName);

		switch (iGame) {
		case MJRules.GAME_LIFE: 
			RuleLife RLife = new RuleLife();
			RLife.InitFromString(sRuleDef);
			sRuleDef = RLife.GetAsString(); 
			break;
		case MJRules.GAME_GENE: 
			RuleGene RGene = new RuleGene();
			RGene.InitFromString(sRuleDef);
			sRuleDef = RGene.GetAsString(); 
			break;

		case MJRules.GAME_VOTE: 
			RuleVote RVote = new RuleVote();
			RVote.InitFromString(sRuleDef);
			sRuleDef = RVote.GetAsString(); 
			break;

		case MJRules.GAME_WLIF: 
			RuleWLife RWLife = new RuleWLife();
			RWLife.InitFromString(sRuleDef);
			sRuleDef = RWLife.GetAsString(); 
			break;
		case MJRules.GAME_RTBL: 
			RuleRTab RRtab = new RuleRTab();
			RRtab.InitFromString(sRuleDef);
			sRuleDef = RRtab.GetAsString(); 
			break;
		case MJRules.GAME_CYCL: 
			RuleCyclic RCyclic = new RuleCyclic();
			RCyclic.InitFromString(sRuleDef);
			sRuleDef = RCyclic.GetAsString(); 
			break;

		case MJRules.GAME_1DTO: 
			Rule1DTotal R1DTo = new Rule1DTotal();
			R1DTo.InitFromString(sRuleDef);
			sRuleDef = R1DTo.GetAsString(); 
			break;

		case MJRules.GAME_1DBI: 
			Rule1DBin R1DBin = new Rule1DBin();
			R1DBin.InitFromString(sRuleDef);
			sRuleDef = R1DBin.GetAsString(); 
			break;
		case MJRules.GAME_NMBI:
			RuleNeumBin RNeumBin = new RuleNeumBin();
			RNeumBin.InitFromString(sRuleDef);
			sRuleDef = RNeumBin.GetAsString(); 
			break;
		case MJRules.GAME_GEBI: 
			RuleGenBin RGenBin = new RuleGenBin();
			RGenBin.InitFromString(sRuleDef);
			sRuleDef = RGenBin.GetAsString(); 
			break;
		case MJRules.GAME_LGTL:
			RuleLgtL RLgtL = new RuleLgtL();
			RLgtL.InitFromString(sRuleDef);
			sRuleDef = RLgtL.GetAsString(); 
			break;
		case MJRules.GAME_MARG:
			RuleMarg RMarg = new RuleMarg();
			RMarg.InitFromString(sRuleDef);
			sRuleDef = RMarg.GetAsString(); 
			break;
		case MJRules.GAME_USER:
			RuleUser RUser = new RuleUser();
			RUser.InitFromString(sRuleDef);
			sRuleDef = RUser.GetAsString(); 
			break;
		case MJRules.GAME_SPEC:
			break;
		}
		return sRuleDef;
	}
	
	
}