package me.indian.discord.jda.command;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import me.indian.bds.extension.Extension;
import me.indian.discord.DiscordExtension;
import me.indian.discord.core.command.SlashCommand;
import me.indian.discord.jda.command.defaults.AllowlistCommand;
import me.indian.discord.jda.command.defaults.BackupCommand;
import me.indian.discord.jda.command.defaults.CmdCommand;
import me.indian.discord.jda.command.defaults.DifficultyCommand;
import me.indian.discord.jda.command.defaults.LinkingCommand;
import me.indian.discord.jda.command.defaults.ListCommand;
import me.indian.discord.jda.command.defaults.PingCommand;
import me.indian.discord.jda.command.defaults.PlayerInfoCommand;
import me.indian.discord.jda.command.defaults.ServerCommand;
import me.indian.discord.jda.command.defaults.StatsCommand;
import me.indian.discord.jda.command.defaults.TOPCommand;
import me.indian.discord.jda.command.defaults.UnLinkCommand;
import me.indian.discord.jda.command.defaults.VersionCommand;
import me.indian.util.MessageUtil;
import me.indian.util.logger.Logger;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.events.interaction.component.ButtonInteractionEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import net.dv8tion.jda.api.interactions.commands.build.CommandData;

public class SlashCommandManager extends ListenerAdapter {

    private final DiscordExtension discordExtension;
    private final Logger logger;
    private final HashMap<SlashCommand, Extension> commands;

    public SlashCommandManager(final DiscordExtension discordExtension) {
        this.discordExtension = discordExtension;
        this.logger = discordExtension.getLogger();
        this.commands = new LinkedHashMap<>();

        this.addCommand(new AllowlistCommand(discordExtension), discordExtension);
        this.addCommand(new BackupCommand(discordExtension), discordExtension);
        this.addCommand(new CmdCommand(discordExtension), discordExtension);
        this.addCommand(new DifficultyCommand(discordExtension), discordExtension);
//        addCommand(new IPCommand(discordExtension), discordExtension);
        this.addCommand(new LinkingCommand(discordExtension), discordExtension);
        this.addCommand(new ListCommand(discordExtension), discordExtension);
        this.addCommand(new PingCommand(discordExtension), discordExtension);
        this.addCommand(new PlayerInfoCommand(discordExtension), discordExtension);
        this.addCommand(new ServerCommand(), discordExtension);
        this.addCommand(new StatsCommand(discordExtension), discordExtension);
        this.addCommand(new TOPCommand(discordExtension), discordExtension);
        this.addCommand(new UnLinkCommand(discordExtension), discordExtension);
        this.addCommand(new VersionCommand(discordExtension), discordExtension);

        this.registerCommands();
    }

    public void addCommand(final SlashCommand slashCommand, final Extension extension) {
        if (this.commands.size() == 100) {
            this.logger.error("&cOsiągnięto limit poleceń slash&b 100");
            return;
        }

        if (extension == null) throw new NullPointerException("Rozszerzenie nie może być nullem!");
        if (slashCommand.getCommand() == null)
            throw new NullPointerException("'SlashCommandData' nie może zwracać nulla!");

        this.commands.put(slashCommand, extension);
        this.logger.debug("Dodano&1 " + slashCommand.getClass().getSimpleName());

        if (ListenerAdapter.class.isAssignableFrom(slashCommand.getClass())) {
            this.discordExtension.getDiscordJDA().getJda().addEventListener(slashCommand);
        }
    }

    public void registerCommands() {
        final List<CommandData> commandDataCollection = new LinkedList<>();

        for (final SlashCommand slashCommand : this.commands.keySet()) {
            commandDataCollection.add(slashCommand.getCommand());
        }

        if (!commandDataCollection.isEmpty()) {
            this.discordExtension.getDiscordJDA()
                    .getGuild().updateCommands().addCommands(commandDataCollection).queue();
        }
    }

    public void unregister(final Extension extension) {
        final List<SlashCommand> slashCommandsToRemove = new ArrayList<>();

        for (final Map.Entry<SlashCommand, Extension> entry : this.commands.entrySet()) {
            final SlashCommand slashCommand = entry.getKey();
            final Extension ex = entry.getValue();

            if (ex == extension) {
                slashCommandsToRemove.add(slashCommand);
            }
        }

        slashCommandsToRemove.forEach(this.commands::remove);
    }

    @Override
    public void onSlashCommandInteraction(final SlashCommandInteractionEvent event) {
        event.deferReply().setEphemeral(true).queue();

        try {
            for (final SlashCommand slashCommand : this.commands.keySet()) {
                if (event.getName().equals(slashCommand.getCommand().getName())) {
                    slashCommand.onExecute(event);
                    return;
                }
            }

            event.reply("Polecenie **" + event.getName() + "** nie jest jeszcze przez nas obsługiwane").setEphemeral(true).queue();
        } catch (final Exception exception) {
            this.logger.error("Wystąpił błąd przy próbie wykonania&b " + event.getName() + "&r przez&e " + event.getMember().getEffectiveName(), exception);
            event.getHook().editOriginal("Wystąpił błąd\n ```" + MessageUtil.getStackTraceAsString(exception) + "```").queue();
        }
    }

    @Override
    public void onButtonInteraction(final ButtonInteractionEvent event) {
        event.deferReply().setEphemeral(true).queue();
    }
}