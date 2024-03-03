package org.u_group13.mamizou.util;

import club.minnced.discord.webhook.WebhookClient;
import club.minnced.discord.webhook.WebhookClientBuilder;
import club.minnced.discord.webhook.receive.ReadonlyMessage;
import club.minnced.discord.webhook.send.AllowedMentions;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.channel.concrete.TextChannel;
import org.eclipse.collections.api.factory.primitive.LongObjectMaps;
import org.eclipse.collections.api.factory.primitive.LongSets;
import org.eclipse.collections.api.factory.primitive.ObjectLongMaps;
import org.eclipse.collections.api.map.primitive.MutableLongObjectMap;
import org.eclipse.collections.api.map.primitive.MutableObjectLongMap;
import org.eclipse.collections.api.set.primitive.LongSet;
import org.eclipse.collections.api.set.primitive.MutableLongSet;
import org.eclipse.collections.api.tuple.Pair;
import org.eclipse.collections.impl.tuple.Tuples;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import org.kitteh.irc.client.library.Client;
import org.kitteh.irc.client.library.element.Channel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.u_group13.mamizou.Main;
import org.u_group13.mamizou.config.Config;
import org.u_group13.mamizou.config.LinkRegistries;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class Helper
{
	// TODO Remake
	private static final Logger LOGGER = LoggerFactory.getLogger(Helper.class);

	public final MutableLongObjectMap<String> discordToIRCMapping;
	public final MutableLongObjectMap<WebhookClient> webhookClientCache;
	public final MutableObjectLongMap<String> ircToDiscordMapping;
	public final LongSet discordChannels;
	public final Set<String> ircChannels;
	public final Map<String, String> passwordMap;
	public final Config.IgnoredUsers ignoredUsers;
	public final MutableObjectLongMap<String> ircToDiscordUserCache = ObjectLongMaps.mutable.empty();
	@Deprecated
	public final MutableLongObjectMap<Pair<String, String>> userCache = LongObjectMaps.mutable.empty();
	@Deprecated
	public final MutableLongObjectMap<MutableLongObjectMap<Pair<String, String>>> discordUserCache = LongObjectMaps.mutable.empty();

	public transient final MutableLongSet sentMessages = LongSets.mutable.empty().asSynchronized();

	private Helper(MutableLongObjectMap<String> discordToIRCMapping, MutableObjectLongMap<String> ircToDiscordMapping,
	               MutableLongObjectMap<WebhookClient> webhookClientCache, LongSet discordChannels, Set<String> ircChannels,
	               Map<String,String> passwordMap, Config.IgnoredUsers ignoredUsers)
	{
		this.discordToIRCMapping = discordToIRCMapping;
		this.ircToDiscordMapping = ircToDiscordMapping;
		this.webhookClientCache = webhookClientCache;
		this.discordChannels = discordChannels;
		this.ignoredUsers = ignoredUsers;
		this.ircChannels = ircChannels;
		this.passwordMap = passwordMap;
	}

	@NotNull
	@Contract("_ -> new")
	public static Helper createHelper(@NotNull Config config)
	{
		LOGGER.info("Creating new ConfigHelper instance");

		LOGGER.trace("Initializing collections...");
		final Map<String, String> passwordMap = new HashMap<>();
		final MutableLongObjectMap<String> discordToIRCMapping = LongObjectMaps.mutable.empty();
		final MutableObjectLongMap<String> ircToDiscordMapping = ObjectLongMaps.mutable.empty();
		final MutableLongObjectMap<WebhookClient> webhookClientCache = LongObjectMaps.mutable.empty();

		LOGGER.trace("Filling collections...");
		config.channelMapping.forEachKeyValue((l, s) ->
		                                      {
			                                      final int pos = s.indexOf(' ');
			                                      if (pos > 1)
			                                      {
				                                      final String channel = s.substring(0, pos), password = s.substring(pos);
				                                      discordToIRCMapping.put(l, channel);
				                                      ircToDiscordMapping.put(channel, l);
				                                      passwordMap.put(channel, password);
			                                      } else
			                                      {
				                                      discordToIRCMapping.put(l, s);
				                                      ircToDiscordMapping.put(s, l);
			                                      }
		                                      });
		LOGGER.trace("Compiling webhook clients...");
		WebhookClient.setDefaultErrorHandler((client, message, throwable) -> LOGGER.error("Uncaught exception for webhooks!", throwable));
		config.webhookMapping.forEachKeyValue((l, s) ->
		                                      {
			                                      final WebhookClientBuilder builder = new WebhookClientBuilder(s);
												  builder.setHttpClient(Main.getJda().getHttpClient()).setAllowedMentions(
														  AllowedMentions.none()).setWait(true);

												  webhookClientCache.put(l, builder.buildJDA());
		                                      });

		final LongSet discordChannels = discordToIRCMapping.keySet().asUnmodifiable();
		final Set<String> ircChannels = discordToIRCMapping.toSet().asUnmodifiable();

		return new Helper(discordToIRCMapping, ircToDiscordMapping, webhookClientCache, discordChannels, ircChannels,
		                  passwordMap, config.ignoredUsers);
	}

	public void setupIRCChannels()
	{
		LOGGER.info("Setting up IRC channels...");
		final Client client = Main.getIrcClient();
		LOGGER.trace("Leaving invalid channels...");
		client.getChannels().stream().map(Channel::getMessagingName)
		      .filter(messagingName -> !ircToDiscordMapping.containsKey(messagingName))
		      .forEach(s -> client.removeChannel(s, "Access removed"));

		LOGGER.trace("Joining valid channels...");
		for (String channel : ircChannels)
		{
			if (passwordMap.containsKey(channel))
				client.addKeyProtectedChannel(channel, passwordMap.get(channel));
			else
				client.addChannel(channel);
		}
	}

	public void offerUserCache(long guildID, long userID, String name, String avatar)
	{
//		userCache.put(userID, Tuples.pair(name, avatar));
		discordUserCache.getIfAbsentPut(guildID, LongObjectMaps.mutable::empty).put(userID, Tuples.pair(name, avatar));
	}

	public void offerUserCache(String user, long guildID, long chanID)
	{
		LOGGER.trace("Caching user {} in channel {}", user, chanID);

		final Guild guild = Main.getJda().getGuildById(guildID);

		if (guild == null)
		{
			LOGGER.warn("Couldn't cache member {} in guild {} because JDA couldn't find!", user, guildID);
			return;
		}

		final TextChannel textChannel = guild.getTextChannelById(chanID);
		if (textChannel != null)
		{
			final List<Member> memberList = guild.getMembersByEffectiveName(user, false);
			if (memberList.isEmpty())
			{
				LOGGER.trace("Tried to cache member {}, but none found in guild {}", user, guild.getId());
				return;
			}

			if (memberList.size() > 1)
			{
				LOGGER.trace("Trying to cache member {}, but found {} possible matches, trying first close match", user, memberList.size());
				for (Member member : memberList)
				{
					if (member.getUser().getName().equals(user))
					{
						LOGGER.trace("Got member {} ({})", member.getUser().getName(), member.getId());
//						ircToDiscordUserCache.put(user, member.getIdLong());
//						userCache.put(member.getIdLong(), Tuples.pair(member.getEffectiveName(),
//						                                              member.getEffectiveAvatarUrl()));
						final MutableLongObjectMap<Pair<String,String>> guildCache = discordUserCache.getIfAbsentPut(
								guildID, LongObjectMaps.mutable::empty);
						guildCache.put(member.getIdLong(), Tuples.pair(member.getEffectiveName(),
						                                               member.getEffectiveAvatarUrl()));
						return;
					}
				}
			} else
			{
				final Member member = memberList.getFirst();
				LOGGER.trace("Got member {} ({})", member.getUser().getName(), member.getId());
				ircToDiscordUserCache.put(user, member.getIdLong());
						userCache.put(member.getIdLong(), Tuples.pair(member.getEffectiveName(),
						                                              member.getEffectiveAvatarUrl()));
			}
		} else
			LOGGER.warn("Was offered cache for user {} in channel {}, but JDA couldn't find!", user, chanID);
	}

	public void removeUserCache(String user)
	{
		LOGGER.trace("Trying to remove user {} from cache", user);
		if (ircToDiscordUserCache.containsKey(user))
		{
			final long userID = ircToDiscordUserCache.get(user);
			LOGGER.trace("Removing user with ID {}...", userID);
			userCache.remove(userID);

			discordUserCache.values().forEach(pairs -> pairs.remove(userID));
		}
	}

	public void addSentMessage(@NotNull ReadonlyMessage message)
	{
		LOGGER.trace("Adding webhook message {} to cached messages", message.getId());
		sentMessages.add(message.getId());
	}

}
