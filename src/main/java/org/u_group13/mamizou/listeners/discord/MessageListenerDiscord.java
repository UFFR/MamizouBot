package org.u_group13.mamizou.listeners.discord;

import static org.u_group13.mamizou.Main.config;
import static org.u_group13.mamizou.Main.helper;

import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import org.jetbrains.annotations.NotNull;
import org.kitteh.irc.client.library.element.Channel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.u_group13.mamizou.Main;
import org.u_group13.mamizou.util.IRCCode;
import org.u_group13.mamizou.util.StringUtil;
import org.u_group13.mamizou.util.converter.DiscordToIRC;

import java.util.Optional;

public class MessageListenerDiscord extends ListenerAdapter
{
	private static final Logger LOGGER = LoggerFactory.getLogger(MessageListenerDiscord.class);
	public static final int MAX_IRC_MESSAGE = 512;

	@Override
	public void onMessageReceived(@NotNull MessageReceivedEvent event)
	{
		LOGGER.trace("Got message {} from {}", event.getMessageId(), event.isWebhookMessage() || event.getMember() == null
				? "webhook or null sender" : event.getMember().getId());
		if (event.isWebhookMessage()
				|| event.getAuthor().getIdLong() == Main.getJda().getSelfUser().getIdLong()
				|| helper.sentMessages.contains(event.getMessageIdLong())
				|| config.ignoredUsers.ignoredDiscordIDs.contains(event.getAuthor().getIdLong())
				|| config.ignoredUsers.ignoredDiscordUsers.contains(event.getAuthor().getName()))
			return;


		final long discordChanID = event.getChannel().getIdLong();
		if (helper.discordToIRCMapping.containsKey(discordChanID))
		{
			final String ircChannelName = helper.discordToIRCMapping.get(discordChanID);
			final Optional<Channel> optionalChannel = Main.getIrcClient().getChannel(ircChannelName);
			if (optionalChannel.isPresent())
			{
				final String messageContent = DiscordToIRC.convert(event.getMessage().getContentDisplay());
				final Channel ircChannel = optionalChannel.get();
				final String name = event.getAuthor().getEffectiveName();
				final String coloredNick = IRCCode.getColoredNick(name);
				if (!messageContent.isEmpty())
					ircChannel.sendMultiLineMessage(coloredNick + ' ' + messageContent);
				for (Message.Attachment attachment : event.getMessage().getAttachments())
					ircChannel.sendMessage(coloredNick + ' ' + attachment.getUrl());
			} else
				LOGGER.warn("Discord channel {} ({}) is mapped to {}, but IRC client couldn't find it!", event.getChannel().getName(), discordChanID, ircChannelName);
		}
	}
}
