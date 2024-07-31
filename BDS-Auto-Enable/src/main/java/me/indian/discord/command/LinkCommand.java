package me.indian.discord.command;

import me.indian.bds.command.Command;
import me.indian.bds.util.MessageUtil;
import me.indian.discord.DiscordExtension;
import me.indian.discord.core.manager.ILinkingManager;

public class LinkCommand extends Command {

    private final DiscordExtension discordExtension;

    public LinkCommand(final DiscordExtension discordExtension) {
        super("link", "Połącz konta Discord i Minecraft");
        this.discordExtension = discordExtension;
    }

    @Override
    public boolean onExecute(final String[] args, final boolean isOp) {
        if (this.player == null) {
            this.sendMessage("&cPolecenie jest tylko dla graczy");
            return true;
        }

        final ILinkingManager linkingManager = this.discordExtension.getDiscordJDA().getLinkingManager();
        if (linkingManager == null) {
            this.sendMessage("&cCoś poszło nie tak , &bLinkingManager&c jest&4 nullem");
            return true;
        }

        if (linkingManager.isLinked(this.player.getPlayerName())) {
            this.sendMessage(
                    "&aTwoje konto jest już połączone z ID:&b " + linkingManager.getIdByName(this.player.getPlayerName()));
            return true;
        }

        final String code = MessageUtil.generateCode(6);
        linkingManager.addAccountToLink(this.player.getPlayerName(), code);
        this.sendMessage("&aTwój kod do połączenia konto to:&b " + code);
        this.sendMessage("&aUżyj na naszym discord&b /link&a aby go użyć");


        return true;
    }
}