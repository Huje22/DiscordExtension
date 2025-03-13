package pl.indianbartonka.discord.jda.command.defaults;

import java.awt.Color;
import java.util.List;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.interactions.commands.build.Commands;
import net.dv8tion.jda.api.interactions.commands.build.SlashCommandData;
import pl.indianbartonka.bds.BDSAutoEnable;
import pl.indianbartonka.bds.server.stats.StatsManager;
import pl.indianbartonka.bds.watchdog.module.pack.PackModule;
import pl.indianbartonka.discord.DiscordExtension;
import pl.indianbartonka.discord.core.command.SlashCommand;
import pl.indianbartonka.discord.core.config.sub.BotConfig;
import pl.indianbartonka.util.DateUtil;
import pl.indianbartonka.util.MessageUtil;

public class ListCommand implements SlashCommand {

    private final BDSAutoEnable bdsAutoEnable;
    private final BotConfig botConfig;
    private final StatsManager statsManager;
    private final PackModule packModule;

    public ListCommand(final DiscordExtension discordExtension) {
        this.botConfig = discordExtension.getConfig().getBotConfig();
        this.bdsAutoEnable = discordExtension.getBdsAutoEnable();
        this.statsManager = this.bdsAutoEnable.getServerManager().getStatsManager();
        this.packModule = this.bdsAutoEnable.getWatchDog().getPackModule();
    }

    @Override
    public void onExecute(final SlashCommandInteractionEvent event) {
        final Member member = event.getMember();
        if (member == null) return;


        if (!this.packModule.isLoaded()) {
            event.getHook().editOriginal("Paczka **" + this.packModule.getPackName() + "** nie została załadowana").queue();
            return;
        }

        final List<String> players = this.bdsAutoEnable.getServerManager().getOnlinePlayers();
        final String list = "`" + MessageUtil.stringListToString(players, "`, `") + "`";
        final int maxPlayers = this.bdsAutoEnable.getServerProperties().getMaxPlayers();
        final EmbedBuilder embed = new EmbedBuilder()
                .setTitle("Lista Graczy")
                .setColor(Color.BLUE);

        if (this.botConfig.isAdvancedPlayerList()) {
            if (!players.isEmpty()) {
                int counter = 0;

                embed.setDescription("Aktualnie gra **" + players.size() + "/" + maxPlayers + "** osób");
                for (final String player : players) {
                    if (counter != 24) {
                        embed.addField(player,
                                "> Czas gry: **" + DateUtil.formatTimeDynamic(this.statsManager.getPlayTime(player))
                                        + "**  \n> Śmierci:** " + this.statsManager.getDeaths(player) + "**", true);
                        counter++;
                    } else {
                        embed.addField("**I pozostałe**", players.size() - 24 + " osób", false);
                        break;
                    }
                }
            } else {
                embed.setDescription("**Brak osób online**");
            }
        } else {
            embed.setDescription(players.size() + "/" + maxPlayers + "\n" + (players.isEmpty() ? "**Brak osób online**" : list) + "\n");
        }
        event.getHook().editOriginalEmbeds(embed.build()).queue();
    }

    @Override
    public SlashCommandData getCommand() {
        return Commands.slash("list", "lista graczy online.");
    }
}