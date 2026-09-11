package com.example.demo;

import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Base64;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.HashMap;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.view.RedirectView;
import org.springframework.ui.Model;
import com.mailjet.client.errors.MailjetException;
import com.mailjet.client.MailjetClient;
import com.mailjet.client.MailjetRequest;
import com.mailjet.client.MailjetResponse;
import com.mailjet.client.ClientOptions;
import com.mailjet.client.resource.Emailv31;
import org.json.JSONArray;
import org.json.JSONObject;

import jakarta.annotation.PostConstruct;
import jakarta.servlet.http.HttpSession;

@Controller
public class Controllers {

    @Autowired
    orderRepositry or;

    @Autowired
    singupRepositoy sr;

    @Autowired
    userRepository ur;

    @Autowired
    EntityRepository er;

    @Autowired
    prodectRepository pr;

    @Autowired(required = false)
    private org.springframework.jdbc.core.JdbcTemplate jdbcTemplate;

    @Value("${project.image}")
    private String uploadDir;

    @PostConstruct
    public void initAdmin() {
        if (jdbcTemplate != null) {
            try (java.sql.Connection conn = jdbcTemplate.getDataSource().getConnection()) {
                String driverName = conn.getMetaData().getDriverName().toLowerCase();
                if (driverName.contains("mysql")) {
                    jdbcTemplate.execute("ALTER TABLE product_table MODIFY COLUMN image1 LONGTEXT");
                    jdbcTemplate.execute("ALTER TABLE product_table MODIFY COLUMN image2 LONGTEXT");
                    jdbcTemplate.execute("ALTER TABLE product_table MODIFY COLUMN image3 LONGTEXT");
                    jdbcTemplate.execute("ALTER TABLE product_table MODIFY COLUMN image4 LONGTEXT");
                    jdbcTemplate.execute("ALTER TABLE product_table MODIFY COLUMN image5 LONGTEXT");
                } else {
                    jdbcTemplate.execute("ALTER TABLE product_table ALTER COLUMN image1 TYPE TEXT");
                    jdbcTemplate.execute("ALTER TABLE product_table ALTER COLUMN image2 TYPE TEXT");
                    jdbcTemplate.execute("ALTER TABLE product_table ALTER COLUMN image3 TYPE TEXT");
                    jdbcTemplate.execute("ALTER TABLE product_table ALTER COLUMN image4 TYPE TEXT");
                    jdbcTemplate.execute("ALTER TABLE product_table ALTER COLUMN image5 TYPE TEXT");
                }
                System.out.println("✦ Successfully updated product_table image columns to TEXT");
            } catch (Exception e) {
                System.out.println("Column alter check: " + e.getMessage());
            }
        }

        if (sr.findByEmail("admin@ksleep.com").isEmpty()) {
            Entitysignup admin = new Entitysignup();
            admin.setName("pankaj");
            admin.setEmail("admin@ksleep.com");
            admin.setPassword("Pankaj@3287");
            admin.setRole("ADMIN");
            sr.save(admin);
        }

        if (pr.count() == 0) {
            prodectentity p1 = new prodectentity();
            p1.setProductName("KSleep Luxury Memory Foam Mattress");
            p1.setPrice(18999.00);
            p1.setMaterial("Orthopedic Memory Foam & Organic Cotton");
            p1.setComfortLevel("High Comfort");
            p1.setProductDescription(
                    "Experience blissful sleep with multi-layer orthopedic support and breathable airflow design.");
            p1.setImage1(
                    "https://images.unsplash.com/photo-1540555700478-4be289fbecef?auto=format&fit=crop&w=800&q=80");
            pr.save(p1);

            prodectentity p2 = new prodectentity();
            p2.setProductName("KSleep Ergonomic Hybrid Mattress");
            p2.setPrice(14499.00);
            p2.setMaterial("Pocket Spring & High Resilience Foam");
            p2.setComfortLevel("Medium Comfort");
            p2.setProductDescription("Engineered for perfect spinal alignment and zero motion transfer.");
            p2.setImage1(
                    "https://images.unsplash.com/photo-1631049307264-da0ec9d70304?auto=format&fit=crop&w=800&q=80");
            pr.save(p2);
        }
    }

    @GetMapping("starting")
    public String firstpage() {
        return "redirect:/index.html";
    }

    @GetMapping("/ping")
    @ResponseBody
    public ResponseEntity<String> ping() {
        return ResponseEntity.ok("PONG");
    }

    @Scheduled(fixedRate = 600000) // Every 10 minutes self-ping
    public void keepAliveSelfPing() {
        try {
            String appUrl = System.getenv("RENDER_EXTERNAL_URL");
            if (appUrl == null || appUrl.trim().isEmpty()) {
                appUrl = "https://ksleep-ecommerce.onrender.com";
            }
            URL url = new URL(appUrl + "/ping");
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("GET");
            conn.setConnectTimeout(5000);
            conn.setReadTimeout(5000);
            int responseCode = conn.getResponseCode();
            System.out.println("✦ Self-Ping Keep-Alive response code: " + responseCode);
        } catch (Exception e) {
            System.out.println("Self-ping status: " + e.getMessage());
        }
    }

    @PostMapping("/adminLogin")
    public String adminLogin(
            @RequestParam String name,
            @RequestParam String password,
            @RequestParam String email,
            HttpSession session,
            jakarta.servlet.http.HttpServletResponse response) {

        boolean isValidAdmin = ("pankaj".equals(name) && "Pankaj@3287".equals(password))
                || !sr.findByEmailAndPassword(email, password).isEmpty();

        if (isValidAdmin) {
            session.setAttribute("userEmail", email);
            session.setAttribute("isAdmin", true);
            session.setAttribute("userRole", "ADMIN");

            // Generate JWT token with ADMIN role
            String token = JwtUtil.generateToken(email, "ADMIN");

            // Store in HttpOnly Cookie (3 days expiration)
            jakarta.servlet.http.Cookie cookie = new jakarta.servlet.http.Cookie("jwt", token);
            cookie.setHttpOnly(true);
            cookie.setSecure(false);
            cookie.setPath("/");
            cookie.setMaxAge(3 * 24 * 60 * 60); // 3 days in seconds
            response.addCookie(cookie);

            return "redirect:/admin/dashboard";
        } else {
            return "redirect:/admin.html?error=invalid";
        }
    }

    @PostMapping("/insertproductdata")
    public String productDetail(
            @RequestParam(required = false) String productName,
            @RequestParam(required = false, defaultValue = "0.0") Double price,
            @RequestParam(required = false) String material,
            @RequestParam(required = false) String comfort,
            @RequestParam(required = false) String description,
            @RequestParam(value = "image1", required = false) MultipartFile file1,
            @RequestParam(value = "image2", required = false) MultipartFile file2,
            @RequestParam(value = "image3", required = false) MultipartFile file3,
            @RequestParam(value = "image4", required = false) MultipartFile file4,
            @RequestParam(value = "image5", required = false) MultipartFile file5,
            @RequestParam(value = "imageUrl1", required = false) String imageUrl1,
            @RequestParam(value = "imageUrl2", required = false) String imageUrl2,
            @RequestParam(value = "imageUrl3", required = false) String imageUrl3,
            @RequestParam(value = "imageUrl4", required = false) String imageUrl4,
            @RequestParam(value = "imageUrl5", required = false) String imageUrl5) {

        try {
            prodectentity pe = new prodectentity();
            pe.setProductName(productName != null && !productName.trim().isEmpty() ? productName : "New Product");
            pe.setPrice(price != null ? price : 0.0);
            pe.setMaterial(material != null && !material.trim().isEmpty() ? material : "Standard Material");
            pe.setComfortLevel(comfort != null && !comfort.trim().isEmpty() ? comfort : "High Comfort");
            pe.setProductDescription(description != null ? description : "");

            String img1Name = processImageInput(file1, imageUrl1);
            String img2Name = processImageInput(file2, imageUrl2);
            String img3Name = processImageInput(file3, imageUrl3);
            String img4Name = processImageInput(file4, imageUrl4);
            String img5Name = processImageInput(file5, imageUrl5);

            if (img1Name != null)
                pe.setImage1(img1Name);
            if (img2Name != null)
                pe.setImage2(img2Name);
            if (img3Name != null)
                pe.setImage3(img3Name);
            if (img4Name != null)
                pe.setImage4(img4Name);
            if (img5Name != null)
                pe.setImage5(img5Name);

            prodectentity saved = pr.save(pe);
            System.out.println(
                    "✦ Successfully saved product ID: " + saved.getId() + " - Name: " + saved.getProductName());
            return "redirect:/admin/products?status=success";
        } catch (Exception e) {
            System.out.println("❌ Error saving product: " + e.getMessage());
            e.printStackTrace();
            String errorMsg = e.getMessage() != null ? e.getMessage() : "Database Save Error";
            try {
                errorMsg = java.net.URLEncoder.encode(errorMsg, "UTF-8");
            } catch (Exception ex) {
            }
            return "redirect:/admin/products?status=error&msg=" + errorMsg;
        }
    }

    // ===============================
    // FETCH ALL PRODUCTS
    // ===============================
    @CrossOrigin(origins = "*")
    @GetMapping("/fechdata")
    @ResponseBody
    public List<prodectentity> fechdata() {

        return pr.findAll().stream().map(product -> {

            if (product.getImage1() != null && !product.getImage1().startsWith("data:")
                    && !product.getImage1().startsWith("http"))
                product.setImage1("/images/" + product.getImage1());
            if (product.getImage2() != null && !product.getImage2().startsWith("data:")
                    && !product.getImage2().startsWith("http"))
                product.setImage2("/images/" + product.getImage2());
            if (product.getImage3() != null && !product.getImage3().startsWith("data:")
                    && !product.getImage3().startsWith("http"))
                product.setImage3("/images/" + product.getImage3());
            if (product.getImage4() != null && !product.getImage4().startsWith("data:")
                    && !product.getImage4().startsWith("http"))
                product.setImage4("/images/" + product.getImage4());
            if (product.getImage5() != null && !product.getImage5().startsWith("data:")
                    && !product.getImage5().startsWith("http"))
                product.setImage5("/images/" + product.getImage5());

            return product;

        }).toList();
    }

    @GetMapping("/fechdata/{id}")
    @ResponseBody
    public prodectentity getProductById(@PathVariable Long id) {

        prodectentity product = pr.findById(id).orElse(null);

        if (product != null) {
            if (product.getImage1() != null && !product.getImage1().startsWith("data:")
                    && !product.getImage1().startsWith("http"))
                product.setImage1("/images/" + product.getImage1());
            if (product.getImage2() != null && !product.getImage2().startsWith("data:")
                    && !product.getImage2().startsWith("http"))
                product.setImage2("/images/" + product.getImage2());
            if (product.getImage3() != null && !product.getImage3().startsWith("data:")
                    && !product.getImage3().startsWith("http"))
                product.setImage3("/images/" + product.getImage3());
            if (product.getImage4() != null && !product.getImage4().startsWith("data:")
                    && !product.getImage4().startsWith("http"))
                product.setImage4("/images/" + product.getImage4());
            if (product.getImage5() != null && !product.getImage5().startsWith("data:")
                    && !product.getImage5().startsWith("http"))
                product.setImage5("/images/" + product.getImage5());
        }

        return product;
    }

    // ===============================
    // FETCH SINGLE IMAGE
    // ===============================
    @GetMapping("/images/{imageName}")
    public ResponseEntity<Resource> getImage(@PathVariable String imageName) throws IOException {

        Path imagePath = Paths.get(uploadDir).resolve(imageName);
        Resource resource = new UrlResource(imagePath.toUri());

        if (!resource.exists() || !resource.isReadable()) {
            return ResponseEntity.notFound().build();
        }

        String contentType = Files.probeContentType(imagePath);
        if (contentType == null) {
            contentType = "application/octet-stream";
        }

        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(contentType))

                .body(resource);
    }

    @PostMapping("/senddeatailEmail")
    public String paymentgateway(
            @RequestParam String username,
            @RequestParam String mobile,
            @RequestParam String address,
            @RequestParam String state,
            @RequestParam String city,
            @RequestParam String pincode,
            @RequestParam String product_id,
            @RequestParam String pprice,
            @RequestParam String pName,
            HttpSession session) {

        try {

            String loginEmail = (String) session.getAttribute("userEmail");

            if (loginEmail == null) {
                return "redirect:/login.html";
            }

            userEntity ue = new userEntity();

            ue.setUser_name(username);
            ue.setEmail(loginEmail); // session email
            ue.setMobile_number(mobile);
            ue.setAddress(address);
            ue.setPincode(pincode);

            ur.save(ue);

            System.out.println("✦ Details saved for order by " + username);
            return "redirect:/index.html";

        } catch (Exception e) {

            return "redirect:/index.html";
        }
    }

    @PostMapping("signup")
    public String sinuppage(
            @RequestParam String username,
            @RequestParam String email,
            @RequestParam String password,
            @RequestParam(required = false) String id) {

        Entitysignup es = new Entitysignup();
        es.setName(username);
        es.setEmail(email);
        es.setPassword(password);
        es.setRole("CUSTOMER"); // Default role
        sr.save(es);
        return "redirect:/login.html" + (id != null && !id.isEmpty() ? "?id=" + id + "&email=" + email : "");
    }

    @PostMapping("/loginpage")
    public String loginpage(
            @RequestParam String email,
            @RequestParam String password,
            @RequestParam(required = false) String id,
            HttpSession session,
            jakarta.servlet.http.HttpServletResponse response) {
        try {

            List<Entitysignup> user = sr.findByEmailAndPassword(email, password);

            if (!user.isEmpty()) {
                Entitysignup member = user.get(0);
                String role = member.getRole();
                if (role == null || role.isEmpty()) {
                    role = "CUSTOMER";
                }

                session.setAttribute("userEmail", email);
                session.setAttribute("userRole", role);

                // Generate JWT token with user's role
                String token = JwtUtil.generateToken(email, role);

                // Store in HttpOnly Cookie (3 days expiration)
                jakarta.servlet.http.Cookie cookie = new jakarta.servlet.http.Cookie("jwt", token);
                cookie.setHttpOnly(true);
                cookie.setSecure(false); // Can be set to true if deployed on HTTPS
                cookie.setPath("/");
                cookie.setMaxAge(3 * 24 * 60 * 60); // 3 days in seconds
                response.addCookie(cookie);

                if ("ADMIN".equals(role)) {
                    return "redirect:/login.html?error=invalid";
                }

                if (id != null && !id.isEmpty() && !id.equals("null")) {
                    return "redirect:/fulldeatailprodect.html?id=" + id + "&email=" + email;
                } else {
                    return "redirect:/index.html";
                }
            } else {
                return "redirect:/login.html?error=invalid";
            }

        } catch (Exception e) {
            return "redirect:/login.html";
        }
    }

    @GetMapping("/getUserName")
    @ResponseBody
    public ResponseEntity<String> getUserName(@RequestParam String email) {
        List<Entitysignup> users = sr.findByEmail(email);
        if (!users.isEmpty()) {
            return ResponseEntity.ok(users.get(0).getName());
        }
        return ResponseEntity.ok("");
    }

    @GetMapping("/placeOrder")
    public String getmethod() {
        return "redirect:/order.html";
    }

    @PostMapping("/placeOrder")
    public String placeOrder(
            @RequestParam(required = false) String username,
            @RequestParam(required = false) String userName,
            @RequestParam(required = false) String email,
            @RequestParam(required = false) String userEmail,
            @RequestParam(required = false) String pName,
            @RequestParam(required = false) String productName,
            @RequestParam(required = false) String pprice,
            @RequestParam(required = false) String amount,
            @RequestParam(required = false) String product_id,
            @RequestParam(required = false) String image,
            @RequestParam(required = false) String qty,
            @RequestParam(required = false) String mobile,
            @RequestParam(required = false) String address,
            HttpSession session) {

        try {
            // Resolve recipient email
            String finalEmail = (email != null && !email.trim().isEmpty()) ? email.trim() : userEmail;
            if (finalEmail == null || finalEmail.trim().isEmpty()) {
                finalEmail = (String) session.getAttribute("userEmail");
            }
            if (finalEmail == null || finalEmail.trim().isEmpty()) {
                finalEmail = "customer@example.com";
            }

            // Resolve recipient name
            String finalName = (username != null && !username.trim().isEmpty()) ? username.trim() : userName;
            if (finalName == null || finalName.trim().isEmpty()) {
                finalName = "Customer";
            }

            // Resolve product item name
            String finalItem = (pName != null && !pName.trim().isEmpty()) ? pName.trim() : productName;
            if (finalItem == null || finalItem.trim().isEmpty()) {
                finalItem = "Ecommerce Item";
            }

            // Resolve price amount
            String finalPriceStr = (pprice != null && !pprice.trim().isEmpty()) ? pprice.trim() : amount;
            if (finalPriceStr == null || finalPriceStr.trim().isEmpty()) {
                finalPriceStr = "0.0";
            }

            double numPrice = 0.0;
            try {
                numPrice = Double.parseDouble(finalPriceStr.replaceAll("[^0-9.]", ""));
            } catch (Exception e) {}

            int numQty = 1;
            if (qty != null && !qty.trim().isEmpty()) {
                try {
                    numQty = Integer.parseInt(qty.trim());
                } catch (Exception e) {}
            }

            Long pId = null;
            if (product_id != null && !product_id.trim().isEmpty()) {
                try {
                    pId = Long.parseLong(product_id.trim());
                } catch (Exception e) {}
            }

            // 💾 Save order to database
            orderEntity order = new orderEntity();
            order.setCustomerName(finalName);
            order.setEmail(finalEmail);
            order.setProductName(finalItem);
            order.setPrice(numPrice);
            order.setQuantity(numQty);
            order.setProductId(pId);
            order.setImage(image);
            order.setMobile_No(mobile);
            order.setAddress(address);
            order.setStatus("PLACED");
            or.save(order);

            // ✉️ Send Confirmation Email
            try {
                String subject = "Order Confirmed! Your Order is Processing for Delivery 🚚";
                String textPart = "Namaste " + finalName + "!\n\nThank you for your purchase!\n\n📦 Order Details:\n- Product: " + finalItem + "\n- Total Amount: ₹" + finalPriceStr + "\n- Status: Processing for Delivery\n\nWe are preparing your package and will deliver it soon.\n\nBest regards,\nEcommerce Support Team";
                String htmlPart = "<h3>Namaste " + finalName + "!</h3><p>Thank you for your purchase!</p><p>📦 Order Details:<br>- Product: " + finalItem + "<br>- Total Amount: ₹" + finalPriceStr + "<br>- Status: Processing for Delivery</p><p>We are preparing your package and will deliver it soon.</p><p>Best regards,<br>Ecommerce Support Team</p>";
                sendMailjetEmail(finalEmail, finalName, subject, textPart, htmlPart);
            } catch (Exception e) {
                System.err.println("Mailjet email sending failed, but order saved: " + e.getMessage());
            }

            return "redirect:/order.html";
        } catch (Exception e) {
            e.printStackTrace();
            return "redirect:/fulldeatailprodect.html";
        }
    }

    @GetMapping("/orderDeatail")
    @ResponseBody
    public List<orderEntity> orderDetail(HttpSession session) {

        String email = (String) session.getAttribute("userEmail");

        if (email == null) {
            return List.of();
        }

        return or.findByEmail(email);
    }

    @PostMapping("/cancelOrder")
    @ResponseBody
    public String cancelOrder(@RequestParam Long id,
            @RequestParam String reason,
            @RequestParam(required = false) String comment) {

        orderEntity order = or.findById(id).orElse(null);

        if (order == null) {
            return "Order not found";
        }

        or.deleteById(id);

        return "Order cancelled successfully";
    }

    @PostMapping("/sendContactEmail")
    public String sendemailforcontect(@RequestParam String name, @RequestParam String email, @RequestParam long phone,
            @RequestParam String type, @RequestParam String message) {

        System.out.println("✦ Contact inquiry received from: " + name + " (" + email + ")");
        return "redirect:/index.html";
    }

    // ===============================
    // ADMIN DASHBOARD & MANAGEMENT PAGES (THYMELEAF)
    // ===============================

    @GetMapping("/admin/dashboard")
    public String adminDashboard(Model model) {
        long totalCustomers = sr.countByRole("CUSTOMER");
        long totalOrders = or.count();
        double totalRevenue = or.findAll().stream()
                .filter(o -> !"cancelled".equalsIgnoreCase(o.getStatus()))
                .mapToDouble(o -> o.getPrice() * o.getQuantity())
                .sum();

        List<orderEntity> recentOrders = or.findAll().stream()
                .sorted((o1, o2) -> {
                    if (o1.getOrderDate() == null || o2.getOrderDate() == null)
                        return 0;
                    return o2.getOrderDate().compareTo(o1.getOrderDate());
                })
                .limit(5)
                .toList();

        model.addAttribute("totalCustomers", totalCustomers);
        model.addAttribute("totalOrders", totalOrders);
        model.addAttribute("totalRevenue", totalRevenue);
        model.addAttribute("recentOrders", recentOrders);
        return "admin/dashboard";
    }

    @GetMapping("/admin/products")
    public String adminProducts(Model model) {
        try {
            model.addAttribute("products", pr.findAll());
        } catch (Exception e) {
            System.out.println("Error fetching products for admin view: " + e.getMessage());
            e.printStackTrace();
            model.addAttribute("products", List.of());
        }
        return "admin/products";
    }

    @GetMapping("/admin/orders")
    public String adminOrders(Model model) {
        model.addAttribute("orders", or.findAll());
        return "admin/orders";
    }

    @GetMapping("/admin/customers")
    public String adminCustomers(Model model) {
        List<Entitysignup> customers = sr.findByRole("CUSTOMER");
        List<userEntity> profiles = ur.findAll();

        Map<String, userEntity> profileMap = new HashMap<>();
        for (userEntity profile : profiles) {
            if (profile.getEmail() != null) {
                profileMap.put(profile.getEmail().toLowerCase(), profile);
            }
        }

        model.addAttribute("customers", customers);
        model.addAttribute("profileMap", profileMap);
        return "admin/customers";
    }

    @GetMapping("/admin/analytics")
    public String adminAnalytics() {
        return "admin/analytics";
    }

    @GetMapping("/admin/reports")
    public String adminReports(Model model) {
        long totalCustomers = sr.countByRole("CUSTOMER");
        long totalOrders = or.count();
        double totalRevenue = or.findAll().stream()
                .filter(o -> !"cancelled".equalsIgnoreCase(o.getStatus()))
                .mapToDouble(o -> o.getPrice() * o.getQuantity())
                .sum();

        model.addAttribute("totalCustomers", totalCustomers);
        model.addAttribute("totalOrders", totalOrders);
        model.addAttribute("totalRevenue", totalRevenue);
        model.addAttribute("orders", or.findAll());
        return "admin/reports";
    }

    // ===============================
    // PRODUCT CRUD OPERATIONS (ADMIN)
    // ===============================

    @PostMapping("/admin/products/delete/{id}")
    public String deleteProduct(@PathVariable Long id) {
        pr.deleteById(id);
        return "redirect:/admin/products";
    }

    @PostMapping("/admin/products/update")
    public String updateProduct(
            @RequestParam Long id,
            @RequestParam(required = false) String productName,
            @RequestParam(required = false, defaultValue = "0.0") Double price,
            @RequestParam(required = false) String material,
            @RequestParam(required = false) String comfort,
            @RequestParam(required = false) String description,
            @RequestParam(value = "image1", required = false) MultipartFile file1,
            @RequestParam(value = "image2", required = false) MultipartFile file2,
            @RequestParam(value = "image3", required = false) MultipartFile file3,
            @RequestParam(value = "image4", required = false) MultipartFile file4,
            @RequestParam(value = "image5", required = false) MultipartFile file5,
            @RequestParam(value = "imageUrl1", required = false) String imageUrl1,
            @RequestParam(value = "imageUrl2", required = false) String imageUrl2,
            @RequestParam(value = "imageUrl3", required = false) String imageUrl3,
            @RequestParam(value = "imageUrl4", required = false) String imageUrl4,
            @RequestParam(value = "imageUrl5", required = false) String imageUrl5) {

        try {
            prodectentity pe = pr.findById(id).orElse(null);
            if (pe != null) {
                if (productName != null)
                    pe.setProductName(productName);
                if (price != null)
                    pe.setPrice(price);
                if (material != null)
                    pe.setMaterial(material);
                if (comfort != null)
                    pe.setComfortLevel(comfort);
                if (description != null)
                    pe.setProductDescription(description);

                String img1Name = processImageInput(file1, imageUrl1);
                if (img1Name != null)
                    pe.setImage1(img1Name);

                String img2Name = processImageInput(file2, imageUrl2);
                if (img2Name != null)
                    pe.setImage2(img2Name);

                String img3Name = processImageInput(file3, imageUrl3);
                if (img3Name != null)
                    pe.setImage3(img3Name);

                String img4Name = processImageInput(file4, imageUrl4);
                if (img4Name != null)
                    pe.setImage4(img4Name);

                String img5Name = processImageInput(file5, imageUrl5);
                if (img5Name != null)
                    pe.setImage5(img5Name);

                pr.save(pe);
                return "redirect:/admin/products?status=updated";
            }
        } catch (Exception e) {
            System.out.println("Error updating product: " + e.getMessage());
            e.printStackTrace();
            String errorMsg = e.getMessage() != null ? e.getMessage() : "Database Update Error";
            try {
                errorMsg = java.net.URLEncoder.encode(errorMsg, "UTF-8");
            } catch (Exception ex) {
            }
            return "redirect:/admin/products?status=error&msg=" + errorMsg;
        }

        return "redirect:/admin/products";
    }

    // ===============================
    // ORDER LIFE CYCLE (ADMIN)
    // ===============================

    @PostMapping("/admin/orders/update-status")
    public String updateOrderStatus(@RequestParam Long id, @RequestParam String status) {
        orderEntity order = or.findById(id).orElseThrow(() -> new IllegalArgumentException("Invalid order Id:" + id));
        order.setStatus(status);
        or.save(order);
        return "redirect:/admin/orders";
    }

    // ===============================
    // BUSINESS ANALYTICS REST APIS
    // ===============================

    @GetMapping("/admin/api/analytics/summary")
    @ResponseBody
    public Map<String, Object> getAnalyticsSummary() {
        Map<String, Object> data = new HashMap<>();

        long totalCustomers = sr.countByRole("CUSTOMER");
        long totalOrders = or.count();
        double totalRevenue = or.findAll().stream()
                .filter(o -> !"cancelled".equalsIgnoreCase(o.getStatus()))
                .mapToDouble(o -> o.getPrice() * o.getQuantity())
                .sum();

        data.put("totalCustomers", totalCustomers);
        data.put("totalOrders", totalOrders);
        data.put("totalRevenue", totalRevenue);

        Map<String, Integer> productQuantities = or.findAll().stream()
                .collect(Collectors.groupingBy(orderEntity::getProductName,
                        Collectors.summingInt(orderEntity::getQuantity)));

        List<Map<String, Object>> topProducts = productQuantities.entrySet().stream()
                .sorted(Map.Entry.<String, Integer>comparingByValue().reversed())
                .limit(5)
                .map(entry -> {
                    Map<String, Object> m = new HashMap<>();
                    m.put("productName", entry.getKey());
                    m.put("quantity", entry.getValue());
                    return m;
                }).toList();
        data.put("mostPurchasedProducts", topProducts);

        List<Map<String, Object>> leastProducts = productQuantities.entrySet().stream()
                .sorted(Map.Entry.comparingByValue())
                .limit(5)
                .map(entry -> {
                    Map<String, Object> m = new HashMap<>();
                    m.put("productName", entry.getKey());
                    m.put("quantity", entry.getValue());
                    return m;
                }).toList();
        data.put("leastPurchasedProducts", leastProducts);

        return data;
    }

    @GetMapping("/admin/api/analytics/monthly-sales")
    @ResponseBody
    public List<Map<String, Object>> getMonthlySales() {
        Map<String, Double> monthlyRevenue = or.findAll().stream()
                .filter(o -> o.getOrderDate() != null && !"cancelled".equalsIgnoreCase(o.getStatus()))
                .collect(Collectors.groupingBy(
                        o -> o.getOrderDate().getMonth().toString(),
                        Collectors.summingDouble(o -> o.getPrice() * o.getQuantity())));

        return monthlyRevenue.entrySet().stream()
                .map(entry -> {
                    Map<String, Object> m = new HashMap<>();
                    m.put("month", entry.getKey());
                    m.put("revenue", entry.getValue());
                    return m;
                }).toList();
    }

    private String processImageInput(MultipartFile file, String imageUrl) throws IOException {
        if (file != null && !file.isEmpty()) {
            String contentType = file.getContentType();
            if (contentType == null || contentType.trim().isEmpty()) {
                contentType = "image/jpeg";
            }
            byte[] bytes = file.getBytes();
            String base64 = Base64.getEncoder().encodeToString(bytes);
            return "data:" + contentType + ";base64," + base64;
        } else if (imageUrl != null && !imageUrl.trim().isEmpty()) {
            return saveImageFromUrl(imageUrl);
        }
        return null;
    }

    private String saveImageFromUrl(String urlString) {
        if (urlString == null || urlString.trim().isEmpty()) {
            return null;
        }
        return urlString.trim();
    }

    private void sendMailjetEmail(String toEmail, String toName, String subject, String textPart, String htmlPart)
            throws MailjetException {
        String apiKey = System.getenv("MJ_APIKEY_PUBLIC");
        if (apiKey == null || apiKey.trim().isEmpty()) {
            apiKey = "b505c2cb577f13b674916957787d3735";
        }

        String apiSecret = System.getenv("MJ_APIKEY_PRIVATE");
        if (apiSecret == null || apiSecret.trim().isEmpty()) {
            apiSecret = "5bc6fdb8495c2b8f9e70b633474c9b0f";
        }

        String senderEmail = System.getenv("MJ_SENDER_EMAIL");
        if (senderEmail == null || senderEmail.trim().isEmpty()) {
            senderEmail = "pkumarsaini178@gmail.com";
        }

        ClientOptions clientOptions = ClientOptions.builder()
                .apiKey(apiKey)
                .apiSecretKey(apiSecret)
                .build();
        MailjetClient client = new MailjetClient(clientOptions);
        MailjetRequest request = new MailjetRequest(Emailv31.resource)
                .property(Emailv31.MESSAGES, new JSONArray()
                        .put(new JSONObject()
                                .put(Emailv31.Message.FROM, new JSONObject()
                                        .put("Email", senderEmail)
                                        .put("Name", "KSleep Support"))
                                .put(Emailv31.Message.TO, new JSONArray()
                                        .put(new JSONObject()
                                                .put("Email", toEmail)
                                                .put("Name", toName)))
                                .put(Emailv31.Message.SUBJECT, subject)
                                .put(Emailv31.Message.TEXTPART, textPart)
                                .put(Emailv31.Message.HTMLPART, htmlPart)));
        MailjetResponse response = client.post(request);
        System.out.println("✦ Mailjet response status: " + response.getStatus());
        System.out.println(response.getData());
    }
}
