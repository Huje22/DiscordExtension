package pl.indianbartonka.discord.jda.voice.component;

import net.dv8tion.jda.api.entities.Member;
import pl.indianbartonka.bds.player.position.Position;

public class VoiceChatMember {

    private final String name;
    private final Member member;

    private Position position;

    public VoiceChatMember(final String name, final Member member, final Position position) {
        this.name = name;
        this.member = member;
        this.position = position;
    }

    public String getName() {
        return this.name;
    }

    public Member getMember() {
        return this.member;
    }

    public Position getPosition() {
        return this.position;
    }

    public void setPosition(final Position position) {
        this.position = position;
    }

    @Override
    public String toString() {
        return "VoiceChatMember{" +
                "name='" + this.name + '\'' +
                ", memberId=" + this.member.getId() +
                "," + this.position +
                '}';
    }
}