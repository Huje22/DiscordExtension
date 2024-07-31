package me.indian.discord.jda.listener;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import me.indian.bds.BDSAutoEnable;
import me.indian.bds.logger.ConsoleColors;
import me.indian.bds.logger.Logger;
import me.indian.bds.server.ServerManager;
import me.indian.bds.server.ServerProcess;
import me.indian.bds.util.DateUtil;
import me.indian.bds.util.MessageUtil;
import me.indian.bds.util.ServerUtil;
import me.indian.discord.DiscordExtension;
import me.indian.discord.core.config.DiscordConfig;
import me.indian.discord.core.config.LinkingConfig;
import me.indian.discord.core.config.MessagesConfig;
import me.indian.discord.core.embed.component.Footer;
import me.indian.discord.core.listener.JDAListener;
import me.indian.discord.core.manager.ILinkingManager;
import me.indian.discord.jda.DiscordJDA;
import net.dv8tion.jda.api.OnlineStatus;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.Role;
import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.entities.channel.concrete.TextChannel;
import net.dv8tion.jda.api.entities.channel.middleman.GuildChannel;
import net.dv8tion.jda.api.events.message.MessageDeleteEvent;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import net.dv8tion.jda.api.events.message.MessageUpdateEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import net.dv8tion.jda.api.utils.cache.CacheFlag;

public class MessageListener extends ListenerAdapter implements JDAListener {

    private final DiscordExtension discordExtension;
    private final DiscordJDA discordJDA;
    private final BDSAutoEnable bdsAutoEnable;
    private final Logger logger;
    private final DiscordConfig discordConfig;
    private final MessagesConfig messagesConfig;
    private final Lock sendToMinecraftLock;
    private final ServerProcess serverProcess;
    private final Map<Long, Message> messagesMap;
    private TextChannel textChannel;

    public MessageListener(final DiscordExtension discordExtension) {
        this.discordExtension = discordExtension;
        this.discordJDA = this.discordExtension.getDiscordJDA();
        this.bdsAutoEnable = discordExtension.getBdsAutoEnable();
        this.logger = discordExtension.getLogger();
        this.discordConfig = discordExtension.getConfig();
        this.messagesConfig = discordExtension.getMessagesConfig();
        this.sendToMinecraftLock = new ReentrantLock();
        this.serverProcess = this.bdsAutoEnable.getServerProcess();
        this.messagesMap = new HashMap<>();
    }

    @Override
    public void init() {
        this.textChannel = this.discordJDA.getTextChannel();
    }

    @Override
    public void onMessageDelete(final MessageDeleteEvent event) {
        if (event.getChannel() instanceof final TextChannel channel) {
            if (channel == this.textChannel) {
                final Message message = this.getMessage(event.getMessageIdLong());

                if (message != null) {
                    final User author = message.getAuthor();

                    this.discordJDA.log("Usunięto wiadomość",
                            "```" + message.getContentRaw() + "```",
                            new Footer(author.getEffectiveName(), author.getEffectiveAvatarUrl()));
                }
            }
        }
    }

    @Override
    public void onMessageUpdate(final MessageUpdateEvent event) {
        if (event.getAuthor().equals(this.discordJDA.getJda().getSelfUser())) return;

        if (event.getChannel() instanceof final TextChannel channel) {
            if (channel == this.textChannel) {
                final Member member = event.getMember();
                final Message message = event.getMessage();
                if (member == null) return;

                final Message oldMessage = this.getMessage(event.getMessageIdLong());

                if (oldMessage != null) {
                    //TODO: Dodac jakos info o obrazach
                    this.discordJDA.log("Edytowano wiadomość",
                            "```" + oldMessage.getContentRaw() + "```" +
                                    " ↓↓↓↓↓↓" +
                                    "```" + message.getContentRaw() + "```" +
                                    "\t-------\n" +
                                    "\t[Skocz](" + message.getJumpUrl() + ")",
                            new Footer(member.getEffectiveName(), member.getEffectiveAvatarUrl()));
                }

                this.sendMessage(member, event.getAuthor(), message, true);
            }
        }
    }

    @Override
    public void onMessageReceived(final MessageReceivedEvent event) {
        if (event.getAuthor().equals(this.discordJDA.getJda().getSelfUser())) return;

        final Member member = event.getMember();
        final User author = event.getAuthor();
        final Message message = event.getMessage();
        final String rawMessage = message.getContentRaw();
        final ServerManager serverManager = this.bdsAutoEnable.getServerManager();
        final LinkingConfig linkingConfig = this.discordExtension.getLinkingConfig();

        if (member == null) return;

        final long id = member.getIdLong();

        if (event.getChannel() instanceof final TextChannel channel) {
            if (channel == this.textChannel) {
            /*
             Usuwanie wiadomości jeśli użytkownik ma status offline
             */

                if (this.discordJDA.isCacheFlagEnabled(CacheFlag.ONLINE_STATUS) && !member.hasPermission(Permission.MANAGE_CHANNEL)) {
                    final OnlineStatus memberStatus = member.getOnlineStatus();
                    if (memberStatus == OnlineStatus.OFFLINE || memberStatus == OnlineStatus.INVISIBLE) {
                        this.discordJDA.mute(member, 10, TimeUnit.SECONDS);
                        message.delete().queue();

                        this.discordJDA.log("Status aktywności offline",
                                "Wiadomość została usunięta z powodu statusu aktywności, jej treść to\n```" +
                                        rawMessage + "```",
                                new Footer(author.getName() + " " + DateUtil.getTimeHMS(), member.getEffectiveAvatarUrl()));

                        this.discordJDA.sendPrivateMessage(author, "Nie możesz wysyłać wiadomość na tym kanale " +
                                "gdy twój status aktywności to `" + memberStatus + "`");
                        return;
                    }
                }

            /*
              Usuwanie wiadomości jeśli użytkownik nie ma połączonych kont lub jest wyciszony
             */

                final ILinkingManager linkingManager = this.discordJDA.getLinkingManager();
                if (linkingManager != null) {
                    if (!linkingConfig.isCanType()) {
                        if (!linkingManager.isLinked(id) && !author.isBot()) {
                            this.discordJDA.mute(member, 10, TimeUnit.SECONDS);
                            message.delete().queue();

                            this.discordJDA.log("Brak połączonych kont",
                                    "Wiadomość została usunięta z powodu braku połączonych kont, jej treść to:\n```" +
                                            rawMessage + "```",
                                    new Footer(author.getName() + " " + DateUtil.getTimeHMS(), member.getEffectiveAvatarUrl()));

                            this.discordJDA.sendPrivateMessage(author, linkingConfig.getCantTypeMessage());
                            return;
                        }
                    }

                    if (serverManager.isMuted(linkingManager.getXuidByID(id))) {
                        this.discordJDA.mute(member, 10, TimeUnit.SECONDS);
                        message.delete().queue();
                        this.discordJDA.log("Wyciszenie w Minecraft",
                                "Wiadomość została usunięta z powodu wyciszenia w minecraft, jej treść to\n```" +
                                        rawMessage + "```",
                                new Footer(author.getName() + " " + DateUtil.getTimeHMS(), member.getEffectiveAvatarUrl()));
                        this.discordJDA.sendPrivateMessage(author, "Jesteś wyciszony!");
                        return;
                    }
                }

                this.sendMessage(member, author, message, false);
            }
        }
    }

    private void sendMessage(final Member member, final User author, final Message message, final boolean edited) {
        if (!this.messagesConfig.isSendDiscordToMinecraft() || this.isMaxLength(message)) return;
        this.sendToMinecraftLock.lock();

        try {
            final Role role = this.discordJDA.getHighestRole(author.getIdLong());
            String msg = this.messagesConfig.getDiscordToMinecraftMessage()
                    .replaceAll("<name>", this.discordJDA.getUserName(member, author))
                    .replaceAll("<msg>", this.generateRawMessage(message))
                    .replaceAll("<reply>", this.generatorReply(message.getReferencedMessage()))
                    .replaceAll("<role>", this.discordJDA.getColoredRole(role));

            if (edited) {
                msg += this.messagesConfig.getEdited();
            }
            if (message.isWebhookMessage()) {
                msg += this.messagesConfig.getWebhook();
            }

            msg = MessageUtil.fixMessage(msg);

            if (this.serverProcess.isEnabled()) ServerUtil.tellrawToAll(msg);
            this.logger.info(msg);
            this.discordJDA.writeConsole(ConsoleColors.removeColors(msg));
            this.addMessage(message.getIdLong(), message);
        } catch (final Exception exception) {
            this.logger.error("Nie udało się wysłać wiadomości z Discord do Minecraft", exception);
        } finally {
            this.sendToMinecraftLock.unlock();
        }
    }

    private boolean isMaxLength(final Message message) {
        if (!this.discordConfig.getBotConfig().isDeleteOnReachLimit()) return false;

        if (message.getContentRaw().length() >= this.discordConfig.getBotConfig().getAllowedLength()) {
            this.discordJDA.sendPrivateMessage(message.getAuthor(), this.discordConfig.getBotConfig().getReachedMessage());
            message.delete().queue();
            this.discordJDA.sendPrivateMessage(message.getAuthor(), "`" + message.getContentRaw() + "`");
            return true;
        }
        return false;
    }

    private String generateRawMessage(final Message message) {
        final List<Member> members = message.getMentions().getMembers();
        String rawMessage = MessageUtil.fixMessage(message.getContentRaw());

        if (!message.getAttachments().isEmpty())
            rawMessage += this.messagesConfig.getAttachment();
        if (members.isEmpty()) {
            for (final User user : message.getMentions().getUsers()) {
                if (user != null) {
                    rawMessage = rawMessage.replaceAll("<@" + user.getIdLong() + ">", "@" + this.discordJDA.getUserName(null, user));
                }
            }
        } else {
            for (final Member member : members) {
                if (member != null) {
                    rawMessage = rawMessage.replaceAll("<@" + member.getIdLong() + ">", "@" + this.discordJDA.getUserName(member, member.getUser()));
                }
            }
        }

        for (final GuildChannel guildChannel : message.getMentions().getChannels()) {
            if (guildChannel != null) {
                rawMessage = rawMessage.replaceAll("<#" + guildChannel.getIdLong() + ">", "#" + guildChannel.getName());
            }
        }

        for (final Role role : message.getMentions().getRoles()) {
            if (role != null) {
                rawMessage = rawMessage.replaceAll("<@&" + role.getIdLong() + ">", this.discordJDA.getColoredRole(role) + "&r");
            }
        }

        //Daje to aby określić czy wiadomość nadal jest pusta
        if (rawMessage.isEmpty()) rawMessage += message.getJumpUrl();

        return rawMessage;
    }

    private String generatorReply(final Message messageReference) {
        if (messageReference == null) return "";

        final Member member = messageReference.getMember();
        final User author = messageReference.getAuthor();

        final String replyStatement = this.messagesConfig.getReplyStatement()
                .replaceAll("<msg>", this.generateRawMessage(messageReference).replaceAll("\\*\\*", ""))
                .replaceAll("<author>", this.discordJDA.getUserName(member, author));

        if (author.equals(this.discordJDA.getJda().getSelfUser())) {
            return this.messagesConfig.getBotReplyStatement()
                    .replaceAll("<msg>", this.generateRawMessage(messageReference)
                            .replaceAll("\\*\\*", ""));
        }

        return replyStatement;
    }


    public void addMessage(final long id, final Message message) {
        if (this.messagesMap.size() == 500) {
            this.messagesMap.clear();
        }

        this.messagesMap.put(id, message);
    }

    public Message getMessage(final long id) {
        return this.messagesMap.get(id);
    }
}
