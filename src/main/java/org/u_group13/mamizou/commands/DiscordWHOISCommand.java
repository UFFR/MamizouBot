package org.u_group13.mamizou.commands;

import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.channel.concrete.TextChannel;
import org.jetbrains.annotations.NotNull;
import org.u_group13.mamizou.Main;
import picocli.CommandLine;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

// FIXME
@CommandLine.Command(name = "whois", description = "Get a WHOIS equivalent on a member in the mapped Discord.",
		mixinStandardHelpOptions = true, version = "1.0.0")
public class DiscordWHOISCommand extends IRCCommandBase
{
	@CommandLine.Parameters(index = "0")
	private String member;

	public DiscordWHOISCommand(@NotNull CommandContext context)
	{
		super(context);
	}

	@Override
	public Integer call() throws Exception
	{
		if (Main.helper.ircToDiscordMapping.containsKey(context.channel().getName()))
		{
			context.channel().sendMessage("Looking for user \"" + member + "\"...");
			final long chanID = Main.helper.ircToDiscordMapping.get(context.channel().getName());
			final TextChannel textChannel = Main.getJda().getTextChannelById(chanID);
			if (textChannel != null)
			{
				final Optional<Member> optionalMember = textChannel.getMembers()
				                                          .stream()
				                                          .filter(m -> m.getEffectiveName().equalsIgnoreCase(member))
				                                          .findFirst();
				if (optionalMember.isPresent())
					getWHOIS(optionalMember.get()).forEach(context.sender()::sendMessage);
				else
					context.channel().sendMessage("Couldn't find user.");

				return 0;
			} else
				context.channel().sendMessage("Channel not mapped!");
		}

		return 1;
	}

	@NotNull
	private static List<String> getWHOIS(@NotNull Member member)
	{
		final List<String> list = new ArrayList<>();

		list.add("Nickname = " + member.getNickname());
		list.add("Username = " + member.getUser().getName());
		list.add("Activities = " + member.getActivities());
		list.add("User ID = " + member.getId());
		list.add("Guild = " + member.getGuild().getName());
		list.add("Guild ID = " + member.getGuild().getId());

		return list;
	}
}
