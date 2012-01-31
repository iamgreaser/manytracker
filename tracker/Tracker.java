package tracker;

import session.*;
import player.*;

import java.awt.*;
import java.awt.event.*;
import java.awt.image.*;
import javax.swing.*;

public class Tracker extends JComponent implements KeyListener, MouseListener, MouseMotionListener
{
	public static enum PaneSelection
	{
		SAMPLES(0),
		INSTRUMENTS(1),
		TRACKS(2),
		PATTERNS(3),
		ORDERS(4),
		USERS(5);
		
		public final int idx;
		PaneSelection(int idx)
		{
			this.idx = idx;
		}
	};
	
	public static final String[] NOTE_NAME = {
		"C-", "C#",
		"D-", "D#",
		"E-",
		"F-", "F#",
		"G-", "G#",
		"A-", "A#",
		"B-",
	};
	
	private JFrame frame = new JFrame("ManyTracker");
	private BufferedImage buf = null;
	private int width = 800, height = 600;
	private Session session;
	private Player player;
	
	private int[] lbsel = new int[] {1,1,1,0,0,0};
	private int[] lboffs = new int[6];
	private int[] lbmax = new int[] {256,256,65536,256,256,65536};
	private int[] lbx = new int[7];
	private int lby = 0;
	
	private PaneSelection pane = PaneSelection.PATTERNS;
	private int patrow = 0, patcol = 0, patcoloffs = 0;
	
	public Tracker(Session session)
	{
		this.session = session;
		this.player = new Player(session);
		
		// set size
		setPreferredSize(new Dimension(width, height));
		
		// add me
		frame.add(this);
		
		// add listeners
		frame.addKeyListener(this);
		frame.addMouseListener(this);
		frame.addMouseMotionListener(this);
		addKeyListener(this);
		addMouseListener(this);
		addMouseMotionListener(this);
		
		// prep + show frame
		frame.pack();
		frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		frame.setVisible(true);
		repaint();
	}
	
	public Player getPlayer()
	{
		return this.player;
	}
	
	private void calcWindow()
	{
		// get current dims
		width = getWidth();
		height = getHeight();
		
		// do some checks to see if we need a new draw buffer
		if(buf == null
			|| width < buf.getWidth() - 40
			|| width > buf.getWidth()
			|| height < buf.getHeight() - 40
			|| height > buf.getHeight()
		){
			buf = new BufferedImage(width+20, height+20, BufferedImage.TYPE_INT_ARGB);
		}
		
		// calculate list boxes
		for(int x = 0; x <= 6; x++)
			lbx[x] = (x*width*2+1)/(6*2);
		
		lby = (16*(TrackerFont.FNT_XAIMUS.getHeight()+1)+1);
		
		for(int i = 0; i < 6; i++)
		{
			if(lbsel[i] < lboffs[i])
				lboffs[i] = lbsel[i];
			if(lbsel[i] > lboffs[i]+15)
				lboffs[i] = lbsel[i]-15;
		}
	}
	
	private String getStrNote(int v)
	{
		if(v < 120)
		{
			return String.format("%s%d"
				, NOTE_NAME[v%12]
				, v/12);
		} else if(v == 255) {
			return "===";
		} else if(v == 254) {
			return "^^^";
		} else if(v == 253) {
			return "...";
		} else {
			return "~~~";
		}
	}
	
	private String getStrIns(int v)
	{
		if(v <= 99)
			return String.format("%02d", v);
		else
			return String.format("%c%d", 'A'+(v-10), v%10);
	}
	
	private String getStrVol(int v)
	{
		// TODO: alter notation for pre-voleffects / pre-pan versions
		if(v <= 64)
			return String.format("%02d", v);
		else if(v <= 74)
			return String.format("A%c", '0'+(v-65));
		else if(v <= 84)
			return String.format("B%c", '0'+(v-75));
		else if(v <= 94)
			return String.format("C%c", '0'+(v-85));
		else if(v <= 104)
			return String.format("D%c", '0'+(v-95));
		else if(v <= 114)
			return String.format("E%c", '0'+(v-105));
		else if(v <= 124)
			return String.format("F%c", '0'+(v-115));
		else if(v < 128)
			return "??";
		else if(v <= 192)
			return String.format("%02d", v-128); // TODO: denote this correctly!
		else if(v <= 202)
			return String.format("G%c", '0'+(v-193));
		else if(v <= 212)
			return String.format("H%c", '0'+(v-203));
		else if(v == 255)
			return "..";
		else
			return "??";
	}
	
	private String getStrEft(int v)
	{
		if(v == 0)
			return ".";
		else
			return String.format("%c", 'A'+(v-1));
	}
	
	private int[] dcell = new int[5];
	private void patternPane(Graphics g, int ystart, int yend)
	{
		int cssize = 3+2+2+3  +1;
		int widthChars = width/4;
		int widthChannels = (widthChars-4)/cssize;
		int heightChars = (yend-ystart)/6;
		g.setColor(Color.BLACK);
		
		int offscol = 0;
		int xoffsrow = heightChars/2;
		int offsrow = patrow-xoffsrow;
		
		// TODO: room for track head
		
		SessionPattern pat = session.getPattern(lbsel[3]);
		
		for(int j = 0, y = ystart; j < heightChars; j++, y += 6)
		{
			TrackerFont.FNT_XAIMUS.write(g, String.format("%03d|", j+offsrow), 0, y);
		}
		
		for(int i = 0, x = 4*4; i < widthChannels; i++, x += cssize*4)
		{
			int rows = 0;
			
			SessionTrack trk = null;
			
			if(pat != null)
			{
				rows = pat.getRows();
				trk = pat.getTrack(i+patcoloffs);
			}
			
			for(int j = 0, y = ystart; j < heightChars; j++, y += 6)
			{
				dcell[0] = 253;
				dcell[1] = dcell[3] = dcell[4] = 0;
				dcell[2] = 255;
				
				int r = j+offsrow;
				
				if(r >= 0 && r < rows)
				{
					if(trk != null)
					{
						trk.getData(r, dcell);
						
						TrackerFont.FNT_XAIMUS.write(g,
							String.format("%s%s%s%s%02X|"
								,getStrNote(dcell[0])
								,getStrIns(dcell[1])
								,getStrVol(dcell[2])
								,getStrEft(dcell[3])
								,dcell[4]
									),
							x, y);
						
						if(dcell[2] >= 128 && dcell[2] <= 192)
						{
							g.setXORMode(Color.BLACK);
							g.setColor(Color.WHITE);
							g.fillRect(x + 4*(3+2), y, 2*4, 5);
							g.setPaintMode();
							g.setColor(Color.BLACK);
						}
					}
				}
			}
		}
		
		g.setXORMode(Color.BLACK);
		g.setColor(Color.WHITE);
		g.fillRect(4*4, ystart+6*xoffsrow, width-4*4, 5);
		g.setPaintMode();
		g.setColor(Color.BLACK);
		
		//g.fillRect(1, ystart+1, width-2, yend-ystart-2);
	}
	
	private void redrawBuffer(Graphics g)
	{
		// update bollocks
		synchronized(player)
		{
			if(player.isSequencing())
			{
				if(player.isPatLock())
				{
					lbsel[3] = player.getOrder();
				} else {
					lbsel[4] = player.getOrder();
					if(lbsel[4] < 0)
						lbsel[4] = 0;
					lbsel[3] = session.getOrder(lbsel[4]);
				}
				patrow = player.getRow();
			}
		}
		
		// whiteout
		g.setColor(Color.WHITE);
		g.fillRect(0,0, width, height);
		
		// draw top section
		g.setColor(Color.BLACK);
		for(int x = 0; x < 6; x++)
		{
			int rx = lbx[x];
			int nrx = lbx[x+1];
			
			for(int sy = 0; sy < 16; sy++)
			{
				int ry = sy*(TrackerFont.FNT_XAIMUS.getHeight()+1)+1;
				int y = sy + lboffs[x];
				
				String s = "TODO!";
				
				switch(x)
				{
					case 0: {
						//y++;
						SessionInstrument ins = session.getInstrument(y);
						
						s = String.format(
							"%02d:%s", y,
							(ins == null ? "<X>" : ins.getName())
						);
						
					} break;
					case 1: {
						//y++;
						SessionSample smp = session.getSample(y);
						
						s = String.format(
							"%02d:%s", y,
							(smp == null ? "<X>" : smp.getName())
						);
						
					} break;
					case 2: {
						//y++;
						SessionTrack trk = session.getTrack(y);
						
						s = String.format(
							"%05d:%s", y,
							(trk == null ? "<X>" : "used")
						);
						
					} break;
					case 3: {
						SessionPattern pat = session.getPattern(y);
						
						s = String.format(
							"%03d:%s", y,
							(pat == null ? "<X>" : "used")
						);
						
					} break;
					case 4: {
						int ord = session.getOrder(y);
						
						s = String.format(
							"%03d:%s", y,
							( ord == 255 ? "---"
							: ord == 254 ? "+++"
							: String.format("%03d", ord)
							)
						);
						
					} break;
					case 5:
						break;
				}
				
				TrackerFont.FNT_XAIMUS.write(g, " " + s, rx+2, ry);
				
				if(lbsel[x] == y)
				{
					//g.fillRect(rx+2, ry+1, 3, 3);
					
					g.setXORMode(Color.BLACK);
					g.setColor(Color.WHITE);
					g.fillRect(rx, ry, (nrx-rx), 5);
					g.setPaintMode();
					g.setColor(Color.BLACK);
				}
			}
			
			if(x != 0)
				g.drawLine(rx,0,rx,lby+1);
			
			rx = ((x*2+1)*width+1)/12;
			g.drawLine(rx,lby+3,rx-13,lby+16);
			g.drawLine(rx,lby+3,rx+13,lby+16);
			g.drawLine(rx-13,lby+16,rx+13,lby+16);
			
			if(x == pane.idx)
				g.fillRect(rx-3, lby+8, 7, 7);
		}
		g.drawLine(0,lby+1,width,lby+1);
		g.drawLine(0,lby+18,width,lby+18);
		
		switch(pane)
		{
			case PATTERNS:
				patternPane(g, lby+19, height);
				break;
		}
	}
	
	public void paintComponent(Graphics g)
	{
		// get correct buffer info
		calcWindow();
		
		// draw stuff
		redrawBuffer(buf.getGraphics());
		
		// blit
		g.drawImage(buf, 0, 0, width, height, 0, 0, width, height, this);
	}
	
	// useless pieces of crap
	public void keyTyped(KeyEvent ev) {}
	public void mouseClicked(MouseEvent ev) {}
	
	// key stuff
	public void keyPressed(KeyEvent ev)
	{
		switch(ev.getKeyCode())
		{
			case KeyEvent.VK_F5:
				player.playFromStart();
				break;
			case KeyEvent.VK_F6:
				if((ev.getModifiers() & KeyEvent.SHIFT_MASK) != 0)
					player.playFromOrder(lbsel[4]);
				else
					player.loopPattern(lbsel[3]);
				break;
			case KeyEvent.VK_F7:
				player.playFromRow(lbsel[3],patrow);
				break;
			case KeyEvent.VK_F8:
				player.stop();
				break;
		}
	}
	
	public void keyReleased(KeyEvent ev) {}
	
	// mouse stuff
	private MouseEvent oldEv = null;
	public void mousePressed(MouseEvent ev)
	{
		oldEv = ev;
		
		if(ev.getY() >= 1 && ev.getY() < lby)
		{
			for(int i = 0; i < 6; i++)
				if(ev.getX() > lbx[i] && ev.getX() < lbx[i+1])
				{
					lbsel[i] = lboffs[i] +
						(ev.getY()-1)/(TrackerFont.FNT_XAIMUS.getHeight()+1);
					
					return;
				}
			
			return;
		}
	}
	public void mouseReleased(MouseEvent ev) {}
	public void mouseEntered(MouseEvent ev) {}
	public void mouseExited(MouseEvent ev) {}
	public void mouseMoved(MouseEvent ev) {}
	public void mouseDragged(MouseEvent ev)
	{
		if(oldEv.getY() >= 1 && oldEv.getY() < lby)
		{
			for(int i = 0; i < 6; i++)
				if(oldEv.getX() > lbx[i] && oldEv.getX() < lbx[i+1])
				{
					lbsel[i] = lboffs[i] +
						(ev.getY()-1)/(TrackerFont.FNT_XAIMUS.getHeight()+1);
					
					if(lbsel[i] < 0)
						lbsel[i] = 0;
					if(lbsel[i] > lbmax[i]-1)
						lbsel[i] = lbmax[i]-1;
					
					return;
				}
			
			return;
		}
	}
	
	
	public static void main(String[] args) throws Exception
	{
		Tracker tracker = new Tracker(new Session(args[0]));
		Player player = tracker.getPlayer();
		
		//player.playFromStart();
		while(true)
		{
			player.tick();
			tracker.repaint();
			Thread.yield();
		}
	}
}
