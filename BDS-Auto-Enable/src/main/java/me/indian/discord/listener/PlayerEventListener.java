package me.indian.discord.listener;

import java.util.HashMap;
import java.util.Map;
import me.indian.bds.BDSAutoEnable;
import me.indian.bds.event.EventHandler;
import me.indian.bds.event.Listener;
import me.indian.bds.event.player.PlayerChatEvent;
import me.indian.bds.event.player.PlayerCommandEvent;
import me.indian.bds.event.player.PlayerDeathEvent;
import me.indian.bds.event.player.PlayerJoinEvent;
import me.indian.bds.event.player.PlayerQuitEvent;
import me.indian.bds.event.player.response.PlayerChatResponse;
import me.indian.bds.server.ServerProcess;
import me.indian.bds.util.DateUtil;
import me.indian.bds.util.MessageUtil;
import me.indian.bds.util.ServerUtil;
import me.indian.discord.DiscordExtension;
import me.indian.discord.core.config.LinkingConfig;
import me.indian.discord.core.config.MessagesConfig;
import me.indian.discord.core.embed.component.Footer;
import me.indian.discord.core.manager.ILinkingManager;
import me.indian.discord.jda.DiscordJDA;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.Role;

public class PlayerEventListener implements Listener {

    private final BDSAutoEnable bdsAutoEnable;
    private final ServerProcess serverProcess;
    private final MessagesConfig messagesConfig;
    private final DiscordJDA discordJDA;
    private final ILinkingManager linkingManager;
    private final LinkingConfig linkingConfig;
    private final Map<String, String> cachedPrefixes;

    public PlayerEventListener(final DiscordExtension discordExtension) {
        this.bdsAutoEnable = discordExtension.getBdsAutoEnable();
        this.serverProcess = this.bdsAutoEnable.getServerProcess();
        this.messagesConfig = discordExtension.getMessagesConfig();
        this.discordJDA = discordExtension.getDiscordJDA();
        this.linkingManager = this.discordJDA.getLinkingManager();
        this.linkingConfig = discordExtension.getLinkingConfig();
        this.cachedPrefixes = new HashMap<>();
    }

    @EventHandler
    private void onPlayerJoin(final PlayerJoinEvent event) {
        final String playerName = event.getPlayer().getPlayerName();
        this.discordJDA.sendJoinMessage(playerName.replaceAll("\"", ""));
        this.setPlayerPrefix(playerName);
    }

    @EventHandler
    private void onPlayerQuit(final PlayerQuitEvent event) {
        final String playerName = event.getPlayer().getPlayerName();
        this.discordJDA.sendLeaveMessage(playerName.replaceAll("\"", ""));
        this.cachedPrefixes.remove(playerName);
    }

    @EventHandler
    private PlayerChatResponse onPlayerChat(final PlayerChatEvent event) {
        final String playerName = event.getPlayer().getPlayerName();
        final String message = event.getMessage();
        final boolean appHandled = event.isAppHandled();

        if (event.isMuted() && appHandled) {
            this.discordJDA.log("Wyciszenie w Minecraft",
                    "Wiadomość została nie wysłana z powodu wyciszenia w Minecraft, jej treść to:\n```" +
                            message + "```",
                    new Footer(playerName.replaceAll("\"", "") + " " + DateUtil.getTimeHMS()));
            return null;
        }

        boolean memberMutedOnDiscord = false;
        String role = "";

        if (!event.isMuted() && this.messagesConfig.isFormatChat() && this.linkingManager.isLinked(playerName)) {
            final Member member = this.linkingManager.getMember(playerName);
            if (member != null) {
                memberMutedOnDiscord = member.isTimedOut();
                role = this.getRole(member, this.linkingConfig.isUseCustomRolesInChat());
                this.setPlayerPrefix(playerName);
            }
        }

        final String format = this.messagesConfig.getChatMessageFormat()
                .replaceAll("<player>", playerName.replaceAll("\"", ""))
                .replaceAll("<message>", event.getMessage())
                .replaceAll("<role>", role.trim());

        if (appHandled) {
            if (!event.isMuted() && !memberMutedOnDiscord) {
                this.discordJDA.sendPlayerMessage(playerName.replaceAll("\"", ""), message);
            }
            if (memberMutedOnDiscord) {
                ServerUtil.tellrawToPlayer(playerName, "&cZostałeś wyciszony na discord!");
                this.discordJDA.log("Wyciszenie na Discord",
                        "Wiadomość została usunięta z powodu wyciszenia na Discord, jej treść to:\n```" +
                                message + "```",
                        new Footer(playerName.replaceAll("\"", "") + " " + DateUtil.getTimeHMS()));
            }

            if (!event.isMuted() && this.messagesConfig.isFormatChat()) {
                return new PlayerChatResponse(format.replaceAll("\"", ""), memberMutedOnDiscord);
            }
        } else {
            this.discordJDA.sendPlayerMessage(playerName.replaceAll("\"", ""), message);
        }

        return null;
    }

    @EventHandler
    private void onPlayerDeath(final PlayerDeathEvent event) {
        this.discordJDA.sendDeathMessage(event.getPlayer().getPlayerName().replaceAll("\"", ""), event.getDeathMessage(),
                event.getKillerName(), event.getUsedItemName());
    }

    @EventHandler
    private void onPlayerCommandEvent(final PlayerCommandEvent playerCommandEvent) {
        this.discordJDA.log("Użycie polecenia",
                playerCommandEvent.getCommand()
                , new Footer(playerCommandEvent.getPlayer().getPlayerName()));
    }

    private void setPlayerPrefix(final String playerName) {
        if (this.linkingManager.isLinked(playerName)) {
            final Member member = this.linkingManager.getMember(playerName);
            if (member != null) {

                if (!this.messagesConfig.isShowInName()) return;
                final String cachedPrefix = this.cachedPrefixes.get(playerName);
                final String prefix = this.getRole(member, this.linkingConfig.isUseCustomRolesInName());

                if (cachedPrefix != null) {
                    if (!cachedPrefix.equals(prefix)) {
                        this.sendPrefixChange(playerName, prefix);
                        this.cachedPrefixes.put(playerName, prefix);
                    }
                } else {
                    this.sendPrefixChange(playerName, prefix);
                    this.cachedPrefixes.put(playerName, prefix);
                }
            }
        }
    }

    private void sendPrefixChange(final String playerName, final String prefix) {
        this.serverProcess.sendToConsole("scriptevent bds:tag_prefix " + playerName.replaceAll("\"", "") + "=" + MessageUtil.colorize(prefix) + " ");
    }

    private String getRole(final Member member, final boolean customRole) {
        final Role highestRole = this.discordJDA.getHighestRole(member.getIdLong());
        if (highestRole != null) {
            if (customRole) {
                final long highestAllowedID = this.getHighestFromAllowed(member);

                if (!this.linkingConfig.isOnlyCustomRoles()) {
                    if (highestAllowedID != highestRole.getIdLong()) {
                        return this.discordJDA.getColoredRole(highestRole);
                    }
                }

                final String roleIcon = this.getRoleIcon(highestAllowedID);
                return roleIcon == null ? "" : roleIcon;
            } else {
                return this.discordJDA.getColoredRole(highestRole);
            }
        }
        return "";
    }

    private long getHighestFromAllowed(final Member member) {
        for (final Role role : member.getRoles()) {
            if (this.linkingConfig.getCustomRoles().containsKey(role.getIdLong())) {
                return role.getIdLong();
            }
        }
        return -1;
    }

    private String getRoleIcon(final long roleID) {
        return this.linkingConfig.getCustomRoles().get(roleID);
    }
}
