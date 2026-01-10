package pl.indianbartonka.discord.jda.command.defaults;

import java.awt.Color;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.MessageEmbed;
import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.interactions.commands.OptionMapping;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.dv8tion.jda.api.interactions.commands.build.Commands;
import net.dv8tion.jda.api.interactions.commands.build.SlashCommandData;
import pl.indianbartonka.bds.BDSAutoEnable;
import pl.indianbartonka.bds.server.stats.StatsManager;
import pl.indianbartonka.bds.util.ServerUtil;
import pl.indianbartonka.bds.watchdog.module.pack.PackModule;
import pl.indianbartonka.discord.DiscordExtension;
import pl.indianbartonka.discord.core.command.SlashCommand;
import pl.indianbartonka.discord.core.embed.component.Footer;
import pl.indianbartonka.discord.core.manager.ILinkingManager;
import pl.indianbartonka.discord.jda.DiscordJDA;
import pl.indianbartonka.util.DateUtil;
import pl.indianbartonka.util.MessageUtil;

public class LinkingCommand implements SlashCommand {

    private final DiscordExtension discordExtension;
    private final DiscordJDA discordJDA;
    private final BDSAutoEnable bdsAutoEnable;
    private final ILinkingManager linkingManager;
    private final StatsManager statsManager;
    private final PackModule packModule;

    public LinkingCommand(final DiscordExtension discordExtension) {
        this.discordExtension = discordExtension;
        this.discordJDA = discordExtension.getDiscordJDA();
        this.bdsAutoEnable = this.discordExtension.getBdsAutoEnable();
        this.linkingManager = this.discordJDA.getLinkingManager();
        this.statsManager = this.bdsAutoEnable.getServerManager().getStatsManager();
        this.packModule = this.bdsAutoEnable.getWatchDog().getPackModule();
    }

    @Override
    public void onExecute(final SlashCommandInteractionEvent event) {
        final Member member = event.getMember();
        if (member == null) return;
        final User author = member.getUser();


        if (!this.packModule.isLoaded()) {
            event.getHook().editOriginal("Paczka **" + this.packModule.getPackName() + "** nie została załadowana").queue();
            return;
        }
        final OptionMapping codeMapping = event.getOption("code");
        if (codeMapping != null && !codeMapping.getAsString().isEmpty()) {
            final String code = codeMapping.getAsString();
            final long id = member.getIdLong();

            final EmbedBuilder linkingEmbed = new EmbedBuilder().setTitle("Łączenie kont").setColor(Color.BLUE);

            if (this.linkingManager.isCanUnlink()) {
                linkingEmbed.setFooter("Aby rozłączyć konto wpisz /unlink");
            }

            if (this.linkingManager.isLinked(id)) {
                linkingEmbed.setDescription("Twoje konto jest już połączone z: **" + this.linkingManager.getNameByID(id) + "**" + this.hasEnoughHours(member));
                event.getHook().editOriginalEmbeds(linkingEmbed.build()).queue();
                return;
            }

            if (this.linkingManager.linkAccount(code, id)) {
                this.discordJDA.log("Połączenie kont",
                        "Użytkownik **" + author.getName() + "** połączył konta",
                        new Footer(author.getName() + " " + DateUtil.getTimeHMS(), member.getEffectiveAvatarUrl()));


                linkingEmbed.setDescription("Połączono konto z nickiem: **" + this.linkingManager.getNameByID(id) + "**" + this.hasEnoughHours(member));
                event.getHook().editOriginalEmbeds(linkingEmbed.build()).queue();
                ServerUtil.tellrawToPlayer(this.linkingManager.getNameByID(id),
                        "&aPołączono konto z ID:&b " + id);
            } else {
                event.getHook().editOriginal("Kod nie jest poprawny").queue();
            }
        } else {
            final List<String> linkedAccounts = this.getLinkedAccounts();
            String linkedAccsString = "Już **" + linkedAccounts.size() + "** osób połączyło konta\n" + MessageUtil.listToNewLineString(linkedAccounts);

            if (linkedAccsString.isEmpty()) {
                linkedAccsString = "**Brak połączonych kont**";
            }

            final MessageEmbed messageEmbed = new EmbedBuilder()
                    .setTitle("Osoby z połączonym kontami")
                    .setDescription(linkedAccsString)
                    .setColor(Color.BLUE)
                    .setFooter("Aby połączyć konto wpisz /link KOD")
                    .build();

            event.getHook().editOriginalEmbeds(messageEmbed).queue();
        }
    }

    private String hasEnoughHours(final Member member) {
        String hoursMessage = "";
        final long roleID = this.discordExtension.getLinkingConfig().getLinkedPlaytimeRoleID();
        final long hours = DateUtil.hoursFrom(this.bdsAutoEnable.getServerManager().getStatsManager()
                .getPlayTime(this.linkingManager.getNameByID(member.getIdLong())), TimeUnit.MILLISECONDS);

        if (hours < 24) {
            if (this.discordJDA.getJda().getRoleById(roleID) != null) {
                hoursMessage = "\nMasz za mało godzin gry aby otrzymać <@&" + roleID + "> (**" + hours + "** godzin gry)" +
                        "\nDostaniesz role gdy wbijesz **1** dzień gry";
            }
        }

        return hoursMessage;
    }

    private List<String> getLinkedAccounts() {
        final Map<Long, Long> linkedAccounts = this.linkingManager.getLinkedAccounts();
        final List<String> linked = new LinkedList<>();
        int place = 1;

        for (final Map.Entry<Long, Long> entry : linkedAccounts.entrySet()) {
            final String playerName = this.statsManager.getNameByXuid(entry.getKey());
            final long hours = DateUtil.hoursFrom(this.bdsAutoEnable.getServerManager().getStatsManager()
                    .getPlayTime(playerName), TimeUnit.MILLISECONDS);

            linked.add(place + ". **" + playerName + "**: " + entry.getValue() + " " + (hours < 5 ? "❌" : "✅"));
            place++;
        }

        return linked;
    }

    @Override
    public SlashCommandData getCommand() {
        return Commands.slash("link", "Łączy konto Discord z kontem nickiem Minecraft.")
                .addOption(OptionType.STRING, "code", "Kod aby połączyć konta", false);
    }
}
