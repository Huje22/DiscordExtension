package pl.indianbartonka.discord.core;


import java.util.List;
import java.util.concurrent.TimeUnit;
import net.dv8tion.jda.api.OnlineStatus;
import net.dv8tion.jda.api.entities.Activity;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.MessageEmbed;
import net.dv8tion.jda.api.entities.Role;
import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.entities.channel.concrete.Category;
import net.dv8tion.jda.api.entities.channel.concrete.TextChannel;
import net.dv8tion.jda.api.entities.channel.concrete.VoiceChannel;
import net.dv8tion.jda.api.requests.GatewayIntent;
import net.dv8tion.jda.api.utils.cache.CacheFlag;
import org.jetbrains.annotations.Nullable;
import pl.indianbartonka.discord.core.embed.component.Field;
import pl.indianbartonka.discord.core.embed.component.Footer;

public interface IDiscordJDA {

    boolean isCacheFlagEnabled(final CacheFlag cacheFlag);

    boolean isGatewayIntent(final GatewayIntent gatewayIntent);

    void sendPrivateMessage(User user, String message);

    void sendPrivateMessage(User user, MessageEmbed embed);

    void mute(Member member, long amount, TimeUnit timeUnit);

    void unMute(Member member);

    @Nullable
    Role getHighestRole(long memberID);

    String getUserName(Member member, User author);

    String getOwnerMention();

    String getRoleColor(Role role);

    String getColoredRole(Role role);

    String getStatusColor(OnlineStatus onlineStatus);

    List<Member> getAllChannelMembers(TextChannel textChannel);

    List<Member> getAllChannelOnlineMembers(TextChannel textChannel);

    @Nullable
    Category getCategoryByID(long categoryId);

    @Nullable
    List<VoiceChannel> getVoiceChannelsInCategoryById(long categoryId);

    void setBotActivityStatus(String activityMessage);

    void setBotActivityStatus(String activityMessage, @Nullable Activity.ActivityType activityType);

    Member getBotMember();

    void leaveGuild(final Guild guild);

    void sendMessage(String message);

    void sendMessage(String message, Throwable throwable);

    void sendEmbedMessage(String title, String message, List<Field> fields, Footer footer);

    void sendEmbedMessage(String title, String message, List<Field> fields, Throwable throwable, Footer footer);

    void sendEmbedMessage(String title, String message, Footer footer);

    void sendEmbedMessage(String title, String message, Throwable throwable, Footer footer);

    void log(String title, String message, List<Field> fields, Footer footer);

    void log(String title, String message, Footer footer);

    void writeConsole(String message);

    void writeConsole(String message, Throwable throwable);

    void sendJoinMessage(String playerName);

    void sendLeaveMessage(String playerName);

    void sendPlayerMessage(String playerName, String playerMessage);

    void sendDeathMessage(String playerName, String deathMessage, String killer, String itemUsed);

    void sendDisabledMessage();

    void sendEnabledMessage();

    void sendBackupDoneMessage();

    void sendBackupFailMessage(Exception exception);

    void sendServerUpdateMessage(String version);
}
