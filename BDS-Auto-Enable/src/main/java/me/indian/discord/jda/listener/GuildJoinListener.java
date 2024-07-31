package me.indian.discord.jda.listener;

import me.indian.discord.jda.DiscordJDA;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.events.guild.GuildJoinEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;

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