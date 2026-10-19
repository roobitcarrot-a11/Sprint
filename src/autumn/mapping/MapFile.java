package autumn.mapping;

/** Représente un fichier reçu dans une requête multipart. */
public class MapFile {
    private String extension;
    private String nomFichier;
    private byte[] contenu;

    public MapFile() {
    }

    public MapFile(String extension, String nomFichier, byte[] contenu) {
        this.extension = extension;
        this.nomFichier = nomFichier;
        this.contenu = contenu;
    }

    public String getExtension() {
        return extension;
    }

    public void setExtension(String extension) {
        this.extension = extension;
    }

    public String getNomFichier() {
        return nomFichier;
    }

    public void setNomFichier(String nomFichier) {
        this.nomFichier = nomFichier;
    }

    public byte[] getContenu() {
        return contenu;
    }

    public void setContenu(byte[] contenu) {
        this.contenu = contenu;
    }
}
