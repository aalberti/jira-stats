package aa.jira;

import java.io.FileInputStream;
import java.io.IOException;
import java.nio.file.Paths;
import java.util.Base64;
import java.util.Properties;

public class Authentication {
	private String user;
	private String password;

	private Authentication(String user, String password) {
		this.user = user;
		this.password = password;
	}

	public static Authentication load() {
		Properties properties = loadProperties();
		String user = (String) properties.get("user");
		String encryptedPassword = (String) properties.get("password");
		String password = new String(Base64.getDecoder().decode(encryptedPassword));
		return new Authentication(user, password);
	}

	private static Properties loadProperties() {
		try (FileInputStream propertiesFile = new FileInputStream(Paths.get(System.getProperty("user.home"), ".jira").toFile())) {
			Properties properties = new Properties();
			properties.load(propertiesFile);
			return properties;
		}
		catch (IOException e) {
			throw new RuntimeException(e);
		}
	}

	public String getUser() {
		return user;
	}

	public String getPassword() {
		return password;
	}
}
