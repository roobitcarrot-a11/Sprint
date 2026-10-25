package autumn.mapping;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;

/** Accès à la session HTTP courante depuis un contrôleur. */
public class MapSession {
    private HttpSession httpsession;

    public MapSession(HttpServletRequest request) {
        this.httpsession = request.getSession();
    }

    public HttpSession getHttpsession() {
        return httpsession;
    }

    public HttpSession getHttpSession() {
        return httpsession;
    }
}
