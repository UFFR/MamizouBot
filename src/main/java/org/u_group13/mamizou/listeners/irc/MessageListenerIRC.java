package org.u_group13.mamizou.listeners.irc;

import static org.u_group13.mamizou.Main.*;

import club.minnced.discord.webhook.WebhookClient;
import club.minnced.discord.webhook.send.AllowedMentions;
import club.minnced.discord.webhook.send.WebhookMessageBuilder;
import net.dv8tion.jda.api.entities.channel.concrete.TextChannel;
import net.engio.mbassy.listener.Handler;
import org.eclipse.collections.api.factory.primitive.LongObjectMaps;
import org.eclipse.collections.api.map.primitive.MutableLongObjectMap;
import org.eclipse.collections.api.tuple.Pair;
import org.jetbrains.annotations.NotNull;
import org.kitteh.irc.client.library.event.channel.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.u_group13.mamizou.commands.IRCCommandBase;
import org.u_group13.mamizou.util.StringUtil;

public class MessageListenerIRC
{
	private static final Logger LOGGER = LoggerFactory.getLogger(MessageListenerIRC.class);

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
				textChannel.sendMessage(StringUtil.getIrcUserJoinedString(event)).queue();
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
				textChannel.sendMessage(StringUtil.getIrcUserKickedString(event)).queue();
			else
				LOGGER.warn("IRC channel is mapped, but JDA couldn't find Discord text channel!");
		}
	}

	@Handler
	public void onUserMessage(@NotNull ChannelMessageEvent event)
	{
		LOGGER.trace("Got message from {}: ", event.getActor().getUserString());

		if (config.ignoredUsers.ignoredHosts.contains(event.getActor().getHost()))
			return;

		final String message = event.getMessage();

		if (message.isEmpty())
			return;

		if (message.charAt(0) == '!')
		{
			IRCCommandBase.tryExecute(event);
			return;
		}

		final String ircChannel = event.getChannel().getMessagingName();
		if (helper.ircToDiscordMapping.containsKey(ircChannel))
		{
			final long discordChanID = helper.ircToDiscordMapping.get(ircChannel);

			TextChannel textChannel = getJda().getTextChannelById(discordChanID);

			if (textChannel == null)
			{
				LOGGER.warn("Channel {} is mapped to {}, but JDA couldn't find it!", ircChannel, discordChanID);
				return;
			}

			if (helper.webhookClientCache.containsKey(discordChanID))
			{
				final WebhookClient webhookClient = helper.webhookClientCache.get(discordChanID);
				final WebhookMessageBuilder messageBuilder = new WebhookMessageBuilder();

				if (helper.ircToDiscordUserCache.containsKey(event.getActor().getMessagingName()))
				{
					final long userID = helper.ircToDiscordUserCache.get(event.getActor().getMessagingName());
					final MutableLongObjectMap<Pair<String,String>> guildCache = helper.discordUserCache.getIfAbsentPut(
							textChannel.getGuild().getIdLong(), LongObjectMaps.mutable::empty);

					if (guildCache.containsKey(userID))
					{
						final Pair<String, String> cache = guildCache.get(userID);
						messageBuilder.setUsername(cache.getOne());
						messageBuilder.setAvatarUrl(cache.getTwo());
					} else
						LOGGER.warn("User with ID {} was found in ircToDiscordUserCache, but not userInfoCache!", userID);
				} else
					messageBuilder.setUsername(event.getActor().getMessagingName());

				messageBuilder.setContent(message).setAllowedMentions(AllowedMentions.none());
				webhookClient.send(messageBuilder.build()).thenAccept(helper::addSentMessage);
			} else
				textChannel.sendMessage(StringUtil.getIrcUserSentMessageString(event)).queue();
		}
	}
}
