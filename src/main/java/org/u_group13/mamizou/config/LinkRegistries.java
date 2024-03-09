package org.u_group13.mamizou.config;

import club.minnced.discord.webhook.send.WebhookMessageBuilder;
import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.eclipse.collections.api.factory.primitive.LongLongMaps;
import org.eclipse.collections.api.factory.primitive.LongObjectMaps;
import org.eclipse.collections.api.factory.primitive.ObjectLongMaps;
import org.eclipse.collections.api.map.primitive.MutableLongLongMap;
import org.eclipse.collections.api.map.primitive.MutableLongObjectMap;
import org.eclipse.collections.api.map.primitive.MutableObjectLongMap;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.u_group13.mamizou.Main;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.Serializable;
import java.nio.file.Files;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

@JsonAutoDetect(fieldVisibility = JsonAutoDetect.Visibility.ANY)
public class LinkRegistries implements Serializable
{
	private static final Logger LOGGER = LoggerFactory.getLogger(LinkRegistries.class);

	private static LinkRegistries instance;

	private final MutableLongLongMap discordToID;
	private final MutableObjectLongMap<String> ircToID;
	private final MutableLongObjectMap<LinkEntry> entryMap;
//	private final MutableLongObjectMap<LinkEntry> discordRegistries;
//	private final Map<String, LinkEntry> ircRegistries;

	private final MutableLongObjectMap<LinkEntry> discordRequests;
	private final Map<String, LinkEntry> ircRequests;

	public LinkRegistries()
	{
		discordToID = LongLongMaps.mutable.empty();
		ircToID = ObjectLongMaps.mutable.empty();
		entryMap = LongObjectMaps.mutable.empty();

//		discordRegistries = LongObjectMaps.mutable.empty();
//		ircRegistries = new HashMap<>();

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
		return discordToID.containsKey(discordID);
	}

	public boolean isRegistered(String ircAccount)
	{
		return ircToID.containsKey(ircAccount);
	}

	public synchronized void addEntry(LinkEntry entry)
	{
		final long id = Main.RANDOM.nextLong();
		LOGGER.debug("Adding registry {} with ID {}", entry, id);
		discordToID.put(entry.discordID, id);
		ircToID.put(entry.ircAccount, id);
		entryMap.put(id, entry);
	}

	public LinkEntry getEntry(long discordID)
	{
		return entryMap.get(discordToID.get(discordID));
	}

	public LinkEntry getEntry(String ircAccount)
	{
		return entryMap.get(ircToID.get(ircAccount));
	}

	public synchronized void removeEntry(long discordID)
	{
		if (!isRegistered(discordID))
			return;

		LOGGER.debug("Removing registry with Discord ID: {}", discordID);

		final LinkEntry entry = entryMap.get(discordToID.get(discordID));
		discordToID.remove(discordID);
		ircToID.remove(entry.ircAccount);
	}

	public synchronized void removeEntry(String ircAccount)
	{
		if (!isRegistered(ircAccount))
			return;

		LOGGER.debug("Removing registry with IRC account: {}", ircAccount);

		final LinkEntry entry = entryMap.get(ircToID.get(ircAccount));
		discordToID.remove(entry.discordID);
		ircToID.remove(ircAccount);
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
		return discordRequests.containsKey(discordID);
	}

	public boolean hasRequest(String ircAccount)
	{
		return ircRequests.containsKey(ircAccount);
	}

	public synchronized boolean acceptRequest(long discordID)
	{
		if (!discordRequests.containsKey(discordID))
			return false;

		addEntry(discordRequests.remove(discordID).tryCache());

		return true;
	}

	public synchronized boolean acceptRequest(String ircAccount)
	{
		if (!ircRequests.containsKey(ircAccount))
			return false;

		addEntry(ircRequests.remove(ircAccount).tryCache());

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
		return Objects.equals(discordToID, that.discordToID) && Objects.equals(ircToID,
		                                                                       that.ircToID) && Objects.equals(
				entryMap, that.entryMap) && Objects.equals(discordRequests,
		                                                   that.discordRequests) && Objects.equals(
				ircRequests, that.ircRequests);
	}

	@Override
	public int hashCode()
	{
		return Objects.hash(discordToID, ircToID, entryMap, discordRequests, ircRequests);
	}

	@Override
	public String toString()
	{
		return "LinkRegistries{" + "discordToID=" + discordToID +
				", ircToID=" + ircToID +
				", entryMap=" + entryMap +
				", discordRequests=" + discordRequests +
				", ircRequests=" + ircRequests +
				'}';
	}

	public static void saveRegistries(@NotNull ObjectMapper mapper, @NotNull OutputStream stream) throws IOException
	{
		mapper.writeValue(stream, getInstance());
	}

	// TODO
	public static void loadRegistries(@NotNull ObjectMapper mapper, @NotNull InputStream stream) throws IOException
	{
		instance = mapper.readValue(stream, LinkRegistries.class);
	}
}
