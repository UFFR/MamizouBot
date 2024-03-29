package org.u_group13.mamizou.util;

import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;

public enum IRCCode
{
	BOLD(0x02),
	ITALIC(0x1d),
	UNDERLINE(0x1f),
	STRIKETHROUGH(0x1e),
	MONOSPACE(0x11),
	COLOR(0x03),
	REVERSE_COLOR(0x16),
	RESET(0x0f);

	public final char code;
	IRCCode(int code)
	{
		this.code = (char) code;
	}

	@NotNull
	@Contract(pure = true)
	public static String getColoredNick(@NotNull String nick)
	{
		final int code = Integer.remainderUnsigned(nick.hashCode(), 14) + 2;
		return String.format("<%s%s%s%s>", COLOR.code, code, nick, COLOR.code);
	}

}
