package org.u_group13.mamizou.listeners.discord;

import static org.u_group13.mamizou.Main.*;

import net.dv8tion.jda.api.entities.channel.concrete.TextChannel;
import net.dv8tion.jda.api.events.guild.member.update.GuildMemberUpdateAvatarEvent;
import net.dv8tion.jda.api.events.guild.member.update.GuildMemberUpdateNicknameEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class UserListenerDiscord extends ListenerAdapter
{
	private static final Logger LOGGER = LoggerFactory.getLogger(UserListenerDiscord.class);

//	@Override
//	public void onGuildMemberUpdateNickname(@NotNull GuildMemberUpdateNicknameEvent event)
//	{
//		LOGGER.trace("Member {} ({}) changed their nickname from {} to {}", event.getMember().getUser().getName(),
//		             event.getMember().getId(), event.getOldNickname(), event.getNewNickname());
//
//		if (config.ignoredUsers.ignoredDiscordIDs.contains(event.getUser().getIdLong()))
//			return;
//
//		event.getGuild().getChannels().stream().mapToLong(
//				net.dv8tion.jda.api.entities.channel.Channel::getIdLong).forEach(id ->
//				                                                                 {
//					                                                                 if (helper.discordToIRCMapping.containsKey(id))
//					                                                                 {
//						                                                                 final Optional<Channel> optionalChannel = Main.getIrcClient()
//						                                                                                                               .getChannel(
//								                                                                                                               helper.discordToIRCMapping.get(id));
//						                                                                 if (optionalChannel.isEmpty())
//						                                                                 {
//							                                                                 LOGGER.warn("Discord channel {} is mapped, but IRC client couldn't find!", id);
//							                                                                 return;
//						                                                                 }
//
//						                                                                 optionalChannel.get().sendMessage(
//								                                                                 String.format("%s is now known as %s", IRCCodes.getColoredNick(event.getOldNickname() == null ? event.getMember().getEffectiveName() : event.getOldNickname()
//								                                                                 ), IRCCodes.getColoredNick(event.getNewNickname() == null ? event.getMember().getEffectiveName() : event.getNewNickname())));
//					                                                                 }
//				                                                                 });
//
//	}

	@Override
	public void onGuildMemberUpdateNickname(@NotNull GuildMemberUpdateNicknameEvent event)
	{
		LOGGER.debug("Member {} updated nickname from {} to {}", event.getMember().getId(), event.getOldNickname(), event.getNewNickname());

		// Wack
		LOGGER.trace("Updating all mapped channels");
		event.getUser()
		     .getMutualGuilds()
		     .stream()
		     .flatMap(guild -> guild.getTextChannels().stream())
			 .mapToLong(TextChannel::getIdLong)
				.filter(helper.discordChannels::contains)
				.mapToObj(helper.discordToIRCMapping::get)
				.forEach(channelName -> getIrcClient().getChannel(channelName)
				                                      .ifPresentOrElse(channel -> channel.sendMessage(
						                                                       String.format("%s is now known as %s",
						                                                                     event.getOldNickname(),
						                                                                     event.getNewNickname())),
				                                                       () -> LOGGER.warn("Channel {} mapped, but IRC client couldn't find!", channelName)));

	}

	@Override
	public void onGuildMemberUpdateAvatar(@NotNull GuildMemberUpdateAvatarEvent event)
	{
		LOGGER.debug("Member {} updated effective avatar from {} to {}", event.getMember().getId(), event.getOldAvatarUrl(), event.getNewAvatarUrl());
	}

}
