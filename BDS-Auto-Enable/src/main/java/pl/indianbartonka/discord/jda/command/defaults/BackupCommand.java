package pl.indianbartonka.discord.jda.command.defaults;

import java.awt.Color;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedList;
import java.util.List;
import pl.indianbartonka.bds.BDSAutoEnable;
import pl.indianbartonka.util.logger.LogState;
import pl.indianbartonka.bds.server.ServerProcess;
import pl.indianbartonka.util.DateUtil;
import pl.indianbartonka.util.MathUtil;
import pl.indianbartonka.util.MessageUtil;
import pl.indianbartonka.bds.util.ServerUtil;
import pl.indianbartonka.bds.util.StatusUtil;
import pl.indianbartonka.util.ThreadUtil;
import pl.indianbartonka.bds.watchdog.module.BackupModule;
import pl.indianbartonka.discord.DiscordExtension;
import pl.indianbartonka.discord.core.command.SlashCommand;
import pl.indianbartonka.discord.jda.DiscordJDA;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.MessageEmbed;
import net.dv8tion.jda.api.entities.emoji.Emoji;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.events.interaction.component.ButtonInteractionEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import net.dv8tion.jda.api.interactions.commands.build.Commands;
import net.dv8tion.jda.api.interactions.commands.build.SlashCommandData;
import net.dv8tion.jda.api.interactions.components.ActionRow;
import net.dv8tion.jda.api.interactions.components.buttons.Button;

public class BackupCommand extends ListenerAdapter implements SlashCommand {

    private final DiscordJDA discordJDA;
    private final BDSAutoEnable bdsAutoEnable;
    private final ServerProcess serverProcess;
    private final BackupModule backupModule;
    private final List<Button> backupButtons;

    public BackupCommand(final DiscordExtension discordExtension) {
        this.discordJDA = discordExtension.getDiscordJDA();
        this.bdsAutoEnable = discordExtension.getBdsAutoEnable();
        this.serverProcess = this.bdsAutoEnable.getServerProcess();
        this.backupModule = this.bdsAutoEnable.getWatchDog().getBackupModule();
        this.backupButtons = new LinkedList<>();
    }

    @Override
    public void onExecute(final SlashCommandInteractionEvent event) {
        final Member member = event.getMember();
        if (member == null) return;


        if (!this.bdsAutoEnable.getAppConfigManager().getWatchDogConfig().getBackupConfig().isEnabled()) {
            event.getHook().editOriginal("Backupy są wyłączone")
                    .queue();
            return;
        }
        if (member.hasPermission(Permission.ADMINISTRATOR)) {
            event.getHook().editOriginalEmbeds(this.getBackupEmbed())
                    .setActionRow(ActionRow.of(this.backupButtons).getComponents())
                    .queue();
        } else {
            event.getHook().editOriginalEmbeds(this.getBackupEmbed()).queue();
        }
    }

    @Override
    public void onButtonInteraction(final ButtonInteractionEvent event) {
        final Member member = event.getMember();
        if (member == null) return;
        final String componentID = event.getComponentId();

        if (componentID.contains("delete") && componentID.contains("backup")) {
            if (!member.hasPermission(Permission.MANAGE_SERVER)) {
                event.getHook().editOriginal("Nie posiadasz permisji " + Permission.MANAGE_SERVER.getName()).queue();
                return;
            }
        }

        if (componentID.contains("backup")) {
            if (!this.handleDelete(event) && !this.handleBackup(event)) {
                event.getHook().editOriginal("Nie odsłużono akcji").queue();
            }
        }
    }

    private boolean handleBackup(final ButtonInteractionEvent event) {
        if (!event.getComponentId().equals("backup")) return false;

        new ThreadUtil("Backup Button").newThread(() -> {
            if (this.backupModule.isBackuping()) {
                event.getHook().editOriginal("Backup jest już robiony!").queue();
                return;
            }
            if (!this.serverProcess.isEnabled()) {
                event.getHook().editOriginal("Server jest wyłączony!").queue();
                return;
            }

            this.backupModule.backup();
            ThreadUtil.sleep((int) this.bdsAutoEnable.getAppConfigManager().getWatchDogConfig().getBackupConfig().getLastBackupTime() + 3);
            event.getHook().editOriginalEmbeds(this.getBackupEmbed())
                    .setActionRow(this.backupButtons).queue();
        }).start();

        return true;
    }

    private boolean handleDelete(final ButtonInteractionEvent event) {
        if (!event.getComponentId().contains("delete")) return false;

        for (final Path path : this.backupModule.getBackups()) {
            final String fileName = path.getFileName().toString();
            if (event.getComponentId().equals("delete_backup:" + fileName)) {
                try {
                    if (!Files.deleteIfExists(path)) {
                        event.getHook().editOriginal("Nie udało się usunąć backupa " + fileName).queue();
                        return true;
                    }
                    this.backupModule.getBackups().remove(path);
                    event.getHook().editOriginalEmbeds(this.getBackupEmbed())
                            .setActionRow(ActionRow.of(this.backupButtons).getComponents())
                            .queue();
                    ServerUtil.tellrawToAllAndLogger("&7[&bDiscord&7]",
                            "&aUżytkownik&b " + this.discordJDA.getUserName(event.getMember(), event.getUser()) +
                                    "&a usunął backup&b " + fileName + "&a za pomocą&e discord"
                            , LogState.INFO);

                    return true;
                } catch (final Exception exception) {
                    event.getHook().editOriginal("Nie udało się usunąć backupa " + fileName + " " + exception.getMessage()).queue();
                }
            }
        }

        return false;
    }


    private MessageEmbed getBackupEmbed() {
        final String backupStatus = "`" + this.backupModule.getStatus() + "`\n";
        final long gbSpace = MathUtil.bytesToGB(StatusUtil.availableDiskSpace());

        final List<String> description = new LinkedList<>();
        this.backupButtons.clear();
        this.backupButtons.add(Button.primary("backup", "Backup")
                .withEmoji(Emoji.fromFormatted("<:bds:1138355151258783745>")));

        for (final Path path : this.backupModule.getBackups()) {
            final String fileName = path.getFileName().toString();
            if (!Files.exists(path)) continue;
            if (!(this.backupButtons.size() == 5)) {
                this.backupButtons.add(Button.danger("delete_backup:" + fileName, "Usuń " + fileName)
                        .withEmoji(Emoji.fromUnicode("🗑️")));
            }

            description.add("Nazwa: `" + fileName + "` Rozmiar: `" + this.backupModule.getBackupSize(path.toFile()) + "`");
        }

        return new EmbedBuilder()
                .setTitle("Backup info")
                .setDescription("Status ostatniego backup: " + backupStatus +
                        "Następny backup za: `" + DateUtil.formatTimeDynamic(this.backupModule.calculateMillisUntilNextBackup()) + "`\n" +
                        (description.isEmpty() ? "**Brak dostępnych backup**" : "**Dostępne backupy**:\n" + MessageUtil.listToSpacedString(description) + "\n") +
                        (gbSpace < 2 ? "**Zbyt mało pamięci aby wykonać backup!**" : ""))
                .setColor(Color.BLUE)
                .build();
    }

    @Override
    public SlashCommandData getCommand() {
        return Commands.slash("backup", "Tworzenie bądź ostatni czas backupa");
    }
}