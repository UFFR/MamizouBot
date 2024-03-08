package org.u_group13.mamizou.util;

import org.u_group13.mamizou.config.LinkRegistries;

public class ConfigSaver implements Runnable
{
	private static ConfigSaver instance = null;
	public static ConfigSaver getInstance()
	{
		return instance == null ? instance = new ConfigSaver() : instance;
	}

	@Override
	public void run()
	{
		LinkRegistries.saveRegistries();
	}
}
