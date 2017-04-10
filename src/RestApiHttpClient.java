/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */


import java.io.IOException;
import java.net.URL;
import java.nio.charset.Charset;
import java.util.ArrayList;
import org.apache.http.Header;
import org.apache.http.HttpHeaders;
import org.apache.http.HttpHost;
import org.apache.http.auth.AuthScope;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpDelete;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpPut;
import org.apache.http.client.methods.HttpRequestBase;
import org.apache.http.client.protocol.HttpClientContext;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.auth.DigestScheme;
import org.apache.http.impl.client.BasicAuthCache;
import org.apache.http.impl.client.BasicCredentialsProvider;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.impl.client.TargetAuthenticationStrategy;
import org.apache.http.message.BasicHeader;
import org.apache.http.protocol.BasicHttpContext;
import org.apache.http.util.EntityUtils;

/**
 * 
 * Base implementation Shopware REST-API Java HttpClient
 * without any JSON processing
 * 
 * @author Sven Eusewig
 * @version 1.0
 * @see <a href="https://developers.shopware.com/developers-guide/rest-api/">Shopware REST-API</a>
 * @see <a href="http://hc.apache.org/">Apache HttpComponents</a>
 */
public class RestApiHttpClient {
    
    /** Valid HTTP-Methods */
    public enum HTTP_METHOD {

        /**
         * GET
         */
        GET,

        /**
         * POST
         */
        POST,

        /**
         * PUT
         */
        PUT,

        /**
         * DELETE
         */
        DELETE
    };

    /** URL Endpoint */
    private URL apiEndpoint = null;
    
    /** HttpClient */
    private CloseableHttpClient httpclient;
    
    /** HttpClient Context */
    private HttpClientContext localContext;
    

    /**
     * Create an new {@link RestApiHttpClient} instance with Endpoint, username and API-Key
     * 
     * @param apiEndpoint The Hostname and Api-Endpoint (http://www.example.com/api)
     * @param username Shopware Username
     * @param password Api-Key from User-Administration
     */
    public RestApiHttpClient(URL apiEndpoint, String username, String password) {
        this.apiEndpoint = apiEndpoint;

        BasicHttpContext context = new BasicHttpContext();
        this.localContext = HttpClientContext.adapt(context);
        HttpHost target = new HttpHost(this.apiEndpoint.getHost(), -1, this.apiEndpoint.getProtocol());
        this.localContext.setTargetHost(target);
        
        
        TargetAuthenticationStrategy authStrat = new TargetAuthenticationStrategy();
        UsernamePasswordCredentials creds = new UsernamePasswordCredentials(username, password);
        BasicCredentialsProvider credsProvider = new BasicCredentialsProvider();
        AuthScope aScope = new AuthScope(target.getHostName(), target.getPort());
        credsProvider.setCredentials(aScope, creds);

        BasicAuthCache authCache = new BasicAuthCache();
        // Digest Authentication
        DigestScheme digestAuth = new DigestScheme(Charset.forName("UTF-8"));
        authCache.put(target, digestAuth);
        this.localContext.setAuthCache(authCache);
        this.localContext.setCredentialsProvider(credsProvider);
        
        ArrayList<Header> defHeaders = new ArrayList<>();
        defHeaders.add(new BasicHeader(HttpHeaders.ACCEPT, ContentType.APPLICATION_JSON.getMimeType()));
        this.httpclient = HttpClients.custom()
                .useSystemProperties()
                .setTargetAuthenticationStrategy(authStrat)
                .disableRedirectHandling()
                // make Rest-API Endpoint GZIP-Compression enable comment this out
                // Response-Compression is also possible
                .disableContentCompression()
                .setDefaultHeaders(defHeaders)
                .setDefaultCredentialsProvider(credsProvider)
                .build();

    }


    /**
     * Make a REST-Api Call with given HTTP-Method, Path and Data
     * 
     * @param method HTTP-Method (GET, POST, PUT and DELETE)
     * @param path String URL path with additional Parameters (articles/3)
     * @param data String POST or PUT data or null
     * @return String
     * @throws IOException in case of a problem or the connection was aborted
     */
    public String call(RestApiHttpClient.HTTP_METHOD method, String path, String data) throws IOException {
        HttpRequestBase httpquery;
        
        switch (method) {
            case POST:
                {
                    httpquery = new HttpPost(this.apiEndpoint + path);
                    StringEntity sendData = new StringEntity(data, ContentType.create(ContentType.APPLICATION_JSON.getMimeType(), "UTF-8"));

                    ((HttpPost) httpquery).setEntity(sendData);

                    break;
                }
            case PUT:
                {
                    httpquery = new HttpPut(this.apiEndpoint + path);
                    if(data != null) {
                        StringEntity sendData = new StringEntity(data, ContentType.create(ContentType.APPLICATION_JSON.getMimeType(), "UTF-8"));
                        ((HttpPut) httpquery).setEntity(sendData);
                    }
                    
                    break;
                }
            case DELETE:
                {
                    httpquery = new HttpDelete(this.apiEndpoint + path);
                    break;
                }
            default:
                httpquery = new HttpGet(this.apiEndpoint.toString() + path);
                break;
        }


        CloseableHttpResponse response = httpclient.execute(httpquery, this.localContext);

        String result = EntityUtils.toString(response.getEntity());
        response.close();

        return result;
    }
    
    
    /**
     * Make a GET REST-Api Call to URL
     * 
     * @param url String url
     * @return String
     * @throws IOException in case of a problem or the connection was aborted
     */
    public String get(String url) throws IOException {
        return call(RestApiHttpClient.HTTP_METHOD.GET, url, null);
    }
    
    
    /**
     * Make a PUT REST-Api Call to URL with String data (Batch)
     * 
     * @param url String url
     * @param data String data
     * @return String
     * @throws IOException in case of a problem or the connection was aborted
     */
    public String put(String url, String data) throws IOException {
        return call(RestApiHttpClient.HTTP_METHOD.PUT, url, data);
    }
    
    
    /**
     * Make a POST REST-Api Call to URL with String data
     * 
     * @param url String url
     * @param data String data
     * @return String
     * @throws IOException in case of a problem or the connection was aborted
     */
    public String post(String url, String data) throws IOException {
        return call(RestApiHttpClient.HTTP_METHOD.POST, url, data);
    }
    
    /**
     * Make a DELETE Rest-API Call with URL
     * 
     * @param url String url to Api
     * @return String
     * @throws IOException in case of a problem or the connection was aborted
     */
    public String delete(String url) throws IOException {
        return call(RestApiHttpClient.HTTP_METHOD.DELETE, url, null);
    }
    
    
    /**
     * Close and mark free the HttpClient Resources
     * 
     * @throws IOException in case of a problem or the connection was aborted
     */
    public void close() throws IOException {
        httpclient.close();
    }
}
