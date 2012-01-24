package main;

import player.*;
import session.*;
import tracker.*;

public class Main
{
	public static void main(String[] args) throws Exception
	{
		Player player = new Player(new Session(args[0]));
		
		player.playFromStart();
		while(true)
		{
			player.tick();
			Thread.yield();
		}
	}
}
