package org.u_group13.mamizou.util;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Objects;

@Deprecated
public class DiscordUserCache
{
	private static final Logger LOGGER = LoggerFactory.getLogger(DiscordUserCache.class);

	public final long id;
	public String name, avatar;

	public DiscordUserCache(long id, String name, String avatar)
	{
		this.id = id;
		this.name = name;
		this.avatar = avatar;
	}

	@Override
	public boolean equals(Object o)
	{
		if (this == o) return true;
		if (o == null || getClass() != o.getClass()) return false;
		final DiscordUserCache discordUserCache = (DiscordUserCache) o;
		return id == discordUserCache.id && Objects.equals(name, discordUserCache.name) && Objects.equals(avatar,
		                                                                                                  discordUserCache.avatar);
	}

	@Override
	public int hashCode()
	{
		return Objects.hash(id, name, avatar);
	}

	@Override
	public String toString()
	{
		return "DiscordUserCache{" + "id=" + id +
				", name='" + name + '\'' +
				", avatar='" + avatar + '\'' +
				'}';
	}
}
