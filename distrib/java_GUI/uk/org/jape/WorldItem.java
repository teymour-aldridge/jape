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

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Component;
import java.awt.Container;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.Rectangle;

import java.awt.event.MouseEvent;

import java.awt.geom.Ellipse2D;

import java.awt.image.BufferedImage;

import java.util.Enumeration;
import java.util.Vector;

import javax.swing.JFrame;
import javax.swing.JLayeredPane;
import javax.swing.SwingUtilities;

public class WorldItem extends DisplayItem implements DebugConstants, MiscellaneousConstants,
                                                      SelectionConstants,
                                                      LineTarget, LabelTarget, WorldTarget, TileTarget {

    protected WorldCanvas canvas;
    protected JFrame window;
    protected JLayeredPane layeredPane;
    protected Container contentPane;
    protected SelectionRing selectionRing;
    protected Ellipse2D.Float outline;

    private final int x0, y0, radius, labelgap;
    private int labelx;
    private Vector labelv = new Vector(), // labels attached to me
                   fromv  = new Vector(), // lines which lead from me
                   tov    = new Vector(); // lines which lead to me
    
    public WorldItem(WorldCanvas canvas, JFrame window, int x, int y) {
        super(x, y);
        this.canvas = canvas; this.window = window;
        this.layeredPane = window.getLayeredPane();
        this.contentPane = window.getContentPane();
        this.x0 = x; this.y0 = -y;
        this.radius = canvas.worldRadius();
        setBounds(x0-radius, y0-radius, 2*radius, 2*radius);

        selectionRing = new SelectionRing(x0, y0, radius+2*canvas.linethickness);
        canvas.add(selectionRing);

        outline = new Ellipse2D.Float(0, 0, getWidth(), getHeight());
        if (geometry_tracing)
            System.err.println("world bounds are "+getBounds()+"; outline is "+outline.getBounds2D());

        setForeground(Preferences.WorldColour);

        labelx = selectionRing.getX()+selectionRing.getWidth()+canvas.linethickness;
        labelgap = 4*canvas.linethickness;

        addJapeMouseListener(new JapeMouseAdapter() {
            private byte clickKind, dragKind;
            public void clicked(MouseEvent e) {
                if (clickKind==WorldClick)
                    Reply.sendCOMMAND("worldselect "+idX+" "+idY);
            }
            public void pressed(MouseEvent e) {
                WorldItem.this.canvas.getProofWindow().claimDisproofFocus();
                dragKind = LocalSettings.mousePressWorldItemMeans(e);
                clickKind = LocalSettings.mouseClickWorldItemMeans(e);
                WorldItem.this.pressed(dragKind, e);
            }
            public void dragged(boolean wobbly, MouseEvent e) {
                if (wobbly)
                    WorldItem.this.dragged(dragKind, e); // don't take notice of small movements
            }
            public void released(MouseEvent e) {
                WorldItem.this.released(dragKind, e);
            }
        });
    }

    private boolean alreadyFrom(WorldItem from) {
        for (int i=0; i<tov.size(); i++) {
            WorldConnector wc1 = (WorldConnector)tov.get(i);
            if (wc1.from==from)
                return true;
        }
        return false;
    }
    
    public void registerTo(WorldConnector wc) {
        if (!alreadyFrom(wc.from))
            tov.add(wc);
    }

    public boolean alreadyTo(WorldItem to) {
        for (int i=0; i<fromv.size(); i++) {
            WorldConnector wc1 = (WorldConnector)fromv.get(i);
            if (wc1.to==to)
                return true;
        }
        return false;
    }
    
    public void registerFrom(WorldConnector wc) {
        if (!alreadyTo(wc.to))
            fromv.add(wc);
    }

    public void addlabel(String s) {
        WorldLabel label = new WorldLabel(canvas, window, this, labelx, y0, s);
        canvas.add(label);
        labelv.add(label);
        labelx += label.getWidth()+labelgap;
    }
    
    public void select(boolean selected) {
        selectionRing.select(selected);
    }

    public void paint(Graphics g) {
        if (paint_tracing)
            System.err.println("painting WorldItem at "+getX()+","+getY());
        g.setColor(getForeground());
        if (g instanceof Graphics2D) {
            if (antialias_tracing) {
                System.err.print("blob hints "+((Graphics2D)g).getRenderingHints());
                if (japeserver.onMacOS)
                    System.err.println(" hwaccel "+System.getProperty("com.apple.hwaccel"));
                else
                    System.err.println();
            }
            ((Graphics2D)g).fill(outline);
        }
        else
            g.fillOval(0, 0, getWidth(), getHeight());
    }
    
    protected class SelectionRing extends CircleItem {
        private boolean selected;

        SelectionRing(int x, int y, int radius) {
            super(WorldItem.this.canvas, x, y, radius);
            setForeground(Preferences.SelectionColour);
            if (geometry_tracing)
                System.err.println("ring bounds are "+getBounds()+
                                   "; outline is "+this.outline.getBounds2D());
        }

        public void paint(Graphics g) {
            if (selected) super.paint(g);
        }

        public void select(boolean selected) {
            this.selected = selected;
            WorldItem.this.canvas.imageRepaint(); repaint();
        }
    }

    private boolean draghighlight;
    Color oldForeground;

    private void setDragHighlight(boolean state) {
        if (state && !draghighlight) {
            draghighlight = true;
            oldForeground = getForeground();
            setForeground(Preferences.SelectionColour);
            if (worldpaint_tracing)
                System.err.println("highlighting world");
            canvas.imageRepaint(); repaint();
        }
        else
            if (!state && draghighlight) {
                draghighlight = false;
                setForeground(oldForeground);
                if (worldpaint_tracing)
                    System.err.println("de-highlighting world");
                canvas.imageRepaint(); repaint();
            }
    }

    /* ****************************** world as drag target ****************************** */

    private boolean novelLabel(String label) { // only have the label once, thankyou
        for (int i=0; i<labelv.size(); i++)
            if (label.equals(((WorldLabel)labelv.get(i)).text))
                return false;
        return true;
    }

    private boolean acceptDrag(Object o) {
        if (o instanceof Tile)
            return novelLabel((String)((Tile)o).text);
        else
        if (o instanceof WorldItem)
            return o!=this && !alreadyFrom((WorldItem) o);
        else
        if (o instanceof WorldLabel)
            return novelLabel((String)((WorldLabel)o).text);
        else
            return false;
    }

    private boolean dragEnter(boolean ok) { 
        if (ok) { 
            setDragHighlight(true); return true;
        }
        else
            return false;
    }

    private void dragExit() {
        setDragHighlight(false);
    }

    // LineTarget
    public boolean dragEnter(WorldConnector l) {
        return dragEnter(l.from!=this && l.to!=this /*&& !(alreadyFrom(l.from) && alreadyTo(l.to))*/);
    }
    public void dragExit(WorldConnector l) { dragExit(); }
    public void drop(WorldConnector l) {
        if (draghighlight) {
            Reply.sendCOMMAND("splitworldlink "+l.from.idX+" "+l.from.idY+
                                            " "+l.to.idX+" "+l.to.idY+
                                            " "+idX+" "+idY);
            setDragHighlight(false);
        }
        else
            Alert.abort("line drop on non-accepting world");
    }
    
    // LabelTarget
    public boolean dragEnter(WorldItem w, String label) { return dragEnter(novelLabel(label)); }
    public void dragExit(WorldItem w, String label) { dragExit(); }
    public void drop(WorldItem w, String label) {
        if (draghighlight) {
            Reply.sendCOMMAND("addworldlabel "+idX+" "+idY+" "+"\""+label+"\"");
            setDragHighlight(false);
        }
        else
            Alert.abort("label drop on non-accepting world");
    }
    
    // TileTarget
    public boolean dragEnter(Tile t) { return dragEnter(novelLabel(t.text)); }
    public void dragExit(Tile t) { dragExit(); }
    public void drop(Tile t) {
        if (draghighlight) {
            Reply.sendCOMMAND("addworldlabel "+idX+" "+idY+" "+"\""+t.text+"\"");
            setDragHighlight(false);
        }
        else
            Alert.abort("tile drop on non-accepting world");
    }
    
    // WorldTarget
    public boolean dragEnter(byte dragKind, WorldItem w) {
        return dragEnter(w!=this && !(dragKind==NewWorldDrag && alreadyFrom(w)));
    }
    public void dragExit(byte dragKind, WorldItem w) { dragExit(); }
    public void drop(byte dragKind, WorldItem w, int x, int y) {
        if (draghighlight)
            Reply.sendCOMMAND((dragKind==MoveWorldDrag ? "moveworld" : "addworld")+
                              " "+w.idX+" "+w.idY+" "+idX+" "+idY);
        else
            Alert.abort("world drop on non-accepting world");
    }
    
    /* ****************************** world as drag source ****************************** */

    private int startx, starty, lastx, lasty, offsetx, offsety, centreoffsetx, centreoffsety;
    private boolean firstDrag;

    private void pressed(byte dragKind, MouseEvent e) {
        startx = e.getX(); starty = e.getY(); firstDrag = true;
    }

    protected class WorldImage extends DragImage {
        public final byte dragKind;
        public WorldImage(byte dragKind) {
            super(Transparent); this.dragKind = dragKind;
            include(WorldItem.this);
            if (selectionRing.selected && dragKind==MoveWorldDrag)
                include(selectionRing);
            for (int i=0; i<labelv.size(); i++)
                include((WorldLabel)labelv.get(i));
            fixImage();
        }
    }

    private WorldImage worldImage;
    private WorldTarget over;
    private Class targetClass;

    private void setDrageesVisible(boolean state) {
        this.setVisible(state); selectionRing.setVisible(state);
        for (int i=0; i<labelv.size(); i++)
            ((WorldLabel)labelv.get(i)).setVisible(state);
        for (int i=0; i<fromv.size(); i++)
            ((WorldConnector)fromv.get(i)).setVisible(state);
        for (int i=0; i<tov.size(); i++)
            ((WorldConnector)tov.get(i)).setVisible(state);
        canvas.forcerepaint();
    }

    public Point dragCentre() {
        return SwingUtilities.convertPoint(this, radius, radius, layeredPane);
    }

    public void addLine(WorldItem w, boolean dragParent) {
        DragWorldLine dl = new DragWorldLine(w, worldImage.getX()+offsetx+radius,
                                             worldImage.getY()+offsety+radius,
                                             canvas.linethickness, dragParent);
        worldImage.addFriend(dl);
        layeredPane.add(dl);
        dl.repaint();
    }
    
    public void dragged(byte dragKind, MouseEvent e) {
        if (firstDrag) {
            firstDrag = false;
            try {
                targetClass = Class.forName("WorldTarget");
            } catch (ClassNotFoundException exn) {
                Alert.abort("can't make WorldTarget a Class");
            }
            over = null;
            worldImage = new WorldImage(dragKind);
            Point p = worldImage.getImageLocation();
            offsetx = getX()-p.x; offsety = getY()-p.y;
            centreoffsetx = offsetx+radius; centreoffsety = offsety+radius;
            layeredPane.add(worldImage, JLayeredPane.DRAG_LAYER);
            worldImage.setLocation(
                SwingUtilities.convertPoint(this, e.getX()-startx-offsetx,
                                                  e.getY()-starty-offsety,
                                                  layeredPane));
            worldImage.repaint();
            switch (dragKind) {
                case MoveWorldDrag:
                    setDrageesVisible(false);
                    for (int i=0; i<fromv.size(); i++)
                        addLine(((WorldConnector)fromv.get(i)).to, true);
                    for (int i=0; i<tov.size(); i++)
                        addLine(((WorldConnector)tov.get(i)).from, false);
                    break;
                case NewWorldDrag:
                    addLine(this, false);
                    break;
                default:
                    Alert.abort("WorldItem.dragged dragKind="+dragKind);
            }
            if (dragKind==MoveWorldDrag && canvas.worldCount()==1)
                canvas.wasteBin.setEnabled(false);
        }
        else {
            if (drag_tracing)
                System.err.print("mouse dragged to "+e.getX()+","+e.getY());
            int deltax = e.getX()-lastx, deltay = e.getY()-lasty;
            worldImage.moveBy(deltax, deltay);
            if (drag_tracing)
                System.err.println("; dragged world now at "+worldImage.getX()+","+worldImage.getY());
        }
            
        Point p = SwingUtilities.convertPoint(this, e.getX(), e.getY(), contentPane);
        WorldTarget target = (WorldTarget)japeserver.findTargetAt(targetClass, contentPane, p.x, p.y);
        if (target!=over) {
            if (over!=null) {
                over.dragExit(dragKind, WorldItem.this); over=null;
            }
            if (target!=null && target.dragEnter(dragKind, WorldItem.this))
                over = target;
        }
        lastx = e.getX(); lasty = e.getY();
    }

    protected void released(final byte dragKind, MouseEvent e) {
        if (drag_tracing)
            System.err.println("mouse released at "+e.getX()+","+e.getY()+
                               "; dragged world at "+worldImage.getX()+","+worldImage.getY());
        if (over==null)
            new Flyback(worldImage, worldImage.getLocation(),
                        SwingUtilities.convertPoint(this, -offsetx, -offsety, layeredPane)) {
                protected void finishFlyback() {
                    finishDrag();
                    if (dragKind==MoveWorldDrag)
                        setDrageesVisible(true);
                }
            };
        else {
            Point p = SwingUtilities.convertPoint(layeredPane, worldImage.getX()+offsetx+radius,
                                                  worldImage.getY()+offsety+radius, (Component)over);
            finishDrag();
            over.drop(dragKind, this, p.x, p.y);
        }
    }

    protected void finishDrag() {
        layeredPane.remove(worldImage);
        for (Enumeration e = worldImage.friends(); e.hasMoreElements(); )
            layeredPane.remove((DragWorldLine)e.nextElement());
        layeredPane.repaint();
        canvas.wasteBin.setEnabled(true);
    }
}