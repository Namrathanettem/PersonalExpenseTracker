
import com.sun.net.httpserver.HttpServer;
import com.sun.net.httpserver.HttpExchange;
import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

public class ExpenseServer {

    private static final Path WEB =
            Paths.get("/app/web").toAbsolutePath().normalize();

    public static void main(String[] args) throws Exception {

        String portValue = System.getenv("PORT");
        int port = portValue == null
                ? 10000
                : Integer.parseInt(portValue);

        HttpServer server = HttpServer.create(
                new InetSocketAddress("0.0.0.0", port), 0);

        server.createContext("/", exchange -> serveWebsite(exchange));

        server.setExecutor(null);
        server.start();

        System.out.println(
                "Personal Expense Tracker started on port " + port);
        System.out.println("Listening on 0.0.0.0");
    }

    private static void serveWebsite(HttpExchange exchange)
            throws IOException {

        String requestPath = exchange.getRequestURI().getPath();

        if (requestPath.equals("/")) {
            requestPath = "/index.html";
        }

        Path file = WEB.resolve(requestPath.substring(1)).normalize();

        if (!file.startsWith(WEB) || !Files.isRegularFile(file)) {
            sendResponse(exchange, 404,
                    "File not found".getBytes(StandardCharsets.UTF_8),
                    "text/plain; charset=UTF-8");
            return;
        }

        String name = file.getFileName().toString();
        String contentType = "application/octet-stream";

        if (name.endsWith(".html")) {
            contentType = "text/html; charset=UTF-8";
        } else if (name.endsWith(".css")) {
            contentType = "text/css; charset=UTF-8";
        } else if (name.endsWith(".js")) {
            contentType = "text/javascript; charset=UTF-8";
        } else if (name.endsWith(".json")) {
            contentType = "application/json; charset=UTF-8";
        } else if (name.endsWith(".png")) {
            contentType = "image/png";
        } else if (name.endsWith(".jpg")
                || name.endsWith(".jpeg")) {
            contentType = "image/jpeg";
        } else if (name.endsWith(".svg")) {
            contentType = "image/svg+xml";
        }

        byte[] content = Files.readAllBytes(file);

        sendResponse(exchange, 200, content, contentType);
    }

    private static void sendResponse(
            HttpExchange exchange,
            int status,
            byte[] content,
            String contentType) throws IOException {

        exchange.getResponseHeaders().set(
                "Content-Type", contentType);

        exchange.getResponseHeaders().set(
                "Cache-Control", "no-cache");

        exchange.sendResponseHeaders(status, content.length);

        try (OutputStream output = exchange.getResponseBody()) {
            output.write(content);
        }
    }
}
