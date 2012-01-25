package misc;

import java.io.*;

public class Util
{
	public static double getWaveform(int type, int offs)
	{
		// TODO: types that aren't 0
		return -Math.sin((offs&255)*Math.PI/128.0);
	}
	
	public static String readString(RandomAccessFile fp, byte[] b, int len) throws IOException
	{
		int i = 0;
		
		fp.read(b, 0, len);
		
		while(i < len-1)
		{
			if(b[i] == 0)
				break;
			
			i++;
		}
		
		return new String(b, 0, i, "ISO-8859-1");
	}
	
	public static String readStringNoNul(RandomAccessFile fp, byte[] b, int len) throws IOException
	{
		int i = 0;
		
		fp.read(b, 0, len);
		
		while(i < len)
		{
			if(b[i] == 0)
				break;
			
			i++;
		}
		
		return new String(b, 0, i, "ISO-8859-1");
	}
}
