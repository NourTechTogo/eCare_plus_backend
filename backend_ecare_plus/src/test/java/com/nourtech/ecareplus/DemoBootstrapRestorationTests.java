package com.nourtech.ecareplus;
import org.junit.jupiter.api.Test;
import jakarta.persistence.EntityManager;
import com.nourtech.ecareplus.identity.infrastructure.persistence.UtilisateurRepository;
import com.nourtech.ecareplus.identity.application.service.MotDePasseService;
import com.nourtech.ecareplus.portail.application.DemoBootstrap;
import org.springframework.boot.DefaultApplicationArguments;
import static org.mockito.Mockito.*;
class DemoBootstrapRestorationTests {
    @Test void existingRenamedDemoAccountsArePreservedOnRestart() {
        var users=mock(UtilisateurRepository.class);
        var em=mock(EntityManager.class);
        var passwords=mock(MotDePasseService.class);
        when(users.count()).thenReturn(3L);
        when(passwords.hacher(anyString())).thenReturn("existing-test-hash");
        new DemoBootstrap(em,users,passwords,"Restoration123").run(new DefaultApplicationArguments());
        verifyNoInteractions(em);
    }
}
