package me.indian.discord.hook;

import me.indian.discord.DiscordExtension;
import me.indian.discord.rest.DiscordMessagePostRequest;
import me.indian.rest.RestWebsite;

public class RestWebsiteHook {

    public RestWebsiteHook(final DiscordExtension discordExtension, final RestWebsite restWebsite) {
        restWebsite.register(new DiscordMessagePostRequest(discordExtension, restWebsite));
    }
}