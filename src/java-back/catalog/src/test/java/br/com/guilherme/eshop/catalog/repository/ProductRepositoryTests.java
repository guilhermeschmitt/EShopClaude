package br.com.guilherme.eshop.catalog.repository;

import static org.assertj.core.api.Assertions.assertThat;

import org.flywaydb.core.Flyway;
import org.flywaydb.core.api.MigrationInfo;
import org.flywaydb.core.api.MigrationState;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;

import br.com.guilherme.eshop.catalog.TestcontainersConfiguration;
import br.com.guilherme.eshop.catalog.domain.Product;

@SpringBootTest
@Import(TestcontainersConfiguration.class)
class ProductRepositoryTests {

    @Autowired
    private Flyway flyway;

    @Autowired
    private ProductRepository productRepository;

    @Test
    void flywayAppliedV2() {
        MigrationInfo current = flyway.info().current();

        assertThat(current).isNotNull();
        assertThat(current.getVersion().getVersion()).isEqualTo("2");
        assertThat(current.getState()).isEqualTo(MigrationState.SUCCESS);
    }

    @Test
    void seedLoadsProducts() {
        assertThat(productRepository.count()).isEqualTo(24);
    }

    @Test
    void seedHasFourDistinctCategories() {
        long categories = productRepository.findAll().stream()
                .map(Product::getCategory)
                .distinct()
                .count();

        assertThat(categories).isEqualTo(4);
    }
}
