package player;

import session.*;

public class PlayerChannel
{
	private Player player = null;
	
	// sample handle
	private SessionSample csmp = null;
	
	private int smp_idx = 0;
	
	// instrument handle
	private SessionInstrument cins = null;
	
	private int ins_idx = 0;
	
	// playback info
	private boolean mixing = false;
	private boolean active = false;
	
	private int offs = 0;
	private int uoffs = 0;
	private int suboffs = 0;
	private boolean looping = false;
	private boolean pingpong = false;
	private boolean reverse = false;
	
	private SessionInstrument.Envelope.Handle env_vol = null;
	private SessionInstrument.Envelope.Handle env_pan = null;
	private SessionInstrument.Envelope.Handle env_per = null;
	
	private int curlplen = 0;
	private int curlpbeg = 0;
	
	private int vol_glb = 64*128; // sample + instrument global volumes
	private int vol_chn = 64;
	private int vol_note = 64;
	private int vol_out = 64;
	private int vol_env = 64;
	private int vol_fadeout = 1024;
	
	private float vol_calculated = 1.0f;
	
	private int pan_chn = 32;
	private int pan_env = 32;
	
	private int per_note = 0;
	private int per_targ = 0;
	private int per_targ_note = 0;
	private int per_out = 0;
	private int per_env = 32;
	
	private int filt_chn = 127;
	private int filt_env = 64;
	private int filt_res = 0;
	
	private float filt_k1l = 0.0f;
	private float filt_k2l = 0.0f;
	private float filt_k1r = 0.0f;
	private float filt_k2r = 0.0f;
	
	private int last_note = 253;
	private boolean note_off = false;
	private boolean note_fade = false;
	
	private PlayerChannel master = null;
	private PlayerChannel slave = null;
	
	private int pat_note = 0;
	private int pat_ins = 0;
	private int pat_vol = 0;
	private int pat_eft = 0;
	private int pat_efp = 0;
	
	// effect memory
	
	private int eff_dxx = 0;
	private int eff_efxx = 0;
	private int eff_gxx = 0;
	private int eff_hxx = 0;
	private int eff_ixx = 0;
	private int eff_jxx = 0;
	private int eff_sxx = 0;
	private int eff_oxx = 0;
	private int eff_qxx = 0;
	private int eff_sax = 0;
	private int eff_txx = 0;
	private int eff_wxx = 0;
	private int eff_vabcdx = 0;
	private int eff_vefx = 0;
	private int eff_vgx = 0;
	private int eff_vhx = 0;
	
	private boolean eff_hxx_fine = false;
	private int eff_hxx_offs = 0;
	private int eff_ixx_tickdown = 0;
	private boolean eff_ixx_on = false;
	private int eff_scx_tickdown = 0;
	private int eff_sdx_tickdown = 0;
	private int eff_jxx_tickup = 0;
	private int eff_qxx_tickdown = 0;
	private int eff_sbx_loopback = 0;
	private int eff_sbx_amount = 0;
	
	// methods.
	
	public PlayerChannel(Player player)
	{
		this.player = player;
	}
	
	public void reset()
	{
		// sample handle
		csmp = null;
		smp_idx = 0;
		
		// instrument handle
		cins = null;
		ins_idx = 0;
		
		// playback info
		mixing = false;
		active = false;
		
		offs = 0;
		uoffs = 0;
		suboffs = 0;
		looping = false;
		pingpong = false;
		reverse = false;
		
		env_vol = null;
		env_pan = null;
		env_per = null;
		
		curlplen = 0;
		curlpbeg = 0;
		
		vol_glb = 64*128; // sample + instrument global volumes
		vol_chn = 64;
		vol_note = 64;
		vol_out = 64;
		vol_env = 64;
		vol_fadeout = 1024;
		
		pan_chn = 32;
		pan_env = 32;
		
		per_note = 0;
		per_targ = 0;
		per_out = 0;
		per_env = 32;
		
		filt_chn = 127;
		filt_env = 64;
		filt_res = 0;
		
		filt_k2l = filt_k1l = 0.0f;
		filt_k2r = filt_k1r = 0.0f;
		
		last_note = 60;
		note_off = false;
		note_fade = false;
		
		master = null;
		slave = null;
	}
	
	// stuff
	
	private void loadSlave(PlayerChannel other)
	{
		// sample handle
		other.csmp = csmp;
		other.smp_idx = smp_idx;
		
		// instrument handle
		other.cins = cins;
		other.ins_idx = ins_idx;
		
		// playback info
		other.mixing = mixing;
		other.active = active;
		
		other.offs = offs;
		other.uoffs = uoffs;
		other.suboffs = suboffs;
		other.looping = looping;
		other.pingpong = pingpong;
		other.reverse = reverse;
		
		other.env_vol = env_vol == null ? null : env_vol.dup();
		other.env_pan = env_pan == null ? null : env_pan.dup();
		other.env_per = env_per == null ? null : env_per.dup();
		
		other.curlplen = curlplen;
		other.curlpbeg = curlpbeg;
		
		other.vol_glb = vol_glb;
		other.vol_chn = vol_chn;
		other.vol_note = vol_note;
		other.vol_out = vol_out;
		other.vol_env = vol_env;
		other.vol_fadeout = vol_fadeout;
		
		other.pan_chn = pan_chn;
		other.pan_env = pan_env;
		
		other.per_note = per_note;
		other.per_targ = per_targ;
		other.per_out = per_out;
		other.per_env = per_env;
		
		other.filt_chn = filt_chn;
		other.filt_env = filt_env;
		other.filt_res = filt_res;
		
		other.filt_k2l = filt_k2l;
		other.filt_k1l = filt_k1l;
		other.filt_k2r = filt_k2r;
		other.filt_k1r = filt_k1r;
		
		other.note_off = note_off;
		other.note_fade = note_fade;
		
		other.master = this;
		other.slave = slave;
		slave = other;
	}
	
	private int calcSlide1(int base, int amt)
	{
		return (int)(0.5 + base * Math.pow(2.0f, amt / 12.0f));
	}
	
	private int calcSlide2(int base, int amt)
	{
		return (int)(0.5 + base * Math.pow(2.0f, amt / (2.0f * 12.0f)));
	}
	
	private int calcSlide16(int base, int amt)
	{
		return calcSlide64(base, amt*4);
	}
	
	private int calcSlide64(int base, int amt)
	{
		if(player.hasLinearSlides())
			return (int)(0.5 + base * Math.pow(2.0f, amt / (64.0f * 12.0f)));
		else {
			int amiclk = 8363*428*4;
			
			int oper = amiclk / base;
			int nper = oper - amt;
			
			return amiclk / nper;
		}
	}
	
	public void mix(float[][] buf, int offs, int len)
	{
		if((!active) || ((vol_chn&0x80) != 0))
			return;
		
		doMix(buf, offs, len);
		// TODO: ensure vol0 mix optimisations (heh) works properly
		/*
		if(mixing)
			doMix(buf, offs, len);
		else
			doFix(len);
		*/
	}
	
	public float getCalculatedVol()
	{
		return vol_calculated;
	}
	
	private void calculateVol()
	{
		int base_vol_c = vol_glb * vol_chn * vol_env * vol_out >> (13+6+6+6-16);
		float base_vol = (base_vol_c * vol_fadeout) / (float)(1<<(9+16));
		
		vol_calculated = base_vol;
	}
	
	private void doMix(float[][] buf, int boffs, int blen)
	{
		// calculate main volume
		calculateVol();
		float base_vol = getCalculatedVol();
		
		// calculate main speed
		int base_spd_c = calcSlide2(per_out, (per_env-32));
		float base_spd_x = (float)base_spd_c / (float)player.getFreq();
		int base_spd = (int)(base_spd_x * (float)(1<<13));
		
		if(reverse)
			base_spd = -base_spd;
		
		// calculate panning
		float vol1 = base_vol;
		float vol2 = base_vol;
		
		if(player.hasStereo())
		{
			int base_pan_c = (pan_chn == 100 ? 32 : pan_chn) - 32;
			base_pan_c = base_pan_c + (((32-Math.abs(base_pan_c))*(pan_env-32))>>5);
			
			if(base_pan_c < 0)
				vol2 *= (32+base_pan_c)/32.0f;
			else
				vol1 *= (32-base_pan_c)/32.0f;
			
			if(pan_chn == 100)
				vol2 = -vol2;
		}
		
		// calculate filter coefficients
		float fa = 1.0f;
		float fb = 0.0f;
		float fc = 0.0f;
		if(filt_chn != 127 || filt_res != 0 || filt_env != 64)
		{
			// Word of Jeff:
			// d = 2*(damping factor)*(sampling rate)/(natural frequency) + 2*(damping factor)-1. 
			// e = (sampling rate/natural frequency)^2 
			//
			// a = 1/(1+d+e)
			// b = (d+2e)/(1+d+e)
			// c = -e/(1+d+e)
			//
			// according to the slightly more accurate Word of Jeff (some source code snippets),
			// there's a weird thing you multiply by. i don't know why.
			// with that said, those formulae are correct.
			
			float r = (float)Math.pow(2.0,(filt_chn*filt_env)/(24.0*64.0));
			r = ((float)player.getFreq()*0.0012166620101443976f)/r;
			float p = (float)Math.pow(10.0f,((-filt_res*24.0)/(128.0f*20.0f)));
			
			float d = 2.0f*p*(r+1.0f)-1.0f;
			float e = r*r;
			
			fa = 1.0f/(1.0f+d+e);
			fb = (d+2.0f*e)*fa;
			fc = -e*fa;
			
			// XXX: attempt to de-NaN the code
			//float tfc = fc/(fa*fb);
			//if(fc < tfc)
			//	fc = tfc;
			
			//System.out.printf("%f %f [%f, %f, %f / %f, %f]\n", filt_k1l, filt_k1r, fa, fb, fc, d, e);
		}
		
		// load some things
		
		float[][] data = csmp.getData();
		float[] dl = data[0];
		float[] dr = data[1];
		int length = data[0].length;
		//System.out.printf("mix %d [%.3f, %.3f]\n", length, vol1, vol2);
		int lpend = curlpbeg + curlplen;
		int end = boffs + blen;
		
		vol1 *= fa;
		vol2 *= fa;
		
		float[] bufl = buf[0];
		float[] bufr = buf[1];
		
		if(curlplen <= 0)
			looping = false;
		
		// FIXME: THIS IS SLOOOOOOOW
		// and by slow, i mean the reso filter is probably the fastest part
		// it probably has something to do with the looping bollocks
		// FIXME: also ping-pong loops are buggered (ok maybe not so much now)
		// FIXME: some would say loops are buggered full-stop
		// FIXME: THIS MIXER SUCKS
		//
		// in short, this is the bottleneck and it's broken
		for(int i = boffs; i < end; i++)
		{
			assert(offs >= 0);
			
			if(offs >= length)
			{
				// ASSES I HATE THIS CRAP
				assert(!looping);
				active = mixing = false;
				break;
			}
			
			float outl = dl[offs] * vol1 + filt_k1l * fb + filt_k2l * fc;
			float outr = dr[offs] * vol2 + filt_k1r * fb + filt_k2r * fc;
			
			// TODO: work out the appropriate threshold for de-NaN'ing
			// would also be wise to fiddle with fa,fb,fc
			// instead of cluttering up the mix function with if statements
			if(outl >= 1.0f)
				outl = 1.0f;
			if(outr >= 1.0f)
				outr = 1.0f;
			if(outl <= -1.0f)
				outl = -1.0f;
			if(outr <= -1.0f)
				outr = -1.0f;
			
			bufl[i] += outl;
			bufr[i] += outr;
			
			filt_k2l = filt_k1l;
			filt_k1l = outl;
			filt_k2r = filt_k1r;
			filt_k1r = outr;
			
			/*
			bufl[i] += dl[offs] * vol1;
			bufr[i] += dr[offs] * vol2;
			*/
			
			suboffs += base_spd;
			offs += suboffs>>13;
			suboffs &= (1<<13)-1;
			
			if(looping)
			{
				if(pingpong)
				{
					if(reverse && offs < curlpbeg)
					{
						offs = curlpbeg*2 - offs;
						reverse = !reverse;
						base_spd = -base_spd;
					}
					
					if(offs >= lpend)
					{
						offs -= curlpbeg;
						
						// beg=1,len=4 => {1234321}
						offs %= curlplen*2-1;
						boolean oldreverse = reverse;
						reverse = offs >= curlplen;
						if(reverse)
							offs = curlplen*2-1 - offs;
						
						if(oldreverse != reverse)
							base_spd = -base_spd;
						
						offs += curlpbeg;
					}
				} else {
					if(offs >= lpend)
					{
						offs = (offs-curlpbeg)%curlplen + curlpbeg;
					}
				}
			}
			
			if(offs < 0)
			{
				offs = -offs;
			}
		}
	}
	
	private void doFix(int len)
	{
		// if "mixing" is false, calculate next part
		int length = csmp.getData()[0].length;
		
		// get time
		double time = ((double)len)/player.getFreq();
		
		// move sample stuff across
		suboffs += time;
		offs += (reverse ? -1 : 1) * (int)suboffs;
		suboffs %= 1.0f;
		
		// flip if reversed too far left
		if(offs < 0)
		{
			offs = -offs;
			reverse = false;
		}
		
		// XXX: NEEDS LOTS OF TESTING!
		
		// check if looping
		if(looping)
		{
			// check if ping-pong
			if(pingpong)
			{
				// check if gone over end
				if(offs >= curlpbeg+curlplen)
				{
					// adjust for loop
					offs -= curlpbeg;
					reverse = offs > curlplen;
					offs += curlpbeg;
				}
			} else {
				// check if gone over end
				if(offs >= curlpbeg+curlplen)
				{
					// adjust for loop
					offs = (offs - curlpbeg) % curlplen + curlpbeg;
				}
			}
		} else {
			// check end of sample
			if(offs >= length)
			{
				// kill the sample
				active = false;
			}
		}
	}
	
	public void calcOutputVolFreq()
	{
		vol_out = vol_note;
		per_out = per_note;
	}
	
	private int calcVolSlide(int mask)
	{
		if((mask & 0x0F) == 0x00)
			return mask>>4;
		else if((mask & 0xF0) == 0x00)
			return -(mask&15);
		else if((mask & 0x0F) == 0x0F)
			return mask>>4;
		else if((mask & 0xF0) == 0xF0)
			return -(mask&15);
		else
			return 0; // TODO: work out any other weirdness
	}
	
	private void doVolSlide(int mask)
	{
		vol_note += calcVolSlide(mask);
		
		if(vol_note < 0)
			vol_note = 0;
		if(vol_note > 64)
			vol_note = 64;
		
		vol_out = vol_note;
	}
	
	private void doPorta(int amt)
	{
		if(per_note < per_targ)
		{
			per_note = calcSlide16(per_note, amt);
			if(per_note > per_targ)
			{
				per_note = per_targ;
				last_note = per_targ_note;
			}
		} else {
			per_note = calcSlide16(per_note, -amt);
			if(per_note < per_targ)
			{
				per_note = per_targ;
				last_note = per_targ_note;
			}
		}
	}
	
	private void doTremor()
	{
		// aside from a hiccup pertaining to the volume being set in Update0,
		// this worked perfectly first time.
		// (yes, even with old effects.)
		//
		// mikmod + modplug authors: YOU DIDN'T TEST THIS DID YOU
		
		if(--eff_ixx_tickdown <= 0)
		{
			eff_ixx_on = !eff_ixx_on;
			eff_ixx_tickdown = (eff_ixx_on ? (eff_ixx>>4) : (eff_ixx&15));
			
			if(player.hasOldEffects() && player.getITVersion() >= 0x0203)
				eff_ixx_tickdown++;
		}
		
		if(!eff_ixx_on)
			vol_out = 0;
	}
	
	private void doRetrig()
	{
		/*
		WHERE ARE YOU, OLD VERSIONS OF IT
		
		1.01:
		- Bug Fix: Fixed a problem with the retrig command.
		
		1.02:
		- Bug fix: Again. The retrig command. D'oh! Sorry about this - there were
		    actually two bugs in the original retrig command. When I found
		    one, I thought that I'd fixed it (well it fixed it for mixed
		    devices, anyway). Ah well. Should work perfectly fine now. (for
		    both SBs and GUSs)
		    <Benjamin Bruheim>
		
		WHAT WAS THAT BUG JEFF?!?!
		
		
		*/
		
		// Now that this is implemented, every piece by Lachesis should work --GM
		if(--eff_qxx_tickdown <= 0)
		{
			switch(eff_qxx>>4)
			{
				case 0:
				case 8:
					break;
				case 1:
					vol_note -= 1;
					break;
				case 2:
					vol_note -= 2;
					break;
				case 3:
					vol_note -= 4;
					break;
				case 4:
					vol_note -= 8;
					break;
				case 5:
					vol_note -= 16;
					break;
				case 6:
					vol_note *= 2;
					vol_note /= 3;
					break;
				case 7:
					vol_note /= 2;
					break;
				case 9:
					vol_note += 1;
					break;
				case 10:
					vol_note += 2;
					break;
				case 11:
					vol_note += 4;
					break;
				case 12:
					vol_note += 8;
					break;
				case 13:
					vol_note += 16;
					break;
				case 14:
					vol_note *= 3;
					vol_note /= 2;
					break;
				case 15:
					vol_note *= 2;
					break;
			}
			
			if(vol_note < 0)
				vol_note = 0;
			if(vol_note > 64)
				vol_note = 64;
			
			vol_out = vol_note;
			
			retrigNote();
			
			eff_qxx_tickdown = eff_qxx&15;
		}
	}
	
	private void doSlide(int mask, boolean down, boolean first)
	{
		// stay in your "zone"
		if(first == (mask < 0xE0))
			return;
		
		int mul = down ? -1 : 1;
		
		if((mask & 0xF0) == 0xF0)
			per_note = calcSlide16(per_note, (mask & 0x0F)*mul);
		else if((mask & 0xF0) == 0xE0)
			per_note = calcSlide64(per_note, (mask & 0x0F)*mul);
		else
			per_note = calcSlide16(per_note, mask*mul);
	}
	
	private double getWaveform(int type, int offs)
	{
		// TODO: types that aren't 0
		return Math.sin((offs&255)*Math.PI/128.0);
	}
	
	public void doVibrato(int mask, boolean fine)
	{
		// TODO: read vibrato waveform
		
		double amp = getWaveform(0, eff_hxx_offs)*(mask&15);
		
		if(player.hasOldEffects())
			amp *= 2.0;
		
		if(fine)
		{
			eff_hxx_offs += (mask>>4);
			amp *= 1.0/4.0;
		} else {
			eff_hxx_offs += (mask>>2) & 0x3C;
		}
		
		//System.out.printf("vib %.3f\n", amp);
		
		per_out = calcSlide16(per_out, (int)(amp+0.5));
	}
	
	private boolean checkVolSlide0(int mask)
	{
		// D0F/DF0 S3M compatibility added in v1.04
		if(player.getITVersion() < 0x0104)
			if ((mask & 0x0F) == 0x00 || (mask & 0xF0) == 0x00)
				return false;
		
		return ((mask & 0x0F) == 0x0F || (mask & 0xF0) == 0xF0);
	}
	
	private boolean checkVolSlideN(int mask)
	{
		return ((mask & 0x0F) == 0x00 || (mask & 0xF0) == 0x00);
	}
	
	public void updateEnvelopes()
	{
		if(!active)
			return;
		
		if(env_vol != null)
			vol_env = env_vol.read();
		if(env_pan != null)
			pan_env = env_pan.read() + 32;
		if(env_per != null)
		{
			int pval = env_per.read() + 32;
			
			if(env_per.hasFilter())
				filt_env = pval + 32;
			else
				per_env = pval;
		}
		
		if(note_fade && cins != null)
		{
			vol_fadeout -= cins.getFadeout();
			
			if(vol_fadeout < 0)
				vol_fadeout = 0;
		}
		
		if((!note_fade) && env_vol.fadeCheck(note_off))
		{
			System.out.printf("NOTEFADE volcheck\n");
			note_fade = true;
		}
	}
	
	public void update0(int note, int ins, int vol, int eft, int efp)
	{
		pat_note = note;
		pat_ins = ins;
		pat_vol = vol;
		pat_eft = eft;
		pat_efp = efp;
		
		this.uoffs = 0;
		
		int unote = note;
		int usmp = ins;
		
		boolean smpchange = false;
		boolean reset_filt_reso = true;
		boolean reset_filt_cutoff = true;
		boolean unlockvol = true;
		
		if(eft == 0x13)
			if(efp != 0)
				eff_sxx = efp;
		
		switch(eft)
		{
			case 0x01: // Axx - set speed
				if(efp != 0)
					player.setSpeed(efp);
				break;
			case 0x02: // Bxx - jump to order
				player.setOrderDeferred(efp);
				break;
			case 0x03: // Cxx - pattern jump
				player.breakRowDeferred(efp);
				break;
			case 0x04: // Dxy - volume slide
			case 0x0B: // Kxy - H00 + Dxy
			case 0x0C: // Lxy - G00 + Dxy
				if(efp != 0)
					eff_dxx = efp;
				
				if(checkVolSlide0(eff_dxx))
					doVolSlide(eff_dxx);
				
				if(eft == 0x0C && !player.hasOldEffects())
					doVibrato(eff_hxx, eff_hxx_fine);
				
				break;
			case 0x05: // Exx - slide down
			case 0x06: // Fxx - slide up
				if(efp != 0)
				{
					eff_efxx = efp;
					if(!player.hasCompatGxx())
						eff_gxx = efp;
				}
				
				doSlide(eff_efxx, pat_eft == 0x05, true);
				
				break;
			case 0x07: // Gxx - portamento
				if(efp != 0)
				{
					eff_gxx = efp;
					if(!player.hasCompatGxx())
						eff_efxx = efp;
				}
				
				break;
			
			case 0x08: // Hxx - vibrato
			case 0x15: // Uxx - fine vibrato
				if(efp != 0)
				{
					eff_hxx = efp;
					eff_hxx_fine = (eft == 0x15);
				}
				
				if(!player.hasOldEffects())
					doVibrato(eff_hxx, eff_hxx_fine);
				
				break;
			
			case 0x09: // Ixx - tremor
				if(efp != 0)
					eff_ixx = efp;
					// NO RETRIG YOU STUPID MODPLUG
				
				unlockvol = false;
				doTremor();
				break;
			
			case 0x0A: // Jxx - arpeggio
				/*
				if((efp & 15) != 0)
					eff_jxx = (eff_jxx & 0xF0) | (efp & 15);
				if((efp & 0xF0) != 0)
					eff_jxx = (eff_jxx & 15) | (efp & 0xF0);
				*/
				
				if(efp != 0)
					eff_jxx = efp;
				
				eff_jxx_tickup = 0;
				break;
			
			// 0x0B / 0x0C covered above
			
			case 0x0D: // Mxx - set channel volume
				if(efp <= 0x40 && ((vol_chn & 0x80) == 0)) // out-of-range values are outright ignored
					setChannelVolume(efp);
				break;
			
			case 0x0F: // Oxx - sample offset
				if(efp != 0)
					eff_oxx = efp;
				
				uoffs = (eff_oxx<<8)|(eff_sax<<16);
				
				break;
			
			case 0x11: // Qxx - retrigger
				if(efp != 0)
					eff_qxx = efp;
				
				doRetrig();
				break;
			
			case 0x13: // Sxy - miscellaneous
				switch(eff_sxx>>4)
				{
					case 0x6: // tick delay
						if(player.getITVersion() >= 0x0106)
							player.addSpeed(eff_sxx&15);
						break;
					case 0x7: // misc crap only Chris uses
						// TODO!
						// IT 1.03 has S70-S77 (apparently).
						// IT 2.14p5 has S70-S7C.
						// So, here we go.
						// IT 1.05 adds S78
						// IT 2.01 adds S79-S7C
						// and that's all you need to know.
						
						break;
					case 0x8: // panning
						// TODO: get correct algorithm for this
						pan_chn = ((eff_sxx&15)*0x11+2)>>2;
						break;
					case 0x9: // exactly one effect, piss off modplug
						if(eff_sxx == 0x91)
							pan_chn = 100;
						break;
					case 0xA: // sample offset high nybble
						if(player.getITVersion() >= 0x0200)
							eff_sax = efp&15;
						break;
					case 0xB: // 
						/*
						TODO: pre-IT104 behaviour
						note, this would require me to screw with the architecture,
						but it could be interesting.
						
						in changelog for 1.04:
						- Bug fix: Pattern loop now is controlled by EACH channel (apparently how
						  MODs did it, but *NOT* how ST3 did it....).
						  (Wave to Firelight for creating his really wierd backwards/
						  topsy-turvied modules!)
						*/
						
						if((eff_sxx&15) != 0)
						{
							// loop some number of times
							if(eff_sbx_amount == 0)
							{
								eff_sbx_amount = (eff_sxx&15);
								player.setProcRow(eff_sbx_loopback-1);
							} else if(--eff_sbx_amount > 0) {
								player.setProcRow(eff_sbx_loopback-1);
							} else {
								// infinite loop "prevention"
								// (still possible to cause one if you're clever though!)
								if(player.getITVersion() >= 0x210)
									eff_sbx_loopback = player.getRow()+1;
							}
						} else {
							// set loopback point
							eff_sbx_loopback = player.getRow();
							//System.out.printf("row = %d\n", eff_sbx_loopback);
						}
						break;
					case 0xC: // note cut (first of these i ever implement :D)
						eff_scx_tickdown = (eff_sxx&15);
						break;
					case 0xD: // note delay
						// ok, this one's a prick.
						eff_sdx_tickdown = (eff_sxx&15);
						return;// NOTE THIS ISN'T "BREAK"
					case 0xE: // row delay
						if(player.getITVersion() >= 0x0102)
							player.setRowCounter((eff_sxx&15)+1);
						break;
				}
				break;
			
			case 0x14: // Txx - set / slide tempo
				if(efp != 0)
					eff_txx = efp;
				
				if(eff_txx >= 0x20)
					player.setTempo(eff_txx);
				else if(player.getITVersion() >= 0x0207) {
					// XXX: I lack IT 2.07.
					// If you have it, PLEASE PLEASE PLEASE send it to me.
					// (it adds lots of crap, and IT 2.08 adds a bit more on top.)
					// It looks like it might be rare, but not quite as rare as, say, IT 1.04.
					// This info is taken from the changelog.
					// Thanks. --GM
					if(eff_txx >= 0x10)
						player.addTempo(eff_txx - 0x10);
					else
						player.addTempo(-eff_txx);
				}
			// 0x15 covered above
			
			
			case 0x16: // Vxx - set global volume
				if(efp <= 0x80) // out-of-range values are outright ignored
					player.setGlobalVolume(efp);
				break;
			case 0x17: // Wxx - global volume slide
				if(efp != 0)
					eff_wxx = efp;
				
				if(checkVolSlide0(eff_wxx))
					player.addGlobalVolume(calcVolSlide(eff_wxx));
				break;
			
			case 0x18: // Xxx - panning
				pan_chn = (efp+2)>>2; // thanks Nickysn for documenting this
				break;
			
			case 0x1A: // Zxx - MIDI macros
				// TODO: load the MIDI data bollocks
				
				if(player.getITVersion() >= 0x0212)
				{
					if(efp < 0x80)
					{
						// TODO: cooperate with SFx
						// (assuming using SF0 which by default is "set cutoff")
						
						if(player.getITVersion() >= 0x0217)
						{
							filt_chn = efp;
							reset_filt_cutoff = false;
						}
					} else {
						// TODO: pinpoint the exact version where this is broken
						// assuming IT216 means 2.14p2, not 2.14p1! (was fixed in patch 2)
						// (BOTH of these versions are missing,
						//  and I *need* them to work out if it's possible to distinguish the two
						//  (also for completeness) --GM)
						
						// I don't think I'll ever get my hands on versions of IT 2.15 ;_; --GM
						//if(player.getITVersion() < 0x0213 || player.getITVersion() >= 0x0216)
						{
							if(player.getITVersion() >= 0x0217)
							{
								if(efp < 0x90)
								{
									filt_res = (efp-0x80)*8;
									reset_filt_reso = false;
								}
							}
						}
					}
				}
				break;
		}
		
		if(vol <= (player.getITVersion() < 0x208 ? 127 : 64))
		{
			vol_out = vol_note = vol;
			//System.out.printf("volume %d\n", vol);
		} else if(vol >= 128 && vol <= 192) {
			pan_chn = vol-128;
			// TODO: check IT pre 2.08 on how this behaves out of range
		} else if(vol < 74) {
			
		}
		
		boolean porta_test_root = (pat_eft != 0x07 && pat_eft != 0x0C);
		boolean porta_test = ((!active) || porta_test_root);
		
		if(note < 120 && cins != null && porta_test)
			triggerNNA();
		
		// TODO: shift this up and cache the damn thing
		// (Storlek test #08: Out Of Range Note Delays)
		if(ins != 0)
		{
			if(player.useInstruments())
			{
				SessionInstrument h_ins = player.getInstrument(ins);
				if(h_ins != null)
				{
					cins = h_ins;
					ins_idx = ins;
					env_vol = cins.getVolEnvHandle();
					env_pan = cins.getPanEnvHandle();
					env_per = cins.getPerEnvHandle();
				}
			} else {
				ins_idx = ins;
			}
		}
		
		if(note < 120)
			last_note = note;
		
		if((ins != 0 && note != 255) || note < 120)
		{
			int smpnote = player.getSampleAndNote(ins_idx, last_note);
			
			unote = smpnote&0xFF;
			usmp = smpnote>>8;
			
			//System.out.printf("smp %d %d\n", usmp, unote);
			
			// only load new sample if different
			smpchange = (usmp != smp_idx);
		}
		
		if(smpchange)
		{
			// TODO: get the compat Gxx behaviour working too
			
			//System.out.printf("smpchange %d %d\n", smp_idx, usmp);
			SessionSample h_smp = player.getSample(usmp);
			if(h_smp != null)
			{
				csmp = h_smp;
				doLoop();
				doSustainLoop();
				smp_idx = usmp;
			}
		}
		
		int vol_glb = 
			(cins == null ? 128 : cins.getGlobalVol())
			* (csmp == null ? 64 : csmp.getGlobalVol());
		
		if(ins != 0 && vol > (player.getITVersion() < 0x208 ? 127 : 64) && csmp != null)
		{
			vol_note = csmp.getVol();
			if(unlockvol)
				vol_out = vol_note;
			vol_fadeout = 1024; // XXX: HORRIBLE ASSUMPTION
		}
		
		if(note < 120 || (note != 254 && smpchange))
		{
			//setNoteTargetByNumber(note);
			setNoteTargetByNumber(unote);
			if(porta_test && csmp != null)
			{
				retrigNote();
				
				if((csmp.getDefaultPan() & 0x80) != 0)
					pan_chn = (csmp.getDefaultPan() & 0x7F);
				
				if(cins != null)
				{
					if((cins.getDefaultPan() & 0x80) == 0)
						pan_chn = (cins.getDefaultPan() & 0x7F);
					
					if(env_vol != null)
						env_vol.retrig();
					if(env_pan != null)
						env_pan.retrig();
					if(env_per != null)
						env_per.retrig();
					
					int ifc = cins.getDefaultCutoff();
					int ifr = cins.getDefaultResonance();
					
					if(reset_filt_cutoff && (ifc & 0x80) != 0)
						filt_chn = ifc&0x7F;
					if(reset_filt_reso && (ifr & 0x80) != 0)
						filt_res = ifr&0x7F;
				}
				
				eff_hxx_offs = 0;
				
				if(porta_test_root)
					latchNoteSpeed();
			}
			
			System.out.printf("note start %d %d %d\n", note, per_note, smp_idx);
			// TODO: NNAs!
			
		} else if(note == 255) {
			// note off
			noteOff();
		} else if(note == 254) {
			// note cut
			noteCut();
		} else if(note != 253) {
			// note fade
			noteFade();
		}
	}
	
	private void retrigNote()
	{
		offs = uoffs;
		suboffs = 0;
		reverse = false;
		active = mixing = true;
		note_off = false;
		note_fade = false;
		vol_fadeout = 1024;
		
		eff_qxx_tickdown = eff_qxx&15;
	}
	
	private void triggerNNA()
	{
		if(!active)
			return;
		
		if(cins == null)
			return;
		
		int nna = cins.getNNA();
		if(nna == SessionInstrument.NNA_CUT)
			return;
		
		PlayerChannel newSlave = player.allocateNNA();
		
		if(newSlave == null)
			return;
		
		loadSlave(newSlave);
		
		switch(nna)
		{
			case SessionInstrument.NNA_OFF:
				slave.noteOff();
				break;
			case SessionInstrument.NNA_FADE:
				slave.noteFade();
				break;
			default:
				assert(nna == SessionInstrument.NNA_CONTINUE);
				//System.out.printf("NNA TYPE %d\n", nna);
				break;
		}
		
	}
	
	public void updateN()
	{
		if(pat_eft != 0x13 || (eff_sxx & 0xF0) != 0xD0)
		{
			// TODO: voleffects
		}
		
		switch(pat_eft)
		{
			case 0x04: // Dxy - volume slide
				if(checkVolSlideN(eff_dxx))
					doVolSlide(eff_dxx);
				
				break;
			
			case 0x0B: // Kxy - H00 + Dxy
				if(checkVolSlideN(eff_dxx))
					doVolSlide(eff_dxx);
				
				doVibrato(eff_hxx, eff_hxx_fine);
				
				break;
			
			case 0x0C: // Lxy - G00 + Dxy
				if(checkVolSlideN(eff_dxx))
					doVolSlide(eff_dxx);
				
				doPorta(eff_gxx);
				
				break;
			
			case 0x05: // Exx - slide down
			case 0x06: // Fxx - slide up
				doSlide(eff_efxx, pat_eft == 0x05, false);
				
				break;
			
			case 0x07: // Gxx - portamento
				doPorta(eff_gxx);
				
				break;
			
			case 0x09: // Ixx - tremor
				doTremor();
				break;
			
			case 0x08: // Hxx - vibrato
			case 0x15: // Uxx - fine vibrato
				doVibrato(eff_hxx, eff_hxx_fine);
				
				break;
			
			case 0x0A: // Jxx - arpeggio
				switch(++eff_jxx_tickup)
				{
					case 0:
						break;
					case 1:
						per_out = calcSlide1(per_note, eff_jxx>>4);
						break;
					default:
						per_out = calcSlide1(per_note, eff_jxx&15);
						eff_jxx_tickup = -1;
						break;
						
				}
				break;
			
			case 0x11: // Qxx - retrigger
				doRetrig();
				break;
			
			case 0x13: // Sxy - miscellaneous
				switch(eff_sxx>>4)
				{
					case 0xC: // note cut (first of these i ever implement :D)
						if(--eff_scx_tickdown <= 0)
							noteCut();
						break;
					case 0xD: // note delay
						if(--eff_sdx_tickdown <= 0)
						{
							update0(pat_note, pat_ins, pat_vol, 0x00, 0x00);
						}
						return;
				}
				break;
			
			case 0x17: // Wxx - global volume slide
				if(checkVolSlideN(eff_wxx))
					player.addGlobalVolume(calcVolSlide(eff_wxx));
				break;
		}
	}
	
	public void noteOff()
	{
		note_off = true;
		
		doLoop();
		
		if(env_vol != null)
			env_vol.noteOff();
		if(env_pan != null)
			env_pan.noteOff();
		if(env_per != null)
			env_per.noteOff();
		
		// TODO: work out how this works in the context of NNAs
	}
	
	public void noteCut()
	{
		active = mixing = false;
		
		if(env_vol != null)
			env_vol.stop();
		if(env_pan != null)
			env_pan.stop();
		if(env_per != null)
			env_per.stop();
		
		// TODO: work out how this works in the context of NNAs
	}
	
	public void noteFade()
	{
		note_fade = true;
		// TODO: work out how this works in the context of NNAs
	}
	
	public boolean isForeground()
	{
		return master == null;
	}
	
	public boolean isNoteOn()
	{
		return !note_off;
	}
	
	public boolean isActive()
	{
		return active;
	}
	
	// setters
	
	private void setNoteTargetByNumber(int note)
	{
		per_targ = (int)((csmp != null ? csmp.getC5Speed() : 0) * Math.pow(2.0f, (note-60.0f)/12.0f));
		per_targ_note = note;
	}
	
	private void latchNoteSpeed()
	{
		per_out = per_note = per_targ;
	}
	
	public void setChannelVolume(int vol)
	{
		vol_chn = vol;
		
		// NO DON'T DO THIS YOU EGG --GM
		//if(slave != null)
		//	slave.setChannelVolume(vol);
	}
	
	public void setChannelPanning(int pan)
	{
		pan_chn = pan;
	}
	
	private void doLoop()
	{
		looping = pingpong = false;
		
		if(csmp != null)
		{
			curlpbeg = csmp.getLpBeg();
			curlplen = csmp.getLpLen();
			
			if((csmp.getFlags() & SessionSample.SFLG_LOOP) != 0)
				looping = true;
			if((csmp.getFlags() & SessionSample.SFLG_BIDI) != 0)
				pingpong = true;
		}
	}
	
	private void doSustainLoop()
	{
		if(csmp == null)
			return;
		
		if((csmp.getFlags() & SessionSample.SFLG_SUSLOOP) != 0)
		{
			looping = true;
			curlpbeg = csmp.getSusBeg();
			curlplen = csmp.getSusLen();
			pingpong = false;
			if((csmp.getFlags() & SessionSample.SFLG_SUSBIDI) != 0)
				pingpong = true;
		}
	}
	
}
