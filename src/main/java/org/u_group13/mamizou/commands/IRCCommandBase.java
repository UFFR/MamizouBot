package org.u_group13.mamizou.commands;

import org.jetbrains.annotations.NotNull;
import org.kitteh.irc.client.library.element.Channel;
import org.kitteh.irc.client.library.element.User;
import org.kitteh.irc.client.library.event.channel.ChannelMessageEvent;
import org.u_group13.mamizou.util.StringUtil;
import picocli.CommandLine;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.function.Function;

public abstract class IRCCommandBase implements Callable<Integer>
{
	public static final Map<String, Function<CommandContext, IRCCommandBase>> COMMAND_MAP = new HashMap<>();

	public record CommandContext(User sender, Channel channel)
		{
		}

	@NotNull
	protected final CommandContext context;

	public IRCCommandBase(@NotNull CommandContext context)
	{
		this.context = context;
	}

	public static void tryExecute(@NotNull ChannelMessageEvent event)
	{
		final int result;
		final String rawCommand = event.getMessage();
		final String[] commandAndArgs = StringUtil.splitStringArray(rawCommand);

		final String command = commandAndArgs[0].toLowerCase();
		final User sender = event.getActor();
		final Channel channel = event.getChannel();
		final StringWriter outStringWriter = new StringWriter(500);

		if (COMMAND_MAP.containsKey(command))
		{
			final String[] args;

			args = commandAndArgs.length > 1 ? Arrays.copyOfRange(commandAndArgs, 1, commandAndArgs.length) : new String[0];

			final PrintWriter out = new PrintWriter(outStringWriter);

//		final int result = new CommandLine(new ExecutorHelper(new CommandContext(sender, channel)))
//				.setOut(out).setErr(out)
//				.execute(commandAndArgs);

			result = new CommandLine(COMMAND_MAP.get(command).apply(new CommandContext(sender, channel)))
					.setOut(out).setErr(out)
					.execute(args);
		} else
			result = -1;

		if (result == -1)
		{
			channel.sendMessage("Unknown command");
			return;
		}

		final String outputRaw = outStringWriter.toString();

		if (result != 0)
			sender.sendMessage(String.format("Command exited with code %s", result));

		if (outputRaw.isBlank())
			return;

		final String[] output = outputRaw.replace("\0", "").split("[\n\r]");

		sender.sendMessage("Begin command dump:");

		for (String s : output)
			sender.sendMessage(s);

		sender.sendMessage("End command dump");
	}

	@Deprecated
	@CommandLine.Command(name = "executor", description = "Helper class for commands.")
	public static class ExecutorHelper implements Callable<Integer>
	{

		@CommandLine.Parameters(index = "0")
		private String command;
		@CommandLine.Parameters(index = "1..*", paramLabel = "ARG")
		private String[] arguments = new String[0];
		private final CommandContext context;

		public ExecutorHelper(CommandContext context)
		{
			this.context = context;
		}

		@NotNull
		@Override
		public Integer call() throws Exception
		{
			return COMMAND_MAP.containsKey(command) ? new CommandLine(COMMAND_MAP.get(command).apply(context)).execute(arguments) : -1;
		}
	}

}
