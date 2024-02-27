package org.u_group13.mamizou.listeners.irc;

import static org.u_group13.mamizou.Main.getJda;
import static org.u_group13.mamizou.Main.helper;

import net.dv8tion.jda.api.entities.channel.concrete.TextChannel;
import net.engio.mbassy.listener.Handler;
import org.jetbrains.annotations.NotNull;
import org.kitteh.irc.client.library.event.channel.*;
import org.kitteh.irc.client.library.event.user.UserModeEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.u_group13.mamizou.util.StringUtil;

public class GenericListenerIRC
{
	private static final Logger LOGGER = LoggerFactory.getLogger(GenericListenerIRC.class);

	@Handler
	public void onChannelUpdateTopic(@NotNull ChannelTopicEvent event)
	{
		LOGGER.debug("Channel {} notified of topic status from \"{}\" to \"{}\"", event.getChannel(), event.getOldTopic(), event.getNewTopic());

		if (!event.isNew())
			return;

		if (helper.ircToDiscordMapping.containsKey(event.getChannel().getMessagingName()))
		{
			final long discordChanID = helper.ircToDiscordMapping.get(event.getChannel().getMessagingName());
			final TextChannel textChannel = getJda().getTextChannelById(discordChanID);
			if (textChannel != null)
				textChannel.sendMessage(StringUtil.getChannelTopicUpdateString(event)).queue();
			else
				LOGGER.warn("IRC channel {} is mapped to {}, but JDA couldn't find!", event.getChannel(), discordChanID);
		}
	}

	@Handler
	public void onUserPartChannel(@NotNull ChannelPartEvent event)
	{
		LOGGER.debug("User {} left channel {}", event.getUser().getHost(), event.getChannel().getMessagingName());

		if (helper.ircToDiscordMapping.containsKey(event.getChannel().getMessagingName()))
		{
			final long discordChanID = helper.ircToDiscordMapping.get(event.getChannel().getMessagingName());
			final TextChannel textChannel = getJda().getTextChannelById(discordChanID);
			if (textChannel != null)
				textChannel.sendMessage(StringUtil.getIrcUserPartString(event)).queue();
			else
				LOGGER.warn("IRC channel {} is mapped to {}, but JDA couldn't find!", event.getChannel(), discordChanID);
		}
	}

	@Handler
	public void onChannelModeChanged(@NotNull ChannelModeEvent event)
	{
		LOGGER.debug("Channel {} updated mode to {}", event.getChannel().getMessagingName(), event.getStatusList());

		if (helper.ircToDiscordMapping.containsKey(event.getChannel().getMessagingName()))
		{
			final long discordChanID = helper.ircToDiscordMapping.get(event.getChannel().getMessagingName());
			final TextChannel textChannel = getJda().getTextChannelById(discordChanID);
			if (textChannel != null)
				textChannel.sendMessage(StringUtil.getIrcChannelModeUpdateString(event)).queue();
			else
				LOGGER.warn("IRC channel {} is mapped to {}, but JDA couldn't find!", event.getChannel(), discordChanID);
		}
	}

	@Handler
	public void onChannelKnock(@NotNull ChannelKnockEvent event)
	{
		LOGGER.debug("User {} knocks on channel {}", event.getActor().getHost(), event.getChannel().getMessagingName());

		if (helper.ircToDiscordMapping.containsKey(event.getChannel().getMessagingName()))
		{
			final long discordChanID = helper.ircToDiscordMapping.get(event.getChannel().getMessagingName());
			final TextChannel textChannel = getJda().getTextChannelById(discordChanID);
			if (textChannel != null)
				textChannel.sendMessage(StringUtil.getIrcChannelKnockString(event)).queue();
			else
				LOGGER.warn("IRC channel {} is mapped to {}, but JDA couldn't find!", event.getChannel(), discordChanID);
		}
	}

	@Handler
	public void onChannelNotice(@NotNull ChannelNoticeEvent event)
	{
		LOGGER.debug("User {} sends notice to channel {}", event.getActor().getHost(), event.getChannel().getMessagingName());

		if (event.getClient().isUser(event.getActor()))
			return;

		if (helper.ircToDiscordMapping.containsKey(event.getChannel().getMessagingName()))
		{
			final long discordChanID = helper.ircToDiscordMapping.get(event.getChannel().getMessagingName());
			final TextChannel textChannel = getJda().getTextChannelById(discordChanID);
			if (textChannel != null)
				textChannel.sendMessage(StringUtil.getIrcNoticeString(event)).queue();
			else
				LOGGER.warn("IRC channel {} is mapped to {}, but JDA couldn't find!", event.getChannel(), discordChanID);
		}
	}

	@Handler
	public void onUserModeUpdated(@NotNull UserModeEvent event)
	{
		// TODO Finish
		LOGGER.debug("User {} updated modes to {}", event.getActor().getName(), event.getStatusList());
	}
}