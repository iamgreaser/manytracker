package session;

import java.io.*;

public class MMCMPUnpacker extends RandomAccessFile
{
	public static class NotPackedException extends Exception
	{
		public NotPackedException(String s)
		{
			super(s);
		}
	}
	
	private boolean decompressing = true;
	private byte[] b = null;
	private int b_pos = 0;
	private byte[] cbuf = null;
	
	public MMCMPUnpacker(File fd) throws FileNotFoundException, NotPackedException
	{
		super(fd, "r");
		
		// check first 8 bytes
		byte[] zirconia = new byte[8];
		
		try
		{
			super.read(zirconia, 0, 8);
			
			if(new String(zirconia, "ISO-8859-1").equals("ziRCONia"))
			{
				unpackMMCMP();
				return;
			}
		} catch(IOException ex) {
			// ignore
		}
		
		throw new NotPackedException("file not packed with MMCMP - use a normal RandomAccessFile!");
	}
	
	public int read() throws IOException
	{
		if(decompressing)
			return super.read();
		else if(b_pos >= 0 && b_pos < b.length)
			return 255&(int)b[b_pos++];
		else
			return -1;
	}
	
	public void seek(int pos) throws IOException
	{
		if(decompressing)
			super.seek(pos);
		else
			b_pos = pos;
		
		// TODO: RTFM and find out what's going on
	}
	
	private void unpackMMCMP() throws IOException
	{
		// some info is from my previous effort (mmpunk.py),
		// of which the base format was fairly well cracked
		// (but the checksum hasn't been sussed out yet).
		
		// MMCMP versions < 1.20 will most likely not be detected.
		// In case you're wondering, I don't have any modules MMCMP'd earlier than 1.30.
		System.out.println("Making an awful attempt at decompressing this MMCMP'd module.");
		
		int unk1 = 0xFFFF&(int)Short.reverseBytes(super.readShort());
		int version = 0xFFFF&(int)Short.reverseBytes(super.readShort());
		int blkcount = 0xFFFF&(int)Short.reverseBytes(super.readShort());
		int fileunpacksize = Integer.reverseBytes(super.readInt());
		int unk3 = 0xFFFF&(int)Short.reverseBytes(super.readShort());
		int unk4 = 0xFFFF&(int)Short.reverseBytes(super.readShort());
		int unk5 = 0xFFFF&(int)Short.reverseBytes(super.readShort());
		
		// known versions:
		// 0x130F = 1.30
		// 0x1330 = 1.33
		// 0x1340 = 1.34
		System.out.printf("unknown 1:     %04X\n", unk1);
		System.out.printf("version:       %04X\n", version);
		System.out.printf("blocks:        %04X\n", blkcount);
		System.out.printf("unpacked size: %08X\n", fileunpacksize);
		System.out.printf("unknown 3:     %04X\n", unk3);
		System.out.printf("unknown 4:     %04X\n", unk4);
		System.out.printf("unknown 5:     %04X\n", unk5);
		
		this.b = new byte[fileunpacksize];
		
		int[] blklist = new int[blkcount];
		
		for(int i = 0; i < blkcount; i++)
			blklist[i] = Integer.reverseBytes(super.readInt());
		
		for(int i = 0; i < blkcount; i++)
		{
			int blkpos = blklist[i];
			System.out.printf("  block at %08X\n", blkpos);
			super.seek(blkpos);
			loadBlock();
		}
		
		decompressing = false;
		
		throw new RuntimeException("TODO: finish off reverse-engineering this damn thing (MMCMP)");
	}
	
	private void loadBlock() throws IOException
	{
		int opensize = Integer.reverseBytes(super.readInt());
		int packsize = Integer.reverseBytes(super.readInt());
		int checksum = Integer.reverseBytes(super.readInt());
		int ccount = 0xFFFF&(int)Short.reverseBytes(super.readShort());
		System.out.printf("    unpacked size:  %08X\n", opensize);
		System.out.printf("    packed size:    %08X\n", packsize);
		System.out.printf("    checksum:       %08X\n", checksum);
		System.out.printf("    chunk count:    %04X\n", ccount);
		
		int[][] clist = new int[ccount][2];
		
		int bunk3 = 0xFFFF&(int)Short.reverseBytes(super.readShort());
		int bcount = 0xFFFF&(int)Short.reverseBytes(super.readShort());
		int bcomp = 0xFFFF&(int)Short.reverseBytes(super.readShort());
		System.out.printf("    unknown 3:      %04X\n", bunk3);
		System.out.printf("    byte orderings: %04X\n", bcount);
		System.out.printf("    compression:    %04X\n", bcomp);
		
		for(int i = 0; i < ccount; i++)
		{
			int cptr = Integer.reverseBytes(super.readInt());
			int clen = Integer.reverseBytes(super.readInt());
			clist[i][0] = cptr;
			clist[i][1] = clen;
			System.out.printf("      chunk %d data: destpos=%08X destlen=%08X\n", i+1, cptr, clen);
		}
		
		this.cbuf = new byte[opensize];
		
	}
}
