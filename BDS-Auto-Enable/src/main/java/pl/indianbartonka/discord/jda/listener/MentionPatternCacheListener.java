package pl.indianbartonka.discord.jda.listener;

import java.util.Map;
import java.util.regex.Pattern;
import pl.indianbartonka.discord.core.listener.JDAListener;
import pl.indianbartonka.discord.jda.DiscordJDA;
import net.dv8tion.jda.api.events.guild.member.update.GuildMemberUpdateNicknameEvent;
import net.dv8tion.jda.api.events.role.update.RoleUpdateNameEvent;
import net.dv8tion.jda.api.events.user.update.UserUpdateNameEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;

public class MentionPatternCacheListener extends ListenerAdapter implements JDAListener {

    private final DiscordJDA discordJDA;
    private final Map<String, Pattern> mentionPatternCache;

    public MentionPatternCacheListener(final DiscordJDA DiscordJDA, final Map<String, Pattern> mentionPatternCache) {
        this.discordJDA = DiscordJDA;
        this.mentionPatternCache = mentionPatternCache;
    }

    @Override
    public void onUserUpdateName(final UserUpdateNameEvent event) {
        this.mentionPatternCache.remove(event.getUser().getId());
    }

    @Override
    public void onGuildMemberUpdateNickname(final GuildMemberUpdateNicknameEvent event) {
        if (event.getGuild() != this.discordJDA.getGuild()) return;
        this.mentionPatternCache.remove(event.getMember().getId());
    }

    @Override
    public void onRoleUpdateName(final RoleUpdateNameEvent event) {
        if (event.getGuild() != this.discordJDA.getGuild()) return;
        this.mentionPatternCache.remove(event.getRole().getId());
    }

    @Override
    public void init() {

    }
}
