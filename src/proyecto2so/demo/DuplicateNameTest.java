package proyecto2so.demo;

import proyecto2so.core.DirectoryNode;
import proyecto2so.core.FileNode;

public class DuplicateNameTest {
    public static void main(String[] args) {
        System.out.println("ENTRANDO A DuplicateNameTest");

        DirectoryNode root = new DirectoryNode("root", "admin", null);

        DirectoryNode docs = new DirectoryNode("docs", "iraia", root);
        root.addSubdirectory(docs);

        boolean duplicateDirectoryBlocked = false;

        try {
            DirectoryNode docs2 = new DirectoryNode("docs", "iraia", root);
            root.addSubdirectory(docs2);
        } catch (IllegalStateException e) {
            duplicateDirectoryBlocked = true;
        }

        if (!duplicateDirectoryBlocked) {
            throw new RuntimeException("Error: se debería bloquear un directorio duplicado.");
        }

        FileNode note1 = new FileNode("nota.txt", "iraia", docs, 3, 0);
        docs.addFile(note1);

        boolean duplicateFileBlocked = false;

        try {
            FileNode note2 = new FileNode("nota.txt", "iraia", docs, 2, 4);
            docs.addFile(note2);
        } catch (IllegalStateException e) {
            duplicateFileBlocked = true;
        }

        if (!duplicateFileBlocked) {
            throw new RuntimeException("Error: se debería bloquear un archivo duplicado.");
        }

        boolean fileDirectoryCollisionBlocked = false;

        try {
            DirectoryNode noteDir = new DirectoryNode("nota.txt", "iraia", docs);
            docs.addSubdirectory(noteDir);
        } catch (IllegalStateException e) {
            fileDirectoryCollisionBlocked = true;
        }

        if (!fileDirectoryCollisionBlocked) {
            throw new RuntimeException("Error: se debería bloquear colisión entre archivo y directorio.");
        }

        DirectoryNode other = new DirectoryNode("other", "iraia", root);
        root.addSubdirectory(other);

        FileNode noteOther = new FileNode("nota.txt", "iraia", other, 1, 7);
        other.addFile(noteOther);

        if (other.getFileCount() != 1) {
            throw new RuntimeException("Error: debería poder existir nota.txt en otro directorio.");
        }

        System.out.println("Bloqueo de directorio duplicado correcto.");
        System.out.println("Bloqueo de archivo duplicado correcto.");
        System.out.println("Bloqueo de colisión archivo/directorio correcto.");
        System.out.println("Mismo nombre en distinto directorio permitido correctamente.");
        System.out.println("Paso 8 completado correctamente.");
    }
}