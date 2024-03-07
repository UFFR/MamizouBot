package org.u_group13.mamizou.listeners.irc;

import net.dv8tion.jda.api.entities.channel.concrete.TextChannel;
import net.engio.mbassy.listener.Handler;
import org.jetbrains.annotations.NotNull;
import org.kitteh.irc.client.library.element.Channel;
import org.kitteh.irc.client.library.element.User;
import org.kitteh.irc.client.library.event.channel.ChannelCtcpEvent;
import org.kitteh.irc.client.library.event.channel.ChannelTargetedCtcpEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.u_group13.mamizou.Main;

public class CTCPListenerIRC
{
	private static final Logger LOGGER = LoggerFactory.getLogger(CTCPListenerIRC.class);

	@Handler
	public void onReceiveCTCP(@NotNull ChannelCtcpEvent event)
	{
		LOGGER.debug("Received broad CTCP \"{}\" from {} in {}", event.getMessage(), event.getActor().getUserString(), event.getChannel().getMessagingName());
	}

	@Handler
	public void onReceiveTargetedCTCP(@NotNull ChannelTargetedCtcpEvent event)
	{
		LOGGER.debug("Received targeted CTCP \"{}\" from {} in {}", event.getMessage(), event.getActor().getUserString(), event.getChannel().getMessagingName());
	}

	private static void doAction(@NotNull User user, @NotNull String channelName, String message)
	{
		LOGGER.debug("Client received ACTION CTCP event");

		if (Main.helper.ircToDiscordMapping.containsKey(channelName))
		{
			final long discordChanID = Main.helper.ircToDiscordMapping.get(channelName);
			final TextChannel textChannel = Main.getJda().getTextChannelById(discordChanID);

			if (textChannel != null)
				textChannel.sendMessage(String.format("**%s** *%s*", user.getMessagingName(), message)).queue();
			else
				LOGGER.warn("Channel {} is mapped to {}, but JDA couldn't find!", channelName, discordChanID);
		}
	}
}
