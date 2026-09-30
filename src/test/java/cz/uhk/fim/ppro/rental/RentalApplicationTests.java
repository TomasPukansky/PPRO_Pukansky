package cz.uhk.fim.ppro.rental;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

@SpringBootTest
class RentalApplicationTests {

    @Test
    @DisplayName("Kontext aplikace se úspěšně načte s testovací H2 konfigurací")
    void contextLoads() {
    }
}
