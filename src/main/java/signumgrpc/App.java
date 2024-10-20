package signumgrpc;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.Reader;
import java.io.Writer;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.zip.GZIPInputStream;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import io.grpc.StatusRuntimeException;
import signumgrpc.proto.PeersServiceGrpc;
import signumgrpc.proto.PeersServiceGrpc.PeersServiceBlockingStub;
import signumgrpc.proto.getInfoReq;
import signumgrpc.proto.getInfoRes;

public class App {
        final public static int runs = 100;

        final public static String address = "xxx.xxx.xxx.xxx"; // Node's IP
        public static ManagedChannel channel = ManagedChannelBuilder.forAddress(address, 8127).usePlaintext().build();
        public static PeersServiceBlockingStub stub = PeersServiceGrpc.newBlockingStub(channel)
                        .withDeadlineAfter(8000, java.util.concurrent.TimeUnit.MILLISECONDS);

        public static JsonObject buildRequest() {
                JsonObject req = new JsonObject();
                req.addProperty("protocol", "B1");
                req.addProperty("requestType", "getInfo");
                req.addProperty("announcedAddress", "192.168.1.200:8123");
                req.addProperty("application", "BRS");
                req.addProperty("version", "3.8.2");
                req.addProperty("platform", "PC");
                req.addProperty("shareAddress", true);
                req.addProperty("networkName", "Signum");
                return req;
        }

        public static JsonObject error(String message) {
                JsonObject object = new JsonObject();
                object.addProperty("error", message);
                return object;
        }

        public static JsonObject sendGrpc(JsonElement request) {
                JsonObject response = new JsonObject();
                getInfoRes res = null;
                try {
                        res = stub.getInfo(getInfoReq.newBuilder()
                                        .setAnnouncedAddress(
                                                        request.getAsJsonObject().get("announcedAddress").getAsString())
                                        .setApplication(request.getAsJsonObject().get("application").getAsString())
                                        .setVersion(request.getAsJsonObject().get("version").getAsString())
                                        .setPlatform(request.getAsJsonObject().get("platform").getAsString())
                                        .setShareAddress(Boolean.parseBoolean(
                                                        request.getAsJsonObject().get("shareAddress").getAsString()))
                                        .setNetworkName(request.getAsJsonObject().get("networkName").getAsString())
                                        .build());
                } catch (StatusRuntimeException e) {
                        response = error(
                                        "Peer response exceeded the deadline. Error: " + e.getClass().toString() + ": "
                                                        + e.getMessage());
                        return response;
                } catch (Exception e) {
                        response = error(e.toString());
                        return response;
                }
                // TODO: (grpc) Check if everything is recieved
                response.addProperty("application", res.getApplication());
                response.addProperty("version", res.getVersion());
                response.addProperty("platform", res.getPlatform());
                response.addProperty("shareAddress", res.getShareAddress());
                response.addProperty("networkName", res.getNetworkName());
                return response;
        }

        public static JsonObject sendHttp(JsonElement request) { // Minimal verion of https://github.com/signum-network/signum-node/blob/7990b0d4cabdaffa1e78e41fbf09c43b9b78200f/src/brs/peer/PeerImpl.java#L343
                JsonObject response;
                HttpURLConnection connection = null;
                try {
                        StringBuilder buf = new StringBuilder("http://");
                        buf.append(address + ":7123");
                        buf.append("/burst");
                        URL url = new URL(buf.toString());

                        connection = (HttpURLConnection) url.openConnection();
                        connection.setRequestMethod("POST");
                        connection.setDoOutput(true);
                        connection.setConnectTimeout(4000);
                        connection.setReadTimeout(8000);
                        connection.addRequestProperty("User-Agent", "BRS/3.8.2");
                        connection.setRequestProperty("Accept-Encoding", "gzip");
                        connection.setRequestProperty("Connection", "close");

                        OutputStream cos = connection.getOutputStream();
                        try (Writer writer = new BufferedWriter(
                                        new OutputStreamWriter(cos, StandardCharsets.UTF_8))) {
                                JSON.writeTo(request, writer);
                        }

                        if (connection.getResponseCode() == HttpURLConnection.HTTP_OK) {
                                InputStream cis = connection.getInputStream();
                                InputStream responseStream = cis;
                                if ("gzip".equals(connection.getHeaderField("Content-Encoding"))) {
                                        responseStream = new GZIPInputStream(cis);
                                }
                                try (Reader reader = new BufferedReader(
                                                new InputStreamReader(responseStream,
                                                                StandardCharsets.UTF_8))) {
                                        response = JSON.getAsJsonObject(JSON.parse(reader));
                                }
                        } else {
                                response = error("Peer responded with HTTP " + connection.getResponseCode());
                        }
                } catch (RuntimeException | IOException e) {
                        response = error("Error getting response from peer: "
                                        + e.getClass().toString() + ": " + e.getMessage());
                }
                return response;
        }

        public static void main(String[] args) {
                if (args.length < 1) {
                        System.out.println("Usage: java -jar signumgrpc.jar <protocol>");
                        return;
                }
                final JsonObject req = buildRequest();

                switch (args[0].charAt(0)) {
                        case 'g':
                                for (int i = 0; i < runs; i++) {
                                        JsonObject res = sendGrpc(req);
                                        if (i % 20 == 0)
                                                System.out.println(res);
                                }
                                break;
                        case 'h':
                                for (int i = 0; i < runs; i++) {
                                        JsonObject res = sendHttp(req);
                                        if (i % 20 == 0)
                                                System.out.println(res);
                                }
                                break;
                }
        }
}
