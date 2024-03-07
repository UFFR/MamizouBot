package org.u_group13.mamizou.util.converter;

import org.jetbrains.annotations.NotNull;
import org.u_group13.mamizou.util.IRCCode;

public class DiscordToIRC
{
	private static final String ITALIC_REGEX = "(?<![\\\\*])[*_](\\S(\\w|\\d|\\s)+\\S)[*_]";
//	private static final Pattern ITALIC_PATTERN = Pattern.compile(ITALIC_REGEX);
	private static final String ITALIC_REPLACEMENT = IRCCode.ITALIC.code + "$1" + IRCCode.ITALIC.code;
	private static final String BOLD_REGEX = "(?<![\\\\*])[*]{2}(\\S(\\w|\\d|\\s)+\\S)[*]{2}";
//	private static final Pattern BOLD_PATTERN = Pattern.compile(BOLD_REGEX);
	private static final String BOLD_REPLACEMENT = IRCCode.BOLD.code + "$1" + IRCCode.BOLD.code;
	private static final String UNDERLINE_REGEX = "(?<![\\\\_])_{2}(\\S(\\w|\\d|\\s)+\\S)_{2}";
//	private static final Pattern UNDERLINE_PATTERN = Pattern.compile(UNDERLINE_REGEX);
	private static final String UNDERLINE_REPLACEMENT = IRCCode.UNDERLINE.code + "$1" + IRCCode.UNDERLINE.code;
	private static final String STRIKETHROUGH_REGEX = "(?<![\\\\~])~{2}(\\S(\\w|\\d|\\s)+\\S)~{2}";
//	private static final Pattern STRIKETHROUGH_PATTERN = Pattern.compile(STRIKETHROUGH_REGEX);
	private static final String STRIKETHROUGH_REPLACEMENT = IRCCode.STRIKETHROUGH.code + "$1" + IRCCode.STRIKETHROUGH.code;
	private static final String SPOILER_REGEX = "(?<![\\\\|])\\|{2}((\\w|\\d|\\s)+)\\|{2}";
//	private static final Pattern SPOILER_PATTERN = Pattern.compile(SPOILER_REGEX);
	private static final String SPOILER_REPLACEMENT = IRCCode.COLOR.code + "01,01$1" + IRCCode.COLOR.code + "01,01";
	private static final String MONOSPACE_REGEX = "(?<![\\\\`])`((\\w|\\d|\\s)+)`";
//	private static final Pattern MONOSPACE_PATTERN = Pattern.compile(MONOSPACE_REGEX);
	private static final String MONOSPACE_REPLACEMENT = IRCCode.MONOSPACE.code + "$1" + IRCCode.MONOSPACE.code;

	@NotNull
	public static String convert(@NotNull String source)
	{
		return source.replaceAll(BOLD_REGEX, BOLD_REPLACEMENT).replaceAll(UNDERLINE_REGEX, UNDERLINE_REPLACEMENT).replaceAll(ITALIC_REGEX, ITALIC_REPLACEMENT).replaceAll(STRIKETHROUGH_REGEX, STRIKETHROUGH_REPLACEMENT).replaceAll(SPOILER_REGEX, SPOILER_REPLACEMENT).replaceAll(MONOSPACE_REGEX, MONOSPACE_REPLACEMENT);
	}
}
