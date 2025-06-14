package xyz.duncanruns.jingle.util;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import org.apache.commons.io.IOUtils;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.NameValuePair;
import org.apache.http.client.HttpClient;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.config.Registry;
import org.apache.http.config.RegistryBuilder;
import org.apache.http.conn.socket.ConnectionSocketFactory;
import org.apache.http.conn.socket.PlainConnectionSocketFactory;
import org.apache.http.conn.ssl.NoopHostnameVerifier;
import org.apache.http.conn.ssl.SSLConnectionSocketFactory;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.impl.conn.PoolingHttpClientConnectionManager;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.ssl.SSLContextBuilder;

import javax.net.ssl.SSLContext;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URL;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.KeyManagementException;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.util.Collections;
import java.util.List;

public class UploadUtil {

    private static final Gson GSON = new Gson();
    private static final HttpClient httpClient;

    static {
        try {
            httpClient = getHttpClient();
        } catch (NoSuchAlgorithmException | KeyStoreException | KeyManagementException e) {
            throw new RuntimeException(e);
        }
    }

    private UploadUtil() {}

    private static HttpClient getHttpClient() throws NoSuchAlgorithmException, KeyStoreException, KeyManagementException {
        //https://stackoverflow.com/a/28847175

        try {
            // Test a Let's Encrypt valid page
            IOUtils.toString(new URL("https://valid-isrgrootx1.letsencrypt.org/").openStream(), Charset.defaultCharset());
            // Normal functionality!
            return HttpClientBuilder.create().build();
        } catch (Exception e) {
            System.out.println("Outdated Java, GrabUtil is using an insecure HttpClient!");
        }

        HttpClientBuilder b = HttpClientBuilder.create();


        SSLContext sslContext = SSLContextBuilder.create().loadTrustMaterial((chain, authType) -> true).build();
        b.setSSLContext(sslContext);

        SSLConnectionSocketFactory sslSocketFactory = new SSLConnectionSocketFactory(sslContext, NoopHostnameVerifier.INSTANCE);
        Registry<ConnectionSocketFactory> socketFactoryRegistry = RegistryBuilder.<ConnectionSocketFactory>create()
                .register("http", PlainConnectionSocketFactory.getSocketFactory())
                .register("https", sslSocketFactory)
                .build();

        PoolingHttpClientConnectionManager connMgr = new PoolingHttpClientConnectionManager(socketFactoryRegistry);
        b.setConnectionManager(connMgr);
        return b.build();
    }

    /*
         https://api.mclo.gs/

         Paste a log file (POST https://api.mclo.gs/1/log)
         Field      Content     Description
         content    string	    The raw log file content as string. Maximum length is 10MiB and 25k lines,
                                will be shortened if necessary.
         */
    public static JsonObject uploadLog(Path latestTxt) throws IOException {
        HttpPost request = new HttpPost("https://api.mclo.gs/1/log");

        List<String> lines = Files.readAllLines(latestTxt);
        List<NameValuePair> pairs = Collections.singletonList(
                new BasicNameValuePair("content", String.join("\n", lines))
        );
        request.setEntity(new UrlEncodedFormEntity(pairs, "UTF-8"));

        HttpResponse response = httpClient.execute(request);
        HttpEntity entity = response.getEntity();

        return GSON.fromJson(new InputStreamReader(entity.getContent(), StandardCharsets.UTF_8), JsonObject.class);
    }

}