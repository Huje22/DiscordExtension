package pl.indianbartonka.discord.jda.command.defaults;

import java.awt.Color;
import pl.indianbartonka.discord.core.command.SlashCommand;
import pl.indianbartonka.util.BedrockQuery;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.MessageEmbed;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.interactions.commands.OptionMapping;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.dv8tion.jda.api.interactions.commands.build.Commands;
import net.dv8tion.jda.api.interactions.commands.build.SlashCommandData;

public class ServerCommand implements SlashCommand {

    @Override
    public void onExecute(final SlashCommandInteractionEvent event) {
        final OptionMapping portOption = event.getOption("port");
        final String adres = event.getOption("ip").getAsString();
        int port = 19132;

        if (portOption != null) port = portOption.getAsInt();


        event.getHook().editOriginalEmbeds(this.getServerInfoEmbed(adres, port)).queue();
    }

    private MessageEmbed getServerInfoEmbed(final String address, final int port) {
        final BedrockQuery query = BedrockQuery.create(address, port);
        final EmbedBuilder embedBuilder = new EmbedBuilder()
                .setTitle("Server info")
                .setFooter(address + ":" + port)
                .setColor(Color.BLUE);

        if (query.online()) {
            final int portV4 = query.portV4();
            final int portV6 = query.portV6();

            embedBuilder.addField("Ping", String.valueOf(query.responseTime()), false);
            embedBuilder.addField("Wersja Minecraft", query.minecraftVersion(), true);
            embedBuilder.addField("Protokół", String.valueOf(query.protocol()), true);
            embedBuilder.addField("MOTD", query.motd(), false);
            embedBuilder.addField("Nazwa Mapy", query.mapName(), false);
            embedBuilder.addField("Gracz online", String.valueOf(query.playerCount()), true);
            embedBuilder.addField("Maksymalna ilość graczy", String.valueOf(query.maxPlayers()), true);
            embedBuilder.addField("Tryb Gry", query.gamemode(), false);
            embedBuilder.addField("Edycja", query.edition(), true);

            if (portV4 != -1) {
                embedBuilder.addField("Port v4", String.valueOf(portV4), true);
            }
            if (portV6 != -1) {
                embedBuilder.addField("Port v6", String.valueOf(portV6), true);
            }
        } else {
            embedBuilder.setDescription("Nie można uzyskać informacji o serwerze ``" + address + ":" + port + "``");
        }

        return embedBuilder.build();
    }

    @Override
    public SlashCommandData getCommand() {
        return Commands.slash("server", "Informacje o danym serwerze")
                .addOption(OptionType.STRING, "ip", "Adres IP servera", true)
                .addOption(OptionType.INTEGER, "port", "Port servera", false);
    }
}