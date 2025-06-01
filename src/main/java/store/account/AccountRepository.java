package store.account;

import java.util.List;

import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface AccountRepository extends CrudRepository<AccountModel, String> {

    AccountModel findByEmailAndSha256(String email, String sha256);
    
    boolean existsByEmail(String email);
    
    List<AccountModel> findByEmail(String email);
    
}