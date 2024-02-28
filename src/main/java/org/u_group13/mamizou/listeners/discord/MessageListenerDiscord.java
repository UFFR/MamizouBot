package org.u_group13.mamizou.listeners.discord;

import static org.u_group13.mamizou.Main.config;
import static org.u_group13.mamizou.Main.helper;

import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.events.guild.member.update.GuildMemberUpdateNicknameEvent;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import org.jetbrains.annotations.NotNull;
import org.kitteh.irc.client.library.element.Channel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.u_group13.mamizou.Main;
import org.u_group13.mamizou.util.IRCCodes;

import java.util.Optional;

public class MessageListenerDiscord extends ListenerAdapter
{
	private static final Logger LOGGER = LoggerFactory.getLogger(MessageListenerDiscord.class);

	@Override
	public void onGuildMemberUpdateNickname(@NotNull GuildMemberUpdateNicknameEvent event)
	{
		// TODO Add message handling
		LOGGER.trace("Member {} ({}) changed their nickname from {} to {}", event.getMember().getUser().getName(),
		             event.getMember().getId(), event.getOldNickname(), event.getNewNickname());

		if (config.ignoredUsers.ignoredDiscordIDs.contains(event.getUser().getIdLong()))
			return;

		event.getGuild().getChannels().stream().mapToLong(
				net.dv8tion.jda.api.entities.channel.Channel::getIdLong).forEach(id ->
        {
			if (helper.discordToIRCMapping.containsKey(id))
			{
				final Optional<Channel> optionalChannel = Main.getIrcClient()
				                                              .getChannel(
						                                              helper.discordToIRCMapping.get(id));
				if (optionalChannel.isEmpty())
				{
					LOGGER.warn("Discord channel {} is mapped, but IRC client couldn't find!", id);
					return;
				}

				optionalChannel.get().sendMessage(
						String.format("%s is now known as %s", IRCCodes.getColoredNick(event.getOldNickname() == null ? event.getMember().getEffectiveName() : event.getOldNickname()
						), IRCCodes.getColoredNick(event.getNewNickname() == null ? event.getMember().getEffectiveName() : event.getNewNickname())));
			}
        });

	}

	@Override
	public void onMessageReceived(@NotNull MessageReceivedEvent event)
	{
		LOGGER.trace("Got message {} from {}", event.getMessageId(), event.isWebhookMessage() ? "webhook" : event.getMember().getId());
		if (event.isWebhookMessage()
				|| event.getAuthor().getIdLong() == Main.getJda().getSelfUser().getIdLong()
				|| helper.sentMessages.contains(event.getMessageIdLong())
				|| config.ignoredUsers.ignoredDiscordIDs.contains(event.getAuthor().getIdLong())
				|| config.ignoredUsers.ignoredDiscordUsers.contains(event.getAuthor().getName()))
			return;

		final String messageContent = event.getMessage().getContentDisplay();
		if (messageContent.isEmpty())
			return;

		final long discordChanID = event.getChannel().getIdLong();
		if (helper.discordToIRCMapping.containsKey(discordChanID))
		{
			final String ircChannelName = helper.discordToIRCMapping.get(discordChanID);
			final Optional<Channel> optionalChannel = Main.getIrcClient().getChannel(ircChannelName);
			if (optionalChannel.isPresent())
			{
				final Channel ircChannel = optionalChannel.get();
				final String name = event.getAuthor().getEffectiveName();
				final String coloredNick = IRCCodes.getColoredNick(name);
				ircChannel.sendMessage(coloredNick + ' ' + messageContent);
				for (Message.Attachment attachment : event.getMessage().getAttachments())
					ircChannel.sendMessage(coloredNick + ' ' + attachment.getUrl());
			} else
				LOGGER.warn("Discord channel {} ({}) is mapped to {}, but IRC client couldn't find it!", event.getChannel().getName(), discordChanID, ircChannelName);
		}
	}
}
