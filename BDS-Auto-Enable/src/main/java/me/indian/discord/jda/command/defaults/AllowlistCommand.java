package me.indian.discord.jda.command.defaults;

import java.awt.Color;
import java.util.ArrayList;
import java.util.List;
import me.indian.bds.BDSAutoEnable;
import me.indian.bds.server.ServerProcess;
import me.indian.bds.server.allowlist.AllowlistManager;
import me.indian.bds.server.allowlist.component.AllowlistPlayer;
import me.indian.util.MessageUtil;
import me.indian.discord.DiscordExtension;
import me.indian.discord.core.command.SlashCommand;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.interactions.commands.OptionMapping;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.dv8tion.jda.api.interactions.commands.build.Commands;
import net.dv8tion.jda.api.interactions.commands.build.SlashCommandData;

public class AllowlistCommand implements SlashCommand {

    private final BDSAutoEnable bdsAutoEnable;
    private final ServerProcess serverProcess;
    private final List<String> allowlistPlayers;

    public AllowlistCommand(final DiscordExtension discordExtension) {
        this.bdsAutoEnable = discordExtension.getBdsAutoEnable();
        this.serverProcess = this.bdsAutoEnable.getServerProcess();
        this.allowlistPlayers = new ArrayList<>();
    }

    @Override
    public void onExecute(final SlashCommandInteractionEvent event) {
        final Member member = event.getMember();
        if (member == null) return;


        final AllowlistManager allowlistManager = this.bdsAutoEnable.getAllowlistManager();
        final OptionMapping addOption = event.getOption("add");
        final OptionMapping removeOption = event.getOption("remove");

        if (!this.bdsAutoEnable.getServerProperties().isAllowList()) {
            event.getHook().sendMessage("Allowlista jest __wyłączona__").setEphemeral(true).queue();
            return;
        }

        if (addOption == null && removeOption == null) {
            final EmbedBuilder embedBuilder = new EmbedBuilder().setTitle("Biała lista").setColor(Color.BLUE);

            this.allowlistPlayers.clear();
            for (final AllowlistPlayer player : allowlistManager.getAllowlistPlayers()) {
                this.allowlistPlayers.add(player.name());
            }

            if (this.allowlistPlayers.isEmpty()) {
                embedBuilder.setDescription("**Nikt jeszcze nie jest na allowlist**");
            } else {
                embedBuilder.setDescription("Aktualnie na białej liście znajduje się **" + this.allowlistPlayers.size() + "** osób \n" +
                        MessageUtil.stringListToString(this.allowlistPlayers, " , "));
            }

            event.getHook().editOriginalEmbeds(embedBuilder.build()).queue();
            return;
        }

        if (!member.hasPermission(Permission.MODERATE_MEMBERS)) {
            event.getHook().editOriginal("Potrzebujesz uprawnienia: **" + Permission.MODERATE_MEMBERS.getName() + "**").queue();
            return;
        }

        if (addOption != null) {
            final String playerName = addOption.getAsString();
            if (allowlistManager.isOnAllowList(playerName)) {
                event.getHook().editOriginal("Gracz **" + playerName + "** jest już na liście").queue();
            } else {
                allowlistManager.addPlayerByName(playerName);
                allowlistManager.saveAllowlist();
                if (this.serverProcess.isEnabled()) {
                    allowlistManager.reloadAllowlist();
                }
                event.getHook().editOriginal("Dodano gracza **" + playerName + "**").queue();
            }
        } else if (removeOption != null) {
            final String playerName = removeOption.getAsString();
            if (allowlistManager.isOnAllowList(playerName)) {
                //Jeśli gracz jest na liście nie może być null :P
                final AllowlistPlayer player = allowlistManager.getPlayer(playerName);
                allowlistManager.removePlayer(player);
                allowlistManager.saveAllowlist();
                if (this.serverProcess.isEnabled()) {
                    allowlistManager.reloadAllowlist();
                }
                event.getHook().editOriginal("Usunięto gracza **" + playerName + "**").queue();
            } else {
                event.getHook().editOriginal("Gracz **" + playerName + "** nie jest na liście").queue();
            }
        }
    }

    @Override
    public SlashCommandData getCommand() {
        return Commands.slash("allowlist", "Zarządzanie białą listą.")
                .addOption(OptionType.STRING, "add", "Nazwa gracza do dodania", false)
                .addOption(OptionType.STRING, "remove", "Nazwa gracza do usunięcia", false);
    }
}