package me.indian.discord.jda;

import java.awt.Color;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.TimeUnit;
import java.util.regex.Pattern;
import me.indian.bds.BDSAutoEnable;
import me.indian.bds.config.AppConfigManager;
import me.indian.bds.logger.ConsoleColors;
import me.indian.bds.logger.Logger;
import me.indian.bds.util.DateUtil;
import me.indian.bds.util.HTTPUtil;
import me.indian.bds.util.MathUtil;
import me.indian.bds.util.MessageUtil;
import me.indian.bds.watchdog.module.pack.PackModule;
import me.indian.discord.DiscordExtension;
import me.indian.discord.core.IDiscordJDA;
import me.indian.discord.core.config.DiscordConfig;
import me.indian.discord.core.config.MessagesConfig;
import me.indian.discord.core.config.ProximityVoiceChatConfig;
import me.indian.discord.core.config.sub.BotConfig;
import me.indian.discord.core.embed.component.Field;
import me.indian.discord.core.embed.component.Footer;
import me.indian.discord.core.listener.JDAListener;
import me.indian.discord.core.manager.ILinkingManager;
import me.indian.discord.core.manager.IStatsChannelsManager;
import me.indian.discord.jda.command.SlashCommandManager;
import me.indian.discord.jda.listener.GuildJoinListener;
import me.indian.discord.jda.listener.MentionPatternCacheListener;
import me.indian.discord.jda.listener.MessageListener;
import me.indian.discord.jda.manager.LinkingManager;
import me.indian.discord.jda.manager.StatsChannelsManager;
import me.indian.discord.jda.voice.ProximityVoiceChat;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.JDABuilder;
import net.dv8tion.jda.api.OnlineStatus;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.Activity;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.MessageEmbed;
import net.dv8tion.jda.api.entities.Role;
import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.entities.channel.concrete.Category;
import net.dv8tion.jda.api.entities.channel.concrete.TextChannel;
import net.dv8tion.jda.api.entities.channel.concrete.VoiceChannel;
import net.dv8tion.jda.api.entities.channel.unions.DefaultGuildChannelUnion;
import net.dv8tion.jda.api.requests.GatewayIntent;
import net.dv8tion.jda.api.utils.cache.CacheFlag;
import org.jetbrains.annotations.Nullable;

public class DiscordJDA implements IDiscordJDA {

    private final DiscordExtension discordExtension;
    private final BDSAutoEnable bdsAutoEnable;
    private final Logger logger;
    private final AppConfigManager appConfigManager;
    private final DiscordConfig discordConfig;
    private final MessagesConfig messagesConfig;
    private final BotConfig botConfig;
    private final long serverID, channelID, logID;
    private final List<JDAListener> listeners;
    private final Map<String, Pattern> mentionPatternCache;
    private JDA jda;
    private Guild guild;
    private TextChannel textChannel, logChannel;
    private StatsChannelsManager statsChannelsManager;
    private ILinkingManager linkingManager;
    private SlashCommandManager slashCommandManager;

    public DiscordJDA(final DiscordExtension discordExtension) {
        this.discordExtension = discordExtension;
        this.bdsAutoEnable = this.discordExtension.getBdsAutoEnable();
        this.logger = this.discordExtension.getLogger();
        this.appConfigManager = this.bdsAutoEnable.getAppConfigManager();
        this.discordConfig = this.discordExtension.getConfig();
        this.messagesConfig = this.discordExtension.getMessagesConfig();
        this.botConfig = this.discordConfig.getBotConfig();
        this.serverID = this.botConfig.getServerID();
        this.channelID = this.botConfig.getChannelID();
        this.logID = this.botConfig.getLogID();
        this.listeners = new ArrayList<>();
        this.mentionPatternCache = new HashMap<>();
    }

    public void init() {
        if (this.botConfig.isEnable()) {
            this.logger.info("&aŁadowanie bota....");
            if (this.botConfig.getToken().isEmpty()) {
                this.logger.alert("&aNie znaleziono tokenu , pomijanie ładowania.");
                this.discordExtension.setBotEnabled(false);
                return;
            }

            final PackModule packModule = this.bdsAutoEnable.getWatchDog().getPackModule();
            if (!packModule.isLoaded()) {
                this.logger.error("&cNie załadowano paczki&b " + packModule.getPackName() + "&4.&cBot nie może bez niej normalnie działać");
                this.logger.error("Możesz znaleźć ją tu:&b https://github.com/Huje22/BDS-Auto-Enable-Managment-Pack");
                return;
            }

            this.listeners.add(new MessageListener(this.discordExtension));
            this.listeners.add(new MentionPatternCacheListener(this, this.mentionPatternCache));

            try {
                this.jda = JDABuilder.create(this.botConfig.getToken(), this.botConfig.getGatewayIntents())
                        .disableCache(this.botConfig.getDisableCacheFlag())
                        .enableCache(this.botConfig.getEnableCacheFlag())
                        .setEnableShutdownHook(false)
                        .setHttpClient(HTTPUtil.getOkHttpClient())
                        .build();
                this.jda.awaitReady();
            } catch (final Exception exception) {
                this.logger.error("&cNie można uruchomić bota", exception);
                this.discordExtension.setBotEnabled(false);
                return;
            }

            this.guild = this.jda.getGuildById(this.serverID);
            if (this.guild == null) {
                this.logger.alert("&aPierw musisz dodać bota:&b " + this.jda.getInviteUrl(Permission.ADMINISTRATOR));
                this.discordExtension.shutdown();
                this.jda = null;
                throw new NullPointerException("Nie można odnaleźć servera o ID " + this.serverID);
            }

            this.textChannel = this.guild.getTextChannelById(this.channelID);
            if (this.textChannel == null) {
                this.discordExtension.shutdown();
                this.jda = null;
                throw new NullPointerException("Nie można odnaleźć kanału z ID&b " + this.channelID);
            }

            this.textChannel.getManager().setSlowmode(1).queue();

            this.logChannel = this.guild.getTextChannelById(this.logID);

            if (this.logChannel == null) {
                this.logger.debug("(log) Nie można odnaleźć kanału z ID &b " + this.logID);
            }

            this.linkingManager = new LinkingManager(this.discordExtension);
            this.statsChannelsManager = new StatsChannelsManager(this.discordExtension, this);
            this.statsChannelsManager.init();

            this.slashCommandManager = new SlashCommandManager(this.discordExtension);
            this.jda.addEventListener(this.slashCommandManager);

            final ProximityVoiceChatConfig proximityVoiceChatConfig = this.discordExtension.getProximityVoiceChatConfig();
            if (this.isGatewayIntent(GatewayIntent.GUILD_VOICE_STATES)) {
                if (this.isCacheFlagEnabled(CacheFlag.VOICE_STATE)) {
                    if (proximityVoiceChatConfig.isEnable()) {
                        this.discordExtension.getBdsAutoEnable().getEventManager()
                                .registerListener(new ProximityVoiceChat(this.discordExtension, this.discordExtension.getProximityVoiceChatConfig()), this.discordExtension);
                    }
                } else {
                    proximityVoiceChatConfig.setEnable(false);
                    proximityVoiceChatConfig.save();
                    this.logger.error("Brak '&bCacheFlag " + CacheFlag.VOICE_STATE + "`&r ProximityVoiceChat nie będzie działać");
                }
            } else {
                proximityVoiceChatConfig.setEnable(false);
                proximityVoiceChatConfig.save();
                this.logger.error("Brak '&bGatewayIntent " + GatewayIntent.GUILD_VOICE_STATES + "`&r ProximityVoiceChat nie będzie działać");
            }

            for (final JDAListener listener : this.listeners) {
                try {
                    listener.init();
                    this.jda.addEventListener(listener);
                    this.logger.debug("Zarejestrowano listener JDA:&b " + listener.getClass().getSimpleName());
                } catch (final Exception exception) {
                    this.logger.critical("Wystąpił błąd podczas ładowania listeneru: &b" + listener.getClass().getSimpleName(), exception);
                    throw exception;
                }
            }

            this.checkBotPermissions();
            this.customStatusUpdate();

            if (this.botConfig.isLeaveServers()) {
                this.leaveGuilds();
                this.jda.addEventListener(new GuildJoinListener(this));
            }

            this.logger.info("&aZaładowano bota&b " + this.getUserName(this.getBotMember(), this.getBotMember().getUser()));
        }
    }

    private void checkBotPermissions() {
        final Member botMember = this.getBotMember();
        if (botMember == null) return;

        if (!botMember.hasPermission(Permission.ADMINISTRATOR)) {
            this.logger.error("Bot nie ma uprawnień administratora , są one wymagane");
            this.sendEmbedMessage("Brak uprawnień",
                    "**Bot nie posiada uprawnień administratora**\n" +
                            "Są one wymagane do `100%` pewności że wszytko bedzie działać w nim",
                    new Footer("Brak uprawnień"));
        }
    }

    @Override
    public boolean isCacheFlagEnabled(final CacheFlag cacheFlag) {
        return this.jda.getCacheFlags().contains(cacheFlag);
    }


    @Override
    public boolean isGatewayIntent(final GatewayIntent gatewayIntent) {
        return this.jda.getGatewayIntents().contains(gatewayIntent);
    }


    @Override
    public void sendPrivateMessage(final User user, final String message) {
        if (user.isBot()) return;
        user.openPrivateChannel().queue(privateChannel -> privateChannel.sendMessage(message).queue(
                success -> this.logger.debug("Wysłano wiadomość do:&b " + user.getName() + " &d(&e" + user.getIdLong() + "&d)"),
                failure -> {
                    this.log("Nie udane wysyłanie prywatnej wiadomości",
                            "Nie udało się wysłać wiadomości do:&b " + user.getName() + " &d(&e" + user.getIdLong() + "&d)",
                            new Footer("", user.getAvatarUrl()));
                    this.textChannel.sendMessage(message).queue(message1 -> message1.delete().queueAfter(5, TimeUnit.SECONDS));
                }
        ));
    }

    @Override
    public void sendPrivateMessage(final User user, final MessageEmbed embed) {
        if (user.isBot()) return;
        user.openPrivateChannel().queue(privateChannel -> privateChannel.sendMessageEmbeds(embed).queue(
                success -> this.logger.debug("Wysłano wiadomość do:&b " + user.getName() + " &d(&e" + user.getIdLong() + "&d)"),
                failure -> {
                    this.log("Nie udane wysyłanie prywatnej wiadomości",
                            "Nie udało się wysłać wiadomości do:&b " + user.getName() + " &d(&e" + user.getIdLong() + "&d)",
                            new Footer("", user.getAvatarUrl()));
                    this.textChannel.sendMessageEmbeds(embed).queue(message1 -> message1.delete().queueAfter(5, TimeUnit.SECONDS));
                }
        ));
    }


    @Override
    public void mute(final Member member, final long amount, final TimeUnit timeUnit) {
        if (!member.isOwner() && this.getBotMember().canInteract(member)) {
            member.timeoutFor(amount, timeUnit).queue();
        }
    }


    @Override
    public void unMute(final Member member) {
        if (!member.isOwner() && member.isTimedOut() && this.getBotMember().canInteract(member)) {
            member.removeTimeout().queue();
        }
    }

    @Override
    @Nullable
    public Role getHighestRole(final long memberID) {
        final Member member = this.guild.getMemberById(memberID);
        if (member == null) return null;
        Role highestRole = null;
        for (final Role role : member.getRoles()) {
            if (highestRole == null || role.getPosition() > highestRole.getPosition()) {
                highestRole = role;
            }
        }
        return highestRole;
    }

    @Override
    public String getUserName(final Member member, final User author) {
        if (member != null && member.getNickname() != null) return member.getNickname();

        //Robie tak aby uzyskać custom nick gracza który ma na serwerze
        return (author != null ? author.getName() : (member != null ? member.getEffectiveName() : ""));
    }

    @Override
    public String getOwnerMention() {
        if (this.guild == null) return "";
        return (this.guild.getOwner() == null ? " " : "<@" + this.guild.getOwner().getIdLong() + ">");
    }

    @Override
    public String getRoleColor(final Role role) {
        if (role == null) return "";
        final Color col = role.getColor();
        final int red = (col == null ? -1 : col.getRed());
        final int green = (col == null ? -1 : col.getGreen());
        final int blue = (col == null ? -1 : col.getBlue());

        return ConsoleColors.getMinecraftColorFromRGB(red, green, blue);
    }

    @Override
    public String getColoredRole(final Role role) {
        return role == null ? "" : (this.getRoleColor(role) + "@" + role.getName() + "&r");
    }

    @Override
    public String getStatusColor(final OnlineStatus onlineStatus) {
        return switch (onlineStatus) {
            case OFFLINE -> "&7";
            case IDLE -> "&e";
            case ONLINE -> "&a";
            case INVISIBLE -> "&f";
            case DO_NOT_DISTURB -> "&4";
            case UNKNOWN -> "&0";
        };
    }

    @Override
    public List<Member> getAllChannelMembers(final TextChannel textChannel) {
        return textChannel.getMembers().stream()
                .filter(member -> !member.getUser().isBot())
                .sorted(Comparator.comparing(Member::getTimeJoined))
                .toList();
    }

    @Override
    public List<Member> getAllChannelOnlineMembers(final TextChannel textChannel) {
        return textChannel.getMembers().stream()
                .filter(member -> !member.getUser().isBot())
                .filter(member -> member.getOnlineStatus() != OnlineStatus.OFFLINE)
                .sorted(Comparator.comparing(Member::getTimeJoined))
                .toList();
    }

    @Override
    @Nullable
    public Category getCategoryByID(final long categoryId) {
        return this.guild.getCategoryById(categoryId);
    }

    @Override
    @Nullable
    public List<VoiceChannel> getVoiceChannelsInCategoryById(final long categoryId) {
        final Category category = this.getCategoryByID(categoryId);
        if (category != null) {
            return category.getVoiceChannels();
        }

        return null;
    }

    private List<Member> getGuildMembers(final Guild guild) {
        return guild.getMembers().stream()
                .filter(member -> !member.getUser().isBot()).sorted(Comparator.comparing(Member::getTimeJoined)).toList();
    }

    @Override
    public void setBotActivityStatus(final String activityMessage) {
        this.setBotActivityStatus(activityMessage, null);
    }

    @Override
    public void setBotActivityStatus(final String activityMessage, @Nullable final Activity.ActivityType activityType) {
        this.botConfig.setActivityMessage(activityMessage);
        this.discordConfig.load();
        if (activityType != null) this.botConfig.setActivity(activityType);
        this.discordConfig.save();
        this.jda.getPresence().setActivity(DiscordJDA.this.getCustomActivity());
    }

    @Override
    public Member getBotMember() {
        return this.guild.getMember(this.jda.getSelfUser());
    }

    private void customStatusUpdate() {
        final Timer timer = new Timer("Discord Status Changer", true);
        final TimerTask statusTask = new TimerTask() {

            public void run() {
                DiscordJDA.this.jda.getPresence().setActivity(DiscordJDA.this.getCustomActivity());
            }
        };

        timer.scheduleAtFixedRate(statusTask, DateUtil.minutesTo(1, TimeUnit.MILLISECONDS), DateUtil.minutesTo(10, TimeUnit.MILLISECONDS));
    }

    private Activity getCustomActivity() {
        final String replacement = String.valueOf(DateUtil.formatTimeDynamic(System.currentTimeMillis() - this.bdsAutoEnable.getServerProcess().getStartTime(), true));
        final String activityMessage = this.botConfig.getActivityMessage().replaceAll("<time>", replacement);

        switch (this.botConfig.getActivity()) {
            case PLAYING -> {
                return Activity.playing(activityMessage);
            }
            case WATCHING -> {
                return Activity.watching(activityMessage);
            }
            case COMPETING -> {
                return Activity.competing(activityMessage);
            }
            case LISTENING -> {
                return Activity.listening(activityMessage);
            }
            case CUSTOM_STATUS -> {
                return Activity.customStatus(activityMessage);
            }
            case STREAMING -> {
                return Activity.streaming(activityMessage, this.botConfig.getStreamUrl());
            }
            default -> {
                this.logger.error("Wykryto nie wspierany status! ");
                return Activity.playing(activityMessage);
            }
        }
    }

    private void leaveGuilds() {
        for (final Guild guild1 : this.jda.getGuilds()) {
            if (guild1 != this.guild) {
                this.leaveGuild(guild1);
            }
        }
    }

    @Override
    public void leaveGuild(final Guild guild) {
        String inviteLink = "";
        final DefaultGuildChannelUnion defaultChannel = guild.getDefaultChannel();
        if (defaultChannel != null) inviteLink += defaultChannel.createInvite().complete().getUrl();

        guild.leave().queue();

        final List<Field> fieldList = new ArrayList<>();
        fieldList.add(new Field("Nazwa", guild.getName(), true));
        if (!inviteLink.isEmpty()) {
            fieldList.add(new Field("Zaproszenie", inviteLink, true));
        }

        this.log("Opuszczenie servera", "", fieldList, new Footer(""));
    }

    /**
     * Kod lekko przerobiony z https://github.com/DiscordSRV/DiscordSRV/blob/master/src/main/java/github/scarsz/discordsrv/util/DiscordUtil.java#L135
     */

    private String convertMentionsFromNames(String message) {
        if (!message.contains("@")) return message;

        final Map<Pattern, String> patterns = new HashMap<>();
        for (final Role role : this.guild.getRoles()) {
            final Pattern pattern = this.mentionPatternCache.computeIfAbsent(
                    role.getId(),
                    mentionable -> Pattern.compile(
                            "(?<!<)" +
                                    Pattern.quote("@" + role.getName()),
                            Pattern.CASE_INSENSITIVE | Pattern.UNICODE_CASE
                    )
            );
            if (!role.isMentionable()) continue;
            patterns.put(pattern, role.getAsMention());
        }

        for (final Member member : this.guild.getMembers()) {
            final Pattern pattern = this.mentionPatternCache.computeIfAbsent(
                    member.getId(),
                    mentionable -> Pattern.compile(
                            "(?<!<)" +
                                    Pattern.quote("@" + member.getEffectiveName()),
                            Pattern.CASE_INSENSITIVE | Pattern.UNICODE_CASE
                    )
            );
            patterns.put(pattern, member.getAsMention());
        }

        for (final Map.Entry<Pattern, String> entry : patterns.entrySet()) {
            message = entry.getKey().matcher(message).replaceAll(entry.getValue());
        }

        return this.removeRoleMention(message);
    }

    private String removeRoleMention(String message) {
        for (final Role role : this.guild.getRoles()) {
            final String rolePattern = "<@&" + role.getId() + ">";
            if (message.contains(rolePattern) && !role.isMentionable()) {
                message = message.replaceAll(rolePattern, role.getName());
            }
        }
        return message;
    }

    @Override
    public void sendMessage(final String message) {
        if (this.jda != null && this.textChannel != null && this.jda.getStatus() == JDA.Status.CONNECTED) {
            if (message.isEmpty()) return;
            this.textChannel.sendMessage(
                    this.convertMentionsFromNames(message).replaceAll("<owner>", this.getOwnerMention())
            ).queue();
        }
    }

    @Override
    public void sendMessage(final String message, final Throwable throwable) {
        this.sendMessage(message +
                (throwable == null ? "" : "\n```" + MessageUtil.getStackTraceAsString(throwable) + "```"));
    }

    @Override
    public void sendEmbedMessage(final String title, final String message, final List<Field> fields, final Footer footer) {
        if (this.jda != null && this.textChannel != null && this.jda.getStatus() == JDA.Status.CONNECTED) {
            if (title.isEmpty() || message.isEmpty()) {
                throw new NullPointerException("'title' ani 'message' nie mogą być puste");
            }
            final EmbedBuilder embed = new EmbedBuilder()
                    .setTitle(title)
                    .setDescription(message.replaceAll("<owner>", this.getOwnerMention()))
                    .setColor(Color.BLUE)
                    .setFooter(footer.text(), footer.imageURL());

            if (fields != null && !fields.isEmpty()) {
                for (final Field field : fields) {
                    embed.addField(field.name(), field.value(), field.inline());
                }
            }

            this.textChannel.sendMessageEmbeds(embed.build()).queue();
        }
    }

    @Override
    public void sendEmbedMessage(final String title, final String message, final List<Field> fields, final Throwable throwable, final Footer footer) {
        this.sendEmbedMessage(title, message +
                (throwable == null ? "" : "\n```" + MessageUtil.getStackTraceAsString(throwable) + "```"), fields, footer);
    }

    @Override
    public void sendEmbedMessage(final String title, final String message, final Footer footer) {
        this.sendEmbedMessage(title, message, (List<Field>) null, footer);
    }

    @Override
    public void sendEmbedMessage(final String title, final String message, final Throwable throwable, final Footer footer) {
        this.sendEmbedMessage(title, message +
                (throwable == null ? "" : "\n```" + MessageUtil.getStackTraceAsString(throwable) + "```"), footer);
    }

    @Override
    public void log(final String title, final String message, final List<Field> fields, final Footer footer) {
        if (this.jda != null && this.logChannel != null && this.jda.getStatus() == JDA.Status.CONNECTED) {
            if (title.isEmpty() || message.isEmpty()) {
                throw new NullPointerException("'title' ani 'message' nie mogą być puste");
            }
            final EmbedBuilder embed = new EmbedBuilder()
                    .setTitle(title)
                    .setDescription(message.replaceAll("<owner>", this.getOwnerMention()))
                    .setColor(Color.BLUE)
                    .setFooter(footer.text(), footer.imageURL());

            if (fields != null && !fields.isEmpty()) {
                for (final Field field : fields) {
                    embed.addField(field.name(), field.value(), field.inline());
                }
            }
            this.logChannel.sendMessageEmbeds(embed.build()).queue();
        }
    }

    @Override
    public void log(final String title, final String message, final Footer footer) {
        this.log(title, message, null, footer);
    }

    @Override
    public void writeConsole(final String message) {
//        if (this.jda != null && this.consoleChannel != null && this.jda.getStatus() == JDA.Status.CONNECTED) {
//            if (message.isEmpty()) return;
//            this.consoleChannel.sendMessage(message.replaceAll("<owner>", this.getOwnerMention())).queue();
//        }
    }

    @Override
    public void writeConsole(final String message, final Throwable throwable) {
        this.writeConsole(message + (throwable == null ? "" : "\n```" + MessageUtil.getStackTraceAsString(throwable) + "```"));
    }

    @Override
    public void sendJoinMessage(final String playerName) {
        if (this.messagesConfig.isSendJoinMessage()) {
            this.sendMessage(this.messagesConfig.getJoinMessage().replaceAll("<name>", playerName));
        }
    }

    @Override
    public void sendLeaveMessage(final String playerName) {
        if (this.messagesConfig.isSendLeaveMessage()) {
            this.sendMessage(this.messagesConfig.getLeaveMessage().replaceAll("<name>", playerName));
        }
    }

    @Override
    public void sendPlayerMessage(final String playerName, final String playerMessage) {
        if (this.messagesConfig.isSendMinecraftToDiscordMessage()) {
            this.sendMessage(this.messagesConfig.getMinecraftToDiscordMessage()
                    .replaceAll("<name>", playerName)
                    .replaceAll("<msg>", playerMessage.replaceAll("\\\\", ""))
                    .replaceAll("@everyone", "/everyone/")
                    .replaceAll("@here", "/here/")
            );
        }
    }

    @Override
    public void sendDeathMessage(final String playerName, final String deathMessage, final String killer, final String itemUsed) {
        if (this.messagesConfig.isSendDeathMessage()) {
            String message = this.messagesConfig.getDeathMessage()
                    .replaceAll("<name>", playerName)
                    .replaceAll("<deathMessage>", deathMessage
                            .replaceAll(killer, "**" + killer + "**"));


            if (!itemUsed.equalsIgnoreCase("none")) {
                message = message.replaceAll("<itemName>", "(" + itemUsed + ")");
            } else {
                message = message.replaceAll("<itemName>", "");
            }

            this.sendMessage(message);
        }
    }

    @Override
    public void sendDisabledMessage() {
        if (this.messagesConfig.isSendDisabledMessage()) {
            this.sendMessage(this.messagesConfig.getDisabledMessage());
        }
    }

    @Override
    public void sendEnabledMessage() {
        if (this.messagesConfig.isSendEnabledMessage()) {
            this.sendMessage(this.messagesConfig.getEnabledMessage());
        }
    }

    @Override
    public void sendBackupDoneMessage() {
        if (this.messagesConfig.isSendBackupMessage()) {
            this.sendMessage(this.messagesConfig.getBackupDoneMessage());
        }
    }

    @Override
    public void sendBackupFailMessage(final Exception exception) {
        if (this.messagesConfig.isSendBackupFailMessage()) {
            this.sendMessage(this.messagesConfig.getBackupFailMessage().replaceAll("<exception>", MessageUtil.getStackTraceAsString(exception)));
        }
    }

    @Override
    public void sendServerUpdateMessage(final String version) {
        if (this.messagesConfig.isSendServerUpdateMessage()) {
            this.sendMessage(this.messagesConfig.getServerUpdate()
                    .replaceAll("<version>", version)
                    .replaceAll("<current>", this.appConfigManager.getVersionManagerConfig().getVersion())

            );
        }
    }

    public JDA getJda() {
        return this.jda;
    }

    public Guild getGuild() {
        return this.guild;
    }

    public TextChannel getTextChannel() {
        return this.textChannel;
    }

    @Nullable
    public IStatsChannelsManager getStatsChannelsManager() {
        return this.statsChannelsManager;
    }

    @Nullable
    public ILinkingManager getLinkingManager() {
        return this.linkingManager;
    }

    @Nullable
    public SlashCommandManager getSlashCommandManager() {
        return this.slashCommandManager;
    }
}
