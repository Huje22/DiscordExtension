package pl.indianbartonka.discord.jda.manager;

import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.TimeUnit;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.channel.concrete.VoiceChannel;
import pl.indianbartonka.bds.BDSAutoEnable;
import pl.indianbartonka.discord.DiscordExtension;
import pl.indianbartonka.discord.core.config.StatsChannelsConfig;
import pl.indianbartonka.discord.core.manager.IStatsChannelsManager;
import pl.indianbartonka.discord.jda.DiscordJDA;
import pl.indianbartonka.util.DateUtil;
import pl.indianbartonka.util.logger.Logger;

public class StatsChannelsManager implements IStatsChannelsManager {

    private final BDSAutoEnable bdsAutoEnable;
    private final Logger logger;
    private final StatsChannelsConfig statsChannelsConfig;
    private final Timer timer;
    private final Guild guild;
    private final long onlinePlayersID, tpsID;
    private double latsTPS;
    private VoiceChannel onlinePlayersChannel, tpsChannel;

    public StatsChannelsManager(final DiscordExtension discordExtension, final DiscordJDA DiscordJDA) {
        this.bdsAutoEnable = discordExtension.getBdsAutoEnable();
        this.logger = discordExtension.getLogger();
        this.statsChannelsConfig = discordExtension.getStatsChannelsConfig();
        this.timer = new Timer("Discord Channel Manager Timer", true);
        this.onlinePlayersID = this.statsChannelsConfig.getOnlinePlayersID();
        this.tpsID = this.statsChannelsConfig.getTpsID();
        this.guild = DiscordJDA.getGuild();

        this.latsTPS = 0;
    }

    public void init() {
        this.onlinePlayersChannel = this.guild.getVoiceChannelById(this.onlinePlayersID);
        this.tpsChannel = this.guild.getVoiceChannelById(this.tpsID);
        this.setOnlinePlayersCount();

        if (this.onlinePlayersChannel == null)
            this.logger.debug("(Gracz online) Nie można odnaleźć kanału głosowego z ID &b " + this.onlinePlayersID);
        if (this.tpsChannel == null)
            this.logger.debug("(TPS) Nie można odnaleźć kanału głosowego z ID &b " + this.onlinePlayersID);
    }

    @Override
    public void setTpsCount(final double tps) {
        if (this.tpsChannel != null) {
            if (tps == this.latsTPS) return;
            this.latsTPS = tps;

            this.tpsChannel.getManager().setName(this.statsChannelsConfig.getTpsName()
                            .replaceAll("<tps>", String.valueOf(tps)))
                    .queue();
        }
    }

    private void setOnlinePlayersCount() {
        if (this.onlinePlayersChannel != null) {

            final TimerTask onlinePlayersTask = new TimerTask() {
                int lastOnlinePlayers = -1;

                @Override
                public void run() {
                    if (!StatsChannelsManager.this.bdsAutoEnable.getServerProcess().isEnabled()) return;
                    final int onlinePlayers = StatsChannelsManager.this.bdsAutoEnable.getServerManager().getOnlinePlayers().size();
                    final int maxPlayers = StatsChannelsManager.this.bdsAutoEnable.getServerProperties().getMaxPlayers();

                    //Sprawdzam tak aby na darmo nie wysyłać requesta do discord
                    if (onlinePlayers == 0 && this.lastOnlinePlayers == 0) return;

                    this.lastOnlinePlayers = onlinePlayers;

                    StatsChannelsManager.this.onlinePlayersChannel.getManager().setName(StatsChannelsManager.this.statsChannelsConfig.getOnlinePlayersName()
                            .replaceAll("<online>", String.valueOf(onlinePlayers))
                            .replaceAll("<max>", String.valueOf(maxPlayers))
                    ).queue();
                }
            };

            this.timer.scheduleAtFixedRate(onlinePlayersTask,
                    DateUtil.minutesTo(1, TimeUnit.MILLISECONDS),
                    DateUtil.secondToMillis(30)
            );
        }
    }

    @Override
    public void onShutdown() {
        if (this.onlinePlayersChannel != null) {
            this.onlinePlayersChannel.getManager().setName("Offline").queue();
        }
        if (this.tpsChannel != null) {
            this.tpsChannel.getManager().setName("Offline").queue();
        }
    }
}
