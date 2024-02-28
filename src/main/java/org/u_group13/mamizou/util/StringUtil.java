package org.u_group13.mamizou.util;

import net.dv8tion.jda.api.events.guild.member.update.GuildMemberUpdateNicknameEvent;
import org.eclipse.collections.api.factory.primitive.CharCharMaps;
import org.eclipse.collections.api.map.primitive.CharCharMap;
import org.eclipse.collections.api.map.primitive.MutableCharCharMap;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import org.kitteh.irc.client.library.element.User;
import org.kitteh.irc.client.library.element.mode.Mode;
import org.kitteh.irc.client.library.element.mode.ModeStatus;
import org.kitteh.irc.client.library.event.channel.*;
import org.kitteh.irc.client.library.event.user.UserModeEvent;

import java.util.ArrayList;
import java.util.List;
import java.util.StringJoiner;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class StringUtil
{
//	private static final Logger LOGGER = LoggerFactory.getLogger(StringUtil.class);
	public static final String VERSION = "0.1.0-SNAPSHOT";
	public static final String SPLIT_REGEX = "[^\\s\"']+|\"([^\"]*)\"|'([^']*)'";
	public static final Pattern SPLIT_PATTERN = Pattern.compile(SPLIT_REGEX);
	public static final String IRC_CHANNEL_TOPIC_UPDATE = "Channel topic updated to \"%s\" by **%s**";
	public static final String IRC_CHANNEL_MODE_UPDATE = "Channel mode updated to **%s** by **%s**";
	public static final String IRC_CHANNEL_KNOCK = "**%s** knocked";
	public static final String IRC_CHANNEL_NOTICE = "**%s** sent notice: %s";
	public static final String IRC_USER_SENT_MESSAGE = "**<%s>** %s";
	public static final String IRC_USER_JOINED = "**%s** has joined the channel";
	public static final String IRC_USER_PARTED = "**%s** has left the channel (%s)";
	public static final String IRC_USER_KICKED = "**%s** was kicked by **%s** (%s)";
	public static final String IRC_USER_MODE_UPDATED = "**%s** mode updated to **%s**";
	public static final String DISCORD_USER_NICK_CHANGED = "%s is now known as %s";

	public static String getChannelTopicUpdateString(@NotNull ChannelTopicEvent event)
	{
		return String.format(IRC_CHANNEL_TOPIC_UPDATE, event.getNewTopic().getValue().orElse(""), event.getNewTopic().getSetter().orElse(null));
	}

	public static String getIrcChannelModeUpdateString(@NotNull ChannelModeEvent event)
	{
		return String.format(IRC_CHANNEL_MODE_UPDATE, modesToString(event.getStatusList().getAll()), event.getActor().getName());
	}

	public static String getIrcChannelKnockString(@NotNull ChannelKnockEvent event)
	{
		return String.format(IRC_CHANNEL_KNOCK, event.getActor().getMessagingName());
	}

	public static String getIrcNoticeString(@NotNull ChannelNoticeEvent event)
	{
		return String.format(IRC_CHANNEL_NOTICE, event.getActor().getMessagingName(), event.getMessage());
	}

	public static String getIrcUserSentMessageString(@NotNull ChannelMessageEvent event)
	{
		return String.format(IRC_USER_SENT_MESSAGE, event.getActor().getMessagingName(), event.getMessage());
	}

	public static String getIrcUserJoinedString(@NotNull ChannelJoinEvent event)
	{
		return String.format(IRC_USER_JOINED, event.getUser().getMessagingName());
	}

	public static String getIrcUserPartedString(@NotNull ChannelPartEvent event)
	{
		return String.format(IRC_USER_PARTED, event.getUser().getMessagingName(), event.getChannel().getMessagingName());
	}

	public static String getIrcUserKickedString(@NotNull ChannelKickEvent event)
	{
		return String.format(IRC_USER_KICKED, event.getTarget().getMessagingName(), event.getActor().getName(), event.getMessage());
	}

	public static String getIrcUserModeUpdated(@NotNull UserModeEvent event)
	{
		return String.format(IRC_USER_MODE_UPDATED, event.getActor().getName(), modesToString(event.getStatusList().getAll()));
	}

	public static String getDiscordUserNickChanged(@NotNull GuildMemberUpdateNicknameEvent event)
	{
		return String.format(DISCORD_USER_NICK_CHANGED, event.getOldNickname(), event.getNewNickname());
	}

	@NotNull
	@Contract(pure = true)
	public static String getCreditsString()
	{
		return "This project was made possible by:\n" +
				"- JDA (v5.0.0-beta.20) by Austin Keener (DV8FromTheWorld), Michael Ritter, Florian Spieß (MinnDevelopment), et al.\n" +
				"- Discord-Webhooks (v0.8.4) by MinnDevelopment\n" +
				"- Kitteh IRC Client Library (KICL) (v9.0.0) by Kitteh\n" +
				"- Picocli (v4.7.5) by remkop\n" +
				"- Logback (v1.4.14) by QOS.ch\n" +
				"- Jackson (v2.16.0) by FasterXML, LLC\n" +
				"- Eclipse Collections (v1.11.0) by the Eclipse Foundation\n";
	}

	@NotNull
	public static String getWHOIS(@NotNull User user)
	{
		final StringBuilder builder = new StringBuilder(500);

		builder.append("```")
				.append("Nick: ").append(user.getNick()).append('\n')
				.append("Hostname: ").append(user.getHost()).append('\n')
				.append("Channels: ").append(user.getChannels()).append('\n')
				.append("Real name: ").append(user.getRealName().orElse(null)).append('\n')
				.append("User string: ").append(user.getUserString()).append('\n');

		if (user.getAccount().isPresent())
			builder.append("Is logged in as: ").append(user.getAccount().get()).append('\n');
		else
			builder.append("Is not logged in with an account\n");

		if (user.getOperatorInformation().isPresent())
			builder.append("Is operator with info: ").append(user.getOperatorInformation().get()).append('\n');

		if (user.isAway())
			builder.append("User is away with message: ").append(user.getAwayMessage().orElse(null)).append('\n');

		if (user.getServer().isPresent())
			builder.append("Connected to server: ").append(user.getServer().get()).append('\n');

		builder.append("```");

		return builder.toString();
	}

	// Cursed
	@NotNull
	public static String modesToString(@NotNull List<? extends ModeStatus<? extends Mode>> modes)
	{
		final StringBuilder combined = new StringBuilder(modes.size() + 2);
		final StringBuilder added = new StringBuilder(modes.size());
		final StringBuilder removed = new StringBuilder(modes.size());
		final StringJoiner parameters = new StringJoiner(", ");
		for (ModeStatus<? extends Mode> mode : modes)
		{
			(mode.getAction() == ModeStatus.Action.ADD ? added : removed).append(mode.getMode().getChar());
			mode.getParameter().ifPresent(parameters::add);
		}

		if (added.length() > 0)
			combined.append('+').append(added);
		if (removed.length() > 0)
			combined.append('-').append(removed);
		if (parameters.length() > 0)
			combined.append(' ').append(parameters);
		return combined.toString();
	}

	@NotNull
	public static List<String> splitStringList(String string)
	{
		final List<String> matchList = new ArrayList<>();
		final Matcher regexMatcher = SPLIT_PATTERN.matcher(string);
		while (regexMatcher.find())
		{
			if (regexMatcher.group(1) != null)
				matchList.add(regexMatcher.group(1));// Add double-quoted string without the quotes
			else if (regexMatcher.group(2) != null)
				matchList.add(regexMatcher.group(2));// Add single-quoted string without the quotes
			else
				matchList.add(regexMatcher.group());// Add unquoted word
		}
		return matchList;
	}

	@NotNull
	public static String[] splitStringArray(String string)
	{
		final List<String> matchList = splitStringList(string);
		return matchList.toArray(new String[0]);
	}

}
