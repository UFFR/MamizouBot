package org.u_group13.mamizou.listeners.discord;

import static org.u_group13.mamizou.Main.*;

import club.minnced.discord.webhook.WebhookClientBuilder;
import club.minnced.discord.webhook.send.AllowedMentions;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.Webhook;
import net.dv8tion.jda.api.entities.channel.concrete.TextChannel;
import net.dv8tion.jda.api.events.interaction.command.CommandAutoCompleteInteractionEvent;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.events.interaction.command.UserContextInteractionEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import net.dv8tion.jda.api.interactions.commands.OptionMapping;
import org.jetbrains.annotations.NotNull;
import org.kitteh.irc.client.library.Client;
import org.kitteh.irc.client.library.element.Channel;
import org.kitteh.irc.client.library.element.User;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.u_group13.mamizou.Main;
import org.u_group13.mamizou.util.StringUtil;

import java.util.ArrayList;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;

public class APIListenerDiscord extends ListenerAdapter
{
	private static final Logger LOGGER = LoggerFactory.getLogger(APIListenerDiscord.class);

	@Override
	public void onSlashCommandInteraction(@NotNull SlashCommandInteractionEvent event)
	{
		// TODO Add command handling
		LOGGER.debug("Received API slash command {}", event.getCommandId());
		if (event.getUser().getIdLong() != Main.MY_ID)
		{
			event.reply("403").queue();
			return;
		}

		switch (event.getName())
		{
			case "whois":
			{
				event.deferReply().queue();
				if (event.getOption("user") == null)
					event.reply("No user supplied.").queue();
				else
				{
					final String username = Objects.requireNonNull(event.getOption("user")).getAsString();
					final Client client = Main.getIrcClient();
					final Optional<Channel> optionalChannel = client.getChannel(
							config.channelMapping.get(event.getChannelIdLong()));
					if (optionalChannel.isPresent())
					{
						final Channel channel = optionalChannel.get();
						final Optional<User> optionalUser = channel.getUser(username);

						if (optionalUser.isEmpty())
						{
							event.getHook().sendMessage("User not found").queue();
							return;
						}

						event.getHook().sendMessage(StringUtil.getWHOIS(optionalUser.get())).queue();
					} else
					{
						LOGGER.warn("Channel {} ({}) in mapping, but IRC mapped channel {} doesn't exist!",
						            event.getChannel().getName(),
						            event.getChannelId(),
						            config.channelMapping.containsKey(event.getChannelIdLong()));
						event.getHook().sendMessage("Couldn't find mapped IRC channel!").queue();
					}
				}
				break;
			}
			case "version":
			{
				event.replyEmbeds(new EmbedBuilder()
						                  .setTitle("MamizouBot v" + StringUtil.VERSION + " by UFFR")
						                  .appendDescription(StringUtil.getCreditsString())
						                  .build()).queue();
				break;
			}
			case "optout":
			{
				final boolean result = helper.ignoredUsers.ignoredDiscordIDs.add(event.getUser().getIdLong());
				event.reply(result ? "You have been added to the ignore list." : "You are already on the ignore list.").setEphemeral(true).queue();
				break;
			}
			case "optin":
			{
				final boolean result = helper.ignoredUsers.ignoredDiscordIDs.remove(event.getUser().getIdLong());
				event.reply(result ? "You have been removed from the ignore list." : "You are already opted in.").setEphemeral(true).queue();
				break;
			}
			case "webhook":
			{
				if (event.getSubcommandName() == null)
				{
					event.reply("Provided no subcommand!").queue();
					return;
				}
				if (event.getGuild() == null)
				{
					event.reply("Can only be called in a server!").queue();
					return;
				}

				event.deferReply().queue();
				switch (event.getSubcommandName())
				{
					case "create":
					{
						final OptionMapping option = event.getOption("channel");

						final TextChannel channel = option == null ? event.getGuildChannel().asTextChannel() : option.getAsChannel().asTextChannel();

						final String webhookName = "Relay-" + Integer.toHexString(Main.RANDOM.nextInt());
						final Webhook webhook = channel.createWebhook(webhookName).setName("IRC Relay").complete();

						final WebhookClientBuilder clientBuilder = new WebhookClientBuilder(webhook.getUrl())
								.setHttpClient(Main.getJda().getHttpClient()).setAllowedMentions(AllowedMentions.none())
								.setWait(true);

						helper.webhookClientCache.put(event.getChannelIdLong(), clientBuilder.buildJDA());

						event.getHook().sendMessage("Created webhook \"" + webhookName + "\" in " + channel.getName()).queue();
						break;
					}
					case "offer":
					{
						// TODO Finish
						final OptionMapping option = event.getOption("url");

						if (option == null)
						{
							event.getHook().sendMessage("No webhook provided!").queue();
							return;
						}

						final String webhookUrl = option.getAsString();

						if (!WebhookClientBuilder.WEBHOOK_PATTERN.matcher(webhookUrl).matches())
						{
							event.getHook().sendMessage("Provided string is not a valid webhook URL!").queue();
							return;
						}

						final WebhookClientBuilder clientBuilder = new WebhookClientBuilder(webhookUrl)
								.setHttpClient(Main.getJda().getHttpClient()).setAllowedMentions(AllowedMentions.none())
								.setWait(true);

						break;
					}
					case "delete":
					{
						// TODO Finish
						final OptionMapping option = event.getOption("id");

						if (option == null)
						{
							event.getHook().sendMessage("No webhook ID provided!").queue();
							return;
						}

						final int id = option.getAsInt();

						final Optional<Webhook> first = event.getGuild()
						                                     .retrieveWebhooks()
						                                     .complete()
						                                     .stream()
						                                     .filter(webhook -> webhook.getIdLong() == id)
						                                     .findFirst();

						if (first.isEmpty())
						{
							event.getHook().sendMessage("Couldn't find webhook with specified ID!").queue();
							return;
						}

						final Webhook webhook = first.get();

						if (webhook.getOwner() != null && getJda().getSelfUser().getIdLong() == webhook.getOwner().getIdLong())
						{
							webhook.delete().reason(String.format("Webhook deletion requested by %s (%s)",
							                                      event.getUser().getName(), event.getUser().getId())).queue();
						}

						break;
					}
					default:
					{
						event.getHook().sendMessage("Unknown or unhandled subcommand!").queue();
						break;
					}
				}
				break;
			}
			case "whowas":
			case "sslinfo":
			{
				event.reply("Command not implemented.").queue();
				break;
			}
			default:
			{
				event.reply("Unknown or unhandled command!").queue();
				break;
			}
		}
	}

	@Override
	public void onCommandAutoCompleteInteraction(@NotNull CommandAutoCompleteInteractionEvent event)
	{
		switch (event.getName())
		{
			case "sslinfo":
			case "whois admin":
			case "whowas":
			case "whois":
			{
				final long chanID = event.getChannelIdLong();
				if (config.channelMapping.containsKey(chanID))
				{
					final Optional<Channel> optionalChannel = Main.getIrcClient().getChannel(config.channelMapping.get(chanID));
					if (optionalChannel.isPresent())
					{
						final Channel channel = optionalChannel.get();
						event.replyChoiceStrings(event.getFocusedOption().getValue().isBlank() ? channel.getNicknames() : channel.getNicknames().stream().filter(s ->  s.contains(event.getFocusedOption().getValue())).collect(
								Collectors.toCollection(ArrayList::new))).queue();
					} else
					{
						LOGGER.warn("Channel {} ({}) in mapping, but IRC mapped channel {} doesn't exist!",
						            event.getChannel().getName(),
						            event.getChannelId(),
						            config.channelMapping.containsKey(event.getChannelIdLong()));
					}
				}
				break;
			}
			default: return;
		}
	}

	@Override
	public void onUserContextInteraction(@NotNull UserContextInteractionEvent event)
	{
		// TODO
		if (event.getUser().getIdLong() == MY_ID)
		{
			event.reply("403").queue();
			return;
		}
		event.deferReply(true).queue();
	}
}
