package org.u_group13.mamizou.commands;

import org.jetbrains.annotations.NotNull;
import org.u_group13.mamizou.Main;
import org.u_group13.mamizou.config.LinkRegistries;
import picocli.CommandLine;

@CommandLine.Command(name = "accept", description = "Accept a link request from Discord", version = "1.0.0")
public class AcceptLinkCommand extends IRCCommandBase
{
	@CommandLine.Parameters(index = "0", description = "Discord user ID of the requester to link with")
	private long discordID;
	public AcceptLinkCommand(@NotNull CommandContext context)
	{
		super(context);
	}

	@Override
	public Integer call() throws Exception
	{
		final boolean result = LinkRegistries.getInstance().acceptRequest(discordID);
		context.sender().sendMessage(result ? "Linked successfully!" : "Unable to locate link request with that user ID");
		return result ? 0 : 10;
	}
}
