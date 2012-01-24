package session;

import player.*;
import misc.Util;

import java.io.*;

public class SessionSample
{
	// format bollocks
	
	public static final int FORMAT_IT = 1;
	public static final int FORMAT_MOD = 2;
	public static final int FORMAT_S3M = 3;
	
	// IMPS bollocks
	
	public static final int SFLG_EXISTS = 0x01;
	public static final int SFLG_16BIT = 0x02;
	public static final int SFLG_STEREO = 0x04;
	public static final int SFLG_IT214 = 0x08;
	public static final int SFLG_LOOP = 0x10;
	public static final int SFLG_SUSLOOP = 0x20;
	public static final int SFLG_BIDI = 0x40;
	public static final int SFLG_SUSBIDI = 0x80;
	
	public static final int SCVT_SIGNED = 0x01;
	public static final int SCVT_BIGENDIAN = 0x02;
	public static final int SCVT_DELTA = 0x04;
	public static final int SCVT_BYTEDELTA = 0x08;
	public static final int SCVT_TXWAVE = 0x10;
	public static final int SCVT_STEREOPROMPT = 0x20; // NOTE: blatantly internal!
	public static final int SCVT_RESERVED1 = 0x40;
	public static final int SCVT_RESERVED2 = 0x80;
	
	private String fname = "";
	private int gvl = 64;
	private int flg = 0;
	private int vol = 64;
	private String name = "";
	private int cvt = 0x01, dfp = 32|0;
	private int length = 0;
	private int lpbeg = 0, lplen = 0;
	private int c5speed = 8363;
	private int susbeg = 0, suslen = 0;
	// samplepointer
	private int vis = 0, vid = 0, vir = 0, vit = 0;
	
	// kinda necessary stuff
	private float[][] data = null;
	private float[][] dataSustain = null;
	private float[][] dataLoop = null;
	
	public SessionSample(RandomAccessFile fp, int format) throws IOException
	{
		switch(format)
		{
			case FORMAT_IT:
				loadDataIT(fp);
				break;
			case FORMAT_MOD:
				loadDataMOD(fp);
				break;
			case FORMAT_S3M:
				throw new RuntimeException("incorrect constructor for sample format");
			default:
				throw new RuntimeException("sample format not supported");
		}
	}
	
	public SessionSample(RandomAccessFile fp, int format, int secondary) throws IOException
	{
		switch(format)
		{
			case FORMAT_S3M:
				loadDataS3M(fp, secondary);
				break;
			case FORMAT_IT:
			case FORMAT_MOD:
				throw new RuntimeException("incorrect constructor for sample format");
			default:
				throw new RuntimeException("sample format not supported");
		}
	}
	
	public void loadSampleDataMOD(RandomAccessFile fp) throws IOException
	{
		if(length == 0)
			return;
		
		// this part's pretty easy really.
		data = new float[2][];
		float[] xdata = new float[length];
		
		for(int i = 0; i < length; i++)
		{
			int v = fp.read();
			if(v == -1)
				v = 0;
			if(v >= 0x80)
				v -= 0x100;
			
			float fv = (float)v / 128.0f;
			xdata[i] = fv;
		}
		
		// TODO: append MOD loop to end
		
		data[0] = data[1] = xdata;
		
		unrollLoops();
	}
	
	public void loadDataS3M(RandomAccessFile fp, int ffi) throws IOException
	{
		byte[] b = new byte[28];
		
		int instype = fp.read();
		
		// don't load adlib instruments!
		if(instype != 1)
			return;
		
		this.fname = Util.readStringNoNul(fp, b, 12);
		
		int samplepointer = 0;
		samplepointer += fp.read()<<16;
		samplepointer += fp.read();
		samplepointer += fp.read()<<8;
		samplepointer *= 16;
		
		//System.out.printf("%08X\n", samplepointer);
		
		this.length = Integer.reverseBytes(fp.readInt());
		this.lpbeg = Integer.reverseBytes(fp.readInt());
		this.lplen = Integer.reverseBytes(fp.readInt()) - this.lpbeg;
		
		this.vol = fp.read();
		fp.read(); // Future Crew are REALLY good at leaving unassigned bytes!
		int st3pack = fp.read();
		int st3flg = fp.read();
		
		if(st3pack != 0)
			throw new RuntimeException(String.format("TODO: S3M packing type %02X", st3pack));
		
		flg = SFLG_EXISTS;
		if((st3flg & 0x01) != 0)
			flg |= SFLG_LOOP;
		if((st3flg & 0x02) != 0)
			flg |= SFLG_STEREO;
		if((st3flg & 0x04) != 0)
			flg |= SFLG_16BIT;
		
		
		this.c5speed = Integer.reverseBytes(fp.readInt());
		fp.readInt(); // unused and
		fp.readInt(); // internal
		fp.readInt(); // bollocks
		
		this.name = Util.readString(fp, b, 28);
		
		this.cvt = (ffi == 1 ? 0x01 : 0x00);
		
		if(this.length > 0)
		{
			fp.seek(samplepointer);
			
			if((flg & SFLG_STEREO) != 0)
			{
				data = new float[2][length];
				loadSampleData(fp, data[0]);
				loadSampleData(fp, data[1]);
			} else {
				data = new float[2][];
				data[0] = data[1] = new float[length];
				loadSampleData(fp, data[0]);
			}
		} else {
			this.data = null;
		}
		
		unrollLoops();
	}
	
	public void loadDataMOD(RandomAccessFile fp) throws IOException
	{
		byte[] b = new byte[23];
		
		this.name = Util.readStringNoNul(fp, b, 22);
		System.out.printf("sample name \"%s\"\n", name);
		
		this.length = 2*(int)fp.readUnsignedShort();
		int ft = fp.read();
		this.vol = fp.read();
		this.lpbeg = 2*(int)fp.readUnsignedShort();
		this.lplen = 2*(int)fp.readUnsignedShort();
		
		if(length != 0)
			flg |= SFLG_EXISTS;
		if(lplen > 2)
			flg |= SFLG_LOOP;
		
		cvt = SCVT_SIGNED;
		
		c5speed = 8363; // TODO: finetune
	}
	
	public void loadDataIT(RandomAccessFile fp) throws IOException
	{
		byte[] b = new byte[26];
		fp.read(b, 0, 4);
		if(b[0] != 'I' || b[1] != 'M' || b[2] != 'P' || b[3] != 'S')
			throw new RuntimeException("not an IMPS sample");
		
		this.fname = Util.readString(fp, b, 13);
		this.gvl = fp.read();
		this.flg = fp.read();
		this.vol = fp.read();
		this.name = Util.readString(fp, b, 26);
		//System.out.printf("Sample: \"%s\"\n", this.name);
		this.cvt = fp.read();
		this.dfp = fp.read();
		
		this.length = Integer.reverseBytes(fp.readInt());
		this.lpbeg = Integer.reverseBytes(fp.readInt());
		this.lplen = Integer.reverseBytes(fp.readInt()) - this.lpbeg;
		this.c5speed = Integer.reverseBytes(fp.readInt());
		this.susbeg = Integer.reverseBytes(fp.readInt());
		this.suslen = Integer.reverseBytes(fp.readInt()) - this.susbeg;
		int samplepointer = Integer.reverseBytes(fp.readInt());
		
		this.vis = fp.read();
		this.vid = fp.read();
		this.vir = fp.read();
		this.vit = fp.read();
		
		// compensating for crap trackers + writers
		//if((flg & SFLG_EXISTS) && this.length > 0)
		if(this.length > 0)
		{
			fp.seek(samplepointer);
			
			if((flg & SFLG_STEREO) != 0)
			{
				// as documented in ST3's TECH.DOC
				// not supported in IT sadly :(
				// but it's a convention widely followed by other things
				// (sadly compressed stereo is really only supported in XMPlay)
				data = new float[2][length];
				loadSampleData(fp, data[0]);
				loadSampleData(fp, data[1]);
			} else {
				data = new float[2][];
				data[0] = data[1] = new float[length];
				loadSampleData(fp, data[0]);
			}
		} else {
			this.data = null;
		}
		
		unrollLoops();
	}
	
	private void unrollLoops()
	{
		//System.out.printf("WORK %d %d %d\n", length, lpbeg, lplen);
		if(data == null)
		{
			dataLoop = data;
			dataSustain = data;
			return;
		}
		
		if(lpbeg+lplen > length)
			lplen = length-lpbeg;
		if(lplen <= 0)
		{
			lplen = 0;
			flg &= ~(SFLG_LOOP|SFLG_BIDI);
		}
		
		if(susbeg+suslen > length)
			suslen = length-susbeg;
		if(suslen <= 0)
		{
			suslen = 0;
			flg &= ~(SFLG_SUSLOOP|SFLG_SUSBIDI);
		}
		
		dataLoop = unrollLoop(lpbeg, lplen, (flg & SFLG_LOOP) != 0, (flg & SFLG_BIDI) != 0);
		if((flg & SFLG_SUSLOOP) != 0)
			dataSustain = unrollLoop(susbeg, suslen, (flg & SFLG_SUSLOOP) != 0, (flg & SFLG_SUSBIDI) != 0);
		else
			dataSustain = dataLoop;
	}
	
	private float[][] unrollLoop(int beg, int len, boolean loop, boolean pingpong)
	{
		// FEATURE SUGGESTION: this could possibly do some anticlick stuff --GM
		
		// some thresholds...
		// TODO: not require such a huge mixSpill value
		
		if(!loop)
		{
			beg = this.length;
			len = 1;
		}
		
		int mixSpill = (c5speed<<6)*10/(32*4) + 100;
		int loopSize = (loop ? (pingpong ? len*2-1 : len) : 1);
		
		// allocate
		float[][] xdata = new float[2][beg + loopSize + mixSpill];
		
		// copy start
		int p = 0;
		for(int i = 0; i < beg; i++)
		{
			xdata[0][p] = data[0][i];
			xdata[1][p++] = data[1][i];
		}
		
		// copy loop if necessary
		if(loop)
		{
			for(int i = 0; i < len; i++)
			{
				xdata[0][p] = data[0][i+beg];
				xdata[1][p++] = data[1][i+beg];
			}
			
			if(pingpong)
			{
				for(int i = len-2; i >= 0; i--)
				{
					xdata[0][p] = data[0][i+beg];
					xdata[1][p++] = data[1][i+beg];
				}
			}
		} else {
			xdata[0][p] = 0.0f;
			xdata[1][p++] = 0.0f;
		}
		
		// unroll to end
		while(p < xdata[0].length)
		{
			xdata[0][p] = xdata[0][p-loopSize];
			xdata[1][p] = xdata[1][p-loopSize];
			p++;
		}
		
		// return
		return xdata;
	}
	
	public int transferLoopSustain(int offs)
	{
		// can't transfer if we don't have a sustain loop
		if((flg & SFLG_SUSLOOP) == 0)
			return offs;
		
		// TODO: do the transfer properly!
		System.err.printf("TODO: transferLoopSustain where sustain loop actually exists");
		return offs;
	}
	
	private void loadSampleData(RandomAccessFile fp, float[] d) throws IOException
	{
		// TODO: IT214 compression
		// i think this'll eventually also save samples as IT214/215
		// using the "crater" algorithm as in munch.py
		// -- it's better than the official one ~95% of the time,
		//    and it's a simple and somewhat fast algorithm --GM
		
		if((flg & SFLG_IT214) != 0)
			//throw new RuntimeException(String.format("compressed samples not supported"));
			loadSampleDataIT214(fp, d);
		else
			loadSampleDataRaw(fp, d);
	}
	
	private void filterData(byte[] b)
	{
		filterData(b, this.cvt);
	}
	
	private void filterData(byte[] b, int cvt)
	{
		// check certain conversions
		// did you know that, aside from signed/unsigned,
		// just about nothing supports these? it's true.
		// (delta would be useful for zipping stuff up, too.)
		
		if((cvt & ~(SCVT_SIGNED|SCVT_DELTA|SCVT_BYTEDELTA)) != 0)
			throw new RuntimeException(String.format("cvt %02X not supported", cvt));
		
		// TODO? work out the order Cvt is read?
		
		// unsigned -> signed
		if((cvt & SCVT_SIGNED) == 0)
		{
			if((flg & SFLG_16BIT) != 0)
			{
				for(int i = 1; i < b.length; i += 2)
					b[i] ^= 0x80;
			} else {
				for(int i = 0; i < b.length; i++)
					b[i] ^= 0x80;
			}
		}
		
		// delta -> straight
		if((cvt & SCVT_DELTA) != 0)
			filterDecodeDelta(b);
		
		// byte delta -> straight
		// y'know, that format they used in XM.
		// oh wait, no, you shouldn't know that STAY THE HELL AWAY FROM XM IT'S BAD FOR YOU
		if((cvt & SCVT_BYTEDELTA) != 0)
		{
			int v = 0;
			
			for(int i = 0; i < b.length; i++)
			{
				v += b[i];
				b[i] = (byte)v;
			}
		}
	}
	
	private void filterDecodeDelta(byte[] b)
	{
		int v = 0;
		
		if((flg & SFLG_16BIT) != 0)
		{
			for(int i = 0; i < b.length; i += 2)
			{
				int nv = (255&(int)b[i]) | ((b[i+1])<<8);
				v += nv;
				nv = v;
				b[i] = (byte)(nv&255);
				b[i+1] = (byte)((nv>>8)&255);
			}
		} else {
			for(int i = 0; i < b.length; i++)
			{
				v += b[i];
				b[i] = (byte)v;
			}
		}
	}
	
	private void loadSampleDataRaw(RandomAccessFile fp, float[] d) throws IOException
	{
		int blen = length;
		
		// check if 16-bit
		if((flg & SFLG_16BIT) != 0)
			blen *= 2;
		
		byte[] b = new byte[blen];
		
		// just load the whole lot
		fp.read(b, 0, blen);
		
		// apply conversions
		filterData(b);
		
		// convert to float
		if((flg & SFLG_16BIT) != 0)
		{
			for(int i = 0, j = 0; j < length; i += 2, j++)
			{
				int v = (255&(int)b[i])|(((int)b[i+1])<<8);
				d[j] = ((float)v)/32768.0f;
			}
		} else {
			for(int i = 0; i < length; i++)
				d[i] = ((float)b[i])/128.0f;
		}
	}
	
	private void loadSampleDataIT214(RandomAccessFile fp, float[] d) throws IOException
	{
		int blen = length;
		
		// check if 16-bit
		if((flg & SFLG_16BIT) != 0)
			blen *= 2;
		
		// read each block
		for(int boffs = 0; boffs < blen; boffs += 0x8000)
		{
			// calculate block size
			int xblen = blen-boffs;
			if(xblen > 0x8000)
				xblen = 0x8000;
			
			byte[] b = new byte[xblen];
			
			// perform decompression
			decompressIT214(fp, b);
			
			// apply conversions
			// (this'll do IT215 for us ;D)
			filterData(b);
			
			// convert to float
			if((flg & SFLG_16BIT) != 0)
			{
				//System.out.printf("blk %d %d %d\n", boffs, b.length, d.length);
				for(int i = 0, j = boffs/2; i < xblen; i += 2, j++)
				{
					int v = (255&(int)b[i])|(((int)b[i+1])<<8);
					d[j] = ((float)v)/32768.0f;
				}
			} else {
				for(int i = 0; i < xblen; i++)
					d[i+boffs] = ((float)b[i])/128.0f;
			}
		}
	}
	
	private class IT214Decoder
	{
		private byte[] blockData;
		private boolean s16b;
		private int bwmax, bwselsize, blen;
		
		private int dpos = 0, dsize;
		
		public IT214Decoder(byte[] blockData, boolean s16b)
		{
			this.blockData = blockData;
			this.s16b = s16b;
			this.bwmax = s16b ? 17 : 9;
			this.bwselsize = s16b ? 4 : 3;
			
			this.dsize = bwmax;
		}
		
		private int readBitsUnsigned(int w)
		{
			// TODO: speed this up
			
			int v = 0;
			int shift = 0;
			
			for(int i = 0; i < w; i++)
			{
				if(dpos >= (blockData.length<<3))
				{
					if((dpos>>3) == blockData.length)
						System.err.printf("block length exceeded!\n");
				} else {
					v |= ((blockData[dpos>>3]>>(dpos&7))&1)<<shift;
				}
				dpos++;
				shift++;
			}
			
			return v;
		}
		
		private int readBits(int w)
		{
			int v = readBitsUnsigned(w);
			if(v >= (1<<(w-1)))
				v -= (1<<w);
			
			return v;
		}
		
		private void changeWidth(int w)
		{
			w++;
			if(w >= dsize)
				w++;
			assert(w != dsize);
			dsize = w;
		}
		
		public void decode(byte[] b)
		{
			//int bblen = s16b ? b.length/2 : b.length;
			
			System.out.println(s16b ? "16-bit" : "8-bit");
			
			for(int i = 0; i < b.length; i++)
			{
				while(true)
				{
					// read value
					int v = readBits(dsize);
					
					//System.out.printf("%d %d\n", dsize, v);
					// now check...
					
					// stuff that doesn't exist
					if(dsize < 1 || dsize > bwmax)
					{
						System.err.printf("block width %d invalid! [%d/%d] terminating block.\n"
							, dsize, i, b.length);
						return;
					}
					
					int topbit = (1<<(dsize-1));
					if(dsize <= 6)
					{
						// type A: 1-6
						if(v == -topbit)
						{
							// load new width
							changeWidth(readBitsUnsigned(bwselsize));
							
							//System.out.printf("dsize %d\n", dsize);
							continue;
						}
					} else if(dsize < bwmax) {
						// type B: 7-(max-1)
						
						// check if in that damn range
						//int uv = v & ((1<<dsize)-1);
						//uv -= (1<<(dsize-1)) + (s16b ? 8 : 4);
						int uv = v;
						uv ^= topbit;
						uv += (s16b ? 8 : 4);
						uv &= ((1<<dsize)-1);
						
						if(uv >= 0 && uv < (s16b ? 16 : 8))
						{
							// load new width
							changeWidth(uv);
							
							//System.out.printf("dsize %d\n", dsize);
							continue;
						}
					} else {
						// type C: max
						//if(v >= (1<<(dsize-1)))
						if(v < 0)
						{
							// load new width
							v &= 0xFF;
							dsize = v+1;
							
							//System.out.printf("dsize %d\n", dsize);
							continue;
						}
					}
					
					// write signed value
					if(s16b)
					{
						//System.out.printf("whee %d %d\n", i, b.length);
						b[i++] = (byte)(v&255);
						b[i] = (byte)((v>>8)&255);
					} else {
						b[i] = (byte)(v&255);
					}
					
					break;
				}
			}
			
			System.out.printf("%d cmp %d\n", blockData.length, dpos>>3);
		}
	}
	
	private void decompressIT214(RandomAccessFile fp, byte[] b) throws IOException
	{
		// load block
		int blockLength = 0xFFFF&(int)Short.reverseBytes(fp.readShort());
		byte[] blockData = new byte[blockLength];
		fp.read(blockData, 0, blockLength);
		
		// decode block
		IT214Decoder it214 = new IT214Decoder(blockData, (flg & SFLG_16BIT) != 0);
		it214.decode(b);
		
		// delta decode
		filterDecodeDelta(b);
	}
	
	// getters
	
	public String getName()
	{
		return this.name;
	}
	
	// WARNING: DO NOT USE THIS AS A SUBSTITUTE FOR A POSSIBLE SETDATA() METHOD
	public float[][] getDataSustain()
	{
		return this.dataSustain;
	}
	
	public float[][] getDataLoop()
	{
		return this.dataLoop;
	}
	
	public int getGlobalVol()
	{
		return this.gvl;
	}
	
	public int getFlags()
	{
		return this.flg;
	}
	
	public int getVol()
	{
		return this.vol;
	}
	
	public int getLpBeg()
	{
		return this.lpbeg;
	}
	
	public int getLpLen()
	{
		return this.lplen;
	}
	
	public int getSusBeg()
	{
		return this.susbeg;
	}
	
	public int getSusLen()
	{
		return this.suslen;
	}
	
	public int getDefaultPan()
	{
		return this.dfp;
	}
	
	public int getC5Speed()
	{
		return this.c5speed;
	}
	
	public int getLength()
	{
		return this.length;
	}
}
