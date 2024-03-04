package org.u_group13.mamizou.listeners.irc;

import static org.u_group13.mamizou.Main.*;

import club.minnced.discord.webhook.WebhookClient;
import club.minnced.discord.webhook.send.AllowedMentions;
import club.minnced.discord.webhook.send.WebhookMessageBuilder;
import net.dv8tion.jda.api.entities.channel.concrete.TextChannel;
import net.engio.mbassy.listener.Handler;
import org.jetbrains.annotations.NotNull;
import org.kitteh.irc.client.library.event.channel.*;
import org.kitteh.irc.client.library.event.user.PrivateMessageEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.u_group13.mamizou.commands.IRCCommandBase;
import org.u_group13.mamizou.config.LinkEntry;
import org.u_group13.mamizou.config.LinkRegistries;

import java.util.Optional;

public class MessageListenerIRC
{
	private static final Logger LOGGER = LoggerFactory.getLogger(MessageListenerIRC.class);

	@Handler
	public void onUserMessage(@NotNull ChannelMessageEvent event)
	{
		LOGGER.trace("Got message from {}: ", event.getActor().getHost());

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

			final TextChannel textChannel = getJda().getTextChannelById(discordChanID);

			if (textChannel == null)
			{
				LOGGER.warn("Channel {} is mapped to {}, but JDA couldn't find it!", ircChannel, discordChanID);
				return;
			}

			if (helper.webhookClientCache.containsKey(discordChanID))
			{
				final WebhookClient webhookClient = helper.webhookClientCache.get(discordChanID);
				final WebhookMessageBuilder messageBuilder = new WebhookMessageBuilder();

				final Optional<String> optionalAccount = event.getActor().getAccount();

				if (optionalAccount.isPresent() && LinkRegistries.getInstance().isRegistered(optionalAccount.get()))
				{
					LOGGER.trace("User is linked and registered");
//					LinkRegistries.getInstance().setupWebhook(userID, textChannel.getGuild().getIdLong(), messageBuilder);
					final LinkEntry entry = LinkRegistries.getInstance().getEntry(optionalAccount.get());
					entry.setupWebhook(textChannel.getIdLong(), messageBuilder);
				} else
				{
					LOGGER.trace("User is not linked");
					messageBuilder.setUsername(event.getActor().getMessagingName());
				}

				messageBuilder.setContent(message).setAllowedMentions(AllowedMentions.none());
				webhookClient.send(messageBuilder.build()).thenAccept(helper::addSentMessage);
			} else
				textChannel.sendMessage(
						String.format("**<%s>** %s", event.getActor().getMessagingName(),
						              event.getMessage())).queue();
		}
	}

	@Handler
	public void onPrivateMessage(@NotNull PrivateMessageEvent event)
	{
		LOGGER.debug("Client received a private message from {}", event.getActor().getHost());
	}
}
