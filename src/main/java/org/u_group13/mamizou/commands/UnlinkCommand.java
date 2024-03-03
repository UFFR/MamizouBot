package org.u_group13.mamizou.commands;

import org.jetbrains.annotations.NotNull;
import org.u_group13.mamizou.Main;
import org.u_group13.mamizou.config.LinkRegistries;
import picocli.CommandLine;

import java.util.Optional;

@CommandLine.Command(name = "unlink", description = "Unlink IRC from Discord account", version = "1.0.0")
public class UnlinkCommand extends IRCCommandBase
{
	public UnlinkCommand(@NotNull CommandContext context)
	{
		super(context);
	}

	@Override
	public Integer call() throws Exception
	{
		final Optional<String> account = context.sender().getAccount();

		if (account.isEmpty())
		{
			context.sender().sendMessage("You must be logged in to unlink!");
			return 10;
		}

		final String ircAccount = account.get();

		final LinkRegistries linkRegistries = LinkRegistries.getInstance();
		if (!linkRegistries.isRegistered(ircAccount))
		{
			context.sender().sendMessage("You are not linked to begin with!");
			return 11;
		}

		linkRegistries.removeEntry(ircAccount);

		return 0;
	}
}
