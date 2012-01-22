package session;

import misc.Util;

import java.io.*;

public class SessionPattern
{
	/*
	public static class TrackEntry
	{
		private SessionTrack trk; // track
		private int pbeg, plen, poffs;
		
		public TrackEntry(SessionTrack trk, int pbeg, int plen, int poffs)
		{
			this.trk = trk;
			this.pbeg = pbeg;
			this.plen = plen;
			this.poffs = poffs;
		}
	}
	*/
	
	private int rows;
	private SessionTrack[] tracks;
	private int[] tidx;
	
	public SessionPattern(Session session, RandomAccessFile fp) throws IOException
	{
		int size = 0xFFFF&(int)Short.reverseBytes(fp.readShort());
		this.rows = 0xFFFF&(int)Short.reverseBytes(fp.readShort());
		fp.readInt(); // skip 4 bytes
		
		this.tracks = new SessionTrack[64];
		
		int[] masks = new int[64];
		int[][] ld = new int[64][];
		
		for(int i = 0; i < 64; i++)
			ld[i] = new int[] {253, 0, 255, 0, 0};
		
		for(int r = 0; r < rows; r++)
		{
			while(true)
			{
				int csel = fp.read();
				
				if((csel&0x7F) == 0)
					break;
				
				int chn = (csel & 0x7F)-1;
				// TODO: check how 0x80 behaves, and chns > 64
				
				if(tracks[chn] == null)
					tracks[chn] = new SessionTrack(rows);
				
				int mask = (csel & 0x80) != 0
					? (masks[chn] = fp.read())
					: masks[chn]
				;
				
				if((mask & 0x01) != 0)
					ld[chn][0] = fp.read();
				if((mask & 0x02) != 0)
					ld[chn][1] = fp.read();
				if((mask & 0x04) != 0)
					ld[chn][2] = fp.read();
				if((mask & 0x08) != 0)
				{
					ld[chn][3] = fp.read();
					ld[chn][4] = fp.read();
				}
				
				if((mask & 0x11) != 0)
					tracks[chn].setDataByte(r, 0, ld[chn][0]);
				if((mask & 0x22) != 0)
					tracks[chn].setDataByte(r, 1, ld[chn][1]);
				if((mask & 0x44) != 0)
					tracks[chn].setDataByte(r, 2, ld[chn][2]);
				if((mask & 0x88) != 0)
				{
					tracks[chn].setDataByte(r, 3, ld[chn][3]);
					tracks[chn].setDataByte(r, 4, ld[chn][4]);
				}
				
			}
		}
		
		tidx = session.addTracks(tracks);
	}
	
	public SessionTrack getTrack(int idx)
	{
		return (idx >= 0 && idx < tracks.length
			? tracks[idx]
			: null);
	}
	
	public int getRows()
	{
		return rows;
	}
}
