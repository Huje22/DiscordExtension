package me.indian.discord.core.config;

import eu.okaeri.configs.OkaeriConfig;
import eu.okaeri.configs.annotation.Comment;
import eu.okaeri.configs.annotation.CustomKey;
import eu.okaeri.configs.annotation.Header;
import me.indian.discord.core.config.sub.BotConfig;
import me.indian.discord.core.config.sub.RestAPIConfig;
import me.indian.discord.core.config.sub.WebHookConfig;

@Header("################################################################")
@Header("#           Ustawienia Integracji z Discord                    #")
@Header("################################################################")

public class DiscordConfig extends OkaeriConfig {

    @Comment({""})
    @Comment({"Ustawienia webhooka"})
    @CustomKey("WebHook")
    private WebHookConfig webHookConfig = new WebHookConfig();
    @Comment({""})
    @Comment({"Ustawienia Bota"})
    @CustomKey("Bot")
    private BotConfig botConfig = new BotConfig();

    @Comment({""})
    @Comment({"Ustawienia REST API"})
    @CustomKey("RestAPI")
    private RestAPIConfig restAPIConfig = new RestAPIConfig();

    public WebHookConfig getWebHookConfig() {
        return this.webHookConfig;
    }

    public BotConfig getBotConfig() {
        return this.botConfig;
    }

    public RestAPIConfig getRestAPIConfig() {
        return this.restAPIConfig;
    }
}