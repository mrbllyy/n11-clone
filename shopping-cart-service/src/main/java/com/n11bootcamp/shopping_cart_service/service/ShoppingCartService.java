package com.n11bootcamp.shopping_cart_service.service;

import java.util.*;

import com.n11bootcamp.shopping_cart_service.entity.CartItem;
import com.n11bootcamp.shopping_cart_service.entity.Product;
import com.n11bootcamp.shopping_cart_service.entity.ShoppingCart;
import com.n11bootcamp.shopping_cart_service.repository.ProductRepository;
import com.n11bootcamp.shopping_cart_service.repository.ShoppingCartRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

@Service
public class ShoppingCartService {

    @Autowired
    private ShoppingCartRepository shoppingCartRepository;

    @Autowired
    private ProductRepository productRepository;

    @Autowired
    private RestTemplate restTemplate;

    private static final String PRODUCT_SERVICE_BASE = "http://PRODUCT-SERVICE";

    public ResponseEntity<ShoppingCart> createCart(String name) {
        ShoppingCart shoppingCart = new ShoppingCart();
        shoppingCart.setShoppingCartName(name);
        return ResponseEntity.ok().body(shoppingCartRepository.save(shoppingCart));
    }

    public ResponseEntity<ShoppingCart> addProducts(Long shoppingCartId, List<Product> products) {
        ShoppingCart shoppingCart = shoppingCartRepository.findById(shoppingCartId)
                .orElseThrow(() -> new RuntimeException("Shopping cart not found"));

        for (Product incoming : products) {
            if (incoming == null) continue;

            // 1. Ürünü yerel DB'de güncelle/kaydet
            Product entity = productRepository.findById(incoming.getId())
                    .orElseGet(() -> {
                        Product p = new Product();
                        p.setId(incoming.getId());
                        return p;
                    });

            updateProductFields(entity, incoming);
            Product savedProduct = productRepository.saveAndFlush(entity);

            // 2. Sepette bu ürün zaten var mı kontrol et
            Optional<CartItem> existingItem = shoppingCart.getCartItems().stream()
                    .filter(item -> item.getProduct().getId() == savedProduct.getId())
                    .findFirst();

            if (existingItem.isPresent()) {
                // Varsa adedi artır
                CartItem item = existingItem.get();
                item.setQuantity(item.getQuantity() + 1);
            } else {
                // Yoksa yeni kalem ekle
                CartItem newItem = new CartItem(shoppingCart, savedProduct, 1);
                shoppingCart.getCartItems().add(newItem);
            }
        }

        return ResponseEntity.ok().body(shoppingCartRepository.save(shoppingCart));
    }

    public ResponseEntity<ShoppingCart> addProductById(String username, Long productId) {
        if (username == null || username.isBlank()) username = "default_user";
        
        String finalUsername = username;
        ShoppingCart cart = shoppingCartRepository.findByShoppingCartName(username)
                .orElseGet(() -> {
                    ShoppingCart newCart = new ShoppingCart();
                    newCart.setShoppingCartName(finalUsername);
                    return shoppingCartRepository.save(newCart);
                });

        // Product service'ten ürünü çek
        Product incoming = restTemplate.getForObject(PRODUCT_SERVICE_BASE + "/api/product/" + productId, Product.class);
        if (incoming == null) throw new RuntimeException("Product not found in Product Service");

        return addProducts(cart.getId(), Collections.singletonList(incoming));
    }

    public ResponseEntity<ShoppingCart> removeProductFromMyCart(String username, Long productId) {
        final String finalUsername = (username == null || username.isBlank()) ? "default_user" : username;

        ShoppingCart cart = shoppingCartRepository.findByShoppingCartName(finalUsername)
                .orElseThrow(() -> new RuntimeException("Cart not found for user: " + finalUsername));

        // Ürünü sepet kalemlerinden (CartItems) bul ve çıkar
        boolean removed = cart.getCartItems().removeIf(item -> item.getProduct().getId() == productId);
        
        if (!removed) {
            throw new RuntimeException("Product not found in cart");
        }

        return ResponseEntity.ok(shoppingCartRepository.save(cart));
    }

    public ResponseEntity<ShoppingCart> clearMyCart(String username) {
        final String finalUsername = (username == null || username.isBlank()) ? "default_user" : username;

        ShoppingCart cart = shoppingCartRepository.findByShoppingCartName(finalUsername)
                .orElseThrow(() -> new RuntimeException("Cart not found for user: " + finalUsername));

        cart.getCartItems().clear();

        return ResponseEntity.ok(shoppingCartRepository.save(cart));
    }

    private void updateProductFields(Product entity, Product incoming) {
        if (incoming.getTitle() != null && !incoming.getTitle().isBlank()) entity.setTitle(incoming.getTitle());
        if (incoming.getCategory() != null && !incoming.getCategory().isBlank()) entity.setCategory(incoming.getCategory());
        if (incoming.getImg() != null && !incoming.getImg().isBlank()) entity.setImg(incoming.getImg());
        if (incoming.getLabels() != null && !incoming.getLabels().isBlank()) entity.setLabels(incoming.getLabels());
        if (incoming.getDescription() != null && !incoming.getDescription().isBlank()) entity.setDescription(incoming.getDescription());
        if (incoming.getPrice() > 0) entity.setPrice(incoming.getPrice());
    }

    public ResponseEntity<ShoppingCart> removeProduct(Long shoppingCartId, Long productId) {
        ShoppingCart shoppingCart = shoppingCartRepository.findById(shoppingCartId)
                .orElseThrow(() -> new RuntimeException("Shopping cart not found"));

        shoppingCart.getCartItems().removeIf(item -> item.getProduct().getId() == productId);

        return ResponseEntity.ok().body(shoppingCartRepository.save(shoppingCart));
    }

    public ResponseEntity<Map<String, String>> getShoppingCartPrice(Long shoppingCartId, String acceptLanguage) {
        ShoppingCart shoppingCart = shoppingCartRepository.findById(shoppingCartId)
                .orElseThrow(() -> new RuntimeException("Shopping cart not found"));

        double totalPrice = shoppingCart.getCartItems().stream()
                .mapToDouble(item -> item.getProduct().getPrice() * item.getQuantity())
                .sum();

        Map<String, String> response = new HashMap<>();
        response.put("total_price", Double.toString(totalPrice));
        return ResponseEntity.ok().body(response);
    }

    public ResponseEntity<ShoppingCart> getCartById(Long shoppingCartId, String acceptLanguage) {
        ShoppingCart shoppingCart = shoppingCartRepository.findById(shoppingCartId)
                .orElseThrow(() -> new RuntimeException("Shopping cart not found"));

        localizeCart(shoppingCart, acceptLanguage);
        return ResponseEntity.ok(shoppingCart);
    }

    public ResponseEntity<ShoppingCart> getCartByShoppingCartName(String shoppingCartName, String acceptLanguage) {
        return shoppingCartRepository.findByShoppingCartName(shoppingCartName)
                .map(cart -> {
                    localizeCart(cart, acceptLanguage);
                    return ResponseEntity.ok(cart);
                })
                .orElse(ResponseEntity.status(HttpStatus.NOT_FOUND).build());
    }

    public ResponseEntity<ShoppingCart> getMyCart(String username, String acceptLanguage) {
        if (username == null || username.isBlank()) username = "default_user";
        
        String finalUsername = username;
        ShoppingCart cart = shoppingCartRepository.findByShoppingCartName(username)
                .orElseGet(() -> {
                    ShoppingCart newCart = new ShoppingCart();
                    newCart.setShoppingCartName(finalUsername);
                    return shoppingCartRepository.save(newCart);
                });

        localizeCart(cart, acceptLanguage);
        return ResponseEntity.ok(cart);
    }

    public ResponseEntity<List<ShoppingCart>> getAllCarts(String acceptLanguage) {
        List<ShoppingCart> shoppingCarts = shoppingCartRepository.findAll();
        shoppingCarts.forEach(c -> localizeCart(c, acceptLanguage));
        return ResponseEntity.ok(shoppingCarts);
    }

    public ResponseEntity<String> deleteCartById(Long shoppingCartId) {
        if (shoppingCartRepository.existsById(shoppingCartId)) {
            shoppingCartRepository.deleteById(shoppingCartId);
            return ResponseEntity.ok("Shopping Cart deleted successfully");
        }
        throw new RuntimeException("Shopping Cart not found in DB");
    }

    public ResponseEntity<String> deleteAllCarts() {
        shoppingCartRepository.deleteAll();
        return ResponseEntity.ok("All Shopping Carts deleted successfully");
    }

    @SuppressWarnings("unchecked")
    private void localizeCart(ShoppingCart cart, String acceptLanguage) {
        if (cart == null || cart.getCartItems() == null || cart.getCartItems().isEmpty()) return;

        String lang = normalizeLang(acceptLanguage);

        for (CartItem item : cart.getCartItems()) {
            Product p = item.getProduct();
            try {
                HttpHeaders headers = new HttpHeaders();
                headers.set("Accept-Language", lang);
                HttpEntity<Void> req = new HttpEntity<>(headers);

                ResponseEntity<Map> resp = restTemplate.exchange(
                        PRODUCT_SERVICE_BASE + "/api/product/" + p.getId(),
                        HttpMethod.GET,
                        req,
                        Map.class
                );

                Map body = resp.getBody();
                if (body == null) continue;

                List<Map<String, Object>> translations = (List<Map<String, Object>>) body.get("translations");
                Map<String, Object> chosen = pickTranslation(translations, lang);

                if (chosen != null) {
                    if (chosen.get("title") instanceof String s && !s.isBlank()) p.setTitle(s);
                    if (chosen.get("description") instanceof String s && !s.isBlank()) p.setDescription(s);
                    if (chosen.get("categoryName") instanceof String s && !s.isBlank()) p.setCategory(s);
                }
            } catch (Exception e) {
                System.out.println("Product-service i18n fetch failed for id=" + p.getId() + " err=" + e.getMessage());
            }
        }
    }

    private String normalizeLang(String acceptLanguage) {
        if (acceptLanguage == null || acceptLanguage.isBlank()) return "tr";
        String first = acceptLanguage.split(",")[0].trim();
        if (first.contains("-")) first = first.substring(0, first.indexOf('-'));
        return first.isBlank() ? "tr" : first;
    }

    private Map<String, Object> pickTranslation(List<Map<String, Object>> translations, String lang) {
        if (translations == null || translations.isEmpty()) return null;
        return translations.stream()
                .filter(t -> lang.equalsIgnoreCase(String.valueOf(t.get("lang"))))
                .findFirst()
                .orElseGet(() -> translations.stream()
                        .filter(t -> "tr".equalsIgnoreCase(String.valueOf(t.get("lang"))))
                        .findFirst()
                        .orElse(translations.get(0)));
    }

    public ResponseEntity<Map<String, String>> getShoppingCartPrice(Long shoppingCartId) {
        return getShoppingCartPrice(shoppingCartId, null);
    }
}
