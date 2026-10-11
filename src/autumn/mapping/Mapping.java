package autumn.mapping;

public class Mapping {
    // il faut assuer l'uniciter de l'url, quand on fait /liste, il faut pas qu'il y ait un autre /liste
    private String nomClasse;
    private String nomMethode;
    private boolean webApiRest;
    private Class<?>[] parameterTypes;

    public Mapping(String nomClasse, String nomMethode) {
        this(nomClasse, nomMethode, false, new Class<?>[0]);
    }

    public Mapping(String nomClasse, String nomMethode, boolean webApiRest) {
        this(nomClasse, nomMethode, webApiRest, new Class<?>[0]);
    }

    public Mapping(String nomClasse, String nomMethode, boolean webApiRest, Class<?>[] parameterTypes) {
        this.nomClasse = nomClasse;
        this.nomMethode = nomMethode;
        this.webApiRest = webApiRest;
        this.parameterTypes = parameterTypes == null ? new Class<?>[0] : parameterTypes.clone();
    }

    public String getNomClasse() {
        return nomClasse;
    }
    public void setNomClasse(String nomClasse) {
        this.nomClasse = nomClasse;
    }
    public String getNomMethode() {
        return nomMethode;
    }
    public void setNomMethode(String nomMethode) {
        this.nomMethode = nomMethode;
    }

    public boolean isWebApiRest() {
        return webApiRest;
    }

    public void setWebApiRest(boolean webApiRest) {
        this.webApiRest = webApiRest;
    }

    public Class<?>[] getParameterTypes() {
        return parameterTypes.clone();
    }

    public void setParameterTypes(Class<?>[] parameterTypes) {
        this.parameterTypes = parameterTypes == null ? new Class<?>[0] : parameterTypes.clone();
    }

    @Override
    public String toString() {
        return "Mapping{" +
            "nomClasse='" + nomClasse + '\'' +
            ", nomMethode='" + nomMethode + '\'' +
            '}';
    }
}
