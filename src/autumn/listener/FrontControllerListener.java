package autumn.listener;


import java.lang.reflect.Constructor;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.io.File;

import autumn.auth.AuthConfig;
import autumn.mapping.Mapping;
import autumn.mapping.UrlKey;
import autumn.utils.MethodScanner;
import autumn.utils.ViewScanner;

import jakarta.servlet.ServletContext;
import jakarta.servlet.ServletContextEvent;
import jakarta.servlet.ServletContextListener;
import jakarta.servlet.annotation.WebListener;
import java.util.HashMap;

@WebListener
public class FrontControllerListener implements ServletContextListener {

    @Override
    public void contextInitialized(ServletContextEvent sce) {
        try {

            ServletContext context = sce.getServletContext();

            List<Class<?>> controllers = new ArrayList<>();
            List<File> views = new ArrayList<>();

            Map<UrlKey, Mapping> routes = new HashMap();


            String packageName = context.getInitParameter("controller");

            String prefix = context.getInitParameter("prefix");
            String suffix = context.getInitParameter("suffix");

            String viewDirectory = context.getRealPath(prefix);

            if (viewDirectory == null) {
                throw new IllegalStateException("Impossible de résoudre le chemin réel des vues: " + prefix);
            }

            AuthConfig authConfig = AuthConfig.load(
                Thread.currentThread().getContextClassLoader()
            );

            MethodScanner.findMappings(packageName, controllers, routes, authConfig);
            ViewScanner.findViews(viewDirectory, suffix, views);

            // Une seule instance par classe de controller, creee au demarrage.
            Map<String, Object> controllerInstances = createControllerInstances(controllers);

            context.setAttribute("routes", routes);
            context.setAttribute("views", views);
            context.setAttribute("authConfig", authConfig);
            context.setAttribute("controllers", controllerInstances);

        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Instancie une fois chaque controller et indexe les instances par nom de
     * classe. Ces instances (singletons) seront reutilisees a chaque requete.
     */
    private static Map<String, Object> createControllerInstances(List<Class<?>> controllers) {
        Map<String, Object> instances = new LinkedHashMap<>();
        for (Class<?> controllerClass : controllers) {
            if (instances.containsKey(controllerClass.getName())) {
                continue;
            }
            instances.put(controllerClass.getName(), instantiate(controllerClass));
        }
        return instances;
    }

    private static Object instantiate(Class<?> controllerClass) {
        Constructor<?> constructor;
        try {
            constructor = controllerClass.getDeclaredConstructor();
        } catch (NoSuchMethodException e) {
            throw new IllegalStateException(
                "Le controller " + controllerClass.getName()
                    + " doit avoir un constructeur sans argument",
                e
            );
        }
        try {
            constructor.setAccessible(true);
            return constructor.newInstance();
        } catch (ReflectiveOperationException e) {
            throw new IllegalStateException(
                "Impossible d'instancier le controller " + controllerClass.getName(),
                e
            );
        }
    }
}
