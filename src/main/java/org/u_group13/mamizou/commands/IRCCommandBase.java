package org.u_group13.mamizou.commands;

import org.jetbrains.annotations.NotNull;
import org.kitteh.irc.client.library.element.Channel;
import org.kitteh.irc.client.library.element.User;
import org.kitteh.irc.client.library.event.channel.ChannelMessageEvent;
import org.kitteh.irc.client.library.event.user.PrivateMessageEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
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
	private static final Logger LOGGER = LoggerFactory.getLogger(IRCCommandBase.class);
	public static final Map<String, Function<CommandContext, IRCCommandBase>> COMMAND_MAP = new HashMap<>();

	public abstract static class MessageSource
	{
		public abstract void sendMessage(String message);
		public abstract String getName();
	}

	public static class ChannelMessageSource extends MessageSource
	{
		private final Channel channel;

		public ChannelMessageSource(Channel channel) {this.channel = channel;}

		@Override
		public void sendMessage(String message)
		{
			channel.sendMessage(message);
		}

		@Override
		public String getName()
		{
			return channel.getMessagingName();
		}
	}

	public static class PrivateMessageSource extends MessageSource
	{
		private final User user;

		public PrivateMessageSource(User user) {this.user = user;}

		@Override
		public void sendMessage(String message)
		{
			user.sendMessage(message);
		}

		@Override
		public String getName()
		{
			return user.getMessagingName();
		}
	}

	public record CommandContext(User sender, MessageSource channel)
		{
		}

	@NotNull
	protected final CommandContext context;

	public IRCCommandBase(@NotNull CommandContext context)
	{
		this.context = context;
	}

	public static void tryExecute(@NotNull PrivateMessageEvent event)
	{
		tryExecute(event.getMessage(), event.getActor(), new PrivateMessageSource(event.getActor()));
	}

	public static void tryExecute(@NotNull ChannelMessageEvent event)
	{
		tryExecute(event.getMessage(), event.getActor(), new ChannelMessageSource(event.getChannel()));
	}

	public static void tryExecute(@NotNull String rawCommand, @NotNull User sender, @NotNull MessageSource source)
	{
		LOGGER.debug("Got possible command from {} in {}", sender, source.getName());
		final int result;
		final String[] commandAndArgs = StringUtil.splitStringArray(rawCommand);

		final String command = commandAndArgs[0].toLowerCase();
		final StringWriter outStringWriter = new StringWriter(500);

		if (COMMAND_MAP.containsKey(command))
		{
			LOGGER.trace("Got command {}", command);
			final String[] args;

			args = commandAndArgs.length > 1 ? Arrays.copyOfRange(commandAndArgs, 1, commandAndArgs.length) : new String[0];

			final PrintWriter out = new PrintWriter(outStringWriter);

			result = new CommandLine(COMMAND_MAP.get(command).apply(new CommandContext(sender, source)))
					.setOut(out).setErr(out)
					.execute(args);
		} else
			result = -1;

		if (result == -1)
		{
			LOGGER.trace("Was not a command");
			source.sendMessage("Unknown command");
			return;
		}

		final String output = outStringWriter.toString();

		if (result != 0)
			sender.sendMessage(String.format("Command exited with code %s", result));

		if (output.isBlank())
			return;

		sender.sendMessage("Begin command dump:");

		if (output.indexOf('\n') >= 0)
			for (String split : output.split("\n"))
				sender.sendMultiLineMessage(split);
		else
			sender.sendMultiLineMessage(output);
		sender.sendMessage("End command dump");
	}
}
