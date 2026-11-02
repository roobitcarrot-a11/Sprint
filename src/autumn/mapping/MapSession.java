package autumn.mapping;

import autumn.auth.AuthConfig;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;

/** Accès à la session HTTP courante depuis un contrôleur. */
public class MapSession {
    private static volatile AuthConfig defaultConfig;

    private final HttpSession httpsession;
    private final AuthConfig authConfig;

    public MapSession(HttpServletRequest request) {
        this(request, defaultConfig());
    }

    public MapSession(HttpServletRequest request, AuthConfig authConfig) {
        this.httpsession = request.getSession();
        this.authConfig = authConfig == null ? defaultConfig() : authConfig;
    }

    public HttpSession getHttpsession() {
        return httpsession;
    }

    public HttpSession getHttpSession() {
        return httpsession;
    }

    /** Identifiant de l'utilisateur connecte, ou null si personne n'est connecte. */
    public Object getUserId() {
        return httpsession.getAttribute(authConfig.getUserKey());
    }

    /** Role de l'utilisateur connecte, ou null s'il n'en a pas. */
    public String getRole() {
        Object role = httpsession.getAttribute(authConfig.getRoleKey());
        return role == null ? null : role.toString();
    }

    public boolean isLoggedIn() {
        return getUserId() != null;
    }

    /** Connecte l'utilisateur sans role. */
    public void login(String userId) {
        login(userId, null);
    }

    /**
     * Connecte l'utilisateur avec un role. Un role null ou vide retire le role
     * eventuellement present dans la session.
     */
    public void login(String userId, String role) {
        if (userId == null || userId.trim().isEmpty()) {
            throw new IllegalArgumentException("L'identifiant utilisateur ne doit pas etre vide");
        }
        httpsession.setAttribute(authConfig.getUserKey(), userId.trim());
        if (role == null || role.trim().isEmpty()) {
            httpsession.removeAttribute(authConfig.getRoleKey());
        } else {
            httpsession.setAttribute(authConfig.getRoleKey(), role.trim());
        }
    }

    /** Invalide la session courante. */
    public void logout() {
        httpsession.invalidate();
    }

    private static AuthConfig defaultConfig() {
        AuthConfig config = defaultConfig;
        if (config == null) {
            synchronized (MapSession.class) {
                config = defaultConfig;
                if (config == null) {
                    config = AuthConfig.load();
                    defaultConfig = config;
                }
            }
        }
        return config;
    }
}
