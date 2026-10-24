
import com.sun.net.httpserver.HttpServer;
import com.sun.net.httpserver.HttpExchange;
import java.io.*;
import java.net.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.*;
import java.util.concurrent.Executors;

public class ExpenseServer {
    static final Path ROOT = Paths.get("").toAbsolutePath();
    static final Path WEB = ROOT.resolve("web");
    static final Path CPP = ROOT.resolve("backend/expense.exe");

    static String runCpp(String... args) throws Exception {
        if (!Files.exists(CPP)) {
            throw new IOException("C++ executable not found. Compile main.cpp first.");
        }

        List<String> command = new ArrayList<>();
        command.add(CPP.toString());
        command.addAll(Arrays.asList(args));

        Process process = new ProcessBuilder(command)
                .directory(ROOT.toFile())
                .redirectErrorStream(true)
                .start();

        String output;
        try (InputStream in = process.getInputStream()) {
            output = new String(in.readAllBytes(), StandardCharsets.UTF_8);
        }

        int exitCode = process.waitFor();
        if (exitCode != 0) {
            throw new IOException(output);
        }
        return output;
    }

    static void respond(HttpExchange ex, int status, String contentType,
                        String body) throws IOException {
        byte[] data = body.getBytes(StandardCharsets.UTF_8);
        ex.getResponseHeaders().set("Content-Type", contentType);
        ex.getResponseHeaders().set("Access-Control-Allow-Origin", "*");
        ex.sendResponseHeaders(status, data.length);
        try (OutputStream out = ex.getResponseBody()) {
            out.write(data);
        }
    }

    static Map<String, String> parseForm(String body) {
        Map<String, String> map = new HashMap<>();
        for (String pair : body.split("&")) {
            String[] parts = pair.split("=", 2);
            String key = URLDecoder.decode(parts[0], StandardCharsets.UTF_8);
            String value = parts.length > 1
                    ? URLDecoder.decode(parts[1], StandardCharsets.UTF_8) : "";
            map.put(key, value);
        }
        return map;
    }

    static void handle(HttpExchange ex) throws IOException {
        String path = ex.getRequestURI().getPath();
        String method = ex.getRequestMethod();

        try {
            if (path.equals("/api/transactions") && method.equals("GET")) {
                respond(ex, 200, "application/json; charset=UTF-8",
                        runCpp("list"));
                return;
            }

            if (path.equals("/api/summary") && method.equals("GET")) {
                respond(ex, 200, "application/json; charset=UTF-8",
                        runCpp("summary"));
                return;
            }

            if (path.equals("/api/add") && method.equals("POST")) {
                String body = new String(ex.getRequestBody().readAllBytes(),
                        StandardCharsets.UTF_8);
                Map<String, String> form = parseForm(body);

                String description = form.getOrDefault("description", "").trim();
                String category = form.getOrDefault("category", "").trim();
                String amountText = form.getOrDefault("amount", "").trim();

                if (description.isEmpty() || category.isEmpty()) {
                    respond(ex, 400, "application/json; charset=UTF-8",
                            "{\"error\":\"Description and category are required\"}");
                    return;
                }

                double amount;
                try {
                    amount = Double.parseDouble(amountText);
                    if (!Double.isFinite(amount) || amount <= 0) {
                        throw new NumberFormatException();
                    }
                } catch (NumberFormatException e) {
                    respond(ex, 400, "application/json; charset=UTF-8",
                            "{\"error\":\"Amount must be a positive number\"}");
                    return;
                }

                String result = runCpp("add", description,
                        Double.toString(amount), category);
                respond(ex, 200, "application/json; charset=UTF-8", result);
                return;
            }

            if (path.startsWith("/api/")) {
                respond(ex, 404, "application/json; charset=UTF-8",
                        "{\"error\":\"API route not found\"}");
                return;
            }

            String fileName = path.equals("/") ? "index.html" : path.substring(1);
            Path file = WEB.resolve(fileName).normalize();

            if (!file.startsWith(WEB) || !Files.isRegularFile(file)) {
                respond(ex, 404, "text/plain; charset=UTF-8", "File not found");
                return;
            }

            String type = fileName.endsWith(".css") ? "text/css; charset=UTF-8"
                    : fileName.endsWith(".js") ? "text/javascript; charset=UTF-8"
                    : "text/html; charset=UTF-8";

            respond(ex, 200, type, Files.readString(file, StandardCharsets.UTF_8));

        } catch (Exception e) {
            e.printStackTrace();
            String message = e.getMessage() == null ? "Server error" : e.getMessage();
            String safeMessage = message.replace("\\", "\\\\")
                    .replace("\"", "\\\"").replace("\n", " ");
            respond(ex, 500, "application/json; charset=UTF-8",
                    "{\"error\":\"" + safeMessage + "\"}");
        }
    }

    public static void main(String[] args) throws Exception {
        HttpServer server = HttpServer.create(
                new InetSocketAddress("localhost", 5000), 0);
        server.createContext("/", ExpenseServer::handle);
        server.setExecutor(Executors.newFixedThreadPool(4));
        server.start();

        System.out.println("Personal Expense Tracker is running!");
        System.out.println("Open http://localhost:5000");
    }
}

