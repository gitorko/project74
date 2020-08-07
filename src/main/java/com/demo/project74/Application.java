package com.demo.project74;

import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.vault.core.VaultKeyValueOperationsSupport;
import org.springframework.vault.core.VaultOperations;
import org.springframework.vault.core.VaultSysOperations;
import org.springframework.vault.core.VaultTemplate;
import org.springframework.vault.core.VaultTransitOperations;
import org.springframework.vault.support.VaultMount;
import org.springframework.vault.support.VaultResponse;

@SpringBootApplication
@Slf4j
@EnableConfigurationProperties(MySecrets.class)
public class Application implements CommandLineRunner {

    @Autowired
    private VaultTemplate vaultTemplate;

    @Value("${username}")
    private String myusername;

    private final MySecrets mySecrets;

    @Autowired
    private VaultOperations operations;

    public Application(MySecrets mySecrets) {
        this.mySecrets = mySecrets;
    }

    public static void main(String[] args) {
        SpringApplication.run(Application.class, args);
    }

    @Override
    public void run(String... strings) {

        log.info("Value injected via @Value : {}", myusername);

        log.info("Value injected via class: {}", mySecrets.getKey1());

        //Reading directly.
        VaultResponse response = vaultTemplate.opsForKeyValue("secret",
                VaultKeyValueOperationsSupport.KeyValueBackend.KV_2).get("myapp");
        log.info("Value of myKey: {} ", response.getData().get("myKey"));

        //Writing new values to different path.
        VaultTransitOperations transitOperations = vaultTemplate.opsForTransit();
        VaultSysOperations sysOperations = vaultTemplate.opsForSys();
        if (!sysOperations.getMounts().containsKey("transit/")) {
            sysOperations.mount("transit", VaultMount.create("transit"));
            transitOperations.createKey("foo-key");
        }

        // Encrypt a plain-text value
        String ciphertext = transitOperations.encrypt("foo-key", "Secure message");
        log.info("Encrypted value: {}", ciphertext);

        // Decrypt
        String plaintext = transitOperations.decrypt("foo-key", ciphertext);
        log.info("Decrypted value: {}", plaintext);
    }

}

@Data
@ConfigurationProperties("group")
class MySecrets{
    String key1;
    String key2;
}
