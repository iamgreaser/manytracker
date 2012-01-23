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
	
	private VirtualChannel vchn = null;
	
	private int uoffs = 0;
	
	private int vol_glb = 64*128; // sample + instrument global volumes
	private int vol_chn = 64;
	private int vol_note = 64;
	private int vol_out = 64;
	
	private float vol_calculated = 1.0f;
	
	private int pan_chn = 32;
	
	private int per_note = 8363;
	private int per_targ = 8363;
	private int per_targ_note = 0;
	private int per_out = 8363;
	
	private int filt_chn = 127;
	private int filt_res = 0;
	
	private int last_note = 253;
	
	private int pat_note = 0;
	private int pat_ins = 0;
	private int pat_vol = 0;
	private int pat_eft = 0;
	private int pat_efp = 0;
	
	private SessionInstrument.Envelope.Handle env_vol = null;
	private SessionInstrument.Envelope.Handle env_pan = null;
	private SessionInstrument.Envelope.Handle env_per = null;
	
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
		// TODO!
	}
	
	// stuff
	
	public float getCalculatedVol()
	{
		return vol_calculated;
	}
	
	private void calculateVol()
	{
		float base_vol = vol_glb * vol_chn * vol_out / (float)(1<<(13+6+6));
		
		vol_calculated = base_vol;
	}
	
	// referred to by Player, silly --GM
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
			per_note = player.calcSlide16(per_note, amt);
			if(per_note > per_targ)
			{
				per_note = per_targ;
				last_note = per_targ_note;
			}
		} else {
			per_note = player.calcSlide16(per_note, -amt);
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
			per_note = player.calcSlide16(per_note, (mask & 0x0F)*mul);
		else if((mask & 0xF0) == 0xE0)
			per_note = player.calcSlide64(per_note, (mask & 0x0F)*mul);
		else
			per_note = player.calcSlide16(per_note, mask*mul);
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
		
		per_out = player.calcSlide16(per_out, (int)(amp+0.5));
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
	
	public void updateVirtualChannel()
	{
		if(vchn == null)
			return;
		
		calculateVol();
		vchn.importVol(getCalculatedVol());
		vchn.importPan(pan_chn);
		vchn.importPer(per_out);
		vchn.importFilt(filt_chn, filt_res);
	}
	
	private void checkVirtualChannel()
	{
		if(vchn == null)
			return;
		
		if(!vchn.imYoursRight(this))
			vchn = null;
	}
	
	public void update0(int note, int ins, int vol, int eft, int efp)
	{
		checkVirtualChannel();
		
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
		boolean porta_test = (isActive() || porta_test_root);
		
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
					changeVirtualInstrument(h_ins);
					ins_idx = ins;
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
				changeVirtualSample(h_smp);
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
	
	private void changeVirtualSample(SessionSample csmp)
	{
		this.csmp = csmp;
		
		if(vchn != null)
			vchn.changeSample(csmp);
	}
	
	private void changeVirtualInstrument(SessionInstrument cins)
	{
		this.cins = cins;
		
		/*
		env_vol = cins.getVolEnvHandle();
		env_pan = cins.getPanEnvHandle();
		env_per = cins.getPerEnvHandle();
		*/
		
		if(vchn != null)
			vchn.changeInstrument(cins);
	}
	
	private void retrigNote()
	{
		retrigVirtualChannel();
		
		eff_qxx_tickdown = eff_qxx&15;
	}
	
	private void retrigVirtualChannel()
	{
		if(vchn == null)
		{
			allocateVirtualChannel();
			if(vchn == null)
				return;
		}
		
		vchn.changeInstrument(cins);
		vchn.changeSample(csmp);
		vchn.retrig(uoffs);
	}
	
	private void allocateVirtualChannel()
	{
		VirtualChannel nvchn = player.allocateVirtualChannel();
		
		if(nvchn != null)
		{
			nvchn.reset();
			if(vchn != null)
				nvchn.enslave(this, vchn);
			nvchn.changeSample(csmp);
			nvchn.changeInstrument(cins);
		}
		
		// XXX: is this a good idea?
		vchn = nvchn;
	}
	
	private void triggerNNA()
	{
		if(vchn != null)
		{
			if(cins == null)
				return;
			
			int nna = cins.getNNA();
			if(nna == SessionInstrument.NNA_CUT)
				return; // reuse same channel
			
			switch(nna)
			{
				case SessionInstrument.NNA_OFF:
					noteOff();
					break;
				case SessionInstrument.NNA_FADE:
					noteFade();
					break;
				default:
					assert(nna == SessionInstrument.NNA_CONTINUE);
					//System.out.printf("NNA TYPE %d\n", nna);
					break;
			}
		}
		
		allocateVirtualChannel();
	}
	
	public void updateN()
	{
		checkVirtualChannel();
		
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
						per_out = player.calcSlide1(per_note, eff_jxx>>4);
						break;
					default:
						per_out = player.calcSlide1(per_note, eff_jxx&15);
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
		if(vchn == null)
			return;
		
		vchn.noteOff();
	}
	
	public void noteCut()
	{
		if(vchn == null)
			return;
		
		vchn.noteCut();
		vchn.detach();
		vchn = null;
	}
	
	public void noteFade()
	{
		if(vchn == null)
			return;
		
		vchn.noteFade();
	}
	
	// getters
	
	private boolean isActive()
	{
		return vchn != null && vchn.isActive();
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
	}
	
	public void setChannelPanning(int pan)
	{
		pan_chn = pan;
	}
	
}
