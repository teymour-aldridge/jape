TACTIC assign(assigntac) IS
	 ALT assigntac 
		(SEQ "consequence(L)" 
			 (LETGOALPATH G1
				 NEXTGOAL assigntac
				 (GOALPATH G1)))

TACTICPANEL Instructions
	ENTRY "skip"
	ENTRY "tilt"
	ENTRY "sequence"
	ENTRY "variable-assignment" IS assign "variable-assignment"
	ENTRY "choice"
	ENTRY "while"
	ENTRY "consequence(L)"
	ENTRY "consequence(R)"
	ENTRY "obviously..."
	
/*	RULE "(A;B);C≜A;(B;C)" IS	A;B;C ≜ A;(B;C)
	ENTRY "flatten ;" IS 
		iterateR2L "rewrite≜"  "symmetric≜" (QUOTE (_A;(_B;_C))) "(A;B);C≜A;(B;C)" (Fail "no semicolons to flatten")
	
	BUTTON	"A≜…"	IS apply rewriteL2R "rewrite≜"  "symmetric≜"  COMMAND
	BUTTON	"…≜B"	IS apply rewriteR2L "rewrite≜"  "symmetric≜"  COMMAND */
	BUTTON	Apply	IS apply COMMAND
END
