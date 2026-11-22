package br.com.guilherme.eshop.catalog.api;

import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class RootController {

    @GetMapping(path = "/", produces = MediaType.TEXT_PLAIN_VALUE)
    public String root() {
        return "Catalog API is running.";
    }
}
