package org.jmeterplugins.repository;

import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.InterruptedIOException;
import java.io.OutputStream;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.net.URI;
import java.net.URLEncoder;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Enumeration;
import java.util.List;
import net.sf.json.JSON;
import net.sf.json.JSONArray;
import net.sf.json.JSONObject;
import net.sf.json.JSONSerializer;
import net.sf.json.JsonConfig;
import org.apache.commons.codec.digest.DigestUtils;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.io.output.ByteArrayOutputStream;
import org.apache.http.Header;
import org.apache.http.HttpEntity;
import org.apache.http.HttpHost;
import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.auth.AuthScope;
import org.apache.http.auth.NTCredentials;
import org.apache.http.client.ServiceUnavailableRetryStrategy;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpRequestBase;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.conn.params.ConnRoutePNames;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.AbstractHttpClient;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.impl.client.DefaultHttpRequestRetryHandler;
import org.apache.http.params.CoreConnectionPNames;
import org.apache.http.params.HttpParams;
import org.apache.http.protocol.BasicHttpContext;
import org.apache.http.protocol.ExecutionContext;
import org.apache.http.protocol.HttpContext;
import org.apache.http.util.EntityUtils;
import org.apache.jmeter.JMeter;
import org.apache.jmeter.gui.GuiPackage;
import org.apache.jmeter.util.JMeterUtils;
import org.apache.jorphan.logging.LoggingManager;
import org.apache.log.Logger;
import org.jmeterplugins.repository.http.HttpRetryStrategy;

import java.util.zip.GZIPInputStream;


public class JARSourceHTTP extends JARSource {
    private static final Logger log = LoggingManager.getLoggerForClass();
    private static final int RETRY_COUNT = 1;
    private final String[] addresses;
    protected AbstractHttpClient httpClient;
    private int timeout = Integer.parseInt(JMeterUtils.getPropDefault("jpgc.repo.timeout", "30000"));
    private final ServiceUnavailableRetryStrategy retryStrategy = new HttpRetryStrategy(RETRY_COUNT, 5000);

    public JARSourceHTTP(String jmProp) {
        this.addresses = jmProp.split("[;]");
        httpClient = getHTTPClient();
    }

    private AbstractHttpClient getHTTPClient() {
        AbstractHttpClient client = new DefaultHttpClient();
        String proxyHost = System.getProperty("https.proxyHost", "");
        if (!proxyHost.isEmpty()) {
            int proxyPort = Integer.parseInt(System.getProperty("https.proxyPort", "-1"));
            log.info("Using proxy " + proxyHost + ":" + proxyPort);
            HttpParams params = client.getParams();
            HttpHost proxy = new HttpHost(proxyHost, proxyPort);
            params.setParameter(ConnRoutePNames.DEFAULT_PROXY, proxy);

            String proxyUser = System.getProperty(JMeter.HTTP_PROXY_USER, org.apache.jmeter.util.JMeterUtils.getProperty(JMeter.HTTP_PROXY_USER));
            if (proxyUser != null) {
                log.info("Using authenticated proxy with username: " + proxyUser);
                String proxyPass = System.getProperty(JMeter.HTTP_PROXY_PASS, JMeterUtils.getProperty(JMeter.HTTP_PROXY_PASS));

                String localHost;
                try {
                    localHost = InetAddress.getLocalHost().getCanonicalHostName();
                } catch (Throwable e) {
                    log.error("Failed to get local host name, defaulting to 'localhost'", e);
                    localHost = "localhost";
                }

                AuthScope authscope = new AuthScope(proxyHost, proxyPort);
                String proxyDomain = JMeterUtils.getPropDefault("http.proxyDomain", "");
                NTCredentials credentials = new NTCredentials(proxyUser, proxyPass, localHost, proxyDomain);
                client.getCredentialsProvider().setCredentials(authscope, credentials);
            }
        }
        client.setHttpRequestRetryHandler(new DefaultHttpRequestRetryHandler(RETRY_COUNT, true));
        return client;
    }

    protected JSON getJSON(String uri) throws IOException {

        log.debug("Requesting " + uri);

        HttpRequestBase get = new HttpGet(uri);
        HttpParams requestParams = get.getParams();
        get.setHeader("Accept-Encoding", "gzip");
        requestParams.setIntParameter(CoreConnectionPNames.SO_TIMEOUT, timeout);
        requestParams.setIntParameter(CoreConnectionPNames.CONNECTION_TIMEOUT, timeout);

        HttpResponse result = execute(get);
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        HttpEntity entity = result.getEntity();
        try {
            entity.writeTo(bos);
            byte[] bytes = bos.toByteArray();
            if (bytes == null) {
                bytes = "null".getBytes();
            }

            String response = isGZIPResponse(result) ? convertGZIPToString(bytes) : new String(bytes);
            int statusCode = result.getStatusLine().getStatusCode();
            if (statusCode >= 300) {
                log.warn("Response with code " + result + ": " + response);
                throw new IOException("Repository responded with wrong status code: " + statusCode);
            } else {
                log.debug("Response with code " + result + ": " + response);
            }
            return JSONSerializer.toJSON(response, new JsonConfig());
        } finally {
            get.abort();
            try {
                entity.getContent().close();
            } catch (IOException | IllegalStateException e) {
                log.warn("Exception in finalizing request", e);
            }
        }
    }

    private boolean isGZIPResponse(HttpResponse result) {
        Header encoding = result.getFirstHeader("Content-Encoding");
        return encoding != null && "gzip".equals(encoding.getValue().toLowerCase());
    }

    private String convertGZIPToString(byte[] bytes) throws IOException {
        GZIPInputStream gzipInputStream = new GZIPInputStream(new ByteArrayInputStream(bytes));
        InputStreamReader reader = new InputStreamReader(gzipInputStream);
        BufferedReader in = new BufferedReader(reader);

        final StringBuilder buffer = new StringBuilder();
        String line;
        while ((line = in.readLine()) != null) {
            buffer.append(line);
        }

        return buffer.toString();
    }

    protected JSONArray getRepositories(String path) throws IOException {
        final List<JSON> repositories = new ArrayList<>(addresses.length);
        for (String address : addresses) {
            repositories.add(getJSON(address + path));
        }

        final JSONArray result = new JSONArray();
        final List<String> pluginsIDs = new ArrayList<>();

        for (JSON json : repositories) {
            if (!(json instanceof JSONArray)) {
                throw new RuntimeException("Result is not array");
            }

            for (Object elm : (JSONArray) json) {
                // resolve plugin-id conflicts
                String id = ((JSONObject) elm).getString("id");
                if (!pluginsIDs.contains(id)) {
                    pluginsIDs.add(id);
                    result.add(elm);
                } else {
                    log.info("Plugin " + id + " will be skipped, because it is duplicated.");
                }
            }
        }
        return result;
    }

    @Override
    public JSON getRepo() throws IOException {
        return getRepositories("?installID=" + getInstallID());
    }

    /**
     * This function makes sure anonymous identifier sent
     *
     * @return unique ID for installation
     */
    public String getInstallID() {
        String str = "";
        str += getClass().getProtectionDomain().getCodeSource().getLocation().getFile();
        try {
            str += "\t" + InetAddress.getLocalHost().getHostName();
        } catch (UnknownHostException e) {
            log.warn("Cannot get local host name", e);
        }

        try {
            Enumeration<NetworkInterface> ifs = NetworkInterface.getNetworkInterfaces();
            for (NetworkInterface netint : Collections.list(ifs)) {
                str += "\t" + Arrays.toString(netint.getHardwareAddress());
            }
        } catch (SocketException e) {
            log.warn("Failed to get network addresses", e);
        }

        return getPlatformName() + '-' + DigestUtils.md5Hex(str) + '-' + getGuiMode();
    }

    private String getGuiMode() {
        return (GuiPackage.getInstance() == null) ? "nongui" : "gui";
    }

    protected String getPlatformName() {
        if (containsEnvironment("JENKINS_HOME")) {
            return "jenkins";
        } else if (containsEnvironment("TRAVIS")) {
            return "travis";
        } else if (containsEnvironmentPrefix("bamboo")) {
            return "bamboo";
        } else if (containsEnvironment("TEAMCITY_VERSION")) {
            return "teamcity";
        } else if (containsEnvironment("DOCKER_HOST")){
            return "docker";
        } else if (containsEnvironmentPrefix("AWS_")) {
            return "amazon";
        } else if (containsEnvironment("GOOGLE_APPLICATION_CREDENTIALS") || containsEnvironment("CLOUDSDK_CONFIG")) {
            return "google_cloud";
        } else if (containsEnvironment("WEBJOBS_NAME")) {
            return "azure";
        } else {
            return getOSName();
        }
    }

    private boolean containsEnvironment(String key) {
        return System.getenv().containsKey(key);
    }

    private boolean containsEnvironmentPrefix(String prefix) {
        for (String key : System.getenv().keySet()) {
            if (key.toLowerCase().startsWith(prefix.toLowerCase())) {
                return true;
            }
        }
        return false;
    }

    private String getOSName() {
        return System.getProperty("os.name").toLowerCase().replace(' ', '_');
    }

    @Override
    public void setTimeout(int timeout) {
        this.timeout = timeout;
    }

    @Override
    public DownloadResult getJAR(final String id, String location, final GenericCallback<String> callback) throws IOException {
        URI url = URI.create(location);
        log.info("Downloading: " + url);
        callback.notify("Downloading " + id + "...");
        HttpGet httpget = new HttpGet(url);

        HttpContext context = new BasicHttpContext();
        HttpResponse response = execute(httpget, context);
        if (response.getStatusLine().getStatusCode() != HttpStatus.SC_OK) {
            log.error("Error downloading url:"+url+" got response code:"+response.getStatusLine().getStatusCode());
            EntityUtils.consumeQuietly(response.getEntity());
            throw new IOException(response.getStatusLine().toString());
        }

        HttpEntity entity = response.getEntity();

        File tempFile = File.createTempFile(id, ".jar");

        final long size = entity.getContentLength();

        try (InputStream inputStream = entity.getContent();
             OutputStream fos = new FileOutputStream(tempFile);
             BufferedOutputStream bos = new BufferedOutputStream(fos)) {

            copyLarge(inputStream, bos, new GenericCallback<Long>() {
                @Override
                public void notify(Long progress) {
                    callback.notify(String.format("Downloading %s: %d%%", id, 100 * progress / size));
                }
            });
            callback.notify("Downloaded " + id + "...");

            Header cd = response.getLastHeader("Content-Disposition");
            String filename;
            if (cd != null) {
                filename = cd.getValue().split(";")[1].split("=")[1];
                if (filename.length() > 2 && filename.startsWith("\"") && filename.endsWith("\"")) {
                    filename = filename.substring(1, filename.length() - 1);
                }
            } else {
                HttpUriRequest currentReq = (HttpUriRequest) context.getAttribute(ExecutionContext.HTTP_REQUEST);
                HttpHost currentHost = (HttpHost) context.getAttribute(ExecutionContext.HTTP_TARGET_HOST);
                String currentUrl = (currentReq.getURI().isAbsolute()) ? currentReq.getURI().toString() : (currentHost.toURI() + currentReq.getURI());
                filename = FilenameUtils.getName(currentUrl);
            }

            return new DownloadResult(tempFile.getPath(), filename);
        }
    }

    @Override
    public void reportStats(String[] usageStats) throws IOException {
        ArrayList<String> stats = new ArrayList<>();
        stats.add(getInstallID());
        Collections.addAll(stats, usageStats);

        for (String uri : addresses) {
            HttpPost post = null;
            try {
                post = new HttpPost(uri);
                post.setHeader("Content-Type", "application/x-www-form-urlencoded");
                post.setHeader("Accept-Encoding", "gzip");
                HttpEntity body = new StringEntity("stats=" + URLEncoder.encode(Arrays.toString(stats.toArray(new String[0])), "UTF-8"));
                post.setEntity(body);
                HttpParams requestParams = post.getParams();
                requestParams.setIntParameter(CoreConnectionPNames.SO_TIMEOUT, 3000);
                requestParams.setIntParameter(CoreConnectionPNames.CONNECTION_TIMEOUT, 1000);

                log.debug("Requesting " + uri);
                execute(post);
            } finally {
                if (post != null) {
                    try {
                        post.abort();
                    } catch (Exception e) {
                        log.warn("Failure while aborting POST", e);
                    }
                }
            }
        }
    }

    private static long copyLarge(InputStream input, OutputStream output, GenericCallback<Long> progressCallback) throws IOException {
        byte[] buffer = new byte[4096];
        long count = 0L;

        int n;
        for (; -1 != (n = input.read(buffer)); count += (long) n) {
            output.write(buffer, 0, n);
            progressCallback.notify(count);
        }

        return count;
    }


    public HttpResponse execute(HttpUriRequest request) throws IOException {
        return execute(request, null);
    }

    public HttpResponse execute(HttpUriRequest request, HttpContext context) throws IOException {
        for (int c = 1;; c++) {
            HttpResponse response = httpClient.execute(request, context);
            try {
                if (retryStrategy.retryRequest(response, c, context)) {
                    EntityUtils.consume(response.getEntity());
                    long nextInterval = retryStrategy.getRetryInterval();
                    try {
                        log.debug("Wait for " + nextInterval);
                        Thread.sleep(nextInterval);
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                        throw new InterruptedIOException();
                    }
                } else {
                    return response;
                }
            } catch (RuntimeException ex) {
                try {
                    EntityUtils.consume(response.getEntity());
                } catch (IOException ioex) {
                    log.warn("I/O error consuming response content", ioex);
                }
                throw ex;
            }
        }
    }

}
