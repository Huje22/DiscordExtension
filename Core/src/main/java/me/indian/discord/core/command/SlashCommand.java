package me.indian.discord.core.command;

import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.interactions.commands.build.SlashCommandData;

public interface SlashCommand {

    void onExecute(SlashCommandInteractionEvent event);

    SlashCommandData getCommand();
}
