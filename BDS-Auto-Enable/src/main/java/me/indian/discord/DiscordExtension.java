package me.indian.discord;

import java.time.Duration;
import java.time.temporal.ChronoUnit;
import me.indian.bds.BDSAutoEnable;
import me.indian.bds.command.CommandManager;
import me.indian.bds.event.EventManager;
import me.indian.bds.extension.Extension;
import me.indian.bds.logger.Logger;
import me.indian.discord.command.DiscordCommand;
import me.indian.discord.command.LinkCommand;
import me.indian.discord.command.UnlinkCommand;
import me.indian.discord.core.config.DiscordConfig;
import me.indian.discord.core.config.LinkingConfig;
import me.indian.discord.core.config.MessagesConfig;
import me.indian.discord.core.config.ProximityVoiceChatConfig;
import me.indian.discord.core.config.StatsChannelsConfig;
import me.indian.discord.core.manager.ILinkingManager;
import me.indian.discord.core.manager.IStatsChannelsManager;
import me.indian.discord.hook.RestWebsiteHook;
import me.indian.discord.jda.DiscordJDA;
import me.indian.discord.listener.BackupListener;
import me.indian.discord.listener.ExtensionDisableListener;
import me.indian.discord.listener.PlayerEventListener;
import me.indian.discord.listener.ServerListener;
import me.indian.discord.webhook.WebHook;
import me.indian.rest.RestWebsite;
import net.dv8tion.jda.api.JDA;

public class DiscordExtension extends Extension {

    private DiscordConfig config;
    private MessagesConfig messagesConfig;
    private LinkingConfig linkingConfig;
    private StatsChannelsConfig statsChannelsConfig;
    private ProximityVoiceChatConfig proximityVoiceChatConfig;
    private Logger logger;
    private DiscordJDA discordJDA;
    private WebHook webHook;
    private boolean botEnabled, webhookEnabled;

    @Override
    public void onEnable() {
        final BDSAutoEnable bdsAutoEnable = this.getBdsAutoEnable();
        this.config = this.createConfig(DiscordConfig.class, "config");
        this.messagesConfig = this.createConfig(MessagesConfig.class, "Messages");
        this.linkingConfig = this.createConfig(LinkingConfig.class, "Linking");
        this.statsChannelsConfig = this.createConfig(StatsChannelsConfig.class, "StatsChannels");
        this.proximityVoiceChatConfig = this.createConfig(ProximityVoiceChatConfig.class, "ProximityVoiceChat");

        this.logger = this.getLogger();

        this.botEnabled = this.getConfig().getBotConfig().isEnable();
        this.webhookEnabled = this.getConfig().getWebHookConfig().isEnable();

        this.discordJDA = new DiscordJDA(this);
        this.webHook = new WebHook(this);

        this.discordJDA.init();

        final CommandManager commandManager = bdsAutoEnable.getCommandManager();
        final EventManager eventManager = bdsAutoEnable.getEventManager();

        if (this.botEnabled) {
            commandManager.registerCommand(new DiscordCommand(this), this);
            commandManager.registerCommand(new LinkCommand(this), this);
            commandManager.registerCommand(new UnlinkCommand(this), this);

            eventManager.registerListener(new BackupListener(this), this);
            eventManager.registerListener(new PlayerEventListener(this), this);
            eventManager.registerListener(new ServerListener(this), this);
            eventManager.registerListener(new ExtensionDisableListener(this.discordJDA), this);
        }

        try {
            final RestWebsite restWebsite = (RestWebsite) bdsAutoEnable.getExtensionManager().getExtension("RestWebsite");
            if (restWebsite != null && restWebsite.isEnabled()) {
                new RestWebsiteHook(this, restWebsite);
                this.logger.info("Wykryto&b " + restWebsite.getName());
            }
        } catch (final Exception exception) {
            this.logger.error("Nie udało się załadować hooka do&b RestWebsite", exception);
        }
    }

    @Override
    public void onDisable() {
        this.startShutdown();
        this.shutdown();
    }

    private void startShutdown() {
        if (this.discordJDA != null) {
            final ILinkingManager linkingManager = this.discordJDA.getLinkingManager();
            final IStatsChannelsManager statsChannelsManager = this.discordJDA.getStatsChannelsManager();

            if (linkingManager != null) linkingManager.saveLinkedAccounts();
            if (statsChannelsManager != null) statsChannelsManager.onShutdown();
        }
    }

    public void shutdown() {
        this.webHook.shutdown();

        final JDA jda = this.discordJDA.getJda();
        if (jda != null) {
            if (jda.getStatus() == JDA.Status.CONNECTED) {
                try {
                    this.logger.info("Wyłączanie bota...");
                    jda.shutdown();
                    if (jda.awaitShutdown(Duration.of(2, ChronoUnit.MINUTES))) {
                        this.logger.info("Wyłączono bota");
                    } else {
                        jda.shutdownNow();
                        this.logger.error("Nie udało się wyłączyć bota w czasie&b 2&e minut");
                    }
                } catch (final Exception exception) {
                    this.logger.critical("Nie można wyłączyć bota", exception);
                }
            }
        }
    }

    public DiscordConfig getConfig() {
        return this.config;
    }

    public MessagesConfig getMessagesConfig() {
        return this.messagesConfig;
    }

    public LinkingConfig getLinkingConfig() {
        return this.linkingConfig;
    }

    public StatsChannelsConfig getStatsChannelsConfig() {
        return this.statsChannelsConfig;
    }

    public ProximityVoiceChatConfig getProximityVoiceChatConfig() {
        return this.proximityVoiceChatConfig;
    }

    public void reloadConfigs() {
        this.config = (DiscordConfig) this.config.load(true);
        this.messagesConfig = (MessagesConfig) this.messagesConfig.load(true);
        this.linkingConfig = (LinkingConfig) this.linkingConfig.load(true);
        this.statsChannelsConfig = (StatsChannelsConfig) this.statsChannelsConfig.load(true);
        this.proximityVoiceChatConfig = (ProximityVoiceChatConfig) this.proximityVoiceChatConfig.load(true);
    }

    public DiscordJDA getDiscordJDA() {
        return this.discordJDA;
    }

    public WebHook getWebHook() {
        return this.webHook;
    }

    public boolean isBotEnabled() {
        return this.botEnabled;
    }

    public void setBotEnabled(final boolean botEnabled) {
        this.botEnabled = botEnabled;
    }

    public boolean isWebhookEnabled() {
        return this.webhookEnabled;
    }

    public void setWebhookEnabled(final boolean webhookEnabled) {
        this.webhookEnabled = webhookEnabled;
    }
}