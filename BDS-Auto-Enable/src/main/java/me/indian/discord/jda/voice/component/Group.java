package me.indian.discord.jda.voice.component;

import java.util.ArrayList;
import java.util.List;

public class Group {

    private final String id;
    private final List<VoiceChatMember> voiceChatMembers;

    public Group(final String id) {
        this.id = id;
        this.voiceChatMembers = new ArrayList<>();
    }

    public void addVoiceMember(final VoiceChatMember voiceChatMember) {
        this.voiceChatMembers.add(voiceChatMember);
    }

    public void removeVoiceMember(final VoiceChatMember voiceChatMember) {
        this.voiceChatMembers.remove(voiceChatMember);
    }

    public String getId() {
        return this.id;
    }

    public List<VoiceChatMember> getVoiceChatMembers() {
        return this.voiceChatMembers;
    }


    @Override
    public String toString() {
        final StringBuilder sb = new StringBuilder();
        sb.append("Group ID: ").append(this.id).append("\n");
        sb.append("Voice Chat Members: ").append(this.voiceChatMembers.size()).append("\n");
        for (final VoiceChatMember member : this.voiceChatMembers) {
            sb.append(member.toString()).append("\n");
        }
        return sb.toString();
    }
}
