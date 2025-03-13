package pl.indianbartonka.discord.hook;

import pl.indianbartonka.discord.DiscordExtension;
import pl.indianbartonka.discord.rest.DiscordMessagePostRequest;
import pl.indianbartonka.rest.RestWebsite;

public class RestWebsiteHook {

    public RestWebsiteHook(final DiscordExtension discordExtension, final RestWebsite restWebsite) {
        restWebsite.register(new DiscordMessagePostRequest(discordExtension, restWebsite));
    }
}