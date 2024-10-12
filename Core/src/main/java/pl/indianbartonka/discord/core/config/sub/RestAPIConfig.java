package pl.indianbartonka.discord.core.config.sub;

import eu.okaeri.configs.OkaeriConfig;
import eu.okaeri.configs.annotation.Comment;
import java.util.ArrayList;
import java.util.List;

public class RestAPIConfig extends OkaeriConfig {

    @Comment({""})
    @Comment({"Te ustawienia są dostępne tylko wtedy gdy jest dostępne rozszerzenie 'RestWebsite'"})

    @Comment({""})
    @Comment({"Klucze które pozwalają na wysłanie wiadomości do Discord jeśli integracja jest włączona"})
    private List<String> discordKeys = new ArrayList<>();

    public List<String> getDiscordKeys() {
        return this.discordKeys;
    }
}