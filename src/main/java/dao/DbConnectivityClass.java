package dao;

import com.google.gson.*;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import model.Person;
import service.MyLogger;

import java.io.InputStreamReader;
import java.net.URI;
import java.net.http.*;
import java.nio.charset.StandardCharsets;
import java.security.*;
import java.security.spec.PKCS8EncodedKeySpec;
import java.util.Base64;

public class DbConnectivityClass {

    private static final String PROJECT_ID = "cscassignment9211";
    private static final String BASE_URL   =
            "https://firestore.googleapis.com/v1/projects/" + PROJECT_ID + "/databases/(default)/documents";
    private static final String TOKEN_URL  = "https://oauth2.googleapis.com/token";

    private final MyLogger   lg   = new MyLogger();
    private final HttpClient http = HttpClient.newHttpClient();
    private final Gson       gson = new Gson();
    private final ObservableList<Person> data = FXCollections.observableArrayList();

    private String     clientEmail;
    private PrivateKey privateKey;
    private String     accessToken;
    private long       tokenExpiry = 0;

    public DbConnectivityClass() {
        try (var is = getClass().getResourceAsStream("/tempKey.json")) {
            JsonObject sa = gson.fromJson(new InputStreamReader(is), JsonObject.class);
            clientEmail = sa.get("client_email").getAsString();
            String pem = sa.get("private_key").getAsString()
                    .replace("-----BEGIN PRIVATE KEY-----", "")
                    .replace("-----END PRIVATE KEY-----", "")
                    .replaceAll("\\s+", "");
            byte[] keyBytes = Base64.getDecoder().decode(pem);
            privateKey = KeyFactory.getInstance("RSA").generatePrivate(new PKCS8EncodedKeySpec(keyBytes));
        } catch (Exception e) {
            throw new RuntimeException("Firebase key load failed", e);
        }
    }

    private String token() {
        long now = System.currentTimeMillis() / 1000;
        if (accessToken != null && now < tokenExpiry - 60) return accessToken;
        try {
            String hdr = b64("{\"alg\":\"RS256\",\"typ\":\"JWT\"}");
            String claims = b64("{\"iss\":\"" + clientEmail + "\","
                    + "\"scope\":\"https://www.googleapis.com/auth/datastore\","
                    + "\"aud\":\"" + TOKEN_URL + "\","
                    + "\"exp\":" + (now + 3600) + ",\"iat\":" + now + "}");
            String input = hdr + "." + claims;
            Signature sig = Signature.getInstance("SHA256withRSA");
            sig.initSign(privateKey);
            sig.update(input.getBytes(StandardCharsets.UTF_8));
            String jwt = input + "." + Base64.getUrlEncoder().withoutPadding().encodeToString(sig.sign());

            String body = "grant_type=urn%3Aietf%3Aparams%3Aoauth%3Agrant-type%3Ajwt-bearer&assertion=" + jwt;
            HttpResponse<String> resp = http.send(
                    HttpRequest.newBuilder().uri(URI.create(TOKEN_URL))
                            .header("Content-Type", "application/x-www-form-urlencoded")
                            .POST(HttpRequest.BodyPublishers.ofString(body)).build(),
                    HttpResponse.BodyHandlers.ofString());
            JsonObject t = gson.fromJson(resp.body(), JsonObject.class);
            accessToken = t.get("access_token").getAsString();
            tokenExpiry = now + t.get("expires_in").getAsLong();
            return accessToken;
        } catch (Exception e) {
            throw new RuntimeException("Token fetch failed", e);
        }
    }

    private static String b64(String s) {
        return Base64.getUrlEncoder().withoutPadding().encodeToString(s.getBytes(StandardCharsets.UTF_8));
    }

    private HttpRequest.Builder auth(String url) {
        return HttpRequest.newBuilder().uri(URI.create(url))
                .header("Authorization", "Bearer " + token())
                .header("Content-Type", "application/json");
    }

    public ObservableList<Person> getData() {
        data.clear();
        try {
            HttpResponse<String> resp = http.send(auth(BASE_URL + "/users").GET().build(),
                    HttpResponse.BodyHandlers.ofString());
            JsonObject json = gson.fromJson(resp.body(), JsonObject.class);
            if (json.has("documents")) {
                for (JsonElement e : json.getAsJsonArray("documents")) {
                    JsonObject doc    = e.getAsJsonObject();
                    JsonObject fields = doc.getAsJsonObject("fields");
                    String docName = doc.get("name").getAsString();
                    int id = (int) fields.getAsJsonObject("id").get("integerValue").getAsLong();
                    data.add(new Person(id,
                            str(fields, "firstName"), str(fields, "lastName"),
                            str(fields, "department"), str(fields, "major"),
                            str(fields, "email"),      str(fields, "imageURL")));
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return data;
    }

    public void insertUser(Person person) {
        try {
            int nextId = nextId();
            person.setId(nextId);
            http.send(auth(BASE_URL + "/users")
                    .POST(HttpRequest.BodyPublishers.ofString(docBody(person))).build(),
                    HttpResponse.BodyHandlers.ofString());
            lg.makeLog("Inserted: " + person.getEmail());
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public int retrieveId(Person p) {
        return p.getId();
    }

    public void editUser(int id, Person p) {
        try {
            String docId = findDocId(id);
            if (docId == null) return;
            String url = BASE_URL + "/users/" + docId
                    + "?updateMask.fieldPaths=firstName&updateMask.fieldPaths=lastName"
                    + "&updateMask.fieldPaths=department&updateMask.fieldPaths=major"
                    + "&updateMask.fieldPaths=email&updateMask.fieldPaths=imageURL";
            p.setId(id);
            http.send(auth(url).method("PATCH", HttpRequest.BodyPublishers.ofString(docBody(p))).build(),
                    HttpResponse.BodyHandlers.ofString());
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public void deleteRecord(Person person) {
        try {
            String docId = findDocId(person.getId());
            if (docId == null) return;
            http.send(auth(BASE_URL + "/users/" + docId).DELETE().build(),
                    HttpResponse.BodyHandlers.ofString());
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private int nextId() throws Exception {
        String counterUrl = BASE_URL + "/meta/counter";
        HttpResponse<String> resp = http.send(auth(counterUrl).GET().build(),
                HttpResponse.BodyHandlers.ofString());
        JsonObject json = gson.fromJson(resp.body(), JsonObject.class);
        int next = 1;
        if (json.has("fields") && json.getAsJsonObject("fields").has("lastId"))
            next = (int) json.getAsJsonObject("fields").getAsJsonObject("lastId").get("integerValue").getAsLong() + 1;
        String patch = "{\"fields\":{\"lastId\":{\"integerValue\":\"" + next + "\"}}}";
        http.send(auth(counterUrl + "?updateMask.fieldPaths=lastId")
                .method("PATCH", HttpRequest.BodyPublishers.ofString(patch)).build(),
                HttpResponse.BodyHandlers.ofString());
        return next;
    }

    private String findDocId(int id) throws Exception {
        String queryUrl = "https://firestore.googleapis.com/v1/projects/" + PROJECT_ID
                + "/databases/(default)/documents:runQuery";
        String body = "{\"structuredQuery\":{\"from\":[{\"collectionId\":\"users\"}],"
                + "\"where\":{\"fieldFilter\":{\"field\":{\"fieldPath\":\"id\"},"
                + "\"op\":\"EQUAL\",\"value\":{\"integerValue\":\"" + id + "\"}}}}}";
        HttpResponse<String> resp = http.send(
                auth(queryUrl).POST(HttpRequest.BodyPublishers.ofString(body)).build(),
                HttpResponse.BodyHandlers.ofString());
        JsonArray arr = gson.fromJson(resp.body(), JsonArray.class);
        if (arr != null && !arr.isEmpty()) {
            JsonObject first = arr.get(0).getAsJsonObject();
            if (first.has("document")) {
                String name = first.getAsJsonObject("document").get("name").getAsString();
                return name.substring(name.lastIndexOf('/') + 1);
            }
        }
        return null;
    }

    private String docBody(Person p) {
        return "{\"fields\":{"
                + "\"id\":{\"integerValue\":\"" + p.getId() + "\"},"
                + "\"firstName\":{\"stringValue\":\"" + p.getFirstName() + "\"},"
                + "\"lastName\":{\"stringValue\":\"" + p.getLastName() + "\"},"
                + "\"department\":{\"stringValue\":\"" + p.getDepartment() + "\"},"
                + "\"major\":{\"stringValue\":\"" + p.getMajor() + "\"},"
                + "\"email\":{\"stringValue\":\"" + p.getEmail() + "\"},"
                + "\"imageURL\":{\"stringValue\":\"" + (p.getImageURL() != null ? p.getImageURL() : "") + "\"}"
                + "}}";
    }

    private String str(JsonObject fields, String key) {
        if (fields.has(key) && fields.getAsJsonObject(key).has("stringValue"))
            return fields.getAsJsonObject(key).get("stringValue").getAsString();
        return "";
    }

    public void queryUserByLastName(String name) {
        try {
            String queryUrl = "https://firestore.googleapis.com/v1/projects/" + PROJECT_ID
                    + "/databases/(default)/documents:runQuery";
            String body = "{\"structuredQuery\":{\"from\":[{\"collectionId\":\"users\"}],"
                    + "\"where\":{\"fieldFilter\":{\"field\":{\"fieldPath\":\"lastName\"},"
                    + "\"op\":\"EQUAL\",\"value\":{\"stringValue\":\"" + name + "\"}}}}}";
            HttpResponse<String> resp = http.send(
                    auth(queryUrl).POST(HttpRequest.BodyPublishers.ofString(body)).build(),
                    HttpResponse.BodyHandlers.ofString());
            JsonArray arr = gson.fromJson(resp.body(), JsonArray.class);
            if (arr != null) for (JsonElement e : arr) {
                JsonObject obj = e.getAsJsonObject();
                if (obj.has("document")) {
                    JsonObject f = obj.getAsJsonObject("document").getAsJsonObject("fields");
                    lg.makeLog("Found: " + str(f, "firstName") + " " + str(f, "lastName"));
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void listAllUsers() {
        for (Person p : data)
            lg.makeLog("ID: " + p.getId() + ", " + p.getFirstName() + " " + p.getLastName());
    }

    public boolean connectToDatabase() {
        return privateKey != null;
    }
}
