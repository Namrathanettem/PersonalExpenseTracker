
import com.sun.net.httpserver.HttpServer;
import com.sun.net.httpserver.HttpExchange;
import java.io.*;
import java.net.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.*;

public class ExpenseServer {

    private static final Path ROOT = Paths.get("/app").toAbsolutePath().normalize();
    private static final Path WEB = ROOT;

    public static void main(String[] args) throws Exception {
        int port = Integer.parseInt(
            System.getenv().getOrDefault("PORT", "10000")
        );

        HttpServer server = HttpServer.create(
            new InetSocketAddress("0.0.0.0", port), 0
        );

        server.createContext("/api/transactions", exchange -> {
            if (!exchange.getRequestMethod().equals("GET")) {
                send(exchange, 405, "{\"error\":\"Method not allowed\"}");
                return;
            }
            runCpp(exchange, "list");
        });

        server.createContext("/api/summary", exchange -> {
            if (!exchange.getRequestMethod().equals("GET")) {
                send(exchange, 405, "{\"error\":\"Method not allowed\"}");
                return;
            }
            runCpp(exchange, "summary");
        });

        server.createContext("/api/add", exchange -> {
            if (!exchange.getRequestMethod().equals("POST")) {
                send(exchange, 405, "{\"error\":\"Method not allowed\"}");
                return;
            }

            String body = new String(
                exchange.getRequestBody().readAllBytes(),
                StandardCharsets.UTF_8
            );

            Map<String, String> form = new HashMap<>();

            for (String pair : body.split("&")) {
                String[] parts = pair.split("=", 2);
                String key = URLDecoder.decode(
                    parts[0], StandardCharsets.UTF_8
                );
                String value = parts.length > 1
                    ? URLDecoder.decode(parts[1], StandardCharsets.UTF_8)
                    : "";
                form.put(key, value);
            }

            String description = form.getOrDefault("description", "").trim();
            String amount = form.getOrDefault("amount", "").trim();
            String category = form.getOrDefault("category", "").trim();

            try {
                if (description.isEmpty() || category.isEmpty()
                        || !Double.isFinite(Double.parseDouble(amount))
                        || Double.parseDouble(amount) <= 0) {
                    send(exchange, 400,
                        "{\"error\":\"Enter a description, category, and positive amount\"}");
                    return;
                }
            } catch (NumberFormatException e) {
                send(exchange, 400, "{\"error\":\"Invalid amount\"}");
                return;
            }

            runCpp(exchange, "add", description, amount, category);
        });

        server.createContext("/", exchange -> serveWebsite(exchange));

        server.setExecutor(null);
        server.start();

        System.out.println("Personal Expense Tracker running on port " + port);
    }

    private static void runCpp(
            HttpExchange exchange, String... args) throws IOException {

        List<String> command = new ArrayList<>();
        command.add("/app/expense");
        command.addAll(Arrays.asList(args));

        ProcessBuilder builder = new ProcessBuilder(command);
        builder.directory(ROOT.toFile());
        builder.redirectErrorStream(true);

        try {
            Process process = builder.start();
            ByteArrayOutputStream output = new ByteArrayOutputStream();

            try (InputStream input = process.getInputStream()) {
                input.transferTo(output);
            }

            int exitCode = process.waitFor();
            String result = output.toString(StandardCharsets.UTF_8).trim();

            if (exitCode != 0 && result.isEmpty()) {
                result = "{\"error\":\"C++ backend failed\"}";
            }

            send(exchange, exitCode == 0 ? 200 : 400, result);

        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            send(exchange, 500, "{\"error\":\"Backend interrupted\"}");
        } catch (IOException e) {
            send(exchange, 500,
                "{\"error\":\"Could not start C++ backend\"}");
        }
    }

    private static void serveWebsite(HttpExchange exchange)
            throws IOException {

        String path = exchange.getRequestURI().getPath();

        if (path.equals("/")) {
            path = "/index.html";
        }

        Path file = WEB.resolve(path.substring(1)).normalize();

        if (!file.startsWith(WEB) || !Files.isRegularFile(file)) {
            send(exchange, 404, "File not found");
            return;
        }

        String name = file.getFileName().toString();
        String type = "application/octet-stream";

        if (name.endsWith(".html")) type = "text/html; charset=UTF-8";
        else if (name.endsWith(".css")) type = "text/css; charset=UTF-8";
        else if (name.endsWith(".js")) type = "text/javascript; charset=UTF-8";
        else if (name.endsWith(".png")) type = "image/png";
        else if (name.endsWith(".jpg") || name.endsWith(".jpeg"))
            type = "image/jpeg";
        else if (name.endsWith(".svg")) type = "image/svg+xml";

        byte[] data = Files.readAllBytes(file);
        exchange.getResponseHeaders().set("Content-Type", type);
        exchange.sendResponseHeaders(200, data.length);

        try (OutputStream out = exchange.getResponseBody()) {
            out.write(data);
        }
    }

    private static void send(
            HttpExchange exchange, int status, String message)
            throws IOException {

        byte[] data = message.getBytes(StandardCharsets.UTF_8);
        exchange.getResponseHeaders().set(
            "Content-Type", "application/json; charset=UTF-8"
        );
        exchange.getResponseHeaders().set("Cache-Control", "no-cache");
        exchange.sendResponseHeaders(status, data.length);

        try (OutputStream out = exchange.getResponseBody()) {
            out.write(data);
        }
    }
}
