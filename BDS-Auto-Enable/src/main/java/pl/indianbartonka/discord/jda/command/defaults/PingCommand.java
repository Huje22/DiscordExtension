package pl.indianbartonka.discord.jda.command.defaults;

import java.awt.Color;
import pl.indianbartonka.discord.DiscordExtension;
import pl.indianbartonka.discord.core.command.SlashCommand;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.entities.MessageEmbed;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.interactions.commands.build.Commands;
import net.dv8tion.jda.api.interactions.commands.build.SlashCommandData;

public class PingCommand implements SlashCommand {

    private final JDA jda;

    public PingCommand(final DiscordExtension discordExtension) {
        this.jda = discordExtension.getDiscordJDA().getJda();
    }

    @Override
    public void onExecute(final SlashCommandInteractionEvent event) {


        final MessageEmbed embed = new EmbedBuilder()
                .setTitle("Ping Bot <-> Discord")
                .setDescription("Aktualny ping z serverami discord wynosi: " + this.jda.getGatewayPing() + " ms")
                .setColor(Color.BLUE)
                .build();

        event.getHook().editOriginalEmbeds(embed).queue();
    }

    @Override
    public SlashCommandData getCommand() {
        return Commands.slash("ping", "Aktualny ping bot z serwerami discord");
    }
}