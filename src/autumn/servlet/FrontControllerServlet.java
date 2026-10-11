package autumn.servlet;

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;

import autumn.mapping.Mapping;
import autumn.mapping.ModelAndView;
import autumn.utils.JsonSerializer;
import autumn.mapping.UrlKey;
import jakarta.servlet.*;
import jakarta.servlet.http.*;

public class FrontControllerServlet extends HttpServlet {
    List<Class<?>> listController;
    private Map<UrlKey, Mapping> routes;
    private List<File> views;

    public void init() throws ServletException {
        ServletContext context = getServletContext();
        routes = (Map<UrlKey, Mapping>) context.getAttribute("routes");
        views = (List<File>) context.getAttribute("views");
        if (routes == null) {
            throw new ServletException("Les routes n'ont pas été initialisées.");
        }
    }

    protected void processRequest(HttpServletRequest req, HttpServletResponse res)
        throws ServletException, IOException {
        res.setContentType("text/html;charset=UTF-8");
        String url = req.getRequestURI().substring(req.getContextPath().length());
        PrintWriter writer = res.getWriter();

        if (url.isEmpty()) {
            url = "/";
        }

        if ("/".equals(url)) {
            writeValidRoutes(writer);
            // for (File view : views) {
            //     writer.write("<p>Vue trouvée : " + view.getAbsolutePath() + "</p>");
            // }
            return;
        }

        UrlKey urlObj = new UrlKey(url, req.getMethod());
        Mapping mapping = routes.get(urlObj);

        if (mapping == null) {
            res.setStatus(HttpServletResponse.SC_NOT_FOUND);
            writer.write("<h1>Erreur 404</h1>");
            writer.write("<p>" + url + " n'est pas un lien valide </p>");
            writeValidRoutes(writer);
            return;
        }

        Method method;
        try {
            Class<?> controllerClass = Class.forName(mapping.getNomClasse());
            method = controllerClass.getDeclaredMethod(
                mapping.getNomMethode(), mapping.getParameterTypes()
            );
            Object controllerInstance = controllerClass.getDeclaredConstructor().newInstance();
            Object[] arguments = bindArguments(method, req);
            Object result = method.invoke(controllerInstance, arguments);

            if (mapping.isWebApiRest()) {
                res.setContentType("application/json;charset=UTF-8");
                writer.write(JsonSerializer.toJson(result));
                return;
            }

            if (result != null) {
                
                if(result instanceof ModelAndView) {

                    String viewName = ((ModelAndView) result).getUrl();
                    Map<String, Object> model = ((ModelAndView) result).getModel();
                    
                    String prefix = getServletContext().getInitParameter("prefix");
                    String suffix = getServletContext().getInitParameter("suffix");
                    
                    String viewPath = prefix + viewName + suffix;
                    // /WEB-INF/views/test/list.jsp

                    String realExpectedPath = getServletContext().getRealPath(viewPath);
                    // /home/itu/.../Framework/src/main/webapp/WEB-INF/views/test/list.jsp
                    boolean exists = false;

                    for (File view : views) {
                        if (view.getAbsolutePath().equals(realExpectedPath)) {
                            exists = true;
                            break;
                        }
                    }

                    if(exists && !model.isEmpty()) {
                        // writer.write("<br>La vue " + viewPath + " existe et le model n'est pas vide.");
                        for(Map.Entry<String, Object> entry : model.entrySet()) {
                            req.setAttribute(entry.getKey(), entry.getValue());
                        }
                        RequestDispatcher dispatcher = req.getRequestDispatcher(viewPath);
                        dispatcher.forward(req, res);
                    } else if(exists) {
                        // writer.write("<br>La vue " + viewPath + " existe mais le model est vide.");
                        RequestDispatcher dispatcher = req.getRequestDispatcher(viewPath);
                        dispatcher.forward(req, res);
                    } else {
                        writer.write("<br>La vue " + viewPath + " n'existe pas.");
                    }
                } else {
                    writer.write("<br>Resultat : " + result.toString());
                }

                
            }
        } catch (Exception e) {
            res.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            writer.write("<h1>Erreur 500</h1>");
            writer.write("<p>Une erreur est survenue lors de l'execution de la methode</p>");
            writer.write("<pre>" + e.getMessage() + "</pre>");
            e.printStackTrace(writer);
        }
    }

    private Object[] bindArguments(Method method, HttpServletRequest request) throws Exception {
        java.lang.reflect.Parameter[] parameters = method.getParameters(); // param anle méthode
        Object[] arguments = new Object[parameters.length];
        for (int i = 0; i < parameters.length; i++) {
            java.lang.reflect.Parameter parameter = parameters[i];
            if (!parameter.isNamePresent()) {
                throw new IllegalStateException(
                    "Le nom du paramètre '" + parameter.getName()
                        + "' est indisponible. Compilez les contrôleurs avec l'option -parameters."
                );
            }
            String parameterName = parameter.getName();
            Class<?> type = parameter.getType();
            if (isSimpleType(type)) {
                arguments[i] = convertValue(request.getParameter(parameterName), type);
            } else if (List.class.isAssignableFrom(type)) {
                arguments[i] = bindObjectList(type, parameter.getParameterizedType(), parameterName, request);
            } else {
                arguments[i] = bindObject(type, request, "");
            }
        }
        return arguments;
    }

    private Object bindObject(Class<?> type, HttpServletRequest request, String prefix) throws Exception {
        java.lang.reflect.Constructor<?> constructor = type.getDeclaredConstructor();
        constructor.setAccessible(true);
        Object instance = constructor.newInstance();
        for (Class<?> current = type;
             current != null && current != Object.class;
             current = current.getSuperclass()) {
            for (java.lang.reflect.Field field : current.getDeclaredFields()) {
                if (java.lang.reflect.Modifier.isStatic(field.getModifiers()) || field.isSynthetic()) {
                    continue;
                }
                String value = request.getParameter(prefix + field.getName());
                if (value == null) {
                    continue;
                }
                field.setAccessible(true);
                field.set(instance, convertValue(value, field.getType()));
            }
        }
        return instance;
    }

    private List<?> bindObjectList(
        Class<?> listType,
        Type parameterizedType,
        String parameterName,
        HttpServletRequest request
    ) throws Exception {
        if (!listType.isAssignableFrom(ArrayList.class)) {
            throw new IllegalArgumentException(
                "Le type de liste doit accepter ArrayList: " + listType.getName()
            );
        }
        if (!(parameterizedType instanceof ParameterizedType)) {
            throw new IllegalArgumentException(
                "La liste " + parameterName + " doit déclarer son type, par exemple List<Etudiant>."
            );
        }
        Type elementType = ((ParameterizedType) parameterizedType).getActualTypeArguments()[0];
        if (!(elementType instanceof Class<?>) || isSimpleType((Class<?>) elementType)) {
            throw new IllegalArgumentException(
                "La liste " + parameterName + " doit contenir des objets avec un type concret."
            );
        }

        String prefix = parameterName + "[";
        Set<Integer> indices = new TreeSet<>();
        for (String requestName : request.getParameterMap().keySet()) {
            if (!requestName.startsWith(prefix)) {
                continue;
            }
            int closingBracket = requestName.indexOf(']', prefix.length());
            if (closingBracket < 0 || closingBracket + 1 >= requestName.length()
                || requestName.charAt(closingBracket + 1) != '.') {
                throw new IllegalArgumentException(
                    "Nom de champ invalide pour la liste " + parameterName + ": " + requestName
                );
            }
            String indexValue = requestName.substring(prefix.length(), closingBracket);
            try {
                indices.add(Integer.valueOf(indexValue));
            } catch (NumberFormatException e) {
                throw new IllegalArgumentException(
                    "Index invalide pour le champ " + requestName, e
                );
            }
        }

        List<Object> objects = new ArrayList<>();
        int expectedIndex = 0;
        for (Integer index : indices) {
            if (index != expectedIndex) {
                throw new IllegalArgumentException(
                    "Les index de la liste " + parameterName + " doivent commencer à 0 et être continus."
                );
            }
            String objectPrefix = parameterName + "[" + index + "].";
            objects.add(bindObject((Class<?>) elementType, request, objectPrefix));
            expectedIndex++;
        }
        return objects;
    }

    private boolean isSimpleType(Class<?> type) {
        return type == String.class || type == int.class || type == Integer.class
            || type == long.class || type == Long.class || type == boolean.class
            || type == Boolean.class || type == double.class || type == Double.class
            || type == float.class || type == Float.class || type == short.class
            || type == Short.class || type == byte.class || type == Byte.class
            || type == char.class || type == Character.class || type.isEnum();
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    private Object convertValue(String value, Class<?> type) {
        if (value == null) {
            if (!type.isPrimitive()) {
                return null;
            }
            if (type == boolean.class) return false;
            if (type == char.class) return '\u0000';
            if (type == byte.class) return (byte) 0;
            if (type == short.class) return (short) 0;
            if (type == int.class) return 0;
            if (type == long.class) return 0L;
            if (type == float.class) return 0.0f;
            if (type == double.class) return 0.0d;
            throw new IllegalArgumentException("Type primitif non pris en charge: " + type.getName());
        }
        if (type == String.class) return value;
        if (type == int.class || type == Integer.class) return Integer.valueOf(value);
        if (type == long.class || type == Long.class) return Long.valueOf(value);
        if (type == boolean.class || type == Boolean.class) return Boolean.valueOf(value);
        if (type == double.class || type == Double.class) return Double.valueOf(value);
        if (type == float.class || type == Float.class) return Float.valueOf(value);
        if (type == short.class || type == Short.class) return Short.valueOf(value);
        if (type == byte.class || type == Byte.class) return Byte.valueOf(value);
        if (type == char.class || type == Character.class) {
            if (value.length() != 1) {
                throw new IllegalArgumentException("Valeur invalide pour un caractère: " + value);
            }
            return value.charAt(0);
        }
        if (type.isEnum()) return Enum.valueOf((Class<? extends Enum>) type, value);
        throw new IllegalArgumentException("Type de paramètre non pris en charge: " + type.getName());
    }

    private void writeValidRoutes(PrintWriter writer) {
        writer.write("<h2>URLs valides</h2>");

        if (routes.isEmpty()) {
            writer.write("<p>Aucune URL n'est enregistree.</p>");
            return;
        }

        writer.write("<ul>");
        for (Map.Entry<UrlKey, Mapping> entry : routes.entrySet()) {
            Mapping mapping = entry.getValue();
            writer.write("<li>");
            writer.write(entry.getKey().toString());
            writer.write(" - " + mapping.getNomClasse() + "#Methode :" + mapping.getNomMethode());
            writer.write("</li>");
        }
        writer.write("</ul>");
    }

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse res)
        throws ServletException, IOException {
        processRequest(req, res);
    }

    @Override
    protected void doPost(HttpServletRequest req,HttpServletResponse res)
        throws ServletException, IOException {
        processRequest(req, res);
    }    
}
