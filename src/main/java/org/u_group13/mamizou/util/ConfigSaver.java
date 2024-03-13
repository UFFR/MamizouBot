package org.u_group13.mamizou.util;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.eclipsecollections.EclipseCollectionsModule;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.u_group13.mamizou.Main;
import org.u_group13.mamizou.config.LinkRegistries;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.Files;

public class ConfigSaver implements Runnable
{
	private static final Logger LOGGER = LoggerFactory.getLogger(ConfigSaver.class);
	private static ConfigSaver instance = null;
	public static ConfigSaver getInstance()
	{
		return instance == null ? instance = new ConfigSaver() : instance;
	}

	@Override
	public void run()
	{
		if (Main.config.saveDataPath == null)
		{
			LOGGER.warn("Cannot save data, path is null!");
			return;
		}

		if (Files.isDirectory(Main.config.saveDataPath))
		{
			LOGGER.warn("Cannot save data, path is directory!");
			return;
		}


		try
		{
			Files.createDirectories(Main.config.saveDataPath.getParent());

			try (final OutputStream stream = Files.newOutputStream(Main.config.saveDataPath))
			{
				final ObjectMapper mapper = new ObjectMapper().registerModule(new EclipseCollectionsModule());

				LinkRegistries.saveRegistries(mapper, stream);
			}
		} catch (IOException e)
		{
			LOGGER.error("Caught exception trying to save!", e);
		}
	}
}
