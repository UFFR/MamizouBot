package org.u_group13.mamizou.commands;

import org.jetbrains.annotations.NotNull;
import org.u_group13.mamizou.util.StringUtil;
import picocli.CommandLine;

@CommandLine.Command(name = "version", description = "Gets version information", version = "1.0.0",
		mixinStandardHelpOptions = true)
public class IRCVersionCommand extends IRCCommandBase
{
	public IRCVersionCommand(@NotNull CommandContext context)
	{
		super(context);
	}

	@Override
	public Integer call() throws Exception
	{
		context.sender.sendMessage("MamizouBot VERSION:");
		context.sender.sendMessage(StringUtil.VERSION);
		context.sender.sendMessage("CREDITS:");
		final String[] credits = StringUtil.getCreditsString().split("\n");

		for (String c : credits)
			context.sender.sendMessage(c);

		context.sender.sendMessage("End VERSION");

		return 0;
	}
}
