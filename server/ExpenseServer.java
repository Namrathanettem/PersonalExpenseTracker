
import com.sun.net.httpserver.HttpServer;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;

import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

public class ExpenseServer {

    public static void main(String[] args) throws Exception {

        int port = Integer.parseInt(
            System.getenv().getOrDefault("PORT", "10000")
        );

        HttpServer server = HttpServer.create(
            new InetSocketAddress("0.0.0.0", port), 0
        );

        Path web = Paths.get("/app/web");

        server.createContext("/", new HttpHandler() {
            @Override
            public void handle(HttpExchange exchange)
                    throws IOException {

                String requestPath =
                    exchange.getRequestURI().getPath();

                if (requestPath.equals("/")) {
                    requestPath = "/index.html";
                }

                Path file = web.resolve(
                    requestPath.substring(1)
                ).normalize();

                if (!file.startsWith(web.normalize())
                        || !Files.isRegularFile(file)) {
                    byte[] message = "404 - File not found"
                        .getBytes("UTF-8");

                    exchange.sendResponseHeaders(404, message.length);
                    try (OutputStream out = exchange.getResponseBody()) {
                        out.write(message);
                    }
                    return;
                }

                String name = file.getFileName().toString();
                String type = "text/plain; charset=UTF-8";

                if (name.endsWith(".html")) {
                    type = "text/html; charset=UTF-8";
                } else if (name.endsWith(".css")) {
                    type = "text/css; charset=UTF-8";
                } else if (name.endsWith(".js")) {
                    type = "application/javascript; charset=UTF-8";
                }

                exchange.getResponseHeaders()
                    .set("Content-Type", type);

                byte[] content = Files.readAllBytes(file);
                exchange.sendResponseHeaders(200, content.length);

                try (OutputStream out = exchange.getResponseBody()) {
                    out.write(content);
                }
            }
        });

        server.setExecutor(null);
        server.start();

        System.out.println("Expense Tracker running on port " + port);
    }
}
