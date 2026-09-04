package com.linkchat.application.owner;

import com.linkchat.application.ChatApplicationService;
import com.linkchat.application.exception.BusinessRuleException;
import com.linkchat.application.exception.ResourceNotFoundException;
import com.linkchat.domain.model.Account;
import com.linkchat.domain.model.Conversation;
import com.linkchat.domain.repository.AccountRepository;
import com.linkchat.domain.repository.ChatMessageRepository;
import com.linkchat.domain.repository.ConversationRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
public class OwnerAccountService {
    private final AccountRepository accounts; private final ChatApplicationService chat; private final ConversationRepository conversations; private final ChatMessageRepository messages;
    public OwnerAccountService(AccountRepository accounts,ChatApplicationService chat,ConversationRepository conversations,ChatMessageRepository messages){this.accounts=accounts;this.chat=chat;this.conversations=conversations;this.messages=messages;}

    @Transactional(readOnly=true) public OwnerView me(String authSubject){Account owner=requireOwner(authSubject);return new OwnerView(owner.getId(),owner.getDisplayName(),owner.getInviteCode(),"/i/"+owner.getInviteCode(),owner.getVaultPasswordHash()!=null);}
    @Transactional(readOnly=true) public Object inbox(String authSubject){return chat.inbox(requireOwner(authSubject).getInviteCode()).stream().filter(item->!Boolean.TRUE.equals(item.get("hidden"))).toList();}
    @Transactional public void hide(String authSubject,UUID conversationId){Account owner=requireOwner(authSubject);Conversation conversation=requireOwnedConversation(owner,conversationId);conversation.hide();conversations.save(conversation);}
    @Transactional public void restore(String authSubject,UUID conversationId){Account owner=requireOwner(authSubject);Conversation conversation=requireOwnedConversation(owner,conversationId);conversation.restore();conversations.save(conversation);}
    @Transactional public void delete(String authSubject,UUID conversationId){Account owner=requireOwner(authSubject);requireOwnedConversation(owner,conversationId);messages.deleteByConversationId(conversationId);conversations.deleteById(conversationId);}
    @Transactional public void setVaultPassword(String authSubject,String password){validatePassword(password);Account owner=requireOwner(authSubject);owner.setVaultPasswordHash(hash(owner.getId(),password));accounts.save(owner);}
    @Transactional(readOnly=true) public List<Map<String,Object>> hidden(String authSubject,String password){Account owner=requireOwner(authSubject);verifyPassword(owner,password);return chat.inbox(owner.getInviteCode()).stream().filter(item->Boolean.TRUE.equals(item.get("hidden"))).toList();}
    @Transactional(readOnly=true) public boolean verifyVaultPassword(String authSubject,String password){Account owner=requireOwner(authSubject);verifyPassword(owner,password);return true;}

    private Conversation requireOwnedConversation(Account owner,UUID id){Conversation c=conversations.findById(id).orElseThrow(()->new ResourceNotFoundException("Conversation not found"));if(!c.getOwnerId().equals(owner.getId()))throw new ResourceNotFoundException("Conversation not found");return c;}
    private Account requireOwner(String subject){return accounts.findByAuthSubject(subject).orElseThrow(()->new ResourceNotFoundException("Owner profile not created yet"));}
    private void verifyPassword(Account owner,String password){if(owner.getVaultPasswordHash()==null)throw new BusinessRuleException("Hidden conversation password has not been set");if(password==null||!MessageDigest.isEqual(owner.getVaultPasswordHash().getBytes(StandardCharsets.UTF_8),hash(owner.getId(),password).getBytes(StandardCharsets.UTF_8)))throw new BusinessRuleException("Incorrect password");}
    private void validatePassword(String password){if(password==null||password.length()<4)throw new BusinessRuleException("Password must be at least 4 characters");if(password.length()>100)throw new BusinessRuleException("Password is too long");}
    private String hash(UUID ownerId,String password){try{MessageDigest digest=MessageDigest.getInstance("SHA-256");return HexFormat.of().formatHex(digest.digest((ownerId+":"+password).getBytes(StandardCharsets.UTF_8)));}catch(NoSuchAlgorithmException e){throw new IllegalStateException(e);}}
    private OwnerView toView(Account account){return new OwnerView(account.getId(),account.getDisplayName(),account.getInviteCode(),"/i/"+account.getInviteCode(),account.getVaultPasswordHash()!=null);}
    public record OwnerView(UUID ownerId,String displayName,String inviteCode,String invitePath,boolean vaultPasswordSet){}
}
