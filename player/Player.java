package player;

import session.*;

import javax.sound.sampled.*;

public class Player
{
	private static final int BUFLEN = 4096;
	
	private Session session;
	
	// playback info
	private PlayerChannel[] chns = new PlayerChannel[64]; {
		for(int i = 0; i < chns.length; i++)
			chns[i] = new PlayerChannel(this);
	}
	
	private VirtualChannel[] vchns = new VirtualChannel[256]; {
		// TODO speed up the reso filter mixer
		for(int i = 0; i < vchns.length; i++)
			vchns[i] = new VirtualChannel(this);
	}
	
	private boolean playing = false;
	private boolean sequencing = false;
	private boolean patlock = false;
	
	private int speed = 6;
	private int tempo = 125;
	private int gvol = 128;
	
	private int itversion = 0x0217;
	// backwards compatibility.
	// we're the only guys that can at least pretend to be bothered.
	
	private int curord = -1;
	private SessionPattern curpat = null;
	private int currow = -2, procrow = -2, breakrow = 0;
	private int tickctr = 1;
	private int rowctr = 1;
	private boolean rowctr_set = false;
	
	private int base_freq = 44100;
	private int[] fbuf_trk = new int[5];
	private float[][] mixbuf = new float[2][BUFLEN];
	private byte[] mixbuf_final = new byte[BUFLEN*4];
	private int fmt_bits = 16;
	private int fmt_chns = 2;
	private boolean fmt_signed = true;
	private boolean fmt_bigend = false;
	
	private float mixoffs1 = 0.0f, mixoffs2 = 0.0f;
	private float mixoffs_spd = calcMixOffsetSpeed();
	
	private AudioFormat afmt = null;
	private SourceDataLine aufp = null;
	
	public Player(Session session)
	{
		this.session = session;
		
		openSound();
	}
	
	public void setSession(Session session)
	{
		// TODO: any necessary locks
		this.session = session;
	}
	
	private void tryLine(float freq, int bits, int chns, boolean signed, boolean bigend)
	{
		if(aufp != null)
			return;
		
		base_freq = (int)freq;
		fmt_bits = bits;
		fmt_chns = chns;
		fmt_signed = signed;
		fmt_bigend = bigend;
		System.out.printf("attempt: freq = %d, bits = %d, chns = %d, %ssigned, %s-endian\n"
				, base_freq
				, bits
				, chns
				, signed ? "" : "un"
				, bigend ? "big" : "little"
					);
		try
		{
			afmt = new AudioFormat(freq, bits, chns, signed, bigend);
			aufp = AudioSystem.getSourceDataLine(afmt);
			System.out.println("LINE SELECTED!");
		} catch(IllegalArgumentException ex) {
			System.out.println("IllegalArgumentException");
			aufp = null;
		} catch(LineUnavailableException ex) {
			System.out.println("LineUnavailableException");
			aufp = null;
		}
	}
	
	private void openSound()
	{
		try
		{
			aufp = null;
			System.out.println("Attempting to open audio line");
			// BRUTE FORCE THAT CRAP
			// TODO: actually look for a nice line
			tryLine(48000.0f, 16, 2, true, false); // just in case!
			
			tryLine(44100.0f, 16, 2, true, false);
			tryLine(44100.0f, 16, 2, false, false);
			tryLine(44100.0f, 16, 2, true, true);
			tryLine(44100.0f, 16, 2, false, true);
			tryLine(44100.0f, 8, 2, true, false);
			tryLine(44100.0f, 8, 2, false, false);
			tryLine(22050.0f, 16, 2, true, false);
			tryLine(22050.0f, 16, 2, false, false);
			tryLine(22050.0f, 16, 2, true, true);
			tryLine(22050.0f, 16, 2, false, true);
			tryLine(22050.0f, 8, 2, true, false);
			tryLine(22050.0f, 8, 2, false, false);
			tryLine(44100.0f, 16, 1, true, false);
			tryLine(44100.0f, 16, 1, false, false);
			tryLine(44100.0f, 16, 1, true, true);
			tryLine(44100.0f, 16, 1, false, true);
			tryLine(44100.0f, 8, 1, true, false);
			tryLine(44100.0f, 8, 1, false, false);
			tryLine(22050.0f, 16, 1, true, false);
			tryLine(22050.0f, 16, 1, false, false);
			tryLine(22050.0f, 16, 1, true, true);
			tryLine(22050.0f, 16, 1, false, true);
			tryLine(22050.0f, 8, 1, true, false);
			tryLine(22050.0f, 8, 1, false, false);
			
			aufp.open(afmt);
			aufp.start();
			System.out.println("Sound started!");
		} catch(Exception ex) {
			// TODO: handle this more cleanly
			throw new RuntimeException(ex);
		}
	}
	
	private synchronized void resetPlayback()
	{
		playing = false;
		sequencing = false;
		patlock = false;
		
		for(int i = 0; i < chns.length; i++)
			chns[i].reset();
		for(int i = 0; i < vchns.length; i++)
			vchns[i].reset();
		
		tempo = session.getTempo();
		speed = session.getSpeed();
		gvol = session.getGlobalVolume();
		itversion = session.getITVersion();
		//itversion = 0x217; // TEST: IT 2.14p5 (the only version people care about)
		//itversion = 0x209; // TEST: IT 2.09 (plenty of fun bugs)
		//itversion = 0x206; // TEST: IT 2.06 (two versions before voleffects (I DON'T HAVE 2.07!))
		
		for(int i = 0; i < 64; i++)
		{
			chns[i].setChannelVolume(session.getChannelVolume(i));
			chns[i].setChannelPanning(session.getChannelPanning(i));
		}
		
		curord = -1;
		curpat = null;
		currow = -2; procrow = -2; breakrow = 0;
		tickctr = 1;
		rowctr = 1;
	}
	
	private synchronized void findPatternOrder(int pat)
	{
		for(int i = 0; i < 257; i++)
		{
			int ordval = (i < 256 ? session.getOrder(i) : 255);
			
			if(ordval == 255)
			{
				patlock = true;
				curpat = session.getPattern(pat);
				curord = pat;
				return;
			}
			
			if(ordval >= 0 && ordval <= 253 && pat == ordval)
			{
				patlock = false;
				setOrderDeferred(i);
				return;
			}
		}
	}
	
	public synchronized void playFromStart()
	{
		sequencing = false;
		patlock = false;
		resetPlayback();
		sequencing = true;
		playing = true;
	}
	
	public synchronized void stop()
	{
		sequencing = false;
		playing = false;
		patlock = false;
		resetPlayback();
	}
	
	public synchronized void playFromOrder(int ord)
	{
		sequencing = false;
		patlock = false;
		resetPlayback();
		setOrderDeferred(ord);
		sequencing = true;
		playing = true;
	}
	
	public synchronized void loopPattern(int pat)
	{
		sequencing = false;
		resetPlayback();
		patlock = true;
		curpat = session.getPattern(pat);
		curord = pat;
		sequencing = true;
		playing = true;
	}
	
	public synchronized void playFromRow(int pat, int row)
	{
		sequencing = false;
		patlock = false;
		resetPlayback();
		findPatternOrder(pat);
		breakRowDeferred(row);
		sequencing = true;
		playing = true;
	}
	
	public synchronized void playNote()
	{
		// TODO!
	}
	
	private void writeMix(byte[] b, int offs, int len)
	{
		aufp.write(b, offs, len*fmt_chns*((fmt_bits+7)/8));
	}
	
	private void doMix(float[][] f, byte[] b, int offs, int len)
	{
		int end = offs + len;
		
		for(int i = offs; i < end; i++)
			//f[0][i] = f[1][i] = ((0.005f*(float)i) % 1.0f) * 2.0f - 1.0f;
			f[0][i] = f[1][i] = 0.0f;
		
		for(int i = 0; i < vchns.length; i++)
			vchns[i].mix(f, offs, len);
		
		float svol = (float)(gvol * session.getMixingVolume())/(1<<(7+7));
		
		// TODO stick in a compressor just in case some egg decided to use modplug
		svol *= 0.4f;
		
		for(int i = offs, j = offs*4; i < end; i++)
		{
			float fv1 = f[0][i] * svol;
			float fv2 = f[1][i] * svol;
			
			if(fv1 - mixoffs1 > 1.0f)
			{
				mixoffs1 = fv1 - 1.0f;
				//fv1 = 1.0f;
			}
			
			if(fv1 - mixoffs1 < -1.0f)
			{
				mixoffs1 = fv1 + 1.0f;
				//fv1 = -1.0f;
			}
			
			if(fv2 - mixoffs2 > 1.0f)
			{
				mixoffs2 = fv2 - 1.0f;
				//fv2 = 1.0f;
			}
			
			if(fv2 - mixoffs2 < -1.0f)
			{
				mixoffs2 = fv2 + 1.0f;
				//fv2 = -1.0f;
			}
			
			/*
			if(fv1 > 1.0f)
				fv1 = 1.0f;
			if(fv1 < -1.0f)
				fv1 = -1.0f;
			if(fv2 > 1.0f)
				fv2 = 1.0f;
			if(fv2 < -1.0f)
				fv2 = -1.0f;
			*/
			
			int v1 = (int)((fv1 - mixoffs1)*32767.0f);
			int v2 = (int)((fv2 - mixoffs2)*32767.0f);
			
			if(!fmt_signed)
			{
				v1 += 0x8000;
				v2 += 0x8000;
			}
			
			if(fmt_bigend)
				b[j++] = (byte)((v1>>8)&255);
			if(fmt_bits >= 16)
				b[j++] = (byte)(v1&255);
			if(!fmt_bigend)
				b[j++] = (byte)((v1>>8)&255);
			
			if(fmt_chns >= 2)
			{
				if(fmt_bigend)
					b[j++] = (byte)((v2>>8)&255);
				if(fmt_bits >= 16)
					b[j++] = (byte)(v2&255);
				if(!fmt_bigend)
					b[j++] = (byte)((v2>>8)&255);
			}
			
			if(mixoffs1 > 0.0f)
			{
				mixoffs1 -= mixoffs_spd;
				if(mixoffs1 < 0.0f)
					mixoffs1 = 0.0f;
			} else {
				mixoffs1 += mixoffs_spd;
				if(mixoffs1 > 0.0f)
					mixoffs1 = 0.0f;
			}
			
			if(mixoffs2 > 0.0f)
			{
				mixoffs2 -= mixoffs_spd;
				if(mixoffs2 < 0.0f)
					mixoffs2 = 0.0f;
			} else {
				mixoffs2 += mixoffs_spd;
				if(mixoffs2 > 0.0f)
					mixoffs2 = 0.0f;
			}
		}
	}
	
	private float calcMixOffsetSpeed()
	{
		return (float)(1.0 / (base_freq*10.0));
	}
	
	public int calcSlide1(int base, int amt)
	{
		return (int)(0.5 + base * Math.pow(2.0f, amt / 12.0f));
	}
	
	public int calcSlide2(int base, int amt)
	{
		return (int)(0.5 + base * Math.pow(2.0f, amt / (2.0f * 12.0f)));
	}
	
	public int calcSlide16(int base, int amt)
	{
		return calcSlide64(base, amt*4);
	}
	
	public int calcSlide64IT(int base, int amt)
	{
		return (int)(0.5 + base * Math.pow(2.0f, amt / (64.0f * 12.0f)));
	}
	
	public int calcSlide64(int base, int amt)
	{
		if(hasLinearSlides())
			return calcSlide64IT(base, amt);
		else {
			int amiclk = 8363*428*4;
			
			int oper = amiclk / base;
			int nper = oper - amt;
			
			if(nper < 1)
				nper = 1;
			return amiclk / nper;
		}
	}
	
	public void tick()
	{
		if(!playing)
			return;
		
		tickData();
		
		int len = (int)((getFreq() * 5.0f) / (tempo * 2.0f));
		//System.out.printf("len %d\n", len);
		while(len > BUFLEN)
		{
			doMix(mixbuf, mixbuf_final, 0, BUFLEN);
			writeMix(mixbuf_final, 0, BUFLEN);
			len -= BUFLEN;
		}
		
		doMix(mixbuf, mixbuf_final, 0, len);
		writeMix(mixbuf_final, 0, len);
	}
	
	private void tickData()
	{
		if(!sequencing)
			return;
		
		// general flow as given in ITTECH.TXT.
		// NOT DEVIATING FROM IT (though some values may change).
		
		// Set note volume to volume set for each channel
		// Set note frequency to frequency set for each channel
		for(int i = 0; i < chns.length; i++)
			chns[i].calcOutputVolFreq();
		
		// Decrease tick counter
		// Is tick counter 0 ?
		
		if(--tickctr <= 0)
		{
			// Yes
			// Tick counter = Tick counter set (the current 'speed')
			tickctr = speed;
			
			// Decrease Row counter.
			// Is row counter 0?
			if(--rowctr <= 0)
			{
				// Yes
				// Row counter = 1
				rowctr = 1;
				rowctr_set = false;
				
				System.out.printf("O=%03d R=%03d\n", curord, currow);
				
				int rows = (curpat == null ? 64 : curpat.getRows());
				
				// XXX: probably not quite how it goes,
				// but at least the SBx / Cxx quirk works
				if(procrow == -3)
				{
					procrow = breakrow;
					breakrow = 0;
				}
				
				// Increase ProcessRow
				// Is ProcessRow > NumberOfRows?
				if(++procrow >= rows || procrow < 0)
				{
					// Yes
					
					// not going to bother c/p'ing this part
					procrow = breakrow;
					breakrow = 0;
					nextOrder();
					rows = (curpat == null ? 64 : curpat.getRows());
					
					// FIXME: work out what it does when procrow is out of range.
					// going to assume this...
					if(procrow >= rows || procrow < 0)
						procrow = 0;
					
				} // (otherwise: No)
				
				// CurrentRow = ProcessRow
				currow = procrow;
				
				// Update Pattern Variables (includes jumping to
				// the appropriate row if requried and getting
				// the NumberOfRows for the pattern)
				
				fbuf_trk[0] = 253;
				fbuf_trk[1] = 0;
				fbuf_trk[2] = 255;
				fbuf_trk[3] = 0;
				fbuf_trk[4] = 0;
				
				for(int c = 0; c < 64; c++)
				{
					if(curpat != null)
					{
						SessionTrack trk = curpat.getTrack(c);
						if(trk != null)
							trk.getData(currow, fbuf_trk);
						else {
							fbuf_trk[0] = 253;
							fbuf_trk[1] = 0;
							fbuf_trk[2] = 255;
							fbuf_trk[3] = 0;
							fbuf_trk[4] = 0;
						}
					}
					
					chns[c].update0(
						fbuf_trk[0],
						fbuf_trk[1],
						fbuf_trk[2],
						fbuf_trk[3],
						fbuf_trk[4]);
				}
			} else {
				// No
				// Call update-effects for each channel.
				for(int c = 0; c < 64; c++)
					chns[c].updateN();
			}
		} else {
			// No
			// Update effects for each channel as required.
			for(int c = 0; c < 64; c++)
				chns[c].updateN();
		}
		
		// ok, this isn't denoted in ITTECH,TXT but "were making this hapen"
		// update the virtual channels.
		for(int c = 0; c < 64; c++)
			chns[c].updateVirtualChannel();
		
		// Instrument mode?
		if((session.getFlags() & Session.FLAG_INSMODE) != 0)
		{
			// Yes
			// Update Envelopes as required
			// Update fadeout as required
			for(int c = 0; c < vchns.length; c++)
				vchns[c].updateEnvelopes();
		}
		
		// and the rest is done in mix().
	}
	
	public VirtualChannel allocateVirtualChannel()
	{
		// using pre-2.03 NNA allocation as 2.03+ isn't documented
		
		/*
		notes on this:
		
		2.03:
		absolutely nothing.
		first ITTECH.DOC/TXT to mention this change was 2.08's
		(or 2.07's, but I don't have that so I can't know right now).
		
		2.08:
		- Player Improvement: NNA mechanism will eliminate channels on two extra
		  conditions now (no difference to playback, but should
		  maximise channel usage)
		*/
		
		// check for stopped channels
		for(int i = 0; i < vchns.length; i++)
			if(!vchns[i].isActive())
				return vchns[i];
		
		// choose quietest backgrounded channel
		float quietest_vol = 0.0f;
		int quietest_idx = -1;
		
		for(int i = 0; i < vchns.length; i++)
		{
			if(!vchns[i].isForeground())
			{
				float cvol = vchns[i].getCalculatedVol();
				if(quietest_idx == -1 || cvol < quietest_vol)
				{
					quietest_idx = i;
					quietest_vol = cvol;
				}
			}
		}
		
		return quietest_idx == -1
			? null
			: vchns[quietest_idx]
				;
	}
	
	private void nextOrder()
	{
		if(patlock)
			return;
		
		curord++;
		while(session.getOrder(curord) == 254)
			curord++;
		if(session.getOrder(curord) == 255)
			curord = 0;
		
		// not mentioned but i'm pretty sure it does this.
		while(session.getOrder(curord) == 254)
			curord++;
		
		// not looping around again.
		curpat = session.getPattern(session.getOrder(curord));
	}
	
	// byte 0: note, byte 1: instrument
	public int getSampleAndNote(int ins, int note)
	{
		return session.getSampleAndNote(ins, note);
	}
	
	// getters
	
	public SessionSample getSample(int idx)
	{
		return session.getSample(idx);
	}
	
	public SessionInstrument getInstrument(int idx)
	{
		return (session.getFlags() & Session.FLAG_INSMODE) != 0
			? session.getInstrument(idx)
			: null
				;
	}
	
	public int getITVersion()
	{
		return itversion;
	}
	
	public int getRow()
	{
		return currow;
	}
	
	public double getFreq()
	{
		return (double)base_freq;
	}
	
	public boolean useInstruments()
	{
		return (session.getFlags() & Session.FLAG_INSMODE) != 0;
	}
	
	public boolean hasStereo()
	{
		return fmt_chns >= 2 && (session.getFlags() & Session.FLAG_STEREO) != 0;
	}
	
	public boolean hasCompatGxx()
	{
		// jeff forgot to mention this flag in the IT 2.09 changelog.
		// it got a mention as an addition to the "Old Effects" command in IT 2.08,
		// but was silently moved to its own category in IT 2.09.
		// (ITTECH.DOC mentioned it, though.)
		//   --GM
		return (
			(itversion >= 0x209 && (session.getFlags() & Session.FLAG_COMPATGXX) != 0)
			|| (itversion == 0x208 && hasOldEffects())
				);
	}
	
	public boolean hasOldEffects()
	{
		return itversion >= 0x106 && (session.getFlags() & Session.FLAG_OLDEFFECTS) != 0;
	}
	
	public boolean hasLinearSlides()
	{
		return (session.getFlags() & Session.FLAG_LINEAR) != 0;
	}
	
	public boolean isSequencing()
	{
		return sequencing;
	}
	
	public boolean isPatLock()
	{
		return patlock;
	}
	
	// setters
	
	public void setSpeed(int speed)
	{
		this.tickctr = this.speed = speed;
	}
	
	public void addSpeed(int amt)
	{
		this.tickctr += amt;
	}
	
	public void setTempo(int tempo)
	{
		this.tempo = tempo;
	}
	
	public void addTempo(int amt)
	{
		this.tempo += amt;
		if(this.tempo < 32)
			this.tempo = 32;
		if(this.tempo > 255)
			this.tempo = 255;
	}
	
	public void setGlobalVolume(int vol)
	{
		this.gvol = vol;
	}
	
	public void addGlobalVolume(int amt)
	{
		this.gvol += amt;
		if(this.gvol < 0)
			this.gvol = 0;
		if(this.gvol > 128)
			this.gvol = itversion < 0x106 ? 0 : 128; // anyone for dubstep? --GM
	}
	
	public synchronized void setOrderDeferred(int ord)
	{
		// Bxx, SBx = wait for loopback to finish
		// SBx, Bxx = who cares about loopback?
		procrow = -2;
		if(!patlock)
			curord = ord-1;
	}
	
	public void breakRowDeferred(int row)
	{
		// Cxx, SBx = wait for loopback to finish
		// SBx, Cxx = wait for loopback to finish <-- HEY STORLEK FIX THIS
		
		// there's a check SOMEWHERE here...
		// WHERE THE HELL IS v2.00
		// THIS CHANGE ISN'T DOCUMENTED IN THE CHANGELOG
		// SO I CAN'T PINPOINT THIS VERY WELL
		if(procrow == -3 && itversion >= 0x0200)
			return;
		
		procrow = -2;
		breakrow = row-1;
	}
	
	public void setRowCounter(int rowctr)
	{
		if(rowctr_set)
			return;
		
		this.rowctr_set = true;
		this.rowctr = rowctr;
	}
	
	public void setProcRow(int row)
	{
		procrow = -3;
		breakrow = row;
	}
	
	public int getOrder()
	{
		return curord;
	}
	
	public int getPanSep()
	{
		return session.getPanSep();
	}
	
	public static void main(String[] args) throws Exception
	{
		Player player = new Player(new Session(args[0]));
		
		player.playFromStart();
		while(true)
		{
			player.tick();
			Thread.yield();
		}
	}
}
