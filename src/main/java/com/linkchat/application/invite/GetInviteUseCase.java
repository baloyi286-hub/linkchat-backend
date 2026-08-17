package com.linkchat.application.invite;

public interface GetInviteUseCase {

    InviteView get(String inviteCode);

    record InviteView(
            java.util.UUID ownerId,
            String displayName,
            String inviteCode) {
    }
}
