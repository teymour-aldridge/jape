/* $Id$ */

/* forcing semantics */

SEMANTICTURNSTILE IS �

FORCE A�B IFF FORCE A AND FORCE B
FORCE A�B IFF FORCE A OR FORCE B
FORCE A�B IFF ALL (FORCE A IMPLIES FORCE B)
FORCE �A IFF NONE FORCE A

FORCE �x.P(x) IFF ALL (FORCE actual i IMPLIES FORCE P(i))
FORCE �x.P(x) IFF FORCE actual i AND FORCE P(i)

