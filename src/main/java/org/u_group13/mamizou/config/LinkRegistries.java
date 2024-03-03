package org.u_group13.mamizou.config;

import club.minnced.discord.webhook.send.WebhookMessageBuilder;
import org.eclipse.collections.api.factory.primitive.LongObjectMaps;
import org.eclipse.collections.api.map.primitive.MutableLongObjectMap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

public class LinkRegistries implements Serializable
{
	private static final Logger LOGGER = LoggerFactory.getLogger(LinkRegistries.class);

	private static LinkRegistries instance;

	private final MutableLongObjectMap<LinkEntry> discordRegistries;
	private final Map<String, LinkEntry> ircRegistries;

	private transient final MutableLongObjectMap<LinkEntry> discordRequests;
	private transient final Map<String, LinkEntry> ircRequests;

	public LinkRegistries()
	{
		discordRegistries = LongObjectMaps.mutable.empty();
		ircRegistries = new HashMap<>();

		discordRequests = LongObjectMaps.mutable.empty();
		ircRequests = new HashMap<>();
	}

	public static LinkRegistries getInstance()
	{
		return instance == null ? instance = new LinkRegistries() : instance;
	}

	public void setupWebhook(long discordID, long guildID, WebhookMessageBuilder messageBuilder)
	{
		if (!isRegistered(discordID))
			return;

		LOGGER.debug("Trying to setup webhook for user {} in guild {}", discordID, guildID);
		getEntry(discordID).setupWebhook(guildID, messageBuilder);
	}

	public boolean isRegistered(long discordID)
	{
		return discordRegistries.containsKey(discordID);
	}

	public boolean isRegistered(String ircAccount)
	{
		return ircRegistries.containsKey(ircAccount);
	}

	public synchronized void addEntry(LinkEntry entry)
	{
		LOGGER.debug("Adding registry {}", entry);
		discordRegistries.put(entry.discordID, entry);
		ircRegistries.put(entry.ircAccount, entry);
	}

	public LinkEntry getEntry(long discordID)
	{
		return discordRegistries.get(discordID);
	}

	public LinkEntry getEntry(String ircAccount)
	{
		return ircRegistries.get(ircAccount);
	}

	public synchronized void removeEntry(long discordID)
	{
		if (!isRegistered(discordID))
			return;

		LOGGER.debug("Removing registry with Discord ID: {}", discordID);

		final LinkEntry entry = discordRegistries.get(discordID);
		discordRegistries.remove(discordID);
		ircRegistries.remove(entry.ircAccount);
	}

	public synchronized void removeEntry(String ircAccount)
	{
		if (!isRegistered(ircAccount))
			return;

		LOGGER.debug("Removing registry with IRC account: {}", ircAccount);

		final LinkEntry entry = ircRegistries.get(ircAccount);
		discordRegistries.remove(entry.discordID);
		ircRegistries.remove(ircAccount);
	}

	public synchronized void addRequest(LinkEntry entry, boolean fromDiscord)
	{
		LOGGER.debug("Adding link request {} from {}", entry, fromDiscord ? "Discord" : "IRC");
		if (fromDiscord)
			discordRequests.put(entry.discordID, entry);
		else
			ircRequests.put(entry.ircAccount, entry);
	}

	public boolean hasRequest(long discordID)
	{
		return discordRegistries.containsKey(discordID);
	}

	public boolean hasRequest(String ircAccount)
	{
		return ircRegistries.containsKey(ircAccount);
	}

	public synchronized boolean acceptRequest(long discordID)
	{
		if (!discordRequests.containsKey(discordID))
			return false;

		final LinkEntry entry = discordRequests.remove(discordID).tryCache();

		discordRegistries.put(discordID, entry);
		ircRegistries.put(entry.ircAccount, entry);

		return true;
	}

	public synchronized boolean acceptRequest(String ircAccount)
	{
		if (!ircRequests.containsKey(ircAccount))
			return false;

		final LinkEntry entry = ircRequests.remove(ircAccount).tryCache();

		discordRegistries.put(entry.discordID, entry);
		ircRegistries.put(entry.ircAccount, entry);

		return true;
	}

	public synchronized void rejectRequest(long discordID)
	{
		discordRequests.remove(discordID);
	}

	public synchronized void rejectRequest(String ircAccount)
	{
		ircRequests.remove(ircAccount);
	}

	public synchronized void rejectAll(long discordID)
	{
		ircRequests.values().removeIf(entry -> entry.discordID == discordID);
	}

	public synchronized void rejectAll(String ircAccount)
	{
		discordRequests.values().removeIf(entry -> Objects.equals(entry.ircAccount, ircAccount));
	}

	@Override
	public boolean equals(Object o)
	{
		if (this == o) return true;
		if (o == null || getClass() != o.getClass()) return false;
		final LinkRegistries that = (LinkRegistries) o;
		return Objects.equals(discordRegistries, that.discordRegistries) && Objects.equals(
				ircRegistries, that.ircRegistries) && Objects.equals(discordRequests,
		                                                             that.discordRequests) && Objects.equals(
				ircRequests, that.ircRequests);
	}

	@Override
	public int hashCode()
	{
		return Objects.hash(discordRegistries, ircRegistries, discordRequests, ircRequests);
	}

	@Override
	public String toString()
	{
		return "LinkRegistries{" + "discordRegistries=" + discordRegistries +
				", ircRegistries=" + ircRegistries +
				", discordRequests=" + discordRequests +
				", ircRequests=" + ircRequests +
				'}';
	}
}
