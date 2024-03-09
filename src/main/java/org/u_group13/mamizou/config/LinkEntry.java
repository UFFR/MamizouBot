package org.u_group13.mamizou.config;

import club.minnced.discord.webhook.send.WebhookMessageBuilder;
import com.fasterxml.jackson.annotation.JsonAutoDetect;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.User;
import org.eclipse.collections.api.factory.primitive.LongObjectMaps;
import org.eclipse.collections.api.map.primitive.MutableLongObjectMap;
import org.eclipse.collections.api.tuple.Pair;
import org.eclipse.collections.impl.tuple.Tuples;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.u_group13.mamizou.Main;

import java.io.Serializable;
import java.util.Objects;

@JsonAutoDetect(fieldVisibility = JsonAutoDetect.Visibility.ANY)
public class LinkEntry implements Serializable
{
	private static final Logger LOGGER = LoggerFactory.getLogger(LinkEntry.class);
	public final String ircAccount;
	public final long discordID;
	private final MutableLongObjectMap<Pair<String, String>> cacheMappings;
	public Pair<String, String> defaultCache;

	public LinkEntry(String ircAccount, long discordID) throws IllegalStateException
	{
		this.ircAccount = ircAccount;
		this.discordID = discordID;
		this.cacheMappings = LongObjectMaps.mutable.empty();

		final User user = Main.getJda().retrieveUserById(discordID).onErrorMap(throwable -> null).complete();

		if (user == null)
		{
			LOGGER.error("Tried to create link instance, but JDA couldn't find user with ID {}!", discordID);
			throw new IllegalStateException("Couldn't find user by ID!");
		}

		defaultCache = Tuples.pair(user.getEffectiveName(), user.getEffectiveAvatarUrl());
	}

	public synchronized void offerCache(@NotNull Member member)
	{
		LOGGER.trace("Was offered cache for {}", member);
		final Pair<String, String> cache = Tuples.pair(member.getEffectiveName(), member.getEffectiveAvatarUrl());
		cacheMappings.put(member.getGuild().getIdLong(), cache);
	}

	public boolean hasCache(@NotNull Guild guild)
	{
		return hasCache(guild.getIdLong());
	}

	public boolean hasCache(long guildID)
	{
		return cacheMappings.containsKey(guildID);
	}

	public Pair<String, String> getCache(@NotNull Guild guild)
	{
		return getCache(guild.getIdLong());
	}

	public Pair<String, String> getCache(long guildID)
	{
		return cacheMappings.get(guildID);
	}

	public synchronized Pair<String, String> removeCache(@NotNull Guild guild)
	{
		return removeCache(guild.getIdLong());
	}

	public synchronized Pair<String, String> removeCache(long guildID)
	{
		return cacheMappings.remove(guildID);
	}

	public void setupWebhook(long guildID, @NotNull WebhookMessageBuilder messageBuilder)
	{
		final Pair<String,String> cache = hasCache(guildID) ? getCache(guildID) : defaultCache;

		LOGGER.trace("Setting up webhook with info: {}", cache);
		messageBuilder.setUsername(cache.getOne()).setAvatarUrl(cache.getTwo());
	}

	public LinkEntry tryCache()
	{
		Main.getJda().retrieveUserById(discordID).queue(this::cacheMembers);
		return this;
	}

	private void cacheMembers(@NotNull User user)
	{
		user.getMutualGuilds().forEach(guild -> guild.retrieveMemberById(discordID).queue(this::offerCache));
	}

	@Override
	public boolean equals(Object o)
	{
		if (this == o) return true;
		if (o == null || getClass() != o.getClass()) return false;
		final LinkEntry entry = (LinkEntry) o;
		return discordID == entry.discordID && Objects.equals(ircAccount,
		                                                      entry.ircAccount) && Objects.equals(
				cacheMappings, entry.cacheMappings) && Objects.equals(defaultCache, entry.defaultCache);
	}

	@Override
	public int hashCode()
	{
		return Objects.hash(ircAccount, discordID, cacheMappings, defaultCache);
	}

	@Override
	public String toString()
	{
		return "LinkEntry{" + "ircAccount='" + ircAccount + '\'' +
				", discordID=" + discordID +
				", cacheMappings=" + cacheMappings +
				", defaultCache=" + defaultCache +
				'}';
	}
}
