package pl.indianbartonka.discord.listener;

import pl.indianbartonka.bds.BDSAutoEnable;
import pl.indianbartonka.bds.event.EventHandler;
import pl.indianbartonka.bds.event.Listener;
import pl.indianbartonka.bds.event.watchdog.BackupDoneEvent;
import pl.indianbartonka.bds.event.watchdog.BackupFailEvent;
import pl.indianbartonka.discord.DiscordExtension;

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
