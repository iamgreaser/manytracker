package session;

import player.*;
import misc.Util;

import java.io.*;
import java.util.*;

public class Session
{
	// format bollocks
	
	public static final int FORMAT_IT = 1;
	public static final int FORMAT_MOD = 2;
	public static final int FORMAT_S3M = 3;
	public static final int FORMAT_XM = 4;
	
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
		if(b[0] == 'I' && b[1] == 'M' && b[2] == 'P' && b[3] == 'M')
		{
			loadDataIT(fp);
			return;
		}
		
		// Attempt to load XM
		fp.seek(0);
		if(Util.readStringNoNul(fp,b,17).equals("Extended Module: "))
		{
			fp.seek(17+20);
			if(fp.read() == 0x1A)
			{
				fp.seek(17);
				loadDataXM(fp);
				return;
			}
		}
		
		// Attempt to load ST3 module
		fp.seek(0x1C);
		if(fp.readUnsignedShort() == 0x1A10)
		{
			fp.seek(0);
			loadDataS3M(fp);
			return;
		}
		
		fp.seek(0);
		
		// Assume this is a .mod file
		if(true)
		{
			loadDataMOD(fp);
			return;
		}
		
		throw new RuntimeException("module format not supported");
	}
	
	private void loadDataXM(RandomAccessFile fp) throws IOException
	{
		byte[] b = new byte[20];
		
		// WHY THE HELL AM I DOING THIS
		name = Util.readStringNoNul(fp, b, 20);
		System.out.printf("name: \"%s\"\n", name);
		fp.read(); // skip 0x1A byte
		
		// THIS CAN'T BE HAPPENING
		fp.read(b, 0, 20); // skip tracker name
		
		// OH HELL NO
		int xmver = 0xFFFF&(int)Short.reverseBytes(fp.readShort());
		System.out.printf("XM version: %04X\n", xmver);
		
		// WHAT IS THIS CRAP
		InhibitedFileBlock ifb = new InhibitedFileBlock(fp, Integer.reverseBytes(fp.readInt())-4);
		
		// HELP ME PLEASE
		int ordnum = 0xFFFF&(int)Short.reverseBytes(ifb.readShort());
		int respos = 0xFFFF&(int)Short.reverseBytes(ifb.readShort()); // can't be bothered right now --GM
		int chnnum = 0xFFFF&(int)Short.reverseBytes(ifb.readShort()); // yeah sure, allow out of range values
		if(chnnum > 64)
			throw new RuntimeException(String.format("%d-channel modules not supported (max 64)",chnnum));
		int patnum = 0xFFFF&(int)Short.reverseBytes(ifb.readShort());
		int insnum = 0xFFFF&(int)Short.reverseBytes(ifb.readShort());
		int xmflags = 0xFFFF&(int)Short.reverseBytes(ifb.readShort());
		int xmspeed = 0xFFFF&(int)Short.reverseBytes(ifb.readShort());
		int xmtempo = 0xFFFF&(int)Short.reverseBytes(ifb.readShort());
		
		// OH PLEASE, STOP IT
		if(ordnum > 255)
			ordnum = 255;
		if(xmtempo > 255)
			xmtempo = 255;
		if(xmspeed > 255)
			xmspeed = 255;
		this.bpm = xmtempo;
		this.spd = xmspeed;
		this.flags = FLAG_COMPATGXX | FLAG_OLDEFFECTS | FLAG_INSMODE
			| FLAG_STEREO | FLAG_VOL0MIX;
		
		if((xmflags & 0x01) != 0)
			this.flags |= FLAG_LINEAR;
		
		// NONONONONONO
		System.out.printf("chn=%d ordnum=%d tempo=%d speed=%s\n", chnnum, ordnum,xmtempo,xmspeed);
		for(int i = 0; i < 256; i++)
			orderlist[i] = ifb.read();
		for(int i = ordnum; i < 256; i++)
			orderlist[i] = 255;
		
		ifb.done();
		
		// SAVE ME PLEEEEEAAASSSSEEEE
		for(int i = 0; i < patnum; i++)
			map_pat.put((Integer)i, new SessionPattern(this, fp, SessionPattern.FORMAT_XM, chnnum));
		for(int i = 0; i < insnum; i++)
			map_ins.put((Integer)(i+1), new SessionInstrument(fp, SessionInstrument.FORMAT_XM, this));
	}
	
	private void loadDataS3M(RandomAccessFile fp) throws IOException
	{
		byte[] b = new byte[28];
		
		this.name = Util.readString(fp, b, 28);
		System.out.printf("name: \"%s\"\n", name);
		
		fp.readInt(); // first two bytes we've seen, second two are unused
		
		int ordnum = 0xFFFF&(int)Short.reverseBytes(fp.readShort());
		int smpnum = 0xFFFF&(int)Short.reverseBytes(fp.readShort());
		int patnum = 0xFFFF&(int)Short.reverseBytes(fp.readShort());
		int s3flags = 0xFFFF&(int)Short.reverseBytes(fp.readShort());
		int s3cwtv = 0xFFFF&(int)Short.reverseBytes(fp.readShort());
		int ffi = 0xFFFF&(int)Short.reverseBytes(fp.readShort());
		
		fp.readInt(); // should have "SCRM" but really doesn't matter
		
		this.flags = FLAG_OLDEFFECTS | FLAG_VOL0MIX;
		
		this.gv = fp.read()*2;
		this.spd = fp.read();
		this.bpm = fp.read();
		this.mv = fp.read();
		fp.read(); // NO WE DON'T HAVE A GUS
		int dfpflag = fp.read();
		
		if((this.mv & 0x80) != 0)
		{
			this.mv &= ~0x80;
			this.flags |= FLAG_STEREO;
			// XXX: this might need to be done internally in the player
			//      if it turns out that IT does the same damn thing --GM
			this.mv = (this.mv*11+4)>>3;
		}
		
		// skip all that bollocks
		fp.seek(0x40);
		
		// load channel mappings
		// yes, we WILL want these!
		// though i don't think anyone's done anything completely bonkers
		// except Storlek and myself --GM
		int[] st3chnmap = new int[32];
		
		for(int i = 32; i < 64; i++)
		{
			chnvol[i] = (byte)0xC0;
			chnpan[i] = 0x20;
		}
		
		for(int i = 0; i < 32; i++)
		{
			chnpan[i] = 0;
			
			int v = fp.read();
			st3chnmap[i] = v & 0x7F;
			
			// don't enable FM channels!
			chnvol[i] = ((v & 0x80) != 0 && (v & 0x7F) < 16)
				? 0x40
				: (byte)0xC0
					;
		}
		
		// orderlist!
		// DON'T EVEN NEED TO FILTER IT YES :D
		for(int i = 0; i < ordnum; i++)
			orderlist[i] = fp.read();
		for(int i = ordnum; i < 256; i++)
			orderlist[i] = 255;
		
		// load pointers
		int[] smpptrs = new int[smpnum];
		int[] patptrs = new int[patnum];
		
		for(int i = 0; i < smpnum; i++)
			smpptrs[i] = (0xFFFF&(int)Short.reverseBytes(fp.readShort()))*16;
		for(int i = 0; i < patnum; i++)
			patptrs[i] = (0xFFFF&(int)Short.reverseBytes(fp.readShort()))*16;
		
		// load default panning if necessary
		for(int i = 0; i < 32; i++)
		{
			int v = (dfpflag == 252)
				? fp.read()
				: 0x10
					;
			
			int pan = (v & 0x10) != 0
				? (flags & FLAG_STEREO) != 0
					? (v & 0x08) != 0
						? 0xC
						: 0x3
					: 0x7
				: v & 15
			;
			
			// TODO: scale this crap correctly
			chnpan[i] = (byte)((pan+2)<<2);
		}
		
		// load data
		for(int i = 0; i < smpnum; i++)
			if(smpptrs[i] != 0)
			{
				fp.seek(smpptrs[i]);
				map_smp.put((Integer)(i+1), new SessionSample(fp, SessionSample.FORMAT_S3M, ffi));
			}
		for(int i = 0; i < patnum; i++)
			if(patptrs[i] != 0)
			{
				fp.seek(patptrs[i]);
				map_pat.put((Integer)i, new SessionPattern(this, fp, SessionPattern.FORMAT_S3M, st3chnmap));
			}
		
		// XXX: any other crap this needs? --GM
	}
	
	private void loadDataMOD(RandomAccessFile fp) throws IOException
	{
		byte[] b = new byte[23];
		
		this.name = Util.readStringNoNul(fp, b, 20);
		System.out.printf("name: \"%s\"\n", name);
		
		// check to see if we're a 15-sampler
		fp.seek(20+15*30+1);
		boolean poss_15smp = (fp.read() == (byte)0x78);
		
		// get M.K. tag
		fp.seek(20+31*30+1+1+128);
		String mktag = Util.readStringNoNul(fp, b, 4);
		
		// set us up for reading interesting things
		int smpnum = 31;
		int chnnum = 4;
		boolean flt8 = false;
		
		// check mktag for gibberish
		for(int i = 0; i < 4; i++)
		{
			//System.out.printf("%d\n",b[i]);
			if(b[i] < 0x20 || b[i] > 0x7E)
			{
				if(!poss_15smp)
					throw new RuntimeException("not a 15-sample or 31-sample MOD");
				
				System.out.println("15-sample module!");
				smpnum = 15;
				mktag = "****";
				break;
			}
		}
		
		if(mktag.equals("FLT8"))
		{
			flt8 = true;
		} else if(mktag.substring(1,4).equals("CHN")) {
			// check values
			int cct = b[0] - '0';
			if(cct >= 1 && cct <= 9)
				chnnum = cct;
		} else if(mktag.substring(2,4).equals("CH")
			|| mktag.substring(2,4).equals("CN")
				) {
			// check values
			int cct1 = b[0] - '0';
			int cct2 = b[1] - '0';
			if(cct1 >= 0 && cct1 <= 9)
				if(cct2 >= 0 && cct2 <= 9)
				{
					int cct = cct1*10+cct2;
					
					if(cct >= 1)
						chnnum = cct;
				}
		}
		
		// I forget what else is used.
		
		if(chnnum > 64)
			throw new RuntimeException(String.format("%d-channel modules not supported (max 64)",chnnum));
		
		if(flt8)
			throw new RuntimeException("TODO: FLT8 loader");
		
		// continue
		fp.seek(20);
		
		// load sample headers
		for(int i = 0; i < smpnum; i++)
			map_smp.put((Integer)(i+1), new SessionSample(fp, SessionSample.FORMAT_MOD));
		
		// load order count
		int ordnum = fp.read();
		
		// ignore this next byte (well, at least for now)
		fp.read();
		
		// load orderlist + calculate patnum
		int patnum = 0;
		for(int i = 0; i < 128; i++)
		{
			int v = orderlist[i] = fp.read();
			
			if(v > patnum)
				patnum = v;
		}
		patnum++;
		
		// fix up orderlist
		for(int i = ordnum; i < 256; i++)
			orderlist[i] = 255;
		
		// skip M.K. tag
		if(smpnum == 31)
			fp.readInt();
		
		// load patterns
		for(int i = 0; i < patnum; i++)
			map_pat.put((Integer)i, new SessionPattern(this, fp, SessionPattern.FORMAT_MOD, chnnum));
		
		// load sample data
		for(int i = 0; i < smpnum; i++)
			map_smp.get((Integer)(i+1)).loadSampleDataMOD(fp);
		
		// set everything else!
		this.flags = FLAG_COMPATGXX | FLAG_OLDEFFECTS | FLAG_STEREO | FLAG_VOL0MIX;
		this.sep = 64;
	}
	
	private void loadDataIT(RandomAccessFile fp) throws IOException
	{
		byte[] b = new byte[26];
		
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
				map_smp.put((Integer)(i+1), new SessionSample(fp, SessionSample.FORMAT_IT));
			}
		}
		
		for(int i = 0; i < patnum; i++)
		{
			if(patptrs[i] != 0)
			{
				fp.seek(patptrs[i]);
				map_pat.put((Integer)i, new SessionPattern(this, fp, SessionPattern.FORMAT_IT));
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
	
	public int[] addSamples(SessionSample ... smps)
	{
		int[] ret = new int[smps.length];
		
		wind: for(int i = 1, j = 0; i <= 0xFF && j < smps.length; i++)
		{
			while(smps[j] == null)
			{
				j++;
				if(j >= smps.length)
					break wind;
			}
			
			if(map_smp.get((Integer)i) == null)
			{
				ret[j] = i;
				map_smp.put((Integer)i, smps[j]);
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
	
	public int getPanSep()
	{
		return sep;
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
