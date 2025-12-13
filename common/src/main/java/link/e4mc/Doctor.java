package link.e4mc;

import com.google.gson.Gson;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.security.MessageDigest;
import java.util.HexFormat;

public class Doctor {
    public static String doctor() {
        var result = new StringBuilder();
        result.append("mod sha512sum: ");
        try {
            var bytes = Files.readAllBytes(Agnos.jarPath());
            var md = MessageDigest.getInstance("SHA-512");
            var digest = md.digest(bytes);
            result.append(HexFormat.of().formatHex(digest));
        } catch (Exception e) {
            result.append("exception during digest:\n");
            var baos = new ByteArrayOutputStream();
            e.printStackTrace(new PrintStream(baos, true, StandardCharsets.UTF_8));
            result.append(baos.toString(StandardCharsets.UTF_8));
        }
        result.append("\n");
        result.append("QuiclimeSession recorded exception:\n");
        var session = E4mcClient.session;
        if (session != null && session.failureCause != null) {
            var baos = new ByteArrayOutputStream();
            session.failureCause.printStackTrace(new PrintStream(baos, true, StandardCharsets.UTF_8));
            result.append(baos.toString(StandardCharsets.UTF_8));
            result.append("\n");
        } else {
            result.append("none recorded.\n");
        }
        result.append("natives CDN test results:\n");
        try {
            var httpClient = HttpClient.newHttpClient();
            var request = HttpRequest
                    .newBuilder(new URI("https://natives.e4mc.link/doctor-test-target"))
                    .build();
            var response = httpClient.send(request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
            var exceptional = false;
            if (response.statusCode() != 200) {
                exceptional = true;
                result.append("status code was not 200, it was: ");
                result.append(response.statusCode());
                result.append("\n");
            }
            if (!response.body().equals("if you can read this, e4mc natives are available. qmqj8c13nzdr0kd10gihcila")) {
                exceptional = true;
                result.append("response was unexpected, got: ");
                result.append(response.body());
                result.append("\n");
            }
            if (!exceptional) {
                result.append("no issues found.\n");
            }
        } catch (Exception e) {
            result.append("exception during request:\n");
            var baos = new ByteArrayOutputStream();
            e.printStackTrace(new PrintStream(baos, true, StandardCharsets.UTF_8));
            result.append(baos.toString(StandardCharsets.UTF_8));
            result.append("\n");
        }
        result.append("broker API test results:\n");
        QuiclimeSession.BrokerResponse brokerResponse = null;
        try {
            if (Config.INSTANCE.useBroker.value()) {
                result.append("using broker ");
                result.append(Config.INSTANCE.brokerUrl.value());
                result.append("\n");
                var httpClient = HttpClient.newHttpClient();
                var request = HttpRequest
                        .newBuilder(new URI(Config.INSTANCE.brokerUrl.value()))
                        .header("Accept", "application/json")
                        .build();
                var response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
                var exceptional = false;
                if (response.statusCode() != 200) {
                    exceptional = true;
                    result.append("status code was not 200, it was: ");
                    result.append(response.statusCode());
                    result.append("\n");
                }
                var gson = new Gson();
                brokerResponse = gson.fromJson(response.body(), QuiclimeSession.BrokerResponse.class);
                if (!exceptional) {
                    result.append("no issues found.\n");
                }
            } else {
                result.append("not using broker.\n");
                var resp = new QuiclimeSession.BrokerResponse();
                resp.id = "custom";
                resp.host = Config.INSTANCE.relayHost.value();
                resp.port = Config.INSTANCE.relayPort.value();
                brokerResponse = resp;
            }
        } catch (Exception e) {
            result.append("exception during request:\n");
            var baos = new ByteArrayOutputStream();
            e.printStackTrace(new PrintStream(baos, true, StandardCharsets.UTF_8));
            result.append(baos.toString(StandardCharsets.UTF_8));
            result.append("\n");
        }
        result.append("broker response:\n");
        if (brokerResponse == null) {
            result.append("none successfully received.\n");
        } else {
            result.append(String.format("relay id is %s, host is %s, port is %d.\n", brokerResponse.id, brokerResponse.host, brokerResponse.port));
        }
        result.append("relay HTTP test results:\n");
        if (brokerResponse == null) {
            result.append("no broker response.\n");
        } else if (!brokerResponse.host.endsWith(".e4mc.link")) {
            result.append("host is not standard. not attempting ping.\n");
        } else {
            try {
                var httpClient = HttpClient.newHttpClient();
                var request = HttpRequest
                        .newBuilder(new URI(String.format("https://%s/ping", brokerResponse.host)))
                        .build();
                var response = httpClient.send(request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
                var exceptional = false;
                if (response.statusCode() != 200) {
                    exceptional = true;
                    result.append("status code was not 200, it was: ");
                    result.append(response.statusCode());
                    result.append("\n");
                }
                if (!response.body().equals("OK")) {
                    exceptional = true;
                    result.append("response was unexpected, got: ");
                    result.append(response.body());
                    result.append("\n");
                }
                if (!exceptional) {
                    result.append("no issues found.\n");
                }
            } catch (Exception e) {
                result.append("exception during request:\n");
                var baos = new ByteArrayOutputStream();
                e.printStackTrace(new PrintStream(baos, true, StandardCharsets.UTF_8));
                result.append(baos.toString(StandardCharsets.UTF_8));
                result.append("\n");
            }
        }
        return result.toString();
    }
}
