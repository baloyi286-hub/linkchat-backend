package com.linkchat.domain.repository;

import com.linkchat.domain.model.Account;
import java.util.*;

public interface AccountRepository {
    Optional<Account> findByInviteCode(String code);

    Optional<Account> findById(UUID id);

    Account save(Account account);
}