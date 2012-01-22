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
