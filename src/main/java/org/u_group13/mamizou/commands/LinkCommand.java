package org.u_group13.mamizou.commands;

import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.interactions.components.buttons.Button;
import org.jetbrains.annotations.NotNull;
import org.u_group13.mamizou.Main;
import org.u_group13.mamizou.config.LinkEntry;
import org.u_group13.mamizou.config.LinkRegistries;
import picocli.CommandLine;

import java.util.Optional;

@CommandLine.Command(name = "link", description = "Link to a Discord account", version = "1.0.0")
public class LinkCommand extends IRCCommandBase
{
	@CommandLine.Parameters(index = "0", description = "Discord user ID to link with")
	private long discordID;

	public LinkCommand(@NotNull CommandContext context)
	{
		super(context);
	}

	@Override
	public Integer call() throws Exception
	{
		final Optional<String> account = context.sender().getAccount();
		if (account.isEmpty())
		{
			context.sender().sendMessage("You need an account with services to link!");
			return 10;
		}

		final User user = Main.getJda().retrieveUserById(discordID).onErrorMap(throwable -> null).complete();

		final LinkRegistries linkRegistries = LinkRegistries.getInstance();
		if (linkRegistries.isRegistered(discordID))
		{
			context.sender().sendMessage("Discord user is already linked!");
			return 11;
		}

		if (user == null)
		{
			context.sender().sendMessage("User was not found!");
			return 12;
		}

		if (user.getMutualGuilds().isEmpty())
		{
			context.sender().sendMessage("User has no mutual guilds with relay!");
			return 13;
		}

		if (!user.hasPrivateChannel())
		{
			context.sender().sendMessage("User doesn't have open PMs!");
			return 14;
		}

		final String accountName = account.get();
		user.openPrivateChannel().complete()
		    .sendMessage("IRC member with user account \"%s\" requested to link with you.".formatted(accountName))
		    .setActionRow(Button.primary("A:" + accountName, "Accept"), Button.danger("R:" + accountName, "Reject"))
		    .queue();

		linkRegistries.addRequest(new LinkEntry(accountName, discordID), false);

		return 0;
	}
}
