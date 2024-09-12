package me.indian.discord.jda.command.defaults;

import java.awt.Color;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import me.indian.bds.BDSAutoEnable;
import me.indian.bds.player.Controller;
import me.indian.bds.player.DeviceOS;
import me.indian.bds.player.PlayerStatistics;
import me.indian.bds.server.stats.StatsManager;
import me.indian.util.DateUtil;
import me.indian.util.MessageUtil;
import me.indian.bds.util.geyser.GeyserUtil;
import me.indian.discord.DiscordExtension;
import me.indian.discord.core.command.SlashCommand;
import me.indian.discord.core.manager.ILinkingManager;
import me.indian.discord.jda.DiscordJDA;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.interactions.commands.OptionMapping;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.dv8tion.jda.api.interactions.commands.build.Commands;
import net.dv8tion.jda.api.interactions.commands.build.SlashCommandData;

public class PlayerInfoCommand implements SlashCommand {

    private final DiscordJDA discordJDA;
    private final BDSAutoEnable bdsAutoEnable;
    private final ILinkingManager linkingManager;
    private final StatsManager statsManager;

    public PlayerInfoCommand(final DiscordExtension discordExtension) {
        this.discordJDA = discordExtension.getDiscordJDA();
        this.bdsAutoEnable = discordExtension.getBdsAutoEnable();
        this.linkingManager = this.discordJDA.getLinkingManager();
        this.statsManager = this.bdsAutoEnable.getServerManager().getStatsManager();
    }

    @Override
    public void onExecute(final SlashCommandInteractionEvent event) {
        final Member member = event.getMember();
        if (member == null) return;


        final OptionMapping mention = event.getOption("player");
        if (mention != null) {
            final Member playerMember = mention.getAsMember();
            if (playerMember != null) {
                final String discordPlayerName = this.discordJDA.getUserName(playerMember, playerMember.getUser());
                final long id = playerMember.getIdLong();
                if (this.linkingManager.isLinked(id)) {
                    final String playerName = this.linkingManager.getNameByID(id);

                    if (playerName == null) {
                        event.getHook().editOriginal("Nie udało się pozyskać informacji na temat **" + discordPlayerName + "**").queue();
                        return;
                    }

                    final PlayerStatistics player = this.statsManager.getPlayer(playerName);

                    if (player == null) {
                        event.getHook().editOriginal("Nie udało się pozyskać informacji na temat **" + playerName + "**").queue();
                        return;
                    }

                    final long xuid = player.getXuid();
                    final EmbedBuilder embedBuilder = new EmbedBuilder()
                            .setTitle("Informacje o graczu " + this.linkingManager.getNameByID(id)).setColor(Color.BLUE);


                    embedBuilder.setThumbnail(GeyserUtil.getBedrockSkinHead(xuid));
                    embedBuilder.addField("Nick", playerName, true);
                    embedBuilder.addField("XUID", String.valueOf(xuid), true);

                    final DeviceOS deviceOS = player.getLastDevice();
                    final Controller controller = player.getLastController();

                    if (deviceOS != DeviceOS.UNKNOWN || controller != Controller.UNKNOWN) {
                        embedBuilder.addBlankField(false);
                        embedBuilder.addField("Platforma", deviceOS.toString(), true);
                        embedBuilder.addField("Kontroler", controller.toString(), true);
                    }

                    final List<String> oldNames = player.getOldNames();
                    if (oldNames != null && !oldNames.isEmpty()) {
                        embedBuilder.addField("Znany również jako", MessageUtil.stringListToString(oldNames, " ,"), false);
                    } else {
                        embedBuilder.addField("Znany również jako", "__Brak danych o innych nick__", false);
                    }

                    final long firstJoin = player.getFirstJoin();
                    final long lastJoin = player.getLastJoin();
                    final long lastQuit = player.getLastQuit();

                    if (firstJoin != 0 && firstJoin != -1) {
                        embedBuilder.addField("Pierwsze dołączenie", this.getTime(DateUtil.millisToLocalDateTime(firstJoin)), true);
                    }
                    if (lastJoin != 0 && lastJoin != -1) {
                        embedBuilder.addField("Ostatnie dołączenie", this.getTime(DateUtil.millisToLocalDateTime(lastJoin)), true);
                    }

                    if (lastQuit != 0 && lastQuit != -1) {
                        embedBuilder.addField("Ostatnie opuszczenie", this.getTime(DateUtil.millisToLocalDateTime(lastQuit)), true);
                    }

                    embedBuilder.addField("Login Streak", String.valueOf(player.getLoginStreak()), true);
                    embedBuilder.addField("Longest Login Streak", String.valueOf(player.getLoginStreak()), true);

                    embedBuilder.addField("Śmierci", String.valueOf(player.getDeaths()), false);
                    embedBuilder.addField("Czas gry", DateUtil.formatTimeDynamic(player.getPlaytime()), false);
                    embedBuilder.addField("Postawione bloki", String.valueOf(player.getBlockPlaced()), true);
                    embedBuilder.addField("Zniszczone bloki", String.valueOf(player.getBlockBroken()), true);

                    String footer = "";

                    if (this.bdsAutoEnable.getServerProperties().isAllowList()) {
                        if (this.bdsAutoEnable.getAllowlistManager().isOnAllowList(playerName)) {
                            footer = "Znajduje się na białej liście";
                        } else {
                            footer = "Nie znajduje się na białej liście";
                        }
                    }

                    embedBuilder.setFooter(footer, GeyserUtil.getBedrockSkinBody(xuid));
                    event.getHook().editOriginalEmbeds(embedBuilder.build()).queue();
                } else {
                    event.getHook()
                            .editOriginal("Użytkownik **" + discordPlayerName + "** nie posiada połączonych kont")
                            .queue();
                }
            } else {
                event.getHook().editOriginal("Podany gracz jest nieprawidłowy").queue();
            }
        }
    }

    public String getTime(final LocalDateTime localDateTime) {
        return localDateTime.format(DateTimeFormatter.ofPattern("dd-MM-yyyy\nHH:mm:ss"));
    }

    @Override
    public SlashCommandData getCommand() {
        return Commands.slash("playerinfo", "Informacje o danym graczu")
                .addOption(OptionType.MENTIONABLE, "player", "Gracz o którym mamy pozyskać info", true);
    }
}