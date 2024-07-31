package me.indian.discord.listener;

import me.indian.bds.event.EventHandler;
import me.indian.bds.event.Listener;
import me.indian.bds.event.server.ExtensionDisableEvent;
import me.indian.discord.jda.DiscordJDA;
import me.indian.discord.jda.command.SlashCommandManager;

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