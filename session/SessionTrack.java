package session;

public class SessionTrack
{
	private int rows;
	private int[][] data;
	
	public SessionTrack(int rows)
	{
		this.rows = rows;
		this.data = new int[rows][];
		
		for(int i = 0; i < rows; i++)
			data[i] = new int[] {253, 0, 255, 0, 0};
	}
	
	public void filterS3MEffects()
	{
		int lastnzefp = 0;
		int lastnzeft = 0;
		int lastnzvol = 255;
		for(int i = 0; i < rows; i++)
		{
			int vol = data[i][2];
			int eft = data[i][3];
			int efp = data[i][4];
			
			// now we look for possible effect memories
			if(lastnzeft != eft)
			{
				switch(eft)
				{
					case 0x04:
					case 0x05:
					case 0x06:
					case 0x09:
					case 0x0A:
					case 0x0B:
					case 0x0C:
					case 0x11:
					case 0x12:
					case 0x13:
						data[i][4] = lastnzefp;
						break;
				}
			}
			
			// fix Rxx volumey thing
			if(eft == 0x12 && vol == 255)
				data[i][2] = lastnzvol;
			
			if(vol != 255)
				lastnzvol = vol;
			if(eft != 0)
				lastnzeft = eft;
			if(efp != 0)
				lastnzefp = efp;
		}
		
		// finally, some quick fixes
		for(int i = 0; i < rows; i++)
		{
			int eft = data[i][3];
			int efp = data[i][4];
			
			switch(eft)
			{
				case 0x04:
					if(
						   (efp & 0x0F) != 0
						&& (efp & 0xF0) != 0
						&& (efp & 0x0F) != 0x0F
						&& (efp & 0xF0) != 0xF0
							)
						efp &= 15;
					break;
				case 0x0B:
				case 0x0C:
					if(
						   (efp & 0x0F) != 0
						&& (efp & 0xF0) != 0
						&& (efp & 0x0F) != 0x0F
						&& (efp & 0xF0) != 0xF0
							)
						efp &= 15;
					
					// fine slides don't work in Kxx/Lxx!
					if((efp & 0x0F) != 0 && (efp & 0xF0) != 0 )
					{
						efp = 0;
						if(eft == 0x0B)
							eft = 0x08;
						else if(eft == 0x0C)
							eft = 0x07;
						else // technically EDOOFUS
							eft = 0x00; 
					}
					break;
				case 0x13:
					switch(efp>>4)
					{
						// TODO: somehow shift SBx into one track
						case 0xC:
						case 0xD:
							if(efp == 0xC0)
								eft = efp = 0;
							break;
					}
			}
			
			data[i][3] = eft;
			data[i][4] = efp;
		}
		
	}
	
	public boolean getData(int r, int[] data)
	{
		if(r < 0 || r >= rows)
			return false;
		
		for(int i = 0; i < 5; i++)
			data[i] = this.data[r][i];
		
		return true;
	}
	
	public void setData(int r, int[] data)
	{
		if(r < 0 || r >= rows)
			return;
		
		for(int i = 0; i < 5; i++)
			this.data[r][i] = data[i];
	}
	
	public void setDataByte(int r, int c, int db)
	{
		if(r < 0 || r >= rows)
			return;
		
		this.data[r][c] = db;
	}
}
