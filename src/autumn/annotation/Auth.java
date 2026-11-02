package autumn.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Exige que l'utilisateur soit connecte avant l'execution de la methode.
 *
 * <p>Sans valeur, il suffit que la session contienne l'identifiant utilisateur.
 * Avec une ou plusieurs valeurs, l'utilisateur doit en plus posseder l'un des
 * roles declares dans le fichier application.properties.</p>
 *
 * <pre>
 * &#64;Auth
 * &#64;Auth("RH")
 * &#64;Auth({"RH", "ADMIN"})
 * </pre>
 *
 * <p>Placee sur une classe, l'annotation protege toutes ses methodes. Une
 * annotation presente sur la methode remplace alors celle de la classe.</p>
 */
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.METHOD, ElementType.TYPE})
public @interface Auth {

    /** Roles autorises. Vide signifie : etre connecte suffit. */
    String[] value() default {};
}
