package me.indian.discord.command;

import me.indian.bds.command.Command;
import me.indian.discord.DiscordExtension;
import me.indian.discord.core.manager.ILinkingManager;

public class UnlinkCommand extends Command {

    private final ILinkingManager linkingManager;

    public UnlinkCommand(final DiscordExtension discordExtension) {
        super("unlink", "Rozłącza konto Discord z kontem nickiem Minecraft");

        this.addOption("[player]");
        this.linkingManager = discordExtension.getDiscordJDA().getLinkingManager();
    }

    @Override
    public boolean onExecute(final String[] args, final boolean isOp) {
        if (args.length == 0) {
            if (this.player == null) {
                this.sendMessage("&cPolecenie jest tylko dla graczy");
                return true;
            }
            if (this.linkingManager.isLinked(this.player.getPlayerName())) {
                if (!this.linkingManager.isCanUnlink() && !isOp) {
                    this.sendMessage("&cNie możesz sobie sam rozłączyć kont");
                    return true;
                }
                this.linkingManager.unLinkAccount(this.player.getPlayerName());
                this.sendMessage("Rozłączono konto " + this.player.getPlayerName());
            } else {
                this.sendMessage("&cNie posiadasz połączonych kont!");
            }
        } else {
            if (!isOp) {
                this.sendMessage("&cNie masz permisji!");
                return true;
            }
            final String target = args[0];
            if (this.linkingManager.isLinked(target)) {
                this.linkingManager.unLinkAccount(target);
                this.sendMessage("Rozłączono konto " + target);
            } else {
                this.sendMessage("&cGracz &b" + target + "&c nie posiada połączonych kont!");
            }
        }
        return true;
    }
}