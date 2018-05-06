package hello;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.security.KeyManagementException;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.cert.CertificateException;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

import javax.naming.ConfigurationException;
import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.SSLContext;

import org.apache.http.HeaderElement;
import org.apache.http.HeaderElementIterator;
import org.apache.http.HttpMessage;
import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpDelete;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpPut;
import org.apache.http.client.methods.HttpRequestBase;
import org.apache.http.client.utils.URIBuilder;
import org.apache.http.config.Registry;
import org.apache.http.config.RegistryBuilder;
import org.apache.http.conn.ConnectionKeepAliveStrategy;
import org.apache.http.conn.socket.ConnectionSocketFactory;
import org.apache.http.conn.socket.PlainConnectionSocketFactory;
import org.apache.http.conn.ssl.DefaultHostnameVerifier;
import org.apache.http.conn.ssl.NoopHostnameVerifier;
import org.apache.http.conn.ssl.SSLConnectionSocketFactory;
import org.apache.http.conn.ssl.TrustSelfSignedStrategy;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.impl.conn.PoolingHttpClientConnectionManager;
import org.apache.http.message.BasicHeaderElementIterator;
import org.apache.http.protocol.BasicHttpContext;
import org.apache.http.protocol.HTTP;
import org.apache.http.protocol.HttpContext;
import org.apache.http.ssl.SSLContexts;
import org.apache.http.util.EntityUtils;

public class HttpClientAdapter {

	private static BasicHttpContext httpContext = new BasicHttpContext();
	private static HashMap<String, CloseableHttpClient> clients = new HashMap<String, CloseableHttpClient>();

	public static CloseableHttpClient getInstance(HttpClientAdapterConfiguration config) throws ConfigurationException,
			KeyManagementException, NoSuchAlgorithmException, KeyStoreException, CertificateException, IOException {
		if (config == null || config.getTenantName() == null || config.getTenantName().trim().isEmpty()) {
			throw new IllegalArgumentException("TenantName is not set");
		}
		CloseableHttpClient client = clients.get(config.getTenantName());
		if (client == null) {
			client = initHttpClient(config);
			clients.put(config.getTenantName(), client);
		}
		return client;
	}

	public static void clear() {
		if (clients != null) {
			clients.clear();
		}
	}

	private static synchronized CloseableHttpClient initHttpClient(HttpClientAdapterConfiguration config)
			throws KeyManagementException, NoSuchAlgorithmException, KeyStoreException, CertificateException,
			IOException {

		Integer maxConnections = config.getMaxConnectionParam() != null ? config.getMaxConnectionParam() : 1000;
		Integer maxConnectionsPerRoute = config.getMaxConnectionPerRoute() != null ? config.getMaxConnectionPerRoute()
				: 200;

		HostnameVerifier HostnameVerifier = NoopHostnameVerifier.INSTANCE;
		if (config.checkHostname) {
			HostnameVerifier = new DefaultHostnameVerifier();
		}
		SSLContext sslcontext = getSSLContext(config);
		SSLConnectionSocketFactory sslSocketFactory = new SSLConnectionSocketFactory(sslcontext, HostnameVerifier);
		Registry<ConnectionSocketFactory> socketFactoryRegistry = RegistryBuilder.<ConnectionSocketFactory> create()
				.register("http", PlainConnectionSocketFactory.INSTANCE).register("https", sslSocketFactory).build();

		PoolingHttpClientConnectionManager cm = new PoolingHttpClientConnectionManager(socketFactoryRegistry);
		cm.setMaxTotal(maxConnections);
		cm.setDefaultMaxPerRoute(maxConnectionsPerRoute);

		System.out.println(
				String.format("maxConnection: {} - maxConnectionPerRoute: {}", maxConnections, maxConnectionsPerRoute));
		ConnectionKeepAliveStrategy myStrategy = new ConnectionKeepAliveStrategy() {

			public long getKeepAliveDuration(HttpResponse response, HttpContext arg1) {
				HeaderElementIterator it = new BasicHeaderElementIterator(
						response.headerIterator(HTTP.CONN_KEEP_ALIVE));
				while (it.hasNext()) {
					HeaderElement he = it.nextElement();
					String param = he.getName();
					String value = he.getValue();
					if (value != null && param.equalsIgnoreCase("timeout")) {
						return Long.parseLong(value) * 1000;
					}
				}
				return 10 * 1000;
			}
		};
		return HttpClients.custom().setKeepAliveStrategy(myStrategy).setConnectionManager(cm).build();
	}

	public static String doGet(HttpClientAdapterConfiguration config, String url, Map<String, String> requestParams,
			Map<String, String> pathParams, Map<String, String> headerParams) throws Exception {
		HttpGet get = new HttpGet(buildURI(url, requestParams, pathParams));
		return execute(config, get, headerParams);
	}

	public static String doPost(HttpClientAdapterConfiguration config, String url, String body, ContentType contentType,
			Map<String, String> requestParams, Map<String, String> pathParams, Map<String, String> headerParams)
			throws Exception {
		HttpPost post = new HttpPost(buildURI(url, requestParams, pathParams));
		if (body.length() > 0) {
			StringEntity entity = new StringEntity(body, contentType);
			post.setEntity(entity);
		}
		return execute(config, post, headerParams);
	}

	public static String doPut(HttpClientAdapterConfiguration config, String url, String body, ContentType contentType,
			Map<String, String> requestParams, Map<String, String> pathParams, Map<String, String> headerParams)
			throws Exception {
		HttpPut put = new HttpPut(buildURI(url, requestParams, pathParams));
		if (body.length() > 0) {
			StringEntity entity = new StringEntity(body, contentType);
			put.setEntity(entity);
		}
		return execute(config, put, headerParams);
	}

	public static String doDelete(HttpClientAdapterConfiguration config, String url, Map<String, String> requestParams,
			Map<String, String> pathParams, Map<String, String> headerParams) throws Exception {
		HttpDelete delete = new HttpDelete(buildURI(url, requestParams, pathParams));
		return execute(config, delete, headerParams);
	}

	private static String execute(HttpClientAdapterConfiguration config, HttpRequestBase message,
			Map<String, String> headerParams) throws Exception {

		CloseableHttpResponse responseClient = null;
		try {
			System.out.println("request url : " + message.getURI());
			fillHeader(message, headerParams);
			CloseableHttpClient httpClient = getInstance(config);
			RequestConfig requestConfig = RequestConfig.custom().setSocketTimeout(config.getSocketTimeout())
					.setConnectTimeout(config.getConnectionTimeout())
					.setConnectionRequestTimeout(config.getConnectionTimeout()).build();
			message.setConfig(requestConfig);
			responseClient = httpClient.execute(message, httpContext);
			int responseCode = responseClient.getStatusLine().getStatusCode();
			String responseBody = null;
			switch (responseCode) {
			case HttpStatus.SC_NOT_MODIFIED:
				responseBody = "";
				break;

			case HttpStatus.SC_OK:
				responseBody = EntityUtils.toString(responseClient.getEntity());
				break;
			default:
				throw new HttpClientException(responseClient.getStatusLine().getReasonPhrase(),
						responseClient.getStatusLine().getStatusCode(), responseBody);
			}
			return responseBody;
		} finally {
			if (responseClient != null) {
				responseClient.close();
			}
		}

	}

	private static SSLContext getSSLContext(HttpClientAdapterConfiguration config) throws KeyManagementException,
			NoSuchAlgorithmException, KeyStoreException, CertificateException, IOException {
		SSLContext sslContext = null;
		if (config.isSelfSigneCertificateAllowed()) {
			sslContext = SSLContexts.custom().loadTrustMaterial(null, new TrustSelfSignedStrategy()).build();
		} else if (config.getKeystorePath() != null && config.getKeystorePassword() != null) {
			sslContext = SSLContexts.custom()
					.loadTrustMaterial(new File(config.getKeystorePath()), config.getKeystorePassword().toCharArray())
					.build();
		} else {
			sslContext = SSLContexts.custom().build();
		}
		return sslContext;
	}

	private static void fillHeader(HttpMessage message, Map<String, String> headerParams) {

	}

	public static URI buildURI(String url, Map<String, String> requestParams, Map<String, String> pathParams)
			throws URISyntaxException {
		if (url.length() > 0) {
			if (pathParams != null) {
				for (Map.Entry<String, String> entry : pathParams.entrySet()) {
					url = url.replace(entry.getKey(), entry.getValue());
				}
			}
			URIBuilder builder = new URIBuilder(url);
			if (requestParams != null) {
				for (Entry<String, String> entry : requestParams.entrySet()) {
					builder.addParameter(entry.getKey(), entry.getValue());
				}
			}
			return builder.build();
		} else {
			throw new IllegalArgumentException("the url can't be null");
		}
	}

	public static class HttpClientAdapterConfiguration {

		private String tenantName;
		private String keystorePath;
		private String keystorePassword;
		private boolean checkHostname;
		private boolean selfSigneCertificateAllowed;
		private Integer maxConnectionParam;
		private Integer maxConnectionPerRoute;
		private Integer connectionTimeout;
		private Integer socketTimeout;

		public HttpClientAdapterConfiguration() {
			super();
		}

		public String getTenantName() {
			return tenantName;
		}

		public void setTenantName(String tenantName) {
			this.tenantName = tenantName;
		}

		public String getKeystorePath() {
			return keystorePath;
		}

		public void setKeystorePath(String keystorePath) {
			this.keystorePath = keystorePath;
		}

		public String getKeystorePassword() {
			return keystorePassword;
		}

		public void setKeystorePassword(String keystorePassword) {
			this.keystorePassword = keystorePassword;
		}

		public boolean getCheckHostname() {
			return checkHostname;
		}

		public void setCheckHostname(boolean checkHostname) {
			this.checkHostname = checkHostname;
		}

		public Integer getMaxConnectionParam() {
			return maxConnectionParam;
		}

		public void setMaxConnectionParam(Integer maxConnectionParam) {
			this.maxConnectionParam = maxConnectionParam;
		}

		public Integer getMaxConnectionPerRoute() {
			return maxConnectionPerRoute;
		}

		public void setMaxConnectionPerRoute(Integer maxConnectionPerRoute) {
			this.maxConnectionPerRoute = maxConnectionPerRoute;
		}

		public boolean isSelfSigneCertificateAllowed() {
			return selfSigneCertificateAllowed;
		}

		public void setSelfSigneCertificateAllowed(boolean selfSigneCertificateAllowed) {
			this.selfSigneCertificateAllowed = selfSigneCertificateAllowed;
		}

		public Integer getConnectionTimeout() {
			return connectionTimeout;
		}

		public void setConnectionTimeout(Integer connectionTimeout) {
			this.connectionTimeout = connectionTimeout;
		}

		public Integer getSocketTimeout() {
			return socketTimeout;
		}

		public void setSocketTimeout(Integer socketTimeout) {
			this.socketTimeout = socketTimeout;
		}

	}

	public static class HttpClientException extends Exception {

		/**
		 * 
		 */
		private static final long serialVersionUID = 1L;
		private String ReasonPhrase;
		private Integer responseCode;
		private String responseBody;

		public HttpClientException(String ReasonPhrase, Integer responseCode, String responseBody) {
			super(ReasonPhrase);
			this.setReasonPhrase(ReasonPhrase);
			this.setResponseCode(responseCode);
			this.setResponseBody(responseBody);
		}

		public String getReasonPhrase() {
			return ReasonPhrase;
		}

		public void setReasonPhrase(String reasonPhrase) {
			ReasonPhrase = reasonPhrase;
		}

		public Integer getResponseCode() {
			return responseCode;
		}

		public void setResponseCode(Integer responseCode) {
			this.responseCode = responseCode;
		}

		public String getResponseBody() {
			return responseBody;
		}

		public void setResponseBody(String responseBody) {
			this.responseBody = responseBody;
		}

		@Override
		public String toString() {
			return "HttpClientException [ReasonPhrase=" + ReasonPhrase + ", responseCode=" + responseCode
					+ ", responseBody=" + responseBody + "]";
		}

	}

}