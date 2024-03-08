package org.u_group13.mamizou.config;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.TreeNode;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import org.eclipse.collections.api.factory.primitive.CharSets;
import org.eclipse.collections.api.factory.primitive.LongObjectMaps;
import org.eclipse.collections.api.factory.primitive.LongSets;
import org.eclipse.collections.api.map.primitive.LongObjectMap;
import org.eclipse.collections.api.map.primitive.MutableLongObjectMap;
import org.eclipse.collections.api.set.primitive.CharSet;
import org.eclipse.collections.api.set.primitive.LongSet;
import org.eclipse.collections.api.set.primitive.MutableCharSet;
import org.eclipse.collections.api.set.primitive.MutableLongSet;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.Serializable;
import java.nio.file.Path;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

public class Config implements Serializable
{
	private static final Logger LOGGER = LoggerFactory.getLogger(Config.class);
	public static class IRCOptions implements Serializable
	{
		public enum AuthType
		{
			NICKSERV,
			PLAIN,
			EXTERNAL,
			ECDSA
		}

		public String nickname = "IRC_Relay", username = "relaybot", realName = "Mamizou", password = "I-WAS-NOT-CONFIGURED", server = "example.com";

		public String[] autoSendCommands = new String[0];
		public int port = 6667;
		public boolean secure = true, sasl = false, nickColors = true, statusNotices = true;
		public AuthType authType = AuthType.NICKSERV;
		public Path pathToCert;
	}

	public static class IgnoredUsers implements Serializable
	{
		public Set<String> ignoredHosts = new HashSet<>(), ignoredDiscordUsers = new HashSet<>();
		@JsonDeserialize(using = LongSetDeserializer.class)
		public MutableLongSet ignoredDiscordIDs = LongSets.mutable.empty();

		public IgnoredUsers(Set<String> ignoredHosts, Set<String> ignoredDiscordUsers, MutableLongSet ignoredDiscordIDs)
		{
			this.ignoredHosts = ignoredHosts;
			this.ignoredDiscordUsers = ignoredDiscordUsers;
			this.ignoredDiscordIDs = ignoredDiscordIDs;
		}

		public IgnoredUsers()
		{
		}
	}

	public String discordToken = "I-WAS-NOT-CONFIGURED";
	// TODO
	public boolean parallelPingFix = true;

	@JsonDeserialize(using = LongStringMapDeserializer.class)
	public LongObjectMap<String> channelMapping = LongObjectMaps.immutable.empty(), webhookMapping = LongObjectMaps.immutable.empty();
	@JsonDeserialize(using = CharSetDeserializer.class)
	public CharSet commandCharacters = CharSets.immutable.empty();

	public IRCOptions ircOptions = new IRCOptions();

	public IgnoredUsers ignoredUsers = new IgnoredUsers();
	public Path saveDataPath;

	static class CharSetDeserializer extends JsonDeserializer<CharSet>
	{
		@Override
		public CharSet deserialize(JsonParser jsonParser,
		                           DeserializationContext deserializationContext) throws IOException
		{
			final MutableCharSet set;

			final ObjectMapper mapper = new ObjectMapper();
			final TreeNode treeNode = mapper.readTree(jsonParser);

			set = CharSets.mutable.withInitialCapacity(treeNode.size());
			for (int i = 0; i < treeNode.size(); i++)
				set.add(treeNode.get(i).toString().charAt(1));

			return set;
		}
	}

	static class LongStringMapDeserializer extends JsonDeserializer<LongObjectMap<String>>
	{
		@Override
		public LongObjectMap<String> deserialize(JsonParser jsonParser,
		                                         DeserializationContext deserializationContext) throws IOException
		{
			final MutableLongObjectMap<String> map = LongObjectMaps.mutable.empty();

			final ObjectMapper mapper = new ObjectMapper();
			final JsonNode node = mapper.readTree(jsonParser);

			final Iterator<String> iterator = node.fieldNames();
			while (iterator.hasNext())
			{
				final String key = iterator.next();
				final String value = node.get(key).asText();

				map.put(Long.parseUnsignedLong(key), value);
			}

			return map;
		}
	}

	static class LongSetDeserializer extends JsonDeserializer<LongSet>
	{
		@Override
		public LongSet deserialize(JsonParser jsonParser,
		                           DeserializationContext deserializationContext) throws IOException
		{
			final MutableLongSet set;

			final ObjectMapper mapper = new ObjectMapper();
			final JsonNode node = mapper.readTree(jsonParser);
			set = LongSets.mutable.withInitialCapacity(node.size());
			for (int i = 0; i < node.size(); i++)
				set.add(node.get(i).asLong());

			return set;
		}
	}
}
