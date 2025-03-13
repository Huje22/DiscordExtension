package pl.indianbartonka.discord.rest;

import com.google.gson.Gson;
import io.javalin.Javalin;
import io.javalin.http.ContentType;
import io.javalin.http.HttpStatus;
import java.net.HttpURLConnection;
import pl.indianbartonka.bds.util.GsonUtil;
import pl.indianbartonka.discord.DiscordExtension;
import pl.indianbartonka.discord.core.config.sub.RestAPIConfig;
import pl.indianbartonka.discord.jda.DiscordJDA;
import pl.indianbartonka.discord.rest.component.DiscordMessagePostData;
import pl.indianbartonka.discord.webhook.WebHook;
import pl.indianbartonka.rest.HttpHandler;
import pl.indianbartonka.rest.RestWebsite;
import pl.indianbartonka.rest.component.Info;
import pl.indianbartonka.rest.util.APIKeyUtil;
import pl.indianbartonka.util.logger.Logger;


public class DiscordMessagePostRequest extends HttpHandler {

    private final DiscordExtension discordExtension;
    private final RestWebsite restWebsite;
    private final DiscordJDA discordJDA;
    private final WebHook webHook;
    private final Logger logger;
    private final RestAPIConfig restAPIConfig;
    private final Gson gson;

    public DiscordMessagePostRequest(final DiscordExtension discordExtension, final RestWebsite restWebsite) {
        this.restWebsite = restWebsite;
        this.discordExtension = discordExtension;
        this.discordJDA = this.discordExtension.getDiscordJDA();
        this.webHook = this.discordExtension.getWebHook();
        this.logger = this.discordExtension.getLogger();
        this.restAPIConfig = this.discordExtension.getConfig().getRestAPIConfig();
        this.gson = GsonUtil.getGson();
    }

    @Override
    public void handle(final Javalin app) {
        app.post("/discord/message", ctx -> {
            if (this.restWebsite.addRateLimit(ctx)) return;
            if (!APIKeyUtil.isCorrectCustomKey(ctx, this.restAPIConfig.getDiscordKeys())) return;

            final String ip = ctx.ip();
            final String requestBody = ctx.body();
            final DiscordMessagePostData data;

            try {
                data = GsonUtil.getGson().fromJson(requestBody, DiscordMessagePostData.class);
            } catch (final Exception exception) {
                this.restWebsite.incorrectJsonMessage(ctx, exception);
                return;
            }

            if (data.name() == null || data.message() == null) {
                this.restWebsite.incorrectJsonMessage(ctx, this.gson.toJson(data));
                return;
            }

            switch (data.messageType()) {
                case WEBHOOK -> {
                    if (!this.discordExtension.isWebhookEnabled()) {
                        ctx.status(HttpStatus.SERVICE_UNAVAILABLE)
                                .contentType(ContentType.APPLICATION_JSON)
                                .result(this.gson.toJson(new Info("Webhook jest wyłączony", HttpStatus.SERVICE_UNAVAILABLE.getCode())));
                        return;

                    }
                    this.webHook.sendMessage(data.message());
                }

                case JDA -> {
                    if (!this.discordExtension.isBotEnabled()) {
                        ctx.status(HttpStatus.SERVICE_UNAVAILABLE)
                                .contentType(ContentType.APPLICATION_JSON)
                                .result(this.gson.toJson(new Info("Bot jest wyłączony", HttpStatus.SERVICE_UNAVAILABLE.getCode())));
                        return;
                    }
                    this.discordJDA.sendPlayerMessage(data.name(), data.message());
                }

                default -> {
                    ctx.status(HttpStatus.BAD_REQUEST).
                            contentType(ContentType.APPLICATION_JSON)
                            .result(this.gson.toJson(new Info("'MessageType' (" + data.messageType() + ") jest nie poprawny!", HttpStatus.BAD_REQUEST.getCode())));
                    return;
                }
            }

            this.logger.debug("&b" + ip + "&r używa poprawnie endpointu&1 DISCORD/MESSAGE");
            ctx.status(HttpURLConnection.HTTP_NO_CONTENT);
        });
    }
}