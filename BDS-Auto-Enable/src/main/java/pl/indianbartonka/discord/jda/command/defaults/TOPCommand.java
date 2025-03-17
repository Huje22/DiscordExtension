package pl.indianbartonka.discord.jda.command.defaults;

import java.awt.Color;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.MessageEmbed;
import net.dv8tion.jda.api.entities.emoji.Emoji;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.events.interaction.component.ButtonInteractionEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import net.dv8tion.jda.api.interactions.commands.build.Commands;
import net.dv8tion.jda.api.interactions.commands.build.SlashCommandData;
import net.dv8tion.jda.api.interactions.components.ActionRow;
import net.dv8tion.jda.api.interactions.components.buttons.Button;
import pl.indianbartonka.bds.BDSAutoEnable;
import pl.indianbartonka.bds.server.stats.ServerStats;
import pl.indianbartonka.bds.server.stats.StatsManager;
import pl.indianbartonka.bds.util.PlayerStatsUtil;
import pl.indianbartonka.bds.watchdog.module.pack.PackModule;
import pl.indianbartonka.discord.DiscordExtension;
import pl.indianbartonka.discord.core.command.SlashCommand;
import pl.indianbartonka.util.DateUtil;
import pl.indianbartonka.util.MessageUtil;

public class TOPCommand extends ListenerAdapter implements SlashCommand {

    private final BDSAutoEnable bdsAutoEnable;
    private final StatsManager statsManager;
    private final PackModule packModule;

    public TOPCommand(final DiscordExtension discordExtension) {
        this.bdsAutoEnable = discordExtension.getBdsAutoEnable();
        this.statsManager = this.bdsAutoEnable.getServerManager().getStatsManager();
        this.packModule = discordExtension.getBdsAutoEnable().getWatchDog().getPackModule();
    }

    @Override
    public void onExecute(final SlashCommandInteractionEvent event) {
        if (!this.packModule.isLoaded()) {
            event.getHook().editOriginal("Paczka **" + this.packModule.getPackName() + "** nie została załadowana").queue();
            return;
        }

        final EmbedBuilder embed = new EmbedBuilder().setTitle("Statystyki graczy").setColor(Color.BLUE);


        event.getHook().editOriginalEmbeds(embed.build())
                .setActionRow(ActionRow.of(
                                Button.primary("playtime", "Czas gry").withEmoji(Emoji.fromFormatted("<a:animated_clock:562493945058164739>")),
                                Button.primary("deaths", "Śmierci").withEmoji(Emoji.fromUnicode("☠️")),
                                Button.primary("blocks", "Bloki").withEmoji(Emoji.fromFormatted("<:kilof:1064228602759102464>")))
                        .getComponents())
                .queue();
    }

    @Override
    public void onButtonInteraction(final ButtonInteractionEvent event) {
        switch (event.getComponentId()) {
            case "playtime" -> event.getHook().editOriginalEmbeds(this.getPlaytimeEmbed()).queue();
            case "deaths" -> event.getHook().editOriginalEmbeds(this.getDeathsEmbed()).queue();
            case "blocks" -> event.getHook().editOriginalEmbeds(this.getTopBlockEmbed()).queue();
        }
    }

    private MessageEmbed getPlaytimeEmbed() {
        final List<String> playTime = PlayerStatsUtil.getTopPlayTime(true, 50);
        final ServerStats serverStats = this.bdsAutoEnable.getServerManager().getStatsManager().getServerStats();
        final String totalUpTime = "Łączny czas działania servera: " + DateUtil.formatTimeDynamic(serverStats.getTotalUpTime());

        return new EmbedBuilder()
                .setTitle("Top 50 graczy z największą ilością przegranego czasu")
                .setDescription((playTime.isEmpty() ? "**Brak Danych**" : MessageUtil.listToNewLineString(playTime)))
                .setColor(Color.BLUE)
                .setFooter(totalUpTime)
                .build();
    }

    private MessageEmbed getDeathsEmbed() {
        final List<String> deaths = PlayerStatsUtil.getTopDeaths(true, 50);
        return new EmbedBuilder()
                .setTitle("Top 50 graczy z największą ilością śmierci")
                .setDescription((deaths.isEmpty() ? "**Brak Danych**" : MessageUtil.listToNewLineString(deaths)))
                .setColor(Color.BLUE)
                .build();
    }

    public MessageEmbed getTopBlockEmbed() {
        final EmbedBuilder embed = new EmbedBuilder()
                .setTitle("Top 25 wykopanych i postawionych bloków")
                .setColor(Color.BLUE);

        final Map<String, Long> brokenMap = this.statsManager.getBlockBroken();
        final Map<String, Long> placedMap = this.statsManager.getBlockPlaced();

        final Map<String, Long> combinedMap = new HashMap<>(brokenMap);

        placedMap.forEach((key, value) ->
                combinedMap.merge(key, value, Long::sum)
        );

        final List<Map.Entry<String, Long>> sortedCombinedEntries = combinedMap.entrySet().stream()
                .sorted(Collections.reverseOrder(Map.Entry.comparingByValue()))
                .limit(25)
                .toList();

        int place = 1;
        int counter = 0;
        for (final Map.Entry<String, Long> entry : sortedCombinedEntries) {
            if (counter != 25) {
                embed.addField(place + ". " + entry.getKey(), "> Wykopane: **" + brokenMap.getOrDefault(entry.getKey(), 0L) + "** \n" +
                                "> Postawione: **" + placedMap.getOrDefault(entry.getKey(), 0L) + "**",
                        true);

                counter++;
            }
            place++;
        }
        return embed.build();
    }

    @Override
    public SlashCommandData getCommand() {
        return Commands.slash("top", "Topka graczy w różnych kategoriach");
    }
}