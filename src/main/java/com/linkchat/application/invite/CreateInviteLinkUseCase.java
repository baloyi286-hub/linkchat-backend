package com.linkchat.application.invite;

public interface CreateInviteLinkUseCase {

    InviteLinkView create(String displayName);

    record InviteLinkView(
            java.util.UUID ownerId,
            String displayName,
            String inviteCode,
            String invitePath) {
    }
}
