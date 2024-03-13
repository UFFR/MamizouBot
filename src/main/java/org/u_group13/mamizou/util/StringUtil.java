package org.u_group13.mamizou.util;

import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import org.kitteh.irc.client.library.element.Channel;
import org.kitteh.irc.client.library.element.User;
import org.kitteh.irc.client.library.element.mode.ChannelUserMode;
import org.kitteh.irc.client.library.element.mode.Mode;
import org.kitteh.irc.client.library.element.mode.ModeStatus;
import org.kitteh.irc.client.library.event.user.UserModeEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.u_group13.mamizou.Main;

import java.io.IOException;
import java.net.URL;
import java.util.*;
import java.util.function.Consumer;
import java.util.jar.Manifest;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class StringUtil
{
	private static final Logger LOGGER = LoggerFactory.getLogger(StringUtil.class);
	public static final String VERSION;
	static
	{
		final String fallback = "0.5.4-SNAPSHOT";
		String v;
		try
		{
			LOGGER.info("Trying to get version from manifest...");
			final URL resource = Main.class.getClassLoader().getResource("META-INF/MANIFEST.MF");
			assert resource != null;
			final Manifest manifest = new Manifest(resource.openStream());

			v = manifest.getMainAttributes().getValue("Build-Version");
			if (v == null)
				v = fallback;
		} catch (IOException e)
		{
			LOGGER.error("Death and hatred to mankind", e);
			v = fallback;
		}
		VERSION = v;
	}
	public static final String SPLIT_REGEX = "[^\\s\"']+|\"([^\"]*)\"|'([^']*)'";
	public static final String BUTTON_ID_REGEX = "(A|R)\\:\\w+";
	public static final Pattern SPLIT_PATTERN = Pattern.compile(SPLIT_REGEX);
	public static final String IRC_USER_MODE_UPDATED = "**%s** mode updated to **%s**";

	public static String getIrcUserModeUpdated(@NotNull UserModeEvent event)
	{
		return String.format(IRC_USER_MODE_UPDATED, event.getActor().getName(), modesToString(event.getStatusList().getAll()));
	}

	@NotNull
	@Contract(pure = true)
	public static String getCreditsString()
	{
		return """
				This project was made possible by:
				- JDA (v5.0.0-beta.20) by Austin Keener (DV8FromTheWorld), Michael Ritter, Florian Spieß (MinnDevelopment), et al.
				- Discord-Webhooks (v0.8.4) by Florian Spieß
				- Kitteh IRC Client Library (KICL) (v9.0.0) by Kitteh
				- Picocli (v4.7.5) by remkop
				- Logback (v1.4.14) by QOS.ch
				- Jackson (v2.16.0) by FasterXML, LLC
				- Eclipse Collections (v1.11.0) by the Eclipse Foundation
				""";
	}

	@NotNull
	public static String getWHOIS(@NotNull User user, @NotNull Channel channel)
	{
		LOGGER.trace("Building WHOIS string for user {} in channel {}", user, channel);
		final StringBuilder builder = new StringBuilder(500);

		builder.append("```")
				.append("Nick: ").append(user.getNick()).append('\n')
				.append("Hostmask: ").append(user.getHost()).append('\n')
				.append("Channels: ").append(user.getChannels()).append('\n')
				.append("Real name: ").append(user.getRealName().orElse(null)).append('\n')
				.append("User string: ").append(user.getUserString()).append('\n');

		final Optional<SortedSet<ChannelUserMode>> userModes = channel.getUserModes(user);
		if (userModes.isPresent() && !userModes.get().isEmpty())
		{
			final SortedSet<ChannelUserMode> modes = userModes.get();
			builder.append("User modes: [").append(userModesToString(modes)).append("]\n");
			builder.append("Nick prefix: ").append(modes.getFirst().getNickPrefix()).append('\n');
		} else
			builder.append("User has no modes\n");

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
	public static String modesToString(@NotNull Collection<? extends ModeStatus<? extends Mode>> modes)
	{
		final StringBuilder combined = new StringBuilder(modes.size() * 2);
		final StringBuilder added = new StringBuilder(modes.size());
		final StringBuilder removed = new StringBuilder(modes.size());
		final StringJoiner parameters = new StringJoiner(", ");
		for (ModeStatus<? extends Mode> mode : modes)
		{
			(mode.getAction() == ModeStatus.Action.ADD ? added : removed).append(mode.getMode().getChar());
			if (mode.getMode().getChar() == 'k')
				parameters.add("<key>");
			else
				mode.getParameter().ifPresent(parameters::add);
		}

		if (!added.isEmpty())
			combined.append('+').append(added);
		if (!removed.isEmpty())
			combined.append('-').append(removed);
		if (parameters.length() > 0)
			combined.append(' ').append(parameters);
		return combined.toString();
	}

	@NotNull
	public static String userModesToString(@NotNull Collection<ChannelUserMode> modes)
	{
		final StringBuilder added = new StringBuilder(modes.size() + 1);

		added.append('+');
		for (ChannelUserMode mode : modes)
			added.append(mode.getChar());

		return added.toString();
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

	public static void consumeString(Consumer<String> consumer, @NotNull String original, int maxLen)
	{
		int pos = 0;
		while (original.length() - pos > maxLen)
		{
			consumer.accept(original.substring(pos, Math.min(maxLen + pos, original.length())));
			pos += maxLen;
		}
	}

}
