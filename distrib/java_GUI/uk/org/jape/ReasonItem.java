/*
    $Id$
    
    Copyright � 2002 Richard Bornat & Bernard Sufrin
    
    richard@bornat.me.uk
    sufrin@comlab.ox.ac.uk
    
    This file is part of japeserver, which is part of jape.
    
    Jape is free software; you can redistribute it and/or modify
    it under the terms of the GNU General Public License as published by
    the Free Software Foundation; either version 2 of the License, or
    (at your option) any later version.
    
    Jape is distributed in the hope that it will be useful,
    but WITHOUT ANY WARRANTY; without even the implied warranty of
    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
    GNU General Public License for more details.
    
    You should have received a copy of the GNU General Public License
    along with jape; if not, write to the Free Software
    Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA
    (or look at http://www.gnu.org).

  */

import java.awt.event.MouseEvent;

public class ReasonItem extends SelectableProofItem {

    public ReasonItem(ProofCanvas canvas, int x, int y, byte fontnum,
                          String annottext, String printtext) {
        super(canvas, x, y, fontnum, annottext, printtext);
    }

    public void clicked(byte eventKind, MouseEvent e) {
        canvas.killSelections((byte)(SelectionConstants.ReasonSel |
                                     SelectionConstants.HypSel |
                                     SelectionConstants.ConcSel));
        doClick();
    }

    private void doClick() {
        selectionRect.setSelkind(SelectionConstants.ReasonSel);
    }

    public void select(byte selkind) {
        if (selkind==SelectionConstants.ReasonSel)
            doClick();
        else
            Alert.abort("ReasonItem.select selkind="+selkind);
    }
}