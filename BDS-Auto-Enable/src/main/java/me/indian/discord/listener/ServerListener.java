package me.indian.discord.listener;

import java.util.LinkedList;
import java.util.List;
import me.indian.bds.event.EventHandler;
import me.indian.bds.event.Listener;
import me.indian.bds.event.server.ServerAlertEvent;
import me.indian.bds.event.server.ServerClosedEvent;
import me.indian.bds.event.server.ServerRestartEvent;
import me.indian.bds.event.server.ServerStartEvent;
import me.indian.bds.event.server.ServerUncaughtExceptionEvent;
import me.indian.bds.event.server.ServerUpdatingEvent;
import me.indian.bds.event.server.TPSChangeEvent;
import me.indian.bds.logger.LogState;
import me.indian.bds.util.MessageUtil;
import me.indian.discord.DiscordExtension;
import me.indian.discord.core.embed.component.Field;
import me.indian.discord.core.embed.component.Footer;
import me.indian.discord.core.manager.IStatsChannelsManager;
import me.indian.discord.jda.DiscordJDA;

public class ServerListener implements Listener {

    private final DiscordJDA discordJDA;
    private double tps, lastTPS;

    public ServerListener(final DiscordExtension discordExtension) {
        this.discordJDA = discordExtension.getDiscordJDA();
    }

    @EventHandler
    private void onServerStart(final ServerStartEvent event) {
        this.discordJDA.sendEnabledMessage();
    }

    @EventHandler
    private void onTpsChange(final TPSChangeEvent event) {
        this.tps = event.getTps();
        this.lastTPS = event.getLastTps();

        final IStatsChannelsManager statsChannelsManager = this.discordJDA.getStatsChannelsManager();
        if (statsChannelsManager != null) {
            statsChannelsManager.setTpsCount(this.tps);
        }

        if (this.tps <= 8) this.discordJDA.sendMessage("Server posiada: **" + this.tps + "** TPS");
    }

    @EventHandler
    private void onServerRestart(final ServerRestartEvent event) {
        final String reason = event.getReason();

        if (reason == null) return;
        if (reason.contains("Niska ilość tps")) {
            this.discordJDA.sendMessage("Zaraz nastąpi restartowanie servera z powodu niskiej ilości TPS"
                    + " (Teraz: **" + this.tps + "** Ostatnie: **" + this.lastTPS + "**)");
        } else {
            this.discordJDA.sendMessage("Zaraz nastąpi restartowanie servera z powodu: **" + reason + "**");
        }
    }

    @EventHandler
    private void onServerClose(final ServerClosedEvent event) {
        this.discordJDA.sendDisabledMessage();
    }

    @EventHandler
    private void onServerAlert(final ServerAlertEvent event) {
        final List<Field> fieldList = new LinkedList<>();
        final String additionalInfo = event.getAdditionalInfo();
        final Throwable throwable = event.getThrowable();
        final LogState state = event.getAlertState();

        if (throwable != null) {
            fieldList.add(new Field("Wyjątek", "```" + MessageUtil.getStackTraceAsString(throwable) + "```", false));
        }

        if (state == LogState.INFO || state == LogState.NONE) {
            this.discordJDA.sendMessage(event.getMessage() + "\n" + (additionalInfo == null ? "" : additionalInfo));
        } else {
            String footerInfo = "";

            if (additionalInfo == null || additionalInfo.isEmpty()) {
                footerInfo = (throwable == null ? "" : throwable.getLocalizedMessage());
            } else {
                footerInfo = additionalInfo;
            }

            this.discordJDA.log("Alert " + state, event.getMessage(), fieldList, new Footer(footerInfo));
        }
    }

    @EventHandler
    private void onServerUpdating(final ServerUpdatingEvent event) {
        this.discordJDA.sendServerUpdateMessage(event.getVersion());
    }

    @EventHandler
    private void onServerUncaughtException(final ServerUncaughtExceptionEvent event) {
        this.discordJDA.log("Niezłapany wyjątek", "**Wykryto niezłapany wyjątek**",
                List.of(new Field("Wystąpił w wątku", event.getThread().getName(), true),
                        new Field("Wyjątek", "```" + MessageUtil.getStackTraceAsString(event.getThrowable()) + "```", false)),
                new Footer(""));
    }
}
