# MamizouBot

A Java-based Discord/IRC bot for relaying messages between linked channels.

Can be in many Discord servers, however, can only be in one IRC server.

It was made to be a replacement for [discord-irc](https://github.com/reactiflux/discord-irc), which I formerly used, but using an up-to-date API (JDA) and with more features for the convenience of its users. More features are planned to be added.

***Be advised, the bot is still in an alpha stage and not intended for major use yet!***

## Building

If you want to build it yourself or even make changes, just run `/gradlew shadowJar`.

To use the build hasher function I used, the source code is [here](https://gist.github.com/UFFR/cf4304250bf42f0d1a58c3003c9bce5b), just so you don't have to worry about a shady binary for a specific platform.

## Usage

The configuration file is in JSON, similar to discord-irc, but the structure has been changed slightly:

```json
{
    "discordToken": "I-WAS-NOT-CONFIGURED",
    "channelMapping": {
        "0123456789": "#general"// JSON map keys must always be a string.
    },
    "ircOptions": {
        "nickname":     "IRC_Relay", // IRC-side nickname.
        "username":     "Mamizou", // User to login with, recommended for convenience with services.
        "server":       "irc.libera.chat", // Self-explanatory.
        "port":         6697, // TLS is always recommended.
        "nickColors":   true, // Doesn't do anything yet, but will toggle whether or not on IRC-side should names be given colors.
        "sasl":         true, // If SASL should be used for authentication, recommended.
        "statusNotices: true, // Doesn't do anything yet, but will toggle sending updates on non-message events.
        "password":     "I-WAS-NOT-CONFIGURED", // IRC account password, for PLAIN or NickServ based authentication.
        "secure":       true, // Explicit TLS, recommended.
        "authType":     "ECDSA", // Supports "NICKSERV" for NickServ password authentication, "PLAIN" for SASL password authentication, "EXTERNAL" for client TLS authentication with SASL, and "ECDSA" for SASL authentication with the ECDSA-NIST256p-CHALLENGE mechanism. P-256 keys must be in unencrypted, PKCS8 format, if yours isn't, you can convert it with OpenSSL.
        "pathToCert":   "/home/mamizou/Certificates/my_cert.pem", // Path to either the TLS certificate or P-256 key, depending on settings.
        "autoSendCommands": ["MODE +b"] // Lines sent to the server upon connection
    },
    "ignoredUsers": {
        "ignoredHosts":         [], // Hostmasks to ignore from relayed messages, must be exact matches, unfortunately.
        "ignoredDiscordUsers":  [], // Discord usernames to ignore from relaying.
        "ignoredDiscordIDs":    [9876543210], // Discord user IDs to ignore from relaying. More reliable than usernames.
    },
    "parallelPingFix": true, // Doesn't work yet, but it is meant to prevent pinging yourself if you happen to be in the IRC and Discord at the same time.
    "commandCharacters": ["!", ".", "&", "-", "=", "?"], // Single characters that might indicate the message was a command and not a proper message. Doesn't do anything yet.
    "webhookMapping": {
        "0123456789": "https://discord.com/api/webhooks/0123456789/abcdef" // Mapping Discord channels to webhooks so relayed messages can be formatted better.
    },
    "saveDataPath": "/home/mamizou/.config/mamizou_bot_savedata.json" // Path for saving configuration data, not finished yet.
}
```

The bot is launched by passing the configuration file path via the command line. The Discord token can be overridden also through the command line interactively to prevent echoing and saving to terminal history or being stored in a file.

## Libraries

This project was made possible by:

- [JDA](https://github.com/discord-jda/JDA), for the Discord API.
- [Discord-Webhooks](https://github.com/MinnDevelopment/discord-webhooks), for webhook convenience.
- [Kitteh IRC Client Library](https://github.com/KittehOrg/KittehIRCClientLib) , for the IRC bot.
- [Picocli](https://picocli.info/), for command line control and IRC bot commands.
- [Logback](https://logback.qos.ch/), for the [SLF4J](https://www.slf4j.org/) implementation.
- [Jackson](https://github.com/FasterXML/jackson), for JSON parsing and serialization.
- [Eclipse Collections](https://github.com/eclipse/eclipse-collections), for primitive collections.
