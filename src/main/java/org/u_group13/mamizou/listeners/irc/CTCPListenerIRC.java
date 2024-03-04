package org.u_group13.mamizou.listeners.irc;

import net.engio.mbassy.listener.Handler;
import org.jetbrains.annotations.NotNull;
import org.kitteh.irc.client.library.event.channel.ChannelCtcpEvent;
import org.kitteh.irc.client.library.event.channel.ChannelTargetedCtcpEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class CTCPListenerIRC
{
	private static final Logger LOGGER = LoggerFactory.getLogger(CTCPListenerIRC.class);

	@Handler
	public void onReceiveCTCP(@NotNull ChannelCtcpEvent event)
	{
		LOGGER.debug("Received broad CTCP \"{}\" from {}", event.getMessage(), event.getActor().getUserString());
	}

	@Handler
	public void onReceiveTargetedCTCP(@NotNull ChannelTargetedCtcpEvent event)
	{

		LOGGER.debug("Received targeted CTCP \"{}\" from {}", event.getMessage(), event.getActor().getUserString());
	}
}
