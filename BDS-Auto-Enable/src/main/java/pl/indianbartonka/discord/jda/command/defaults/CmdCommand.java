package pl.indianbartonka.discord.jda.command.defaults;

import java.awt.Color;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.MessageEmbed;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.dv8tion.jda.api.interactions.commands.build.Commands;
import net.dv8tion.jda.api.interactions.commands.build.SlashCommandData;
import pl.indianbartonka.bds.server.ServerProcess;
import pl.indianbartonka.discord.DiscordExtension;
import pl.indianbartonka.discord.core.command.SlashCommand;
import pl.indianbartonka.discord.core.embed.component.Footer;
import pl.indianbartonka.discord.jda.DiscordJDA;

public class CmdCommand implements SlashCommand {

    private final DiscordExtension discordExtension;
    private final DiscordJDA discordJDA;
    private final ServerProcess serverProcess;

    public CmdCommand(final DiscordExtension discordExtension) {
        this.discordExtension = discordExtension;
        this.discordJDA = discordExtension.getDiscordJDA();
        this.serverProcess = discordExtension.getBdsAutoEnable().getServerProcess();
    }

    @Override
    public void onExecute(final SlashCommandInteractionEvent event) {
        final Member member = event.getMember();
        if (member == null) return;


        if (member.hasPermission(Permission.MANAGE_SERVER)) {
            if (!this.serverProcess.isEnabled()) {
                event.getHook().editOriginal("Server jest wyłączony").queue();
                return;
            }

            final String command = event.getOption("command").getAsString();
            if (command.isEmpty()) {
                event.getHook().editOriginal("Polecenie nie może być puste!").queue();
                return;
            }

            this.discordExtension.getLogger().print(command);
            this.discordJDA.writeConsole(command);

            final MessageEmbed embed = new EmbedBuilder()
                    .setTitle("Ostatnia linijka z konsoli")
                    .setDescription(this.serverProcess.commandAndResponse(command))
                    .setColor(Color.BLUE)
                    .setFooter("Używasz: " + command)
                    .build();

            this.discordJDA.log("Użycie polecenia",
                    "**" + member.getEffectiveName() + "** (" + member.getIdLong() + ")",
                    new Footer(command));

            event.getHook().editOriginalEmbeds(embed).queue();
        } else {
            event.getHook().editOriginal("Nie posiadasz permisji!!").queue();
        }
    }

    @Override
    public SlashCommandData getCommand() {
        return Commands.slash("cmd", "Wykonuje polecenie w konsoli.")
                .addOption(OptionType.STRING, "command", "Polecenie które zostanie wysłane do konsoli.", true);
    }
}