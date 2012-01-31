package tracker;

import java.awt.*;
import java.awt.image.*;
import java.io.*;
import javax.imageio.*;

public class TrackerFont
{
	// fonts that can be used
	public static final TrackerFont FNT_XAIMUS = new TrackerFont("data/font-4x5.bmp", 4,5);
	
	// fields.
	private int width, height;
	private BufferedImage img;
	
	public TrackerFont(String fname, int width, int height)
	{
		this.width = width;
		this.height = height;
		try
		{
			InputStream fp = getClass().getClassLoader().getResourceAsStream(fname);
			this.img = ImageIO.read(fp);
		} catch(IOException ex) {
			// pass it on - can't run without a font.
			System.err.printf("IOEXCEPTION\n");
			throw new RuntimeException(ex);
		}
	}
	
	public void write(Graphics g, String str, int x, int y)
	{
		for(int i = 0; i < str.length(); i++)
		{
			// read + filter char
			char c = str.charAt(i);
			if(c < 32 || c > 126)
				c = '?';
			
			// get pos + blit
			int sx = (c-32)*width;
			g.drawImage(img, x, y, x+width, y+height, sx, 0, sx+width, height, null);
			
			// move along
			x += width;
		}
	}
	
	public int getWidth()
	{
		return width;
	}
	
	public int getHeight()
	{
		return height;
	}
}
