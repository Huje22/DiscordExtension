package me.indian.discord.command;

import me.indian.bds.command.Command;
import me.indian.bds.util.MessageUtil;
import me.indian.discord.DiscordExtension;
import me.indian.discord.jda.DiscordJDA;
import net.dv8tion.jda.api.OnlineStatus;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.channel.concrete.TextChannel;
import net.dv8tion.jda.api.utils.cache.CacheFlag;

public class DiscordCommand extends Command {

    private final DiscordExtension discordExtension;
    private final DiscordJDA discordJDA;
    private final TextChannel textChannel;

    public DiscordCommand(final DiscordExtension discordExtension) {
        super("discord", "Komenda do zarządzania Discord ");
        this.discordExtension = discordExtension;
        this.discordJDA = discordExtension.getDiscordJDA();
        this.textChannel = this.discordJDA.getTextChannel();

        this.addOption("help", "Lista poleceń");
        this.addOption("reload", "Przeładowuje konfiguracje");
        this.addOption("online", "Osoby online mające dostęp do&1 #" + this.textChannel.getName());
        this.addOption("message <wiadomość>", "Wysyła wiadomość do Discord na kanał&1 #" + this.textChannel.getName());
        this.addOption("status <nowy status bota>", "Zmienia status bota");
    }

    @Override
    public boolean onExecute(final String[] args, final boolean isOp) {
        if (args.length == 0) return false;

        if (args[0].equalsIgnoreCase("help")) {
            this.buildHelp();
            return true;
        }

        if (args[0].equalsIgnoreCase("online")) {
            if (!this.discordJDA.isCacheFlagEnabled(CacheFlag.ONLINE_STATUS)) {
                this.sendMessage("&cFlaga&b " + CacheFlag.ONLINE_STATUS + "&c jest wyłączona , bot nie wie kto ma jaki status aktywności");
                return true;
            }

            this.sendMessage("&aOsoby online mające dostęp do&1 #" + this.textChannel.getName());
            for (final Member member : this.discordJDA.getAllChannelOnlineMembers(this.textChannel)) {
                final OnlineStatus onlineStatus = member.getOnlineStatus();
                this.sendMessage(this.discordJDA.getColoredRole(this.discordJDA.getHighestRole(member.getIdLong())) +
                        " &b" + this.discordJDA.getUserName(member, member.getUser()) +
                        "&d - " + this.discordJDA.getStatusColor(member.getOnlineStatus()) + onlineStatus +
                        " &6[&9" + MessageUtil.enumSetToString(member.getActiveClients(), " &a,&9 ") + "&6] "
                );
            }
            return true;
        }

        if (!isOp) {
            this.sendMessage("&cPotrzebujesz wyższych uprawnień");
            this.deniedSound();
            return true;
        }

        if (args[0].equalsIgnoreCase("reload")) {
            try {
                this.discordExtension.reloadConfigs();
                this.sendMessage("&aPrzeładowano pliki konfiguracyjne");
            } catch (final Exception exception) {
                this.discordExtension.getLogger().error("&cNie udało się przeładować configu", exception);
                this.sendMessage("&cNie udało się przeładować plików konfiguracyjnych");
            }
            return true;
        }

        if (args[0].equalsIgnoreCase("message")) {
            final String message = MessageUtil.buildMessageFromArgs(MessageUtil.removeFirstArgs(args));
            if (message.isEmpty()) {
                this.sendMessage("&cWiadomość nie może być pusta!");
                return true;
            }

            this.discordJDA.sendMessage(message);
            this.sendMessage("&aWysłano wiadomość:&b " + message);
            return true;
        }
        if (args[0].equalsIgnoreCase("status")) {
            final String message = MessageUtil.buildMessageFromArgs(MessageUtil.removeFirstArgs(args));
            if (message.isEmpty()) {
                this.sendMessage("&cStatus aktywności nie może być pusty!");
                return true;
            }

            this.discordJDA.setBotActivityStatus(message);
            this.sendMessage("&aUstawiono status aktywności bota na:&b " + message);
            return true;
        }

        return false;
    }
}