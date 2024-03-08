package org.u_group13.mamizou.commands;

import org.jetbrains.annotations.NotNull;
import org.u_group13.mamizou.config.LinkRegistries;
import picocli.CommandLine;

@CommandLine.Command(name = "reject", description = "Reject a single or all link requests", version = "1.0.0")
public class RejectLinkCommand extends IRCCommandBase
{
	@CommandLine.Parameters(index = "0", description = "Discord ID of the requester to reject", arity = "0..*")
	private long[] discordIDs;
	@CommandLine.Option(names = "-a,--all", description = "Reject all instead of a single one, ignores parameter")
	private boolean all = false;

	public RejectLinkCommand(@NotNull CommandContext context)
	{
		super(context);
	}

	@Override
	public Integer call() throws Exception
	{
		final LinkRegistries linkRegistries = LinkRegistries.getInstance();
		if (all)
		{
			linkRegistries.rejectAll(context.sender().getUserString());
			context.sender().sendMessage("All link requests rejected!");
			return 0;
		}

		for (long id : discordIDs)
			linkRegistries.rejectRequest(id);

		context.sender().sendMessage("Rejected specified link requests!");

		return 0;
	}
}
