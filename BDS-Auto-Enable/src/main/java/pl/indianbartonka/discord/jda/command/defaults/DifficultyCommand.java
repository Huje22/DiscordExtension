package pl.indianbartonka.discord.jda.command.defaults;

import java.awt.Color;
import java.util.LinkedList;
import java.util.List;
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
import pl.indianbartonka.bds.BDSAutoEnable;
import pl.indianbartonka.bds.server.ServerProcess;
import pl.indianbartonka.bds.server.properties.component.Difficulty;
import pl.indianbartonka.discord.DiscordExtension;
import pl.indianbartonka.discord.core.command.SlashCommand;

public class DifficultyCommand extends ListenerAdapter implements SlashCommand {

    private final BDSAutoEnable bdsAutoEnable;
    private final ServerProcess serverProcess;
    private final List<Button> difficultyButtons;

    public DifficultyCommand(final DiscordExtension discordExtension) {
        this.bdsAutoEnable = discordExtension.getBdsAutoEnable();
        this.serverProcess = this.bdsAutoEnable.getServerProcess();
        this.difficultyButtons = new LinkedList<>();
    }

    @Override
    public void onExecute(final SlashCommandInteractionEvent event) {
        final Member member = event.getMember();
        if (member == null) return;

        if (member.hasPermission(Permission.ADMINISTRATOR)) {
            event.getHook().editOriginalEmbeds(this.getDifficultyEmbed())
                    .setActionRow(ActionRow.of(this.difficultyButtons).getComponents())
                    .queue();
        } else {
            event.getHook().editOriginalEmbeds(this.getDifficultyEmbed()).queue();
        }
    }

    @Override
    public void onButtonInteraction(final ButtonInteractionEvent event) {
        switch (event.getComponentId()) {
            case "peaceful" -> {
                this.serverProcess.sendToConsole("difficulty " + Difficulty.PEACEFUL.getDifficultyName());
                this.bdsAutoEnable.getServerProperties().setDifficulty(Difficulty.PEACEFUL);
                event.getHook().editOriginalEmbeds(this.getDifficultyEmbed())
                        .setActionRow(ActionRow.of(this.difficultyButtons).getComponents())
                        .queue();
            }
            case "easy" -> {
                this.serverProcess.sendToConsole("difficulty " + Difficulty.EASY.getDifficultyName());
                this.bdsAutoEnable.getServerProperties().setDifficulty(Difficulty.EASY);
                event.getHook().editOriginalEmbeds(this.getDifficultyEmbed())
                        .setActionRow(ActionRow.of(this.difficultyButtons).getComponents())
                        .queue();
            }
            case "normal" -> {
                this.serverProcess.sendToConsole("difficulty " + Difficulty.NORMAL.getDifficultyName());
                this.bdsAutoEnable.getServerProperties().setDifficulty(Difficulty.NORMAL);
                event.getHook().editOriginalEmbeds(this.getDifficultyEmbed())
                        .setActionRow(ActionRow.of(this.difficultyButtons).getComponents())
                        .queue();
            }
            case "hard" -> {
                this.serverProcess.sendToConsole("difficulty " + Difficulty.HARD.getDifficultyName());
                this.bdsAutoEnable.getServerProperties().setDifficulty(Difficulty.HARD);
                event.getHook().editOriginalEmbeds(this.getDifficultyEmbed())
                        .setActionRow(ActionRow.of(this.difficultyButtons).getComponents())
                        .queue();
            }
        }
    }

    private MessageEmbed getDifficultyEmbed() {
        final Difficulty currentDifficulty = this.bdsAutoEnable.getServerProperties().getDifficulty();
        final int currentDifficultyId = this.bdsAutoEnable.getServerProperties().getDifficulty().getDifficultyId();
        this.difficultyButtons.clear();

        final Button peaceful = Button.primary("peaceful", "Pokojowy").withEmoji(Emoji.fromUnicode("☮️"));
        final Button easy = Button.primary("easy", "Łatwy").withEmoji(Emoji.fromFormatted("<:NOOB:717474733167476766>"));
        final Button normal = Button.primary("normal", "Normalny").withEmoji(Emoji.fromFormatted("<:bao_block_grass:1019717534976577617>"));
        final Button hard = Button.primary("hard", "Trudny").withEmoji(Emoji.fromUnicode("⚠️"));

        if (currentDifficultyId != 0) {
            this.difficultyButtons.add(peaceful);
        } else {
            this.difficultyButtons.add(peaceful.asDisabled());
        }

        if (currentDifficultyId != 1) {
            this.difficultyButtons.add(easy);
        } else {
            this.difficultyButtons.add(easy.asDisabled());
        }

        if (currentDifficultyId != 2) {
            this.difficultyButtons.add(normal);
        } else {
            this.difficultyButtons.add(normal.asDisabled());
        }

        if (currentDifficultyId != 3) {
            this.difficultyButtons.add(hard);
        } else {
            this.difficultyButtons.add(hard.asDisabled());
        }

        return new EmbedBuilder()
                .setTitle("Difficulty")
                .setDescription("Aktualny poziom trudności to: " + "`" + currentDifficulty.getDifficultyName() + "`")
                .setColor(Color.BLUE)
                .build();
    }

    @Override
    public SlashCommandData getCommand() {
        return Commands.slash("difficulty", "Zmienia poziom trudności");
    }
}