package org.u_group13.mamizou.listeners.irc;

import net.engio.mbassy.listener.Handler;
import org.jetbrains.annotations.NotNull;
import org.kitteh.irc.client.library.event.client.ClientReceiveCommandEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class CommandListenerIRC
{
	private static final Logger LOGGER = LoggerFactory.getLogger(CommandListenerIRC.class);

	@Handler
	public void onReceiveCommand(@NotNull ClientReceiveCommandEvent event)
	{
		LOGGER.debug("Received command event: {}", event);
	}
}
