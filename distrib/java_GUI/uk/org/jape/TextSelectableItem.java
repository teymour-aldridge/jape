/*
    $Id$
    
    Copyright � 2003 Richard Bornat & Bernard Sufrin
    
    richard@bornat.me.uk
    sufrin@comlab.ox.ac.uk
    
    This file is part of the Jape GUI, which is part of Jape.
    
    Jape is free software; you can redistribute it and/or modify
    it under the terms of the GNU General Public License as published by
    the Free Software Foundation; either version 2 of the License, or
    (at your option) any later version.
    
    Jape is distributed in the hope that it will be useful,
    but WITHOUT ANY WARRANTY; without even the implied warranty of
    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
    GNU General Public License for more details.
    
    You should have received a copy of the GNU General Public License
    along with Jape; if not, write to the Free Software
    Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA
    (or look at http://www.gnu.org).

 */

import java.awt.Component;
import java.util.Enumeration;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseMotionAdapter;
import java.awt.event.MouseEvent;
import java.awt.Rectangle;
import java.util.Vector;

public abstract class TextSelectableItem extends TextItem implements SelectionConstants {

    protected final char[] printchars;
    protected final String annottext;

    // globals for computing ColourTree, FormulaTree
    protected int          annoti, printi; 
    protected final int    annotlen;

    public TextSelectableItem(JapeCanvas canvas, int x, int y, byte fontnum, String annottext) {
        super(canvas,x,y,fontnum,printtext(annottext));
        this.printchars = text.toCharArray();
        this.annottext = annottext;
        annotlen = annottext.length();
        addJapeMouseListener(new JapeMouseTextAdapter() {
            public void textpressed(byte eventKind, MouseEvent e) {
                TextSelectableItem.this.canvas.claimFocus();
                TextSelectableItem.this.textpressed(eventKind, e);
            }
            public void textdragged(byte eventKind, MouseEvent e) {
                TextSelectableItem.this.textdragged(eventKind, e);
            }
            public void textreleased(byte eventKind, boolean isClick, MouseEvent e) {
                TextSelectableItem.this.textreleased(eventKind, isClick, e);
            }
        });
        selectionHalo = canvas.getSelectionHalo();
    }


    protected static char onbra, onket, offbra, offket, outbra, outket, lockbra, lockket;

    public static void setinvischars(char _onbra, char _onket, char _offbra, char _offket,
                                     char _outbra, char _outket, char _lockbra, char _lockket) {
        onbra=_onbra; onket=_onket;
        offbra=_offbra; offket=_offket;
        outbra=_outbra; outket=_outket;
        lockbra=_lockbra; lockket=_lockket;
    }

    static boolean invisbra(char c) {
        return c==onbra || c==offbra || c==outbra || c==lockbra;
    }

    static boolean invisket(char c) {
        return c==onket || c==offket || c==outket || c==lockket;
    }

    static boolean invisible(char c) {
        return invisbra(c) || invisket(c);
    }

    static char bra2ket(char c) {
        return c==onbra  ? onket :
        c==offbra ? offket :
        c==outbra ? outket :
        /* c==lockbra assumed */ lockket;
    }

    static char ket2bra(char c) {
        return c==onket  ? onbra :
        c==offket ? offbra :
        c==outket ? outbra :
        /* c==lockket assumed */ lockbra;
    }

    static String printtext(String annottext) {
        int annotlength = annottext.length();
        StringBuffer printbuf = new StringBuffer(annotlength);
        for (int i=0; i<annotlength; i++) {
            char c = annottext.charAt(i);
            if (!invisible(c))
                printbuf.append(c);
        }
        return printbuf.toString();
    }
    
    protected FormulaTree formulae;
    
    protected class FormulaTree {
        public final int start, end; // start<=i<end -> i is in this subformula
        public final int pxstart, pxend;
        public FormulaTree parent;
        public final FormulaTree[] children;

        public FormulaTree(int start, int end, FormulaTree[] children) {
            this.start = start; this.end = end;
            this.pxstart = JapeFont.charsWidth(printchars, 0, start, fontnum);
            this.pxend = JapeFont.charsWidth(printchars, 0, end, fontnum);
            this.children = children;
            for (int i=0; i<children.length; i++)
                children[i].parent = this;
        }

        public String toString() {
            String s = "FT["+start+","+end+" "+pxstart+","+pxend+" [";
            for (int i=0; i<children.length; i++)
                s = s+children[i]+(i==children.length-1 ? "" : ",");
            return s+"]]";
        }
    }

    // annoti, printi, annotlen are variables in TextItem
    protected FormulaTree computeFormulaTree(char expectedket) {
        int i0 = printi;
        Vector cs = new Vector();
        char c;
        while (annoti<annotlen) {
            c = annottext.charAt(annoti++);
            if (invisbra(c))
                cs.add(computeFormulaTree(bra2ket(c)));
            else
            if (invisket(c)) {
                if (c==expectedket)
                    return computeFormulaTreeResult(i0, cs);
                else
                    Alert.abort("computeFormulaTree saw "+(int)c+", expected "+(int)expectedket);
            }
            else
                printi++;
        }
        
        if (expectedket!=0)
            Alert.abort(this+": computeFormulaTree exhausted text, "+
                        ", expected "+(int)expectedket);

        if (printi!=printchars.length)
            Alert.abort(this+": text is "+printchars.length+
                        " chars, but computeFormulaTree thinks it's "+printi);

        return computeFormulaTreeResult(i0, cs);
    }

    protected FormulaTree computeFormulaTreeResult(int i0, Vector cs) {
        if (cs.size()==1) {
            FormulaTree child = (FormulaTree)cs.get(0);
            if (child.start==i0 && child.end==printi)
                return child;
        }

        return new FormulaTree(i0, printi, (FormulaTree[])cs.toArray(new FormulaTree[cs.size()]));
    }

    protected FormulaTree pixel2Subformula(FormulaTree f, int px) {
        if (px<f.pxstart || px>f.pxend)
            return null;
        else {
            FormulaTree f1;
            for (int i=0; i<f.children.length; i++)
                if ((f1 = pixel2Subformula(f.children[i],px))!=null)
                    return f1;
            return f;
        }
    }

    protected FormulaTree enclosingSubformula(FormulaTree a, FormulaTree b) {
        while (a.parent!=null && !(a.start<=b.start && b.end<=a.end)) {
            a = a.parent;
        }
        return a;
    }

    protected FormulaTree findSubformula(FormulaTree f, int start, int end) {
        if (f==null || f.start==start && f.end==end)
            return f;
        for (int i=0; i<f.children.length; i++) {
            FormulaTree f1;
            if ((f1=findSubformula(f.children[i], start, end))!=null)
                return f1;
        }
        return null;
    }

    public class TextSel {
        public int start, end, pxstart, pxend;
        
        public TextSel(FormulaTree f) { init(f); }
        
        public void reset(FormulaTree f) { repaint(); init(f); }
        
        private void init(FormulaTree f) {
            start=f.start; end=f.end; pxstart=f.pxstart; pxend=f.pxend;
            repaint();
        }
        
        public TextSel(int start, int end) { init(start, end); }
        
        public void reset(int start, int end) { repaint(); init(start, end); }
        
        private void init(int start, int end) {
            this.start = start; this.end = end;
            pxstart = JapeFont.charsWidth(printchars, 0, start, fontnum);
            pxend = JapeFont.charsWidth(printchars, 0, end, fontnum);
            repaint();
        }
        
        public void repaint() {
            TextSelectableItem.this.repaint(pxstart, 0, pxend-pxstart, getHeight());
        }
        
        public void paint(Graphics g) {
            if (paint_tracing)
                Logger.log.println("painting text selection "+start+","+end);
            g.setColor(Preferences.TextSelectionColour);
            g.fillRect(pxstart, 0, pxend-pxstart, getHeight());
        }

        public boolean overlaps(TextSel t) {
            return t!=null && t!=this &&
                   (t.start<=this.start && this.start<=t.start ||
                    this.start<=t.start && t.start<=this.start);
        }
    }

    public Vector textsels;
    private int currenttextselindex = -1;
    FormulaTree anchor, current;

    public void deTextSelect() {
        if (textsels!=null)
            while (textsels.size()!=0) {
                getTextSel(0).repaint();
                textsels.remove(0);
            }
        canvas.getProofWindow().enableCopy();
    }

    public abstract String getTextSelections();

    public int getTextSelectionCount() {
        return textsels==null ? 0 : textsels.size();
    }
    
    private void ensureTextSelectionVars() {
        if (formulae==null) {
            annoti = printi = 0;
            formulae = computeFormulaTree((char)0);
            textsels = new Vector(1);
            canvas.getProofWindow().enableCopy();
        }
    }

    protected TextSel getTextSel(int i) {
        return i<0 || i>=textsels.size() ? null : (TextSel)textsels.get(i);
    }

    protected void newTextSel(MouseEvent e) {
        ensureTextSelectionVars();
        anchor = current = pixel2Subformula(formulae, internalX(e.getX()));
        int i;
        for (i=0; i<textsels.size(); i++)
            if (anchor.start<=getTextSel(i).start)
                break;
        textsels.add(i, new TextSel(anchor));
        currenttextselindex = i;
        canvas.getProofWindow().enableCopy();
    }
        
    protected void textpressed(byte eventKind, MouseEvent e) {
        switch (eventKind) {
            case TextSelection:
                canvas.killTextSelections(null); // kill everybody's, including mine
            case DisjointTextSelection: // don't kill any text selections?
                newTextSel(e);
                break;
                
            case ExtendedTextSelection:
                canvas.killTextSelections(this); // kill everybody else's
            case ExtendedDisjointTextSelection:
                ensureTextSelectionVars();
                if (textsels.size()==0)
                    newTextSel(e);
                else {
                    int i, px=e.getX();
                    for (i=0; i<textsels.size(); i++) {
                        if (px<getTextSel(i).pxend) {
                            // are we nearer to this one or the one before?
                            if (i!=0 && getTextSel(i).pxstart-px>px-getTextSel(i-1).pxend)
                                i--;
                            break;
                        }
                    }
                    // we are nearest to textsel[i], if there is one
                    currenttextselindex = Math.min(i, textsels.size()-1);
                    anchor = findSubformula(formulae, getTextSel(currenttextselindex).start,
                                            getTextSel(currenttextselindex).end);
                    current = enclosingSubformula(anchor, pixel2Subformula(formulae, px));
                    getTextSel(currenttextselindex).reset(current);
                }
                break;

            default:
                Logger.log.println("TextSelectableItem.textpressed eventKind="+eventKind);
        }
    }

    protected void textdragged(byte eventKind, MouseEvent e) {
        FormulaTree sel = enclosingSubformula(anchor, pixel2Subformula(formulae,internalX(e.getX())));
        if (sel!=current) {
            getTextSel(currenttextselindex).reset(sel);
            current = sel;
        }
    }
    
    protected void textreleased(byte eventKind, boolean isClick, MouseEvent e) {
        TextSel current = getTextSel(currenttextselindex);
        for (int i=0; i<textsels.size(); ) {
            TextSel t = getTextSel(i);
            if (t.overlaps(current))
                textsels.remove(i);
            else
                i++;
        }
        currenttextselindex = -1;
        canvas.getProofWindow().enableCopy();
    }

    protected void paintTextSels(Graphics g) {
        if (textsels!=null) {
            TextSel current = getTextSel(currenttextselindex);
            for (int i = 0; i<textsels.size(); i++) {
                TextSel t = getTextSel(i);
                if (!t.overlaps(current))
                    t.paint(g);
            }
        }
    }
    
    public void paint(Graphics g) {
        if (paint_tracing)
            Logger.log.println("painting textselectable item at "+getX()+","+getY());
        paintTextSels(g); super.paint(g);
    }

    public String toString() {
        String s = "TextSelectableItem["+
        "annottext=..."+
        ", currenttextselindex="+currenttextselindex+
        ", textsels=";
        if (textsels==null)
            s = s+"null";
        else {
            s = s+"[";
            for (int i=0; i<textsels.size(); i++) {
                s = s+getTextSel(i);
                if (i+1<textsels.size())
                    s = s+", ";
            }
            s = s+"]";
        }
        return s+", "+super.toString()+"]";
    }
}
