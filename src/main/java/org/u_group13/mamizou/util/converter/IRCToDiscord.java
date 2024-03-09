package org.u_group13.mamizou.util.converter;

import org.eclipse.collections.api.factory.primitive.CharObjectMaps;
import org.eclipse.collections.api.map.primitive.CharObjectMap;
import org.eclipse.collections.api.map.primitive.MutableCharObjectMap;
import org.jetbrains.annotations.NotNull;
import org.u_group13.mamizou.util.IRCCode;

import java.util.EnumSet;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class IRCToDiscord
{
	private static final CharObjectMap<IRCCode> CODE_MAP;
	private static final int[] IRC_TO_ANSI_COLOR_MAP = new int[16];
	private static final String IRC_COLOR_REGEX = "(\\d{1,2})(,(\\d{2}))?";
	private static final Pattern IRC_COLOR_PATTERN = Pattern.compile(IRC_COLOR_REGEX);
	private static final char ANSI_ESCAPE = '\u001b';
	static
	{
		final MutableCharObjectMap<IRCCode> codeMap = CharObjectMaps.mutable.empty();
		for (IRCCode code : IRCCode.values())
			codeMap.put(code.code, code);
		CODE_MAP = codeMap.asUnmodifiable();

		// TODO Configurable
		IRC_TO_ANSI_COLOR_MAP[ 0] = 37;
		IRC_TO_ANSI_COLOR_MAP[ 1] = 30;
		IRC_TO_ANSI_COLOR_MAP[ 2] = 34;
		IRC_TO_ANSI_COLOR_MAP[ 3] = 32;
		IRC_TO_ANSI_COLOR_MAP[ 4] = 31;
		IRC_TO_ANSI_COLOR_MAP[ 5] = 30;
		IRC_TO_ANSI_COLOR_MAP[ 6] = 35;
		IRC_TO_ANSI_COLOR_MAP[ 7] = 31;
		IRC_TO_ANSI_COLOR_MAP[ 8] = 33;
		IRC_TO_ANSI_COLOR_MAP[ 9] = 32;
		IRC_TO_ANSI_COLOR_MAP[10] = 36;
		IRC_TO_ANSI_COLOR_MAP[11] = 36;
		IRC_TO_ANSI_COLOR_MAP[12] = 34;
		IRC_TO_ANSI_COLOR_MAP[13] = 35;
		IRC_TO_ANSI_COLOR_MAP[14] = 37;
		IRC_TO_ANSI_COLOR_MAP[15] = 37;
	}

	@NotNull
	public static String convert(@NotNull String source)
	{
		final StringBuilder builder = new StringBuilder(source.length());

		final Set<IRCCode> unmatchedCodes = EnumSet.noneOf(IRCCode.class);
		if (source.indexOf('\u0003') >= 0)
		{
			// ANSI mode
			builder.append("```ansi\n");

			for (int i = 0; i < source.length(); i++)
			{
				final char c = source.charAt(i);

				if (CODE_MAP.containsKey(c))
				{
					final IRCCode code = CODE_MAP.get(c);
					if (unmatchedCodes.contains(code))
					{
						if (code == IRCCode.COLOR)
						{
							final int color = parseColor(source, i);
							addAnsi(color < 0 ? IRCCode.RESET : code, builder, source, i);
							if (color >= 0)
								i += skipColorCode(source, i);
						}
						unmatchedCodes.remove(code);
						for (IRCCode ircCode : unmatchedCodes)
							addAnsi(ircCode, builder, source, i);
					} else
					{
						unmatchedCodes.add(code);
						addAnsi(code, builder, source, i);
						if (code == IRCCode.COLOR)
							i += skipColorCode(source, i);
					}
				} else
					builder.append(c);
			}

			return builder.append("\n```").toString();
		} else
		{
			// Normal mode
			for (int i = 0; i < source.length(); i++)
			{
				final char c = source.charAt(i);

				if (CODE_MAP.containsKey(c))
				{
					final IRCCode code = CODE_MAP.get(c);

					addMarkdown(code, builder);

					if (unmatchedCodes.contains(code))
						unmatchedCodes.remove(code);
					else
						unmatchedCodes.add(code);
				} else
					builder.append(c);
			}

			for (IRCCode code : unmatchedCodes)
				addMarkdown(code, builder);

			return builder.toString();
		}
	}

	private static void addMarkdown(@NotNull IRCCode code, @NotNull StringBuilder builder)
	{
		switch (code)
		{
			case REVERSE_COLOR:
			case ITALIC: builder.append('*'); break;
			case BOLD: builder.append("**"); break;
			case UNDERLINE: builder.append("__"); break;
			case STRIKETHROUGH: builder.append("~~"); break;
			case MONOSPACE: builder.append('`'); break;
			default: break;
		}
	}

	private static void addAnsi(@NotNull IRCCode code, @NotNull StringBuilder builder, @NotNull String source, int index)
	{
		builder.append(ANSI_ESCAPE);
		if (code == IRCCode.COLOR)
		{
			final int color = parseColor(source, index);
			if (color < 0)
				builder.append("[39;49m");
			else
			{
				builder.append('[')
				       .append(IRC_TO_ANSI_COLOR_MAP[(color >> 4) & 0xf]);
				if ((color & 1 << 9) != 0)
					builder.append('m');
				else
				{
					builder.append(';')
					       .append(IRC_TO_ANSI_COLOR_MAP[color & 0xf] + 10)
					       .append('m');
				}
			}
		} else
		{
			switch (code)
			{
				case ITALIC -> builder.append("[3m");
				case BOLD -> builder.append("[1m");
				case UNDERLINE -> builder.append("[4m");
				case STRIKETHROUGH -> builder.append("[9m");
				case RESET -> builder.append("[0m");
			}
		}
	}

	private static int parseColor(String source, int offset)
	{
		int color = -1;

		final Matcher matcher = IRC_COLOR_PATTERN.matcher(source);
		if (matcher.find(offset) && matcher.start() == (offset + 1))
		{
			final String fore = matcher.group(1);
			final String back = matcher.group(3);

			color = ((Integer.parseInt(fore) % 16) & 15) << 4;

			if (back != null)
				color |= (Integer.parseInt(back) % 16) & 15;
			else
				color |= 1 << 9;
		}

		return color;
	}

	private static int skipColorCode(String source, int offset)
	{
		final Matcher matcher = IRC_COLOR_PATTERN.matcher(source);
		return matcher.find(offset) && matcher.start() == (offset + 1) ? matcher.group().length() : 0;
	}
}
