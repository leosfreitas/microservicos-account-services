package store.account;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Base64;
import java.util.Date;
import java.util.List;
import java.util.stream.StreamSupport;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

@Service
public class AccountService {

    @Autowired
    private AccountRepository accountRepository;

    public Account findById(String id) {
        return accountRepository.findById(id)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Account not found"))
            .to();
    }

    public Account create(Account account) {
        final String pass = account.password().trim();
        if (pass.length() < 8) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Password too short!");
        }
        
        if (accountRepository.existsByEmail(account.email())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Email already exists");
        }
        
        account.sha256(calcHash(pass));
        account.creation(new Date());
        return accountRepository.save(new AccountModel(account)).to();
    }

    public Account findByEmailAndPassword(String email, String password) {
        if (email == null || email.trim().isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Email is required");
        }
        
        if (password == null || password.trim().isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Password is required");
        }
        
        final String sha256 = calcHash(password);
        AccountModel m = accountRepository.findByEmailAndSha256(email, sha256);
        
        if (m == null) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid email or password");
        }
        
        return m.to();
    }

    public List<Account> findAll() {
        return StreamSupport
            .stream(accountRepository.findAll().spliterator(), false)
            .map(AccountModel::to)
            .toList();
    }

    private String calcHash(String value) {
        try {
            MessageDigest digester = MessageDigest.getInstance("SHA-256");
            byte[] hash = digester.digest(value.getBytes(StandardCharsets.UTF_8));
            String encoded = Base64.getEncoder().encodeToString(hash);
            return encoded;
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException(e);
        }
    }
    
}