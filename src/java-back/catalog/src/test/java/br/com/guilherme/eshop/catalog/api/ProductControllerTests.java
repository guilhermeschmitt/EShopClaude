package br.com.guilherme.eshop.catalog.api;

import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.everyItem;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.test.web.servlet.MockMvc;

import br.com.guilherme.eshop.catalog.TestcontainersConfiguration;
import br.com.guilherme.eshop.catalog.domain.Product;
import br.com.guilherme.eshop.catalog.repository.ProductRepository;

/** GET /products and GET /products/{id} against the V2 seed: 24 products, 6 in each of Apparel, Bags, Drinkware, Stationery. */
@SpringBootTest
@AutoConfigureMockMvc
@Import(TestcontainersConfiguration.class)
class ProductControllerTests {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ProductRepository productRepository;

    @Test
    void getByIdReturnsTheProduct() throws Exception {
        Long id = productRepository.findAll().stream()
                .filter(p -> p.getName().equals("Ceramic Mug"))
                .map(Product::getId)
                .findFirst()
                .orElseThrow();

        mockMvc.perform(get("/products/{id}", id))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(id))
                .andExpect(jsonPath("$.name").value("Ceramic Mug"))
                .andExpect(jsonPath("$.description").value("350 ml glazed ceramic mug, dishwasher safe."))
                .andExpect(jsonPath("$.price").value(11.90))
                .andExpect(jsonPath("$.category").value("Drinkware"));
    }

    @Test
    void getUnknownIdIsNotFound() throws Exception {
        mockMvc.perform(get("/products/99999")).andExpect(status().isNotFound());
    }

    @Test
    void defaultPageIsFirstEightByName() throws Exception {
        mockMvc.perform(get("/products"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.page").value(0))
                .andExpect(jsonPath("$.size").value(8))
                .andExpect(jsonPath("$.totalItems").value(24))
                .andExpect(jsonPath("$.totalPages").value(3))
                .andExpect(jsonPath("$.items[*].name", contains(
                        "Baseball Cap",
                        "Canvas Tote Bag",
                        "Ceramic Mug",
                        "Classic Logo T-Shirt",
                        "Crew Socks (3-Pack)",
                        "Crossbody Sling",
                        "Desk Planner",
                        "Dotted Notebook A5")))
                .andExpect(jsonPath("$.items[0].id").isNumber())
                .andExpect(jsonPath("$.items[0].description")
                        .value("Adjustable six-panel cap with an embroidered logo."))
                .andExpect(jsonPath("$.items[0].price").value(17.00))
                .andExpect(jsonPath("$.items[0].category").value("Apparel"));
    }

    @Test
    void lastPageHoldsTheRemainingItems() throws Exception {
        mockMvc.perform(get("/products").param("page", "2"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.page").value(2))
                .andExpect(jsonPath("$.totalItems").value(24))
                .andExpect(jsonPath("$.items", hasSize(8)))
                .andExpect(jsonPath("$.items[0].name").value("Laptop Sleeve 14\""))
                .andExpect(jsonPath("$.items[7].name").value("Zip-Up Hoodie"));
    }

    @Test
    void pageBeyondTheEndIsEmpty() throws Exception {
        mockMvc.perform(get("/products").param("page", "3"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items", hasSize(0)))
                .andExpect(jsonPath("$.totalItems").value(24))
                .andExpect(jsonPath("$.totalPages").value(3));
    }

    @Test
    void customSizeChangesPageCount() throws Exception {
        mockMvc.perform(get("/products").param("size", "5"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.size").value(5))
                .andExpect(jsonPath("$.items", hasSize(5)))
                .andExpect(jsonPath("$.totalPages").value(5));
    }

    @Test
    void categoryFilterIsExact() throws Exception {
        mockMvc.perform(get("/products").param("category", "Bags"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalItems").value(6))
                .andExpect(jsonPath("$.totalPages").value(1))
                .andExpect(jsonPath("$.items[*].category", everyItem(is("Bags"))))
                .andExpect(jsonPath("$.items[*].name", contains(
                        "Canvas Tote Bag",
                        "Crossbody Sling",
                        "Drawstring Gym Bag",
                        "Everyday Backpack",
                        "Laptop Sleeve 14\"",
                        "Weekender Duffel")));

        mockMvc.perform(get("/products").param("category", "bags"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalItems").value(0))
                .andExpect(jsonPath("$.items", hasSize(0)));
    }

    @Test
    void searchIsCaseInsensitiveOnName() throws Exception {
        mockMvc.perform(get("/products").param("q", "MUG"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalItems").value(2))
                .andExpect(jsonPath("$.items[*].name", contains("Ceramic Mug", "Enamel Camp Mug")));

        mockMvc.perform(get("/products").param("q", "t-shirt"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items[*].name", contains("Classic Logo T-Shirt", "Minimalist Black T-Shirt")));
    }

    @Test
    void searchTreatsLikeWildcardsLiterally() throws Exception {
        mockMvc.perform(get("/products").param("q", "%"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalItems").value(0));
    }

    @Test
    void categoryAndSearchCombine() throws Exception {
        mockMvc.perform(get("/products").param("category", "Drinkware").param("q", "cup"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalItems").value(2))
                .andExpect(jsonPath("$.items[*].name", contains("Espresso Cup Set", "Travel Coffee Cup")));
    }

    @Test
    void blankFiltersAreIgnored() throws Exception {
        mockMvc.perform(get("/products").param("category", "").param("q", " "))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalItems").value(24));
    }

    @Test
    void invalidPagingIsBadRequest() throws Exception {
        mockMvc.perform(get("/products").param("page", "-1")).andExpect(status().isBadRequest());
        mockMvc.perform(get("/products").param("size", "0")).andExpect(status().isBadRequest());
        mockMvc.perform(get("/products").param("size", "101")).andExpect(status().isBadRequest());
    }
}
