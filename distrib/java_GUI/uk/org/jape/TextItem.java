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

import java.awt.Color;
import java.awt.Component;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.util.Vector;

class TextItem extends Component {
    /* from displayclass.ml/mli:
       (* Useful translation for Japeserver marshalling.
        * Current C/Java/Tk interfaces believe in these integers.
        *
        *   DisplayPunct  0
        *   DisplayConc   1
        *   DisplayHyp    2
        *   DisplayReason 3
        *   DisplayAmbig  4
        *
        *)
    */

    public static final byte PunctKind  = 0,
                             ConcKind   = 1,
                             HypKind    = 2,
                             ReasonKind = 3,
                             AmbigKind  = 4;
    
    public static Color OnColour     = Color.black,
                        OutColour    = Color.gray,
                        ForcedColour = Color.magenta,
                        NormalColour = Color.black;
    
    private static char onbra, onket, offbra, offket, outbra, outket, lockbra, lockket;

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

    static Color bra2TextColour(char c) {
        return c==onbra  ? ForcedColour :
               c==offbra ? OnColour :
              /* c==outbra assumed */ OutColour;
    }

    protected char[]        printchars;
    protected String        annottext;
    protected byte          fontnum;
    protected Font          font;
    protected TextDimension dimension;
    protected int           inset;
    /*protected boolean selected, 
                      greyed;
            
    protected BitSet  marked;      // the selected character positions.
    
    */
    public TextItem(int x, int y, byte fontnum, String annottext, String printtext) { 
        super();
        this.fontnum = fontnum;
        this.font = JapeFont.getFont(fontnum);
        this.annottext = annottext;
        this.printchars = printtext.toCharArray();
        this.dimension = JapeFont.measure(printtext, fontnum);
        setBounds((int)x, y-dimension.ascent, dimension.width, dimension.ascent+dimension.descent);
        annoti=printi=0;
        Vector cs = new Vector();
        computeColourSegs((char)0, NormalColour, false, cs);
        coloursegs = (ColourSeg[])cs.toArray(new ColourSeg[cs.size()]);
        inset = 0;
        if (Debugging.TextItem)
            System.err.println("TextItem built "+this);
    }

    /*protected void computeBounds() {
        bounds = canvas.stringSize(text, fontnum);
        boundaries = canvas.getBoundaries(text, fontnum);
        for (int i=0; i<boundaries.length; i++) boundaries[i]+=position.x;
        marked = new BitSet(text.length()+1);
    }*/

    /** 
            index of the closest character boundary to the right of x 
    */
    /*protected int charAt(int x) { 
        // sequential search!
        for (int i=0; i<boundaries.length; i++)
            if (x<boundaries[i]) return i;
        return -1;
    } */

    protected class ColourSeg {
        public final Color colour;
        public final int start, offx;
        public int end;
        public ColourSeg(Color colour, int start, int end) {
            this.colour=colour;
            this.start=start; this.end=end;
            this.offx=JapeFont.charsWidth(printchars, 0, start, fontnum);
        }
        
        public void paint(Graphics g) {
            g.setColor(colour);
            g.drawChars(printchars, start, end-start, inset+offx, inset+dimension.ascent);
        }
        
        public String toString() {
            return "ColourSeg[colour="+colour+
                   ", start="+start+
                   ", end="+end+
                   ", offx="+offx+
                   "]";
        }
    }

    protected ColourSeg[] coloursegs;
    
    protected int annoti, printi; // globals for computing ColourTree

    private void extendColourSeg(Vector cs, Color colour, int start, int end) {
        if (cs.size()!=0) {
            ColourSeg cseg = (ColourSeg)cs.lastElement();
            if (cseg.colour.equals(colour) && cseg.end==start) {
                cseg.end=end; return;
            }
        }
        cs.add(new ColourSeg(colour, start, end));
    }
    
    protected void computeColourSegs(char expectedket, Color colour, boolean locked, Vector cs) {
        int i0 = printi;
        char c;
        while (annoti<annottext.length()) {
            c = annottext.charAt(annoti++);
            if (invisbra(c)) {
                boolean newlocked = locked || c==lockbra;
                Color newcolour = bra2TextColour(c);
                char newket = bra2ket(c);
                extendColourSeg(cs, colour, i0, printi);
                computeColourSegs(newket, newcolour, newlocked, cs);
                i0 = printi;
            }
            else
            if (invisket(c)) {
                if (c==expectedket) {
                    extendColourSeg(cs, colour, i0, printi);
                    return;
                }
                else
                    Alert.abort("TextItem.computeColourSegs saw "+(int)c+
                                ", expected "+(int)expectedket);
            }
            else
                printi++;
        }
        if (expectedket!=0)
            Alert.abort(this+": computeColourSegs exhausted text, "+
                        ", expected "+(int)expectedket);

        if (printi!=printchars.length)
            Alert.abort(this+": text is "+printchars.length+
                        " chars, but computeColourSegs thinks it's "+printi);

        extendColourSeg(cs, colour, i0, printi);
        return;
    }

    /**
        state variables used when dragging
         */
    /*int     firstx,         // x-coord of position at which we started the drag
            lastx,          // most recent dragged co-ordinate
            firstpos;       // character position at which we started the drag      
            
    boolean dragging;       // we're dragging

    public void Press(Point position, int button) {
        if (Debugging.canvas_itemevents) Report("press" + charAt(position.x));
        dragging = false;

        // Debugging
        if (button==2) { 
            marked.clear(); repaint();
        }
    }
    
    public void Release(Point position, int button) {
        if (Debugging.canvas_itemevents) Report("release" + charAt(position.x));
        dragging  = false;
    }
    
    public void Leave(Point position, int button) {
        if (Debugging.canvas_itemevents) Report("leave");
        dragging = false;
    }
    
    public void Drag(Point position, int button) {    
        if (Debugging.canvas_itemevents) Report("drag" + charAt(position.x));
        if (button==canvas.TextSelectButton) {
            int currentpos = charAt(position.x);
            if (dragging && currentpos>=0) {
                int lastcount = marked.count();
                if (firstx<=position.x) { 
                    // Selecting rightwards
                    if (lastx>=position.x)
                        // changed direction means undo
                        marked.rem(currentpos-1,  currentpos);
                    else
                        marked.add(Math.max(firstpos-1, 0), currentpos);
                }
                else { 
                    // Selecting leftwards
                    int leftpos  = Math.max(currentpos-1, 0); 
                    if (lastx<position.x)
                        // changed direction means undo
                        marked.rem(leftpos, leftpos+1);
                    else
                        marked.add(leftpos, firstpos);
                }
                lastx = position.x;
                if (marked.count()!=lastcount) repaint(); 
            }
            else
            if (currentpos>=0) { 
                dragging = true;
                lastx = firstx = position.x;
                firstpos = currentpos;
            }
        }
    }
    */
    /** Repaint this text. */

    public void paint(Graphics g) {
        g.setFont(font);
        int len = coloursegs.length;
        for (int i=0; i<len; i++)
            coloursegs[i].paint(g);
        /* canvas.fonts[fontnum].setGraphics(g);
        // Background painting
        int[]   sel    = marked.runs();
        boolean normal = true;
        int     here   = position.x+canvas.textInset.width;
        int     there;
        
        g.setColor(Color.green);
        for (int i=0; i<sel.length; i++) { 
            there = boundaries[sel[i]];
            if (!normal) g.fillRect(here, position.y, there-here, bounds.height);
            normal = !normal;
            here = there;
        }
            
        g.setColor(greyed?canvas.getGreyedColour():canvas.getNormalColour());
        */
        //g.drawString(printtext, 0, dimension.ascent);     
        /*if (Debugging.text_baselines)
            g.drawLine(position.x, position.y+bounds.height-bounds.descent, position.x+bounds.width-1, position.y+bounds.height-bounds.descent);
        if (selected) {
            g.setColor(canvas.getSelectedColour());
            // java draws a w+1 x h+1 rectangle!
            g.drawRect(position.x, position.y, bounds.width-1, bounds.height-1);
            g.drawRect(position.x+1, position.y+1, bounds.width-3, bounds.height-3);
        }
        */
    }

    public String toString() {
        String s = super.toString()+": [printchars=\"";
        int i;
        for (i=0; i<printchars.length; i++)
            s = s+printchars[i];
        s = s+"\", annottext=..."+", fontnum="+fontnum+", font=..."+", dimension="+dimension+
            ", coloursegs=[";
        for (i=0; i<coloursegs.length; i++) {
            s=s+coloursegs[i];
            if (i+1<coloursegs.length)
                s=s+",";
        }
        return s+"]]"; 
    }
}



