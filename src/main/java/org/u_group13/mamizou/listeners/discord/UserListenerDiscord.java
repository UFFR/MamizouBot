package org.u_group13.mamizou.listeners.discord;

import static org.u_group13.mamizou.Main.getIrcClient;
import static org.u_group13.mamizou.Main.helper;

import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.channel.concrete.TextChannel;
import net.dv8tion.jda.api.events.guild.member.update.GenericGuildMemberUpdateEvent;
import net.dv8tion.jda.api.events.guild.member.update.GuildMemberUpdateAvatarEvent;
import net.dv8tion.jda.api.events.guild.member.update.GuildMemberUpdateNicknameEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.u_group13.mamizou.util.StringUtil;

import java.util.List;
import java.util.stream.Collectors;

public class UserListenerDiscord extends ListenerAdapter
{
	private static final Logger LOGGER = LoggerFactory.getLogger(UserListenerDiscord.class);

	@Override
	public void onGuildMemberUpdateNickname(@NotNull GuildMemberUpdateNicknameEvent event)
	{
		LOGGER.debug("Member {} updated nickname from {} to {}", event.getMember().getId(), event.getOldNickname(), event.getNewNickname());
		updateCache(event);

		LOGGER.trace("Updating all mapped channels");
		event.getUser()
		     .getMutualGuilds()
		     .stream()
		     .flatMap(guild -> guild.getTextChannels().stream())
			 .mapToLong(TextChannel::getIdLong)
				.filter(helper.discordChannels::contains)
				.mapToObj(helper.discordToIRCMapping::get)
				.forEach(channelName -> getIrcClient().getChannel(channelName)
				                                      .ifPresentOrElse(channel -> channel.sendMessage(StringUtil.getDiscordUserNickChanged(event)),
				                                                       () -> LOGGER.warn("Channel {} mapped, but IRC client couldn't find!", channelName)));

	}

	@Override
	public void onGuildMemberUpdateAvatar(@NotNull GuildMemberUpdateAvatarEvent event)
	{
		LOGGER.debug("Member {} updated effective avatar from {} to {}", event.getMember().getId(), event.getOldAvatarUrl(), event.getNewAvatarUrl());
		updateCache(event);
	}

	private static void updateCache(@NotNull GenericGuildMemberUpdateEvent<String> event)
	{
		LOGGER.debug("Updating cache for member {} in guild {}", event.getMember().getId(), event.getGuild().getId());
		final long guildID = event.getGuild().getIdLong();
		final long userID = event.getMember().getIdLong();
		helper.offerUserCache(guildID, userID, event.getMember().getEffectiveName(), event.getNewValue());
	}
}