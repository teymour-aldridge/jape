TACTIC ":=" IS
  LETGOALPATH G	
	(ALT assign (Fail "not an assignment statement")) 
	(LAYOUT HIDEROOT "→ intro")
	(LETGOALPATH G1
		(ALT (SEQ hyp (GOALPATH G) (LAYOUT ":=" ()) (GOALPATH G1) NEXTGOAL)
			(SEQ (GOALPATH G )(LAYOUT ":=") (GOALPATH G1))
		)
	)
TACTICPANEL Instructions
	ENTRY "X:=E" IS ":="
	RULE "while-do-od"(P, OBJECT x, OBJECT vt) WHERE FRESH vt IS
		FROM  Q→ P AND {P∧B} S {P} AND P∧¬B→R AND P∧B→(∃x.(T>x)) AND {P∧B∧vt=T} S {T<vt}
		INFER { Q } while B do S od { R}
	RULE "if-then-else-fi" IS FROM {Q∧B} S1 {R} AND {Q∧¬B} S2  {R} INFER { Q } if B then S1 else S2 fi { R }
	RULE "skip" IS INFER   { Q } skip { Q }
	ENTRY "S1;S2" IS 
		(LETGOALPATH G (LAYOUT COMPRESS "sequence") semicolon (GOALPATH (SUBGOAL G 1)))
	/* ENTRY "strengthen postcondition" IS 
		(LETGOALPATH G "strengthen postcondition" (GOALPATH (SUBGOAL G 1)) (LAYOUT HIDEROOT "→-I")) */
	
	ENTRY "***assert***"
	
/*	RULE "(A;B);C≜A;(B;C)" IS	A;B;C ≜ A;(B;C)
	ENTRY "flatten ;" IS 
		iterateR2L "rewrite≜"  "symmetric≜" (QUOTE (_A;(_B;_C))) "(A;B);C≜A;(B;C)" (Fail "no semicolons to flatten")
	
	BUTTON	"A≜…"	IS apply rewriteL2R "rewrite≜"  "symmetric≜"  COMMAND
	BUTTON	"…≜B"	IS apply rewriteR2L "rewrite≜"  "symmetric≜"  COMMAND */
	BUTTON	Apply	IS apply COMMAND
END
