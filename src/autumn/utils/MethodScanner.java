package autumn.utils;

import java.lang.reflect.Method;
import java.util.List;
import java.util.Map;

import autumn.annotation.Auth;
import autumn.annotation.Controller;
import autumn.annotation.UrlMapping;
import autumn.annotation.WebApiRest;
import autumn.auth.AuthConfig;
import autumn.mapping.Mapping;
import autumn.mapping.UrlKey;

public final class MethodScanner {

    private MethodScanner() {
    }

    public static void findMappings(
        String packageName,
        List<Class<?>> controllers,
        Map<UrlKey, Mapping> mappings,
        AuthConfig authConfig
    ) {
        if (controllers == null) {
            throw new IllegalArgumentException("La liste des controllers ne doit pas etre nulle");
        }
        if (authConfig == null) {
            throw new IllegalArgumentException("La configuration d'authentification ne doit pas etre nulle");
        }

        ControllerScanner.findControllers(packageName, controllers);

        for (Class<?> controllerClass : controllers) {
            registerControllerMappings(controllerClass, mappings, authConfig);
        }

        // return mappings;
    }

    private static void registerControllerMappings(
        Class<?> controllerClass,
        Map<UrlKey, Mapping> mappings,
        AuthConfig authConfig
    ) {
        if (controllerClass == null) {
            throw new IllegalArgumentException("Une classe controller ne doit pas etre nulle");
        }

        Controller controller = controllerClass.getAnnotation(Controller.class);
        if (controller == null) {
            throw new IllegalArgumentException(
                "La classe " + controllerClass.getName() + " n'est pas annotee avec @Controller"
            );
        }

        for (Method method : controllerClass.getDeclaredMethods()) {
            UrlMapping urlMapping = method.getAnnotation(UrlMapping.class);

            if (urlMapping == null || method.isBridge() || method.isSynthetic()) {
                continue;
            }

            String url = buildUrl(controller.path(), urlMapping.value());

            UrlKey urlObj = new UrlKey(url, urlMapping.method());
            Mapping mapping = new Mapping(
                controllerClass.getName(),
                method.getName(),
                method.isAnnotationPresent(WebApiRest.class),
                method.getParameterTypes()
            );

            Auth auth = resolveAuth(controllerClass, method);
            if (auth != null) {
                mapping.setAuthRequired(true);
                mapping.setRoles(normalizeAndValidateRoles(controllerClass, method, auth.value(), authConfig));
            }

            // Assure l'unicité de l'URL. Si une URL est déjà enregistrée, une exception est levée.
            if (mappings.containsKey(urlObj)) {
                throw new IllegalStateException("URL deja prise: " + urlObj);
            }

            mappings.put(urlObj, mapping);
        }
    }

    /** L'annotation de la methode remplace celle de la classe. */
    private static Auth resolveAuth(Class<?> controllerClass, Method method) {
        Auth auth = method.getAnnotation(Auth.class);
        return auth != null ? auth : controllerClass.getAnnotation(Auth.class);
    }

    private static String[] normalizeAndValidateRoles(
        Class<?> controllerClass,
        Method method,
        String[] roles,
        AuthConfig authConfig
    ) {
        String[] normalized = new String[roles.length];
        for (int i = 0; i < roles.length; i++) {
            String role = roles[i] == null ? "" : roles[i].trim();
            if (role.isEmpty()) {
                throw new IllegalStateException(
                    "Role vide dans @Auth sur " + describe(controllerClass, method)
                );
            }
            if (!authConfig.hasRole(role)) {
                throw new IllegalStateException(
                    "Role '" + role + "' utilise par " + describe(controllerClass, method)
                        + " absent de " + AuthConfig.ROLES_PROPERTY + " ("
                        + AuthConfig.FILE_NAME + "). Roles declares: " + authConfig.getRoles()
                );
            }
            normalized[i] = role;
        }
        return normalized;
    }

    private static String describe(Class<?> controllerClass, Method method) {
        return controllerClass.getName() + "#" + method.getName();
    }

    private static String buildUrl(String controllerPath, String methodPath) {
        String url = (normalizePath(controllerPath) + "/" + normalizePath(methodPath))
            .replaceAll("/{2,}", "/");

        if (url.length() > 1 && url.endsWith("/")) {
            return url.substring(0, url.length() - 1);
        }

        return url;
    }

    private static String normalizePath(String path) {
        if (path == null || path.isBlank() || "/".equals(path.trim())) {
            return "";
        }

        String normalizedPath = path.trim();

        if (!normalizedPath.startsWith("/")) {
            normalizedPath = "/" + normalizedPath;
        }

        while (normalizedPath.length() > 1 && normalizedPath.endsWith("/")) {
            normalizedPath = normalizedPath.substring(0, normalizedPath.length() - 1);
        }

        return normalizedPath;
    }
}
