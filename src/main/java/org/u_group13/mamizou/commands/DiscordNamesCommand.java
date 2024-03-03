package org.u_group13.mamizou.commands;

import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.entities.channel.concrete.TextChannel;
import org.jetbrains.annotations.NotNull;
import org.u_group13.mamizou.Main;
import picocli.CommandLine;

import java.util.List;
import java.util.stream.Collectors;

@CommandLine.Command(name = "names", description = "Get the available users from the Discord",
		mixinStandardHelpOptions = true, version = "1.0.0")
public class DiscordNamesCommand extends IRCCommandBase
{

	@CommandLine.Option(names = "-e,--expression", description = "RegEx to filter names by")
	private String regex = null;
	@CommandLine.Option(names = "-n,--nicks", description = "If the list should be nicknames and not usernames")
	private boolean nicks = false;

	public DiscordNamesCommand(@NotNull CommandContext context)
	{
		super(context);
	}

	@Override
	public Integer call() throws Exception
	{
		final String ircChannel = context.channel().getMessagingName();
		if (Main.helper.ircToDiscordMapping.containsKey(ircChannel))
		{
			final long chanID = Main.helper.ircToDiscordMapping.get(ircChannel);
			final TextChannel textChannel = Main.getJda().getTextChannelById(chanID);

			if (textChannel != null)
			{
				final List<String> names = nicks ? textChannel.getMembers().stream().map(Member::getEffectiveName).collect(
						Collectors.toList()) : textChannel.getMembers().stream().map(Member::getUser).map(User::getName).collect(
						Collectors.toList());

				if (regex != null)
					names.removeIf(s -> !s.matches(regex));

				context.sender().sendMessage("Begin NAMES:");

				final StringBuilder builder = new StringBuilder(names.size() * 5);

				for (String name : names)
				{
					if (builder.length() + name.length() >= 4096)
					{
						context.sender().sendMessage(builder.toString());
						builder.delete(0, builder.length());
					}

					builder.append(name).append(", ");
				}

				context.sender().sendMessage("End of NAMES");
			} else
				context.channel().sendMessage("Channel mapped, but JDA couldn't find!");
		} else
			context.channel().sendMessage("Channel not mapped!");

		return 0;
	}
}
