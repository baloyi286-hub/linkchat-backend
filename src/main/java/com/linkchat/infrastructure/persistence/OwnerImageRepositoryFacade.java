package com.linkchat.infrastructure.persistence;
import com.linkchat.domain.model.OwnerImage; import org.springframework.stereotype.Repository; import java.util.*;
@Repository public class OwnerImageRepositoryFacade {
 private final OwnerImageJpaRepository repo; public OwnerImageRepositoryFacade(OwnerImageJpaRepository r){repo=r;}
 public OwnerImage save(OwnerImage i){return repo.save(i);} public List<OwnerImage> findByOwnerId(UUID id){return repo.findByOwnerId(id);} public void deleteByOwnerId(UUID id){repo.deleteByOwnerId(id);}
}