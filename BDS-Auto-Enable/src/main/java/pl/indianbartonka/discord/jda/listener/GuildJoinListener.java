package pl.indianbartonka.discord.jda.listener;

import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.events.guild.GuildJoinEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import pl.indianbartonka.discord.jda.DiscordJDA;

public class GuildJoinListener extends ListenerAdapter {

    private final DiscordJDA discordJDA;
    private final Guild mainGuild;

    public GuildJoinListener(final DiscordJDA discordJDA) {
        this.discordJDA = discordJDA;
        this.mainGuild = this.discordJDA.getGuild();
    }

    @Override
    public void onGuildJoin(final GuildJoinEvent event) {
        final Guild guild = event.getGuild();
        if (guild == this.mainGuild) return;
        this.discordJDA.leaveGuild(guild);
    }
}