import com.lisovskyi.model.AppUser;
import com.lisovskyi.model.Order;
import com.lisovskyi.model.Product;
import com.lisovskyi.orm.OrmManager;
import jdbc.DatabaseConnection;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;

void main() {
    final String jdbcUrl = "jdbc:h2:mem:testdb;DB_CLOSE_DELAY=-1";

    try {
        DataSource dataSource = DatabaseConnection.createDataSource(jdbcUrl, "sa", "");
        try (Connection connection = dataSource.getConnection()) {
            IO.println("--- [1] Ініціалізація БД ---");
            createTable(connection);

            IO.println("\n--- [2] Запуск OrmManager (Сканування) ---");
            OrmManager ormManager = new OrmManager(dataSource);

            IO.println("\n--- [3] Тест з помилкою валідації ---");
            testWithValidationError(ormManager);

            IO.println("\n--- [4] Тест успішного збереження ---");
            testSuccess(ormManager);

            IO.println("\n--- [5] Тестування SELECT ---");
            testSelect(ormManager);
        }
    } catch (Exception e) {
        System.err.println(e.getMessage());
    }
}

private static void testWithValidationError(OrmManager ormManager) {
    IO.println("-- AppUser з username=null --");
    try {
        AppUser user = new AppUser();
        user.setId(99L);
        user.setUsername(null);
        user.setEmail("invalid@gmail.com");
        ormManager.saveEntity(user);
        IO.println("ПОМИЛКА: виняток не був кинутий");
    } catch (Exception e) {
        IO.println("Очікувана помилка: " + e.getMessage());
    }

    IO.println("-- Product з name=null --");
    try {
        Product product = new Product();
        product.setId(999L);
        product.setName(null);
        product.setPrice(100.0);
        product.setStockQuantity(5);
        ormManager.saveEntity(product);
        IO.println("ПОМИЛКА: виняток не був кинутий");
    } catch (Exception e) {
        IO.println("Очікувана помилка: " + e.getMessage());
    }
}

private static void testSuccess(OrmManager ormManager) {
    IO.println("-- INSERT AppUser --");
    AppUser user = new AppUser();
    user.setId(1L);
    user.setUsername("arsenii");
    user.setEmail("arsenii@gmail.com");
    user.setRole("ADMIN");
    ormManager.saveEntity(user);

    IO.println("-- INSERT Product --");
    Product product = new Product();
    product.setId(101L);
    product.setName("Laptop");
    product.setPrice(1500.0);
    product.setStockQuantity(10);
    ormManager.saveEntity(product);

    IO.println("-- INSERT Order --");
    Order order = new Order();
    order.setId(201L);
    order.setUserId(1L);
    order.setProductId(101L);
    order.setQuantity(2);
    order.setTotalAmount(3000.0);
    ormManager.saveEntity(order);
}

private static void testSelect(OrmManager ormManager) {
    AppUser fetchedUser = ormManager.findById(AppUser.class, 1L);
    if (fetchedUser != null) {
        IO.println("Користувача знайдено: " + fetchedUser);
    }

    Product fetchedProduct = ormManager.findById(Product.class, 101L);
    if (fetchedProduct != null) {
        IO.println("Товар знайдено: " + fetchedProduct.getName() + " ($" + fetchedProduct.getPrice() + ")");
    }

    Order fetchedOrder = ormManager.findById(Order.class, 201L);
    if (fetchedOrder != null) {
        IO.println("Замовлення знайдено: id=" + fetchedOrder.getId()
                + ", userId=" + fetchedOrder.getUserId()
                + ", total=$" + fetchedOrder.getTotalAmount());
    }
}

private static void createTable(Connection connection) throws SQLException {
    try (Statement stmt = connection.createStatement()) {
        stmt.execute("""
        CREATE TABLE IF NOT EXISTS app_users (
            id BIGINT PRIMARY KEY,
            username VARCHAR(255),
            user_email VARCHAR(255),
            role VARCHAR(255)
        );
        """);

        stmt.execute("""
        CREATE TABLE IF NOT EXISTS products (
            id BIGINT PRIMARY KEY,
            name VARCHAR(255),
            price DOUBLE,
            stock_quantity INT
        );
        """);

        stmt.execute("""
        CREATE TABLE IF NOT EXISTS orders (
            id BIGINT PRIMARY KEY,
            user_id BIGINT,
            product_id BIGINT,
            quantity INT,
            total_amount DOUBLE
        );
        """);
        IO.println("Таблиці створено.");
    }
}
