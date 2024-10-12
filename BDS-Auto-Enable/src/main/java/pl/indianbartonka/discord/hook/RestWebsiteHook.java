package pl.indianbartonka.discord.hook;

import pl.indianbartonka.rest.RestWebsite;
import pl.indianbartonka.discord.DiscordExtension;
import pl.indianbartonka.discord.rest.DiscordMessagePostRequest;

public class RestWebsiteHook {

    public RestWebsiteHook(final DiscordExtension discordExtension, final RestWebsite restWebsite) {
        restWebsite.register(new DiscordMessagePostRequest(discordExtension, restWebsite));
    }
}