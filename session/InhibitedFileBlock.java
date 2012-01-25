package session;

import java.io.*;

public class InhibitedFileBlock
{
	private RandomAccessFile fp;
	private int len;
	
	public InhibitedFileBlock(RandomAccessFile fp, int len)
	{
		this.fp = fp;
		this.len = len;
		//System.out.printf("len %d %08X\n", len, len);
	}
	
	public int read() throws IOException
	{
		if(len <= 0)
			return 0;
		
		len--;
		return fp.read();
	}
	
	public void done() throws IOException
	{
		while(len --> 0)
			fp.read();
	}
	
	public short readShort() throws IOException
	{
		int v = read()<<8;
		v += read();
		return (short)v;
	}
	
	public int readInt() throws IOException
	{
		int v = read()<<24;
		v += read()<<16;
		v += read()<<8;
		v += read();
		return v;
	}
}
