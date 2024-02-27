package org.u_group13.mamizou.commands;

import org.kitteh.irc.client.library.Client;
import org.kitteh.irc.client.library.command.Command;
import org.kitteh.irc.client.library.util.Sanity;
import org.kitteh.irc.client.library.util.ToStringer;

public class SslInfoCommand extends Command<SslInfoCommand>
{
	private String server, target;

	/**
	 * Constructs the command.
	 *
	 * @param client the client
	 *
	 * @throws IllegalArgumentException if client is null
	 */
	protected SslInfoCommand(Client client)
	{
		super(client);
	}

	public SslInfoCommand server(String server)
	{
		this.server = (server == null) ? null : Sanity.safeMessageCheck(server, "server");
		return this;
	}

	public SslInfoCommand target(String target)
	{
		this.target = Sanity.safeMessageCheck(target, "target");
		return this;
	}

	@Override
	public void execute()
	{
		if (this.target == null) throw new IllegalStateException("Target not defined");
		StringBuilder builder = new StringBuilder(5 + this.target.length() + ((this.server == null) ? 1 : (2 + this.server.length())));
		builder.append("SSLINFO ");
		if (this.server != null) builder.append(this.server).append(' ');
		builder.append(this.target);
		this.sendCommandLine(builder.toString());
	}

	@Override
	public String toString()
	{
        return new ToStringer(this).add("client", this.getClient()).add("server", this.server).add("target", this.target).toString();
    }
}
