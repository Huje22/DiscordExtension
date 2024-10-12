package pl.indianbartonka.discord.listener;

import pl.indianbartonka.bds.event.EventHandler;
import pl.indianbartonka.bds.event.Listener;
import pl.indianbartonka.bds.event.server.ExtensionDisableEvent;
import pl.indianbartonka.discord.jda.DiscordJDA;
import pl.indianbartonka.discord.jda.command.SlashCommandManager;

public class ExtensionDisableListener implements Listener {

    private final SlashCommandManager slashCommandManager;

    public ExtensionDisableListener(final DiscordJDA discordJDA) {
        this.slashCommandManager = discordJDA.getSlashCommandManager();
    }

    @EventHandler
    private void onExtensionDisable(final ExtensionDisableEvent event) {
        this.slashCommandManager.unregister(event.getExtension());
    }
}