package me.indian.discord.listener;

import me.indian.bds.BDSAutoEnable;
import me.indian.bds.event.EventHandler;
import me.indian.bds.event.Listener;
import me.indian.bds.event.watchdog.BackupDoneEvent;
import me.indian.bds.event.watchdog.BackupFailEvent;
import me.indian.discord.DiscordExtension;

public class BackupListener implements Listener {

    private final DiscordExtension discordExtension;
    private final BDSAutoEnable bdsAutoEnable;

    public BackupListener(final DiscordExtension discordExtension) {
        this.discordExtension = discordExtension;
        this.bdsAutoEnable = this.discordExtension.getBdsAutoEnable();
    }

    @EventHandler
    private void onBackupDone(final BackupDoneEvent event) {
        this.discordExtension.getDiscordJDA().sendBackupDoneMessage();
    }

    @EventHandler
    private void onBackupFail(final BackupFailEvent event) {
        this.discordExtension.getDiscordJDA().sendBackupFailMessage(event.getException());
    }
}
