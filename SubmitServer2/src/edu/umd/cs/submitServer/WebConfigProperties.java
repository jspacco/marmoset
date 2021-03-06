package edu.umd.cs.submitServer;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

import javax.annotation.CheckForNull;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import com.google.common.base.Preconditions;
import com.google.common.base.Strings;

/**
 * Singleton for accessing configuration properties specified in
 * {@code web.properties}.
 * 
 * @author rwsims
 * 
 */
public final class WebConfigProperties {
	private static final WebConfigProperties instance;

	public static WebConfigProperties get() {
		return instance;
	}

	static {
		InputStream in = WebConfigProperties.class.getResourceAsStream("/web.properties");
		try {
			instance = new WebConfigProperties(in);
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}

	private final Properties props = new Properties();

	private WebConfigProperties(InputStream propertyStream) throws IOException {
		Preconditions.checkNotNull(propertyStream,
				"Could not load /web.properties");
		props.load(propertyStream);

		if (!Strings.isNullOrEmpty(props.getProperty("grades.server.jdbc.url"))) {
			props.setProperty("grades.server", "true");
		}
		String keyStore = props.getProperty(SubmitServerConstants.AUTHENTICATION_KEYSTORE_PATH);
		String keyPass = props.getProperty(SubmitServerConstants.AUTHENTICATION_KEYSTORE_PASSWORD);
		useSSL(keyStore, keyPass);
	}

	private void useSSL(String cacertsFile, String cacertsPassword) {
		if (cacertsFile != null)
			System.setProperty("javax.net.ssl.trustStore", cacertsFile);
		if (cacertsPassword != null)
			System.setProperty("javax.net.ssl.trustStorePassword",
					cacertsPassword);
	}
	
	@CheckForNull
	public String getProperty(String key) {
		Preconditions.checkArgument(!Strings.isNullOrEmpty(key), "Key must not be empty or null");
		return props.getProperty(key);
	}
	
	@Nonnull
	public String getRequiredProperty(String key) {
		return Preconditions.checkNotNull(getProperty(key), "Required web property " + key + " is not set.");
	}
	
	@Nonnull
	public String getRequiredProperty(String key, String defaultValue) {
		Preconditions.checkNotNull(defaultValue);
		String property = getProperty(key);
		return Strings.isNullOrEmpty(property) ? defaultValue : property; 
	}
	
	@Nullable
	public static String getConfigProperty(String key) {
		return instance.getProperty(key);
	}
}
