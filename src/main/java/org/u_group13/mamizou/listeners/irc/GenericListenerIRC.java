package org.u_group13.mamizou.listeners.irc;

import static org.u_group13.mamizou.Main.*;

import net.dv8tion.jda.api.entities.channel.concrete.TextChannel;
import net.engio.mbassy.listener.Handler;
import org.jetbrains.annotations.NotNull;
import org.kitteh.irc.client.library.element.Channel;
import org.kitteh.irc.client.library.event.channel.*;
import org.kitteh.irc.client.library.event.user.UserModeEvent;
import org.kitteh.irc.client.library.event.user.UserQuitEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.u_group13.mamizou.util.StringUtil;

import java.util.Optional;

public class GenericListenerIRC
{
	private static final Logger LOGGER = LoggerFactory.getLogger(GenericListenerIRC.class);

	@Handler
	public void onUserJoinChannelIRC(@NotNull ChannelJoinEvent event)
	{
		final String channelName = event.getChannel().getMessagingName();
		LOGGER.trace("User {} joined channel {}", event.getUser().getMessagingName(), channelName);
		if (event.getClient().isUser(event.getUser()) || helper.ignoredUsers.ignoredHosts.contains(event.getUser().getHost()))
			return;

		if (helper.ircToDiscordMapping.containsKey(channelName))
		{
			final long discordChanID = helper.ircToDiscordMapping.get(channelName);

			final TextChannel textChannel = getJda().getTextChannelById(discordChanID);
			if (textChannel != null)
				textChannel.sendMessage(String.format("**%s** has joined the channel", event.getUser().getMessagingName())).queue();
			else
				LOGGER.warn("IRC channel is mapped, but JDA couldn't find Discord text channel!");
		}
	}

	@Handler
	public void onUserKicked(@NotNull ChannelKickEvent event)
	{
		LOGGER.debug("User {} kicked from channel {}", event.getUser().getUserString(), event.getChannel().getMessagingName());

		if (helper.ircToDiscordMapping.containsKey(event.getChannel().getMessagingName()))
		{
			final TextChannel textChannel = getJda()
					.getTextChannelById(helper.ircToDiscordMapping.get(
							event.getChannel().getMessagingName()));
			if (textChannel != null)
				textChannel.sendMessage(String.format("**%s** was kicked by **%s** (%s)", event.getTarget().getMessagingName(),
				                                      event.getActor().getName(), event.getMessage())).queue();
			else
				LOGGER.warn("IRC channel is mapped, but JDA couldn't find Discord text channel!");
		}
	}

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
				textChannel.sendMessage(
						String.format("Channel topic updated to \"%s\" by **%s**", event.getNewTopic().getValue().orElse(""),
						              event.getNewTopic().getSetter().orElse(null))).queue();
			else
				LOGGER.warn("IRC channel {} is mapped to {}, but JDA couldn't find!", event.getChannel(), discordChanID);
		}
	}

	@Handler
	public void onUserPartChannelIRC(@NotNull ChannelPartEvent event)
	{
		final String channelName = event.getChannel().getMessagingName();
		LOGGER.trace("User {} left channel {}", event.getUser().getMessagingName(), channelName);
		if (helper.ignoredUsers.ignoredHosts.contains(event.getUser().getHost()))
			return;

		if (helper.ircToDiscordMapping.containsKey(channelName))
		{
			final long discordChanID = helper.ircToDiscordMapping.get(channelName);
			final TextChannel textChannel = getJda().getTextChannelById(discordChanID);
			if (textChannel != null)
				textChannel.sendMessage(String.format("**%s** has left the channel (%s)", event.getUser().getMessagingName(),
				                                      event.getMessage())).queue();
			else
				LOGGER.warn("Channel {} is mapped to {}, but JDA couldn't find!", channelName, discordChanID);
		}
	}

	@Handler
	public void onUserQuitServer(@NotNull UserQuitEvent event)
	{
		LOGGER.trace("User {} quit the server", event.getUser().getMessagingName());
		if (helper.ignoredUsers.ignoredHosts.contains(event.getUser().getHost()))
			return;

		for (String channel : event.getUser().getChannels())
		{
			if (!helper.ircToDiscordMapping.containsKey(channel))
				continue;

			final long discordChanID = helper.ircToDiscordMapping.get(channel);
			final TextChannel textChannel = getJda().getTextChannelById(discordChanID);
			if (textChannel != null)
				textChannel.sendMessage(
						           String.format("**%s** has quit (%s)", event.getUser().getMessagingName(), event.getMessage()))
				           .queue();
			else
				LOGGER.warn("Channel {} is mapped to {}, but JDA couldn't find!", channel, discordChanID);
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
				textChannel.sendMessage(String.format("Channel mode updated to [**%s**] by **%s**",
				                                      StringUtil.modesToString(event.getStatusList().getAll()),
				                                      event.getActor().getName())).queue();
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
				textChannel.sendMessage(
						String.format("**%s** knocked", event.getActor().getMessagingName())).queue();
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
				textChannel.sendMessage(
						String.format("**%s** sent notice: %s", event.getActor().getMessagingName(),
						              event.getMessage())).queue();
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
