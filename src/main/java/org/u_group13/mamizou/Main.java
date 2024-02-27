package org.u_group13.mamizou;

import com.fasterxml.jackson.databind.ObjectMapper;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.JDABuilder;
import net.dv8tion.jda.api.entities.Activity;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import net.dv8tion.jda.api.interactions.commands.DefaultMemberPermissions;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.dv8tion.jda.api.interactions.commands.build.Commands;
import net.dv8tion.jda.api.interactions.commands.build.SubcommandData;
import net.dv8tion.jda.api.requests.GatewayIntent;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import org.kitteh.irc.client.library.Client;
import org.kitteh.irc.client.library.feature.auth.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.u_group13.mamizou.commands.DiscordNamesCommand;
import org.u_group13.mamizou.commands.DiscordWHOISCommand;
import org.u_group13.mamizou.commands.IRCCommandBase;
import org.u_group13.mamizou.commands.IRCVersionCommand;
import org.u_group13.mamizou.config.Config;
import org.u_group13.mamizou.listeners.discord.APIListenerDiscord;
import org.u_group13.mamizou.listeners.discord.GenericListenerDiscord;
import org.u_group13.mamizou.listeners.discord.MessageListenerDiscord;
import org.u_group13.mamizou.listeners.discord.UserListenerDiscord;
import org.u_group13.mamizou.listeners.irc.CTCPListenerIRC;
import org.u_group13.mamizou.listeners.irc.GenericListenerIRC;
import org.u_group13.mamizou.listeners.irc.MessageListenerIRC;
import org.u_group13.mamizou.util.Helper;
import org.u_group13.mamizou.util.StringUtil;
import picocli.CommandLine;
import sun.misc.Signal;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.NoSuchAlgorithmException;
import java.security.spec.InvalidKeySpecException;
import java.util.Random;
import java.util.concurrent.Callable;

@CommandLine.Command(name = "MamizouBot", mixinStandardHelpOptions = true, version = StringUtil.VERSION,
					 description = "Begins an instance of the MamizouBot relay")
public class Main implements Callable<Integer>
{
	private static final Logger LOGGER = LoggerFactory.getLogger(Main.class);
	public static final long MY_ID = 394580185074368525L;
	public static final Random RANDOM = new Random();

	private static Main instance;

	private static JDA jda;
	private static Client ircClient;

	public static Config config;
	public static Helper helper;

	@CommandLine.Parameters(index = "0", description = "JSON configuration file")
	private Path configPath;
	@CommandLine.Option(names = "-k,--key", description = "Discord API key, overrides configuration", interactive = true)
	private char[] apiKey = null;

	private final ListenerAdapter api = new APIListenerDiscord(), generic = new GenericListenerDiscord(), message = new MessageListenerDiscord();

	public static JDA getJda()
	{
		return jda;
	}

	public static Client getIrcClient()
	{
		return ircClient;
	}

	public static Main getInstance()
	{
		return instance;
	}

	@Override
	public Integer call() throws Exception
	{
		LOGGER.info("Main call reached, got config path as: {}", configPath);
		LOGGER.info("Parsing configuration file...");

		readConfig(configPath);

		LOGGER.info("Setting up signal handlers...");
		Signal.handle(new Signal("HUP"), Main::hangupHandler);
		Signal.handle(new Signal("TERM"), Main::termHandler);

		LOGGER.info("Setting up JDA...");
		jda = JDABuilder.createLight(apiKey == null ? config.discordToken : new String(apiKey))
						.enableIntents(GatewayIntent.MESSAGE_CONTENT,
						               GatewayIntent.GUILD_WEBHOOKS)
		                .disableIntents(GatewayIntent.AUTO_MODERATION_CONFIGURATION,
		                                       GatewayIntent.GUILD_MODERATION,
											   GatewayIntent.GUILD_VOICE_STATES,
		                                       GatewayIntent.GUILD_EMOJIS_AND_STICKERS,
		                                       GatewayIntent.GUILD_INVITES,
		                                       GatewayIntent.GUILD_PRESENCES,
		                                       GatewayIntent.GUILD_MESSAGE_REACTIONS,
											   GatewayIntent.GUILD_MESSAGE_TYPING,
		                                       GatewayIntent.DIRECT_MESSAGE_REACTIONS,
		                                       GatewayIntent.DIRECT_MESSAGE_TYPING)
		                .addEventListeners(api, generic, message, new UserListenerDiscord())
		                .setActivity(Activity.watching("IRC Channels"))
		                .build();

		LOGGER.info("Connection to Discord complete!");

		LOGGER.info("Awaiting cache...");
		jda.awaitReady();
		LOGGER.info("Caching complete!");

		registerCommands();

		LOGGER.info("Setting up KICL...");

		final Client.Builder clientBuilder = Client.builder();

		setupIRCInfo(clientBuilder);
		setupIRCServer(clientBuilder);

		ircClient = clientBuilder.build();

		LOGGER.trace("Registering event listeners...");

		ircClient.getEventManager().registerEventListener(new MessageListenerIRC());
		ircClient.getEventManager().registerEventListener(new GenericListenerIRC());
		ircClient.getEventManager().registerEventListener(new CTCPListenerIRC());

		LOGGER.trace("Registering exception listener...");

		ircClient.setExceptionListener(e -> LOGGER.error("Unhandled exception in IRC client!", e));

		LOGGER.trace("Trying to setup SASL...");

		if (config.ircOptions.sasl)
		{
			try
			{
				ircClient.getAuthManager().addProtocol(getAuthProtocol());
			} catch (IOException e)
			{
				LOGGER.error("Couldn't read specified file!", e);
			} catch (NoSuchAlgorithmException e)
			{
				LOGGER.error("Couldn't get algorithm for ECC", e);
			} catch (InvalidKeySpecException e)
			{
				LOGGER.error("Key data was invalid!", e);
			}
		}

		LOGGER.info("Connecting to IRC...");

		ircClient.connect();

		for (String command : config.ircOptions.autoSendCommands)
			ircClient.sendRawLine(command);

		helper = Helper.createHelper(config);
		helper.setupIRCChannels();

		LOGGER.info("Connection to IRC complete!");

		return 0;
	}

	private static void setupIRCInfo(@NotNull Client.Builder builder)
	{
		LOGGER.trace("Setting up basic IRC info...");
		builder
				.name("Mamizou")
				.nick(config.ircOptions.nickname)
				.user(config.ircOptions.username)
				.realName(config.ircOptions.realName);
	}

	private static void setupIRCServer(@NotNull Client.Builder builder)
	{
		LOGGER.trace("Setting up IRC server info...");
		builder.server()
				.host(config.ircOptions.server)
                .port(config.ircOptions.port, config.ircOptions.secure
		                ? Client.Builder.Server.SecurityType.SECURE
		                : Client.Builder.Server.SecurityType.INSECURE);
		if (config.ircOptions.sasl && config.ircOptions.authType == Config.IRCOptions.AuthType.EXTERNAL && config.ircOptions.pathToCert != null)
			builder.server().secureKeyCertChain(config.ircOptions.pathToCert);
	}

	public static void main(String[] args)
	{
		LOGGER.info("Entry point reached");
		new CommandLine(new Main()).execute(args);
//		System.out.println("Hello world!");
	}

	private static void registerCommands()
	{
		LOGGER.info("Registering API commands...");
		final JDA jda = getJda();

		jda.updateCommands().addCommands
		(
				Commands.slash("whois", "Perform a WHOIS check on a user in the IRC")
						.addOption(OptionType.STRING, "user", "The user to check", true, true)
						.setGuildOnly(true),
				Commands.slash("whowas", "Perform a WHOWAS check on a user no longer in the IRC")
						.addOption(OptionType.STRING, "user", "The user to check", true)
						.setGuildOnly(true),
				Commands.slash("sslinfo", "Get SSL information on a user in the IRC")
						.addOption(OptionType.STRING, "user", "The user to check", true, true)
						.setGuildOnly(true)
						.setDefaultPermissions(DefaultMemberPermissions.DISABLED),
				Commands.slash("version", "Gets version and library info"),
				Commands.slash("webhook", "Edit webhooks")
						.addSubcommands(
								new SubcommandData("create", "Create a new, managed webhook to use")
										.addOption(OptionType.CHANNEL, "channel", "Channel to create in, defaults to current channel"),
								new SubcommandData("offer", "Offer an existing webhook to use")
										.addOption(OptionType.STRING, "url", "URL of the webhook to use"),
								new SubcommandData("delete", "Delete a webhook"))
				        .setGuildOnly(true)
				        .setDefaultPermissions(DefaultMemberPermissions.DISABLED),
				Commands.slash("optout", "Opt out of being included in relay messages and WHOIS queries."),
				Commands.slash("optin", "Opt in for being included in relay messages and WHOIS queries."),
				Commands.slash("names", "Get a list of names in the IRC."),
				Commands.user("Is User in IRC?").setGuildOnly(true)
		).queue();


		LOGGER.info("Registering IRC commands...");

		IRCCommandBase.ExecutorHelper.COMMAND_MAP.put("!whois", DiscordWHOISCommand::new);
		IRCCommandBase.ExecutorHelper.COMMAND_MAP.put("!names", DiscordNamesCommand::new);
		IRCCommandBase.ExecutorHelper.COMMAND_MAP.put("!version", IRCVersionCommand::new);
	}

	public static Config getConfig()
	{
		return config;
	}

	public static void setConfig(Config config)
	{
		Main.config = config;
	}

	public Path getConfigPath()
	{
		return configPath;
	}

	public void setConfigPath(Path configPath)
	{
		this.configPath = configPath;
		readConfig(configPath);
	}

	private static void readConfig(Path path)
	{
		try (final InputStream inputStream = Files.newInputStream(path))
		{
			final ObjectMapper mapper = new ObjectMapper();

			setConfig(mapper.readValue(inputStream, Config.class));
		} catch (IOException e)
		{
			LOGGER.error("Couldn't read configuration file!", e);
			System.exit(10);
		}
	}

	@NotNull
	@Contract(value = " -> new", pure = true)
	private static AuthProtocol getAuthProtocol() throws IOException, NoSuchAlgorithmException, InvalidKeySpecException
	{
		switch (config.ircOptions.authType)
		{
			case NICKSERV: return NickServ.builder(ircClient).account(config.ircOptions.username).password(
					config.ircOptions.password).build();
			case PLAIN: return new SaslPlain(ircClient, config.ircOptions.username, config.ircOptions.password);
			case EXTERNAL: return new SaslExternal(ircClient);
			case ECDSA:
			{
				final String keyString = new String(Files.readAllBytes(config.ircOptions.pathToCert))
						.replace("-----BEGIN EC PRIVATE KEY-----", "")
						.replace("-----END EC PRIVATE KEY-----", "")
						.replace(System.lineSeparator(), "");
				return new SaslEcdsaNist256PChallenge(ircClient, config.ircOptions.username, SaslEcdsaNist256PChallenge.getPrivateKey(keyString));
			}
			default:
				throw new IllegalStateException("Unexpected value: " + config.ircOptions.authType);
		}
	}

	private static void hangupHandler(Signal signal)
	{
		LOGGER.info("Received signal {}", signal);

		if ("HUP".equalsIgnoreCase(signal.getName()))
		{
			LOGGER.info("SIGHUP received, reloading configuration...");
			readConfig(getInstance().getConfigPath());
		}
	}

	private static void termHandler(Signal signal)
	{
		LOGGER.info("Received signal {}", signal);

		if ("TERM".equalsIgnoreCase(signal.getName()))
		{
			LOGGER.info("Attempting to shutdown gracefully...");
			jda.shutdown();
			ircClient.shutdown("SIGTERM received, shutting down gracefully...");
		}
	}
}