package org.u_group13.mamizou.listeners.discord;

import static org.u_group13.mamizou.Main.*;

import club.minnced.discord.webhook.WebhookClientBuilder;
import club.minnced.discord.webhook.send.AllowedMentions;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.Webhook;
import net.dv8tion.jda.api.entities.channel.concrete.TextChannel;
import net.dv8tion.jda.api.events.interaction.command.CommandAutoCompleteInteractionEvent;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.events.interaction.command.UserContextInteractionEvent;
import net.dv8tion.jda.api.events.interaction.component.ButtonInteractionEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import net.dv8tion.jda.api.interactions.commands.OptionMapping;
import org.jetbrains.annotations.NotNull;
import org.kitteh.irc.client.library.element.Channel;
import org.kitteh.irc.client.library.element.User;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.u_group13.mamizou.Main;
import org.u_group13.mamizou.config.LinkEntry;
import org.u_group13.mamizou.config.LinkRegistries;
import org.u_group13.mamizou.util.StringUtil;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;

public class APIListenerDiscord extends ListenerAdapter
{
	private static final Logger LOGGER = LoggerFactory.getLogger(APIListenerDiscord.class);

	@Override
	public void onSlashCommandInteraction(@NotNull SlashCommandInteractionEvent event)
	{
		LOGGER.debug("Received API slash command {}", event.getCommandId());
		final long userID = event.getUser().getIdLong();

		final LinkRegistries linkRegistries = LinkRegistries.getInstance();
		switch (event.getName())
		{
			case "whois":
			{
				LOGGER.debug("WHOIS command issued");
				event.deferReply().queue();
				if (event.getOption("user") == null)
					event.reply("No user supplied.").queue();
				else
				{
					final String username = Objects.requireNonNull(event.getOption("user")).getAsString();
					LOGGER.trace("Querying user {}", username);
					final Optional<Channel> optionalChannel = getIrcClient().getChannel(
							config.channelMapping.get(event.getChannelIdLong()));
					if (optionalChannel.isPresent())
					{
						getIrcClient().commands().whois().server(config.ircOptions.server).target(username).execute();
						final Channel channel = optionalChannel.get().getLatest().orElseGet(optionalChannel::get);
						final Optional<User> optionalUser = channel.getUser(username);

						if (optionalUser.isEmpty())
						{
							LOGGER.trace("User wasn't found");
							event.getHook().sendMessage("User not found").queue();
							return;
						}

						LOGGER.trace("User was found");
						event.getHook().sendMessage(StringUtil.getWHOIS(optionalUser.get(), channel)).queue();
						if (!channel.hasCompleteUserData())
							event.getChannel().sendMessage("Warning! Mapped channel has incomplete data!").queue();
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
			case "names":
			{
				LOGGER.debug("NAMES command issued");

				if (helper.discordToIRCMapping.containsKey(event.getChannelIdLong()))
				{
					final String channelName = helper.discordToIRCMapping.get(event.getChannelIdLong());
					final Optional<Channel> optionalChannel = getIrcClient().getChannel(channelName);
					if (optionalChannel.isPresent())
					{
						LOGGER.debug("Found channel");
						final Channel channel = optionalChannel.get().getLatest().orElseGet(optionalChannel::get);
						event.reply("Got nicks: " + channel.getNicknames()).queue();
						if (!channel.hasCompleteUserData())
							event.getChannel().sendMessage("Warning! Mapped channel has incomplete data!").queue();
					} else
					{
						LOGGER.warn("Channel {} ({}) in mapping, but IRC mapped channel {} doesn't exist!",
						             event.getChannel().getName(),
						             event.getChannelId(),
						             channelName);
						event.reply("Couldn't find mapped IRC channel!").queue();
					}
				} else
					event.reply("Channel not mapped!").queue();

				break;
			}
			case "version":
			{
				LOGGER.trace("VERSION command issued");
				event.replyEmbeds(new EmbedBuilder()
						                  .setTitle("MamizouBot v" + StringUtil.VERSION + " by UFFR")
						                  .appendDescription(StringUtil.getCreditsString())
						                  .build()).queue();
				break;
			}
			case "optout":
			{
				LOGGER.trace("OPTOUT command issued for {}", userID);
				final boolean result = helper.ignoredUsers.ignoredDiscordIDs.add(userID);
				linkRegistries.removeEntry(userID);
				event.reply(result ? "You have been added to the ignore list." : "You are already on the ignore list.").setEphemeral(true).queue();
				break;
			}
			case "optin":
			{
				LOGGER.trace("OPTIN command issued for {}", userID);
				final boolean result = helper.ignoredUsers.ignoredDiscordIDs.remove(userID);
				event.reply(result ? "You have been removed from the ignore list." : "You are already opted in.").setEphemeral(true).queue();
				break;
			}
			case "webhook":
			{
				LOGGER.trace("WEBHOOK command issued");
				if (event.getSubcommandName() == null)
				{
					LOGGER.trace("No subcommand provided");
					event.reply("Provided no subcommand!").queue();
					return;
				}
				if (event.getGuild() == null)
				{
					LOGGER.trace("Wasn't called within a guild");
					event.reply("Can only be called in a server!").queue();
					return;
				}

				event.deferReply().queue();
				switch (event.getSubcommandName())
				{
					case "create":
					{
						LOGGER.trace("Attempting to create new webhook...");
						final OptionMapping option = event.getOption("channel");

						final TextChannel channel = option == null ? event.getGuildChannel().asTextChannel() : option.getAsChannel().asTextChannel();

						if (!helper.discordChannels.contains(channel.getIdLong()))
						{
							event.getHook().sendMessage("Channel not mapped!").queue();
							return;
						}

						final String webhookName = "Relay-" + Integer.toHexString(Main.RANDOM.nextInt());
						final Webhook webhook = channel.createWebhook(webhookName).setName("IRC Relay").complete();

						final WebhookClientBuilder clientBuilder = WebhookClientBuilder.fromJDA(webhook)
								.setHttpClient(Main.getJda().getHttpClient()).setAllowedMentions(AllowedMentions.none())
								.setWait(true);

						helper.webhookClientCache.put(event.getChannelIdLong(), clientBuilder.buildJDA());

						event.getHook().sendMessage("Created webhook \"" + webhookName + "\" in " + channel.getName()).queue();
						break;
					}
					case "offer":
					{
						LOGGER.trace("Was offered existing webhook");

						if (event.getGuild() == null)
						{
							event.getHook().sendMessage("Can only be called in a server!").queue();
							return;
						}

						if (!helper.discordChannels.contains(event.getChannelIdLong()))
						{
							event.getHook().sendMessage("Channel not mapped!").queue();
							return;
						}

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

						helper.webhookClientCache.put(event.getChannelIdLong(), clientBuilder.buildJDA());
						event.getHook().sendMessage("Done.").queue();

						break;
					}
					case "delete":
					{
						LOGGER.trace("Attempting to delete/remove webhook");
						final OptionMapping option = event.getOption("id");

						if (option == null)
						{
							event.getHook().sendMessage("No webhook ID provided!").queue();
							return;
						}

						final long id = option.getAsLong();

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
							                                      event.getUser().getName(),
							                                      event.getUser().getId())).queue();
						}

						helper.webhookClientCache.values().removeIf(webhookClient -> webhookClient.getId() == id);

						event.getHook().sendMessage("Webhook deleted.").queue();

						break;
					}
					default:
					{
						LOGGER.trace("Subcommand \"{}\" was not recognized", event.getSubcommandName());
						event.getHook().sendMessage("Unknown or unhandled subcommand!").queue();
						break;
					}
				}
				break;
			}
			case "link":
			{
				LOGGER.debug("LINK command issued");
				final OptionMapping option = event.getOption("user");

				event.deferReply(true).queue();

				if (option == null)
				{
					LOGGER.trace("User wasn't specified");
					event.getHook().sendMessage("Must specify user account to link with!").queue();
					return;
				}

				final List<User> users = getIrcClient().getChannels()
				                                         .stream()
				                                         .flatMap(channel -> channel.getUsers().stream())
				                                         .toList();

				final String username = option.getAsString();
				final List<User> matchingUsers = users.stream()
				                                     .filter(user -> user.getAccount().isPresent() && user.getAccount().get().equals(username))
				                                     .collect(Collectors.toList());
				if (matchingUsers.isEmpty())
				{
					LOGGER.trace("User {} was not found", username);
					event.getHook().sendMessage("Unable to locate user in server.").queue();
					return;
				}

				LOGGER.debug("User {} was found in: {}", username, matchingUsers);

				for (User matchingUser : matchingUsers)
				{
					matchingUser.sendMessage(
							"User %s (ID: %s) on Discord requested to link, respond with !ACCEPT [ID] to accept, !REJECT [ID] to reject.".formatted(
									event.getUser().getEffectiveName(), event.getUser().getId()));
				}

				linkRegistries.addRequest(new LinkEntry(username, userID), true);

				event.getHook().sendMessage("Link request has been sent.").setEphemeral(true).queue();

				break;
			}
			case "unlink":
			{
				LOGGER.trace("UNLINK command issued");

				if (!linkRegistries.isRegistered(userID))
				{
					event.reply("You are not linked to begin with!").queue();
					return;
				}

				linkRegistries.removeEntry(userID);

				event.reply("Unlinked.").setEphemeral(true).queue();

				break;
			}
			case "reject":
			{
				final OptionMapping userOption = event.getOption("user");
				final OptionMapping allOption = event.getOption("all");

				final boolean all = allOption != null && allOption.getAsBoolean();

				if (all)
				{
					linkRegistries.rejectAll(userID);
					event.reply("All requests rejected!").setEphemeral(true).queue();
				} else if (userOption != null)
				{
					linkRegistries.rejectRequest(userOption.getAsString());
					event.reply("User request rejected!").setEphemeral(true).queue();
				} else
					event.reply("No user specified!").setEphemeral(true).queue();

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
			case "whowas":
			case "whois":
			case "link":
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
		LOGGER.debug("Received user context event {}", event.getId());

		event.reply(LinkRegistries.getInstance().isRegistered(event.getTarget().getIdLong()) ? "Yes" : "No").setEphemeral(true).queue();
	}

	@Override
	public void onButtonInteraction(@NotNull ButtonInteractionEvent event)
	{
		LOGGER.debug("Received button interaction event {}", event.getId());

		final String componentId = event.getComponentId();
		if (!componentId.matches(StringUtil.BUTTON_ID_REGEX))
		{
			LOGGER.warn("Received button ID of \"{}\" which shouldn't be possible!", componentId);
			return;
		}

		final boolean accept = componentId.charAt(0) == 'A';
		final String ircAccount = componentId.substring(2);

		LOGGER.trace("Request {} for account \"{}\"", accept ? "accepted" : "rejected", ircAccount);

		if (accept)
		{
			final boolean result = LinkRegistries.getInstance().acceptRequest(ircAccount);
			if (result)
				event.editMessage("Accepted").queue();
			else
			{
				LOGGER.warn("No request found, couldn't accept nonexistent request!");
				event.editMessage("Interaction failed!").queue();
			}
		} else
		{
			LinkRegistries.getInstance().rejectRequest(ircAccount);
			event.editMessage("Rejected").queue();
		}

	}
}
