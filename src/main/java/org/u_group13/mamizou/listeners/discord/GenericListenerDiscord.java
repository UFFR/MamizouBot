package org.u_group13.mamizou.listeners.discord;

import net.dv8tion.jda.api.events.ExceptionEvent;
import net.dv8tion.jda.api.events.GatewayPingEvent;
import net.dv8tion.jda.api.events.StatusChangeEvent;
import net.dv8tion.jda.api.events.guild.GuildReadyEvent;
import net.dv8tion.jda.api.events.session.*;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class GenericListenerDiscord extends ListenerAdapter
{
	private static final Logger LOGGER = LoggerFactory.getLogger(GenericListenerDiscord.class);

	@Override
	public void onGatewayPing(@NotNull GatewayPingEvent event)
	{
		LOGGER.trace("Received heartbeat ping, previously was {} ms, is now {} ms", event.getOldPing(), event.getNewPing());
	}

	@Override
	public void onReady(@NotNull ReadyEvent event)
	{
		LOGGER.info("Ready event received. Have {} available and {} unavailable guilds, for a total of {}",
		            event.getGuildAvailableCount(), event.getGuildUnavailableCount(), event.getGuildTotalCount());
	}

	@Override
	public void onException(@NotNull ExceptionEvent event)
	{
		LOGGER.error("Unhandled exception caught!", event.getCause());
	}

	@Override
	public void onSessionInvalidate(@NotNull SessionInvalidateEvent event)
	{
		LOGGER.warn("Session caches have been invalidated");
	}

	@Override
	public void onSessionDisconnect(@NotNull SessionDisconnectEvent event)
	{
		LOGGER.warn("Received disconnect event at time {} with code {}", event.getTimeDisconnected(), event.getCloseCode());
		LOGGER.warn(event.isClosedByServer() ? "Was closed by Discord server" : "Was not closed by remote server");
	}

	@Override
	public void onSessionResume(@NotNull SessionResumeEvent event)
	{
		LOGGER.info("Session resumed, caches still valid");
	}

	@Override
	public void onShutdown(@NotNull ShutdownEvent event)
	{
		LOGGER.info("Shutdown sequence initiated on {} with code {}", event.getTimeShutdown(), event.getCloseCode());
	}

	@Override
	public void onSessionRecreate(@NotNull SessionRecreateEvent event)
	{
		LOGGER.info("Session recreated, caches likely out of date");
	}

	@Override
	public void onStatusChange(@NotNull StatusChangeEvent event)
	{
		LOGGER.info("Status changing from {} to {}", event.getOldStatus(), event.getNewStatus());
	}

	@Override
	public void onGuildReady(@NotNull GuildReadyEvent event)
	{
		LOGGER.debug("Guild {} ({}) has reported ready", event.getGuild().getName(), event.getGuild().getId());
	}
}
