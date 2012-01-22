package session;

import player.*;
import misc.Util;

import java.io.*;
import java.util.*;

public class Session
{
	// network bollocks
	private ArrayList<SessionUser> users = new ArrayList<SessionUser>();
	
	// parts
	private HashMap<Integer,SessionInstrument> map_ins = new HashMap<Integer,SessionInstrument>();
	private HashMap<Integer,SessionSample> map_smp = new HashMap<Integer,SessionSample>();
	private HashMap<Integer,SessionTrack> map_trk = new HashMap<Integer,SessionTrack>();
	private HashMap<Integer,SessionPattern> map_pat = new HashMap<Integer,SessionPattern>();
	private int[] orderlist = new int[256]; {
		for(int i = 0; i < orderlist.length; i++)
			orderlist[i] = 255;
	}
	
	// IMPM bollocks
	
	public static final int FLAG_STEREO = 0x0001;
	public static final int FLAG_VOL0MIX = 0x0002; // useless since 1.04 but we'll set it anyway
	public static final int FLAG_INSMODE = 0x0004;
	public static final int FLAG_LINEAR = 0x0008;
	public static final int FLAG_OLDEFFECTS = 0x0010;
	public static final int FLAG_COMPATGXX = 0x0020;
	public static final int FLAG_MIDICTL = 0x0040;
	public static final int FLAG_MIDICFG = 0x0080;
	
	public static final int SPECIAL_MSG = 0x0001;
	public static final int SPECIAL_UNK1 = 0x0002;
	public static final int SPECIAL_UNK2 = 0x0004;
	public static final int SPECIAL_MIDICFG = 0x0008;
	
	private String name = "";
	private int philigt = 0x1004;
	// ordnum, insnum, smpnum, patnum
	private int cwtv = 0x0217, cmwt = 0x0217;
	private int flags = FLAG_STEREO | FLAG_VOL0MIX | FLAG_LINEAR;
	private int special = 0;
	private int gv = 128, mv = 48, spd = 6, bpm = 125, sep = 128, pwd = 0;
	// msglgth
	private String msg = "";
	private int timestamp = 0x796E616D;
	private byte[] chnpan = new byte[64], chnvol = new byte[64]; {
		for(int i = 0; i < 64; i++)
		{
			chnpan[i] = 32|0;
			chnvol[i] = 64;
		}
	}
	
	public Session()
	{
		
	}
	
	public Session(String fname) throws IOException
	{
		this(new File(fname));
	}
	
	public Session(File fd) throws IOException
	{
		try
		{
			RandomAccessFile mmcmp = new MMCMPUnpacker(fd);
			doTheDamnThing(mmcmp);
		} catch(MMCMPUnpacker.NotPackedException ex) {
			// do it normally
			doTheDamnThing(new RandomAccessFile(fd, "r"));
		}
	}
	
	public Session(RandomAccessFile fp) throws IOException
	{
		doTheDamnThing(fp);
	}
	
	private void doTheDamnThing(RandomAccessFile fp) throws IOException
	{
		byte[] b = new byte[26];
		
		// Load IMPM header
		fp.read(b, 0, 4);
		if(b[0] != 'I' || b[1] != 'M' || b[2] != 'P' || b[3] != 'M')
			throw new RuntimeException("not an IMPM module");
		
		this.name = Util.readString(fp, b, 26);
		System.out.printf("name: \"%s\"\n", name);
		
		this.philigt = 0xFFFF&(int)Short.reverseBytes(fp.readShort());
		int ordnum = 0xFFFF&(int)Short.reverseBytes(fp.readShort());
		int insnum = 0xFFFF&(int)Short.reverseBytes(fp.readShort());
		int smpnum = 0xFFFF&(int)Short.reverseBytes(fp.readShort());
		int patnum = 0xFFFF&(int)Short.reverseBytes(fp.readShort());
		this.cwtv = 0xFFFF&(int)Short.reverseBytes(fp.readShort());
		this.cmwt = 0xFFFF&(int)Short.reverseBytes(fp.readShort());
		this.flags = 0xFFFF&(int)Short.reverseBytes(fp.readShort());
		this.special = 0xFFFF&(int)Short.reverseBytes(fp.readShort());
		this.gv = fp.read();
		this.mv = fp.read();
		this.spd = fp.read();
		this.bpm = fp.read();
		this.pwd = fp.read();
		this.sep = fp.read();
		int msglgth = 0xFFFF&(int)Short.reverseBytes(fp.readShort());
		int msgoffs = Integer.reverseBytes(fp.readInt());
		this.timestamp = Integer.reverseBytes(fp.readInt());
		fp.read(chnpan, 0, 64);
		fp.read(chnvol, 0, 64);
		
		// Load orderlist
		for(int i = 0; i < ordnum; i++)
			orderlist[i] = fp.readUnsignedByte();
		for(int i = ordnum; i < 256; i++)
			orderlist[i] = 255;
		
		// Load pointers
		int[] insptrs = new int[insnum];
		int[] smpptrs = new int[smpnum];
		int[] patptrs = new int[patnum];
		
		for(int i = 0; i < insnum; i++)
			insptrs[i] = Integer.reverseBytes(fp.readInt());
		for(int i = 0; i < smpnum; i++)
			smpptrs[i] = Integer.reverseBytes(fp.readInt());
		for(int i = 0; i < patnum; i++)
			patptrs[i] = Integer.reverseBytes(fp.readInt());
		
		// TODO: read MIDI + timestamp bollocks
		//   TODO: look up and/or reverse engineer said bollocks
		
		// Load data
		for(int i = 0; i < insnum; i++)
		{
			if(insptrs[i] != 0)
			{
				fp.seek(insptrs[i]);
				map_ins.put((Integer)(i+1), new SessionInstrument(fp,
					cmwt < 0x200
						? SessionInstrument.FORMAT_IT100
						: SessionInstrument.FORMAT_IT200
					));
			}
		}
		
		for(int i = 0; i < smpnum; i++)
		{
			if(smpptrs[i] != 0)
			{
				fp.seek(smpptrs[i]);
				map_smp.put((Integer)(i+1), new SessionSample(fp));
			}
		}
		
		for(int i = 0; i < patnum; i++)
		{
			if(patptrs[i] != 0)
			{
				fp.seek(patptrs[i]);
				map_pat.put((Integer)i, new SessionPattern(this, fp));
			}
		}
		
	}
	
	// stuff
	
	public int[] addTracks(SessionTrack ... trks)
	{
		int[] ret = new int[trks.length];
		
		wind: for(int i = 1, j = 0; i <= 0xFFFF && j < trks.length; i++)
		{
			while(trks[j] == null)
			{
				j++;
				if(j >= trks.length)
					break wind;
			}
			
			if(map_trk.get((Integer)i) == null)
			{
				ret[j] = i;
				map_trk.put((Integer)i, trks[j]);
				j++;
			}
		}
		
		return ret;
	}
	
	// byte 0: note, byte 1: instrument
	public int getSampleAndNote(int ins, int note)
	{
		if((flags & FLAG_INSMODE) != 0)
		{
			// instrument mode
			SessionInstrument dta = map_ins.get((Integer)ins);
			return dta == null ? 0|note : dta.getSampleAndNote(note);
		} else {
			// sample mode
			return (ins<<8)|note;
		}
	}
	
	// getters
	
	public SessionInstrument getInstrument(int idx)
	{
		return map_ins.get((Integer)idx);
	}
	
	public SessionSample getSample(int idx)
	{
		return map_smp.get((Integer)idx);
	}
	
	public SessionTrack getTrack(int idx)
	{
		return map_trk.get((Integer)idx);
	}
	
	public SessionPattern getPattern(int idx)
	{
		return map_pat.get((Integer)idx);
	}
	
	public int getOrder(int idx)
	{
		return (idx >= 0 && idx < orderlist.length
			? orderlist[idx]
			: 255
				);
	}
	
	public int getITVersion()
	{
		return cwtv;
	}
	
	public int getTempo()
	{
		return bpm;
	}
	
	public int getSpeed()
	{
		return spd;
	}
	
	public int getGlobalVolume()
	{
		return gv;
	}
	
	public int getMixingVolume()
	{
		return mv;
	}
	
	public int getChannelVolume(int idx)
	{
		return chnvol[idx];
	}
	
	public int getChannelPanning(int idx)
	{
		return chnpan[idx];
	}
	
	public int getFlags()
	{
		return flags;
	}
	
	// setters
	// TODO: range check!
	
	public void setInstrument(int idx, SessionInstrument ins)
	{
		map_ins.put((Integer)idx, ins);
	}
	
	public void setSample(int idx, SessionSample smp)
	{
		map_smp.put((Integer)idx, smp);
	}
	
	public void setTrack(int idx, SessionTrack trk)
	{
		map_trk.put((Integer)idx, trk);
	}
	
	public void setPattern(int idx, SessionPattern pat)
	{
		map_pat.put((Integer)idx, pat);
	}
	
	public void setOrder(int idx, int ord)
	{
		if(idx >= 0 && idx < orderlist.length)
			orderlist[idx] = ord;
	}
}
