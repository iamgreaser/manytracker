package main;

import player.*;
import session.*;
import tracker.*;

public class Main
{
	public static void main(String[] args) throws Exception
	{
		//Player player = new Player(new Session(args[0]));
		Tracker tracker = new Tracker(new Session(args[0]));
		Player player = tracker.getPlayer();
		
		
		player.playFromStart();
		while(true)
		{
			player.tick();
			tracker.repaint();
			Thread.yield();
		}
	}
}
