package pl.indianbartonka.discord.jda.command.defaults;

import java.awt.Color;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.components.actionrow.ActionRow;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.MessageEmbed;
import net.dv8tion.jda.api.entities.emoji.Emoji;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.events.interaction.component.ButtonInteractionEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import net.dv8tion.jda.api.interactions.commands.build.Commands;
import net.dv8tion.jda.api.interactions.commands.build.SlashCommandData;
import net.dv8tion.jda.api.components.buttons.Button;
import pl.indianbartonka.bds.BDSAutoEnable;
import pl.indianbartonka.bds.version.VersionManager;
import pl.indianbartonka.discord.DiscordExtension;
import pl.indianbartonka.discord.core.command.SlashCommand;
import pl.indianbartonka.util.ThreadUtil;

public class VersionCommand extends ListenerAdapter implements SlashCommand {

    private final BDSAutoEnable bdsAutoEnable;

    public VersionCommand(final DiscordExtension discordExtension) {
        this.bdsAutoEnable = discordExtension.getBdsAutoEnable();
    }

    @Override
    public void onExecute(final SlashCommandInteractionEvent event) {
        final Member member = event.getMember();
        if (member == null) return;


        final VersionManager versionManager = this.bdsAutoEnable.getVersionManager();
        final String current = versionManager.getLoadedVersion();
        final int protocol = versionManager.getLastKnownProtocol();
        String latest = versionManager.getLatestVersion();
        if (latest.equals("")) {
            latest = current;
        }

        final String checkLatest = (current.equals(latest) ? "`" + latest + "` (" + protocol + ")" : "`" + current + "` (" + protocol + ") (Najnowsza to: `" + latest + "`)");

        final MessageEmbed embed = new EmbedBuilder()
                .setTitle("Informacje o wersji")
                .setDescription("**Wersjia __BDS-Auto-Enable__**: `" + this.bdsAutoEnable.getProjectVersion() + "`\n" +
                        "**Wersjia Servera **: " + checkLatest + "\n")
                .setColor(Color.BLUE)
                .build();

        if (member.hasPermission(Permission.ADMINISTRATOR)) {
            Button button = Button.primary("update", "Update")
                    .withEmoji(Emoji.fromUnicode("\uD83D\uDD3C"));
            if (current.equals(latest)) {
                button = button.asDisabled();
            } else {
                button = button.asEnabled();
            }

            event.getHook().editOriginalEmbeds(embed).setComponents(ActionRow.of(button)).queue();
        } else {
            event.getHook().editOriginalEmbeds(embed).queue();
        }
    }

    @Override
    public void onButtonInteraction(final ButtonInteractionEvent event) {
        new ThreadUtil("Update button").newThread(() -> {
            if (event.getComponentId().equals("update")) {
                event.getHook().editOriginal("Server jest już prawdopodobnie aktualizowany , jeśli nie zajrzyj w konsole")
                        .queue();
                this.bdsAutoEnable.getVersionManager().getVersionUpdater().updateToLatest();
            }
        }).start();
    }

    @Override
    public SlashCommandData getCommand() {
        return Commands.slash("version", "Wersja BDS-Auto-Enable i severa, umożliwia update servera");
    }
}