package autumn.auth;

import java.io.IOException;
import java.io.InputStream;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Properties;
import java.util.Set;

/**
 * Configuration de l'authentification, lue depuis application.properties.
 *
 * <pre>
 * auth.roles=ADMIN,RH,USER
 * auth.login.url=/login
 * auth.session.user-key=userId
 * auth.session.role-key=role
 * </pre>
 *
 * Le fichier est cherche a la racine du classpath (WEB-INF/classes cote
 * application web). Les valeurs par defaut sont utilisees si une cle manque.
 */
public final class AuthConfig {

    public static final String FILE_NAME = "application.properties";

    public static final String ROLES_PROPERTY = "auth.roles";
    public static final String LOGIN_URL_PROPERTY = "auth.login.url";
    public static final String USER_KEY_PROPERTY = "auth.session.user-key";
    public static final String ROLE_KEY_PROPERTY = "auth.session.role-key";

    public static final String DEFAULT_LOGIN_URL = "/login";
    public static final String DEFAULT_USER_KEY = "userId";
    public static final String DEFAULT_ROLE_KEY = "role";

    private final Set<String> roles;
    private final String loginUrl;
    private final String userKey;
    private final String roleKey;

    private AuthConfig(Set<String> roles, String loginUrl, String userKey, String roleKey) {
        this.roles = roles;
        this.loginUrl = loginUrl;
        this.userKey = userKey;
        this.roleKey = roleKey;
    }

    /** Charge la configuration depuis le classpath du thread courant. */
    public static AuthConfig load() {
        return load(Thread.currentThread().getContextClassLoader());
    }

    /** Charge la configuration depuis le classpath fourni. */
    public static AuthConfig load(ClassLoader classLoader) {
        Properties properties = new Properties();
        InputStream input = classLoader == null ? null : classLoader.getResourceAsStream(FILE_NAME);
        if (input == null) {
            input = AuthConfig.class.getClassLoader().getResourceAsStream(FILE_NAME);
        }
        if (input != null) {
            try (InputStream stream = input) {
                properties.load(stream);
            } catch (IOException e) {
                throw new IllegalStateException("Impossible de lire " + FILE_NAME, e);
            }
        }
        return fromProperties(properties);
    }

    /** Construit la configuration a partir de proprietes deja chargees. */
    public static AuthConfig fromProperties(Properties properties) {
        if (properties == null) {
            properties = new Properties();
        }

        Set<String> roles = new LinkedHashSet<>();
        for (String role : split(properties.getProperty(ROLES_PROPERTY))) {
            roles.add(role);
        }

        return new AuthConfig(
            Collections.unmodifiableSet(roles),
            value(properties, LOGIN_URL_PROPERTY, DEFAULT_LOGIN_URL),
            value(properties, USER_KEY_PROPERTY, DEFAULT_USER_KEY),
            value(properties, ROLE_KEY_PROPERTY, DEFAULT_ROLE_KEY)
        );
    }

    /** Configuration par defaut, sans aucun role declare. */
    public static AuthConfig defaults() {
        return fromProperties(new Properties());
    }

    public Set<String> getRoles() {
        return roles;
    }

    public boolean hasRole(String role) {
        return roles.contains(role);
    }

    public String getLoginUrl() {
        return loginUrl;
    }

    public String getUserKey() {
        return userKey;
    }

    public String getRoleKey() {
        return roleKey;
    }

    private static String value(Properties properties, String key, String defaultValue) {
        String value = properties.getProperty(key);
        if (value == null || value.trim().isEmpty()) {
            return defaultValue;
        }
        return value.trim();
    }

    private static String[] split(String value) {
        if (value == null || value.trim().isEmpty()) {
            return new String[0];
        }
        String[] parts = value.split(",");
        String[] cleaned = new String[parts.length];
        for (int i = 0; i < parts.length; i++) {
            cleaned[i] = parts[i].trim();
        }
        return cleaned;
    }
}
