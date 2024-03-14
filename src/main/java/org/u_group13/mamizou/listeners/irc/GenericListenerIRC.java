package org.u_group13.mamizou.listeners.irc;

import static org.u_group13.mamizou.Main.*;

import net.dv8tion.jda.api.entities.channel.concrete.TextChannel;
import net.engio.mbassy.listener.Handler;
import org.jetbrains.annotations.NotNull;
import org.kitteh.irc.client.library.element.Channel;
import org.kitteh.irc.client.library.element.User;
import org.kitteh.irc.client.library.event.channel.*;
import org.kitteh.irc.client.library.event.user.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.u_group13.mamizou.util.StringUtil;
import org.u_group13.mamizou.util.converter.IRCToDiscord;

import java.util.Optional;

public class GenericListenerIRC
{
	private static final Logger LOGGER = LoggerFactory.getLogger(GenericListenerIRC.class);

	@Handler
	public void onUserJoinChannel(@NotNull ChannelJoinEvent event)
	{
		LOGGER.trace("User {} joined channel {}", event.getUser(), event.getChannel());

		final String channelName = event.getChannel().getMessagingName();
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
		LOGGER.debug("User {} kicked from channel {}", event.getUser(), event.getChannel());

		final String channelName = event.getChannel().getMessagingName();
		if (helper.ircToDiscordMapping.containsKey(channelName))
		{
			final TextChannel textChannel = getJda()
					.getTextChannelById(helper.ircToDiscordMapping.get(
							channelName));
			if (textChannel != null)
			{
				textChannel.sendMessage(String.format("**%s** was kicked by **%s** (%s)", event.getTarget().getMessagingName(),
				                                      event.getActor().getName(), event.getMessage())).queue();
			} else
				LOGGER.warn("IRC channel is mapped, but JDA couldn't find Discord text channel!");
		}
	}

	@Handler
	public void onUserAwayUpdated(@NotNull UserAwayMessageEvent event)
	{
		LOGGER.trace("User {} away status updated to \"{}\"", event.getActor(), event.getAwayMessage());

		final String message = event.isAway()
				? String.format("**%s** is away with message: %s", event.getActor().getMessagingName(),
		                                    event.getAwayMessage().orElse(""))
				: String.format("**%s** is no longer away", event.getActor().getMessagingName());

		for (String channelName : event.getActor().getChannels())
		{
			if (helper.ircToDiscordMapping.containsKey(channelName))
			{
				final long discordChanID = helper.ircToDiscordMapping.get(channelName);

				final TextChannel textChannel = getJda().getTextChannelById(discordChanID);
				if (textChannel != null)
					textChannel.sendMessage(message).queue();
				else
					LOGGER.warn("Channel {} is mapped to {}, but JDA couldn't find!", channelName, discordChanID);
			}
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
			{
				textChannel.sendMessage(
						String.format("Channel topic updated to \"%s\" by **%s**", event.getNewTopic().getValue().orElse(""),
						              event.getNewTopic().getSetter().orElse(null))).queue();
			} else
				LOGGER.warn("IRC channel {} is mapped to {}, but JDA couldn't find!", event.getChannel(), discordChanID);
		}
	}

	@Handler
	public void onUserPartChannelIRC(@NotNull ChannelPartEvent event)
	{
		LOGGER.trace("User {} left channel {}", event.getUser(), event.getChannel());

		final String channelName = event.getChannel().getMessagingName();
		if (helper.ignoredUsers.ignoredHosts.contains(event.getUser().getHost()))
			return;

		if (helper.ircToDiscordMapping.containsKey(channelName))
		{
			final long discordChanID = helper.ircToDiscordMapping.get(channelName);
			final TextChannel textChannel = getJda().getTextChannelById(discordChanID);
			if (textChannel != null)
			{
				textChannel.sendMessage(String.format("**%s** has left the channel (%s)", event.getUser().getMessagingName(),
				                                      event.getMessage())).queue();
			} else
				LOGGER.warn("Channel {} is mapped to {}, but JDA couldn't find!", channelName, discordChanID);
		}
	}

	@Handler
	public void onUserQuitServer(@NotNull UserQuitEvent event)
	{
		LOGGER.trace("User {} quit the server", event.getUser());

		if (helper.ignoredUsers.ignoredHosts.contains(event.getUser().getHost()))
			return;

		for (String channel : event.getUser().getChannels())
		{
			if (!helper.ircToDiscordMapping.containsKey(channel))
				continue;

			final long discordChanID = helper.ircToDiscordMapping.get(channel);
			final TextChannel textChannel = getJda().getTextChannelById(discordChanID);
			if (textChannel != null)
			{
				textChannel.sendMessage(
						           String.format("**%s** has quit (%s)", event.getUser().getMessagingName(), event.getMessage()))
				           .queue();
			} else
				LOGGER.warn("Channel {} is mapped to {}, but JDA couldn't find!", channel, discordChanID);
		}

	}

	@Handler
	public void onChannelModeChanged(@NotNull ChannelModeEvent event)
	{
		LOGGER.debug("Channel {} updated mode to {}", event.getChannel(), event.getStatusList());

		if (helper.ircToDiscordMapping.containsKey(event.getChannel().getMessagingName()))
		{
			final long discordChanID = helper.ircToDiscordMapping.get(event.getChannel().getMessagingName());
			final TextChannel textChannel = getJda().getTextChannelById(discordChanID);
			if (textChannel != null)
			{
				textChannel.sendMessage(String.format("Channel mode updated to [**%s**] by **%s**",
				                                      StringUtil.modesToString(event.getStatusList().getAll()),
				                                      event.getActor().getName())).queue();
			} else
				LOGGER.warn("IRC channel {} is mapped to {}, but JDA couldn't find!", event.getChannel(), discordChanID);
		}
	}

	@Handler
	public void onChannelKnock(@NotNull ChannelKnockEvent event)
	{
		LOGGER.debug("User {} knocks on channel {}", event.getActor(), event.getChannel());

		if (helper.ircToDiscordMapping.containsKey(event.getChannel().getMessagingName()))
		{
			final long discordChanID = helper.ircToDiscordMapping.get(event.getChannel().getMessagingName());
			final TextChannel textChannel = getJda().getTextChannelById(discordChanID);
			if (textChannel != null)
			{
				textChannel.sendMessage(
						String.format("**%s** knocked", event.getActor().getMessagingName())).queue();
			} else
				LOGGER.warn("IRC channel {} is mapped to {}, but JDA couldn't find!", event.getChannel(), discordChanID);
		}
	}

	@Handler
	public void onChannelNotice(@NotNull ChannelNoticeEvent event)
	{
		LOGGER.debug("User {} sends notice to channel {}", event.getActor(), event.getChannel());

		if (event.getClient().isUser(event.getActor()))
			return;

		if (helper.ircToDiscordMapping.containsKey(event.getChannel().getMessagingName()))
		{
			final long discordChanID = helper.ircToDiscordMapping.get(event.getChannel().getMessagingName());
			final TextChannel textChannel = getJda().getTextChannelById(discordChanID);
			if (textChannel != null)
			{
				textChannel.sendMessage(
						String.format("**%s** sent notice: %s", event.getActor().getMessagingName(),
						              IRCToDiscord.convert(event.getMessage()))).queue();
			} else
				LOGGER.warn("IRC channel {} is mapped to {}, but JDA couldn't find!", event.getChannel(), discordChanID);
		}
	}

	@Handler
	public void onUserModeUpdated(@NotNull UserModeEvent event)
	{
		// TODO Finish
		LOGGER.debug("User {} updated modes to {}", event.getActor(), event.getStatusList());
		for (Channel channel : getIrcClient().getChannels())
		{
			final Optional<User> optionalUser = channel.getUser(event.getActor().getName());
			final String channelName = channel.getMessagingName();
			if (helper.ircToDiscordMapping.containsKey(channelName) && optionalUser.isPresent())
			{
				LOGGER.trace("Found possible matching channel to send");
				final long discordChanID = helper.ircToDiscordMapping.get(channelName);
				final TextChannel textChannel = getJda().getTextChannelById(discordChanID);
				if (textChannel != null)
					textChannel.sendMessage(String.format("**%s** changed modes to [**%s**]", event.getActor().getName(), StringUtil.modesToString(event.getStatusList().getAll()))).queue();
				else
					LOGGER.warn("Channel {} is mapped to {}, but JDA couldn't find!", channelName, discordChanID);
			}
		}

	}

	@Handler
	public void onUserNickUpdated(@NotNull UserNickChangeEvent event)
	{
		LOGGER.debug("User {} changed nick to {}", event.getActor(), event.getActor());

		for (String channelName : event.getActor().getChannels())
		{
			if (helper.ircToDiscordMapping.containsKey(channelName))
			{
				final long discordChanID = helper.ircToDiscordMapping.get(channelName);
				final TextChannel textChannel = getJda().getTextChannelById(discordChanID);
				if (textChannel != null)
					textChannel.sendMessage(String.format("**%s** is now known as **%s**", event.getOldUser().getMessagingName(), event.getNewUser().getMessagingName())).queue();
				else
					LOGGER.warn("Channel {} is mapped to {}, but JDA couldn't find!", channelName, discordChanID);
			}
		}
	}

	@Handler
	public void onUserLoginUpdated(@NotNull UserAccountStatusEvent event)
	{
		LOGGER.debug("User {} changed status to {}", event.getActor(), event.getAccount());

		for (String channelName : event.getActor().getChannels())
		{
			if (helper.ircToDiscordMapping.containsKey(channelName))
			{
				final long discordChanID = helper.ircToDiscordMapping.get(channelName);
				final TextChannel textChannel = getJda().getTextChannelById(discordChanID);
				if (textChannel != null)
				{
					textChannel.sendMessage(
							event.getAccount().isPresent()
									? String.format("**%s** has identified as **%s**",
									                event.getActor().getMessagingName(), event.getAccount().get())
			                        : String.format("**%s** has unidentified", event.getActor().getMessagingName())).queue();
				} else
					LOGGER.warn("IRC channel is mapped, but JDA couldn't find Discord text channel!");
			}
		}

	}
}
