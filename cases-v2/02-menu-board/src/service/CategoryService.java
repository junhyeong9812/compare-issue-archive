package service;

import domain.Category;
import orm.Transactions;
import repo.MenuRepository;

import java.util.Locale;

public class CategoryService {
    private final Transactions tx;
    private final MenuRepository repo;

    public CategoryService(Transactions tx, MenuRepository repo) {
        this.tx = tx;
        this.repo = repo;
    }

    public String create(String title) {
        if (title == null || title.isBlank()) {
            throw ApiException.badRequest("title required");
        }
        String id = title.strip().toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9]+", "-");
        tx.run(s -> {
            if (repo.category(s, id) != null) {
                throw new ApiException(409, "category exists");
            }
            repo.add(s, new Category(id, title.strip()));
        });
        return id;
    }

    public void retitle(String id, String title) {
        tx.run(s -> {
            Category c = repo.category(s, id);
            if (c == null) {
                throw ApiException.notFound("category " + id);
            }
            c.retitle(title.strip());
        });
    }
}
