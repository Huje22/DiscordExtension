package pl.indianbartonka.discord.jda.listener;

import java.util.concurrent.TimeUnit;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.entities.channel.concrete.TextChannel;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import pl.indianbartonka.bds.BDSAutoEnable;
import pl.indianbartonka.bds.server.ServerProcess;
import pl.indianbartonka.discord.DiscordExtension;
import pl.indianbartonka.discord.core.listener.JDAListener;
import pl.indianbartonka.discord.jda.DiscordJDA;
import pl.indianbartonka.util.DateUtil;
import pl.indianbartonka.util.logger.Logger;

public class ConsoleListener extends ListenerAdapter implements JDAListener {

    private final DiscordJDA discordJDA;
    private final Logger logger;
    private final ServerProcess serverProcess;
    private TextChannel consoleChannel;

    public ConsoleListener(final DiscordJDA DiscordJDA, final DiscordExtension discordExtension) {
        final BDSAutoEnable bdsAutoEnable = discordExtension.getBdsAutoEnable();

        this.discordJDA = DiscordJDA;
        this.logger = discordExtension.getLogger();
        this.serverProcess = bdsAutoEnable.getServerProcess();
    }

    @Override
    public void init() {
//        this.consoleChannel = this.discordJDA.getConsoleChannel();
    }

    @Override
    public void onMessageReceived(final MessageReceivedEvent event) {
        if (event.getAuthor().equals(this.discordJDA.getJda().getSelfUser())) return;

        final Member member = event.getMember();
        final User author = event.getAuthor();
        final Message message = event.getMessage();
        final String rawMessage = message.getContentRaw();

        if (member == null) return;

        if (event.getChannel() instanceof final TextChannel channel) {
            if (channel == this.consoleChannel) {
                if (!this.serverProcess.isEnabled()) return;
                if (member.hasPermission(Permission.ADMINISTRATOR)) {
                    this.serverProcess.sendToConsole(rawMessage);
                    this.logger.print("[" + DateUtil.getDate() + " DISCORD] " +
                            this.discordJDA.getUserName(member, author) +
                            " (" + author.getIdLong() + ") -> " +
                            rawMessage);
                } else {
                    event.getChannel().sendMessage("Nie masz uprawnień administratora aby wysłać tu wiadomość").queue(msg -> {
                        msg.delete().queueAfter(5, TimeUnit.SECONDS);
                        message.delete().queueAfter(4, TimeUnit.SECONDS);
                    });
                }
            }
        }
    }
}