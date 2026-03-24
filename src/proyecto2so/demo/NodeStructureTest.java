package proyecto2so.demo;

import proyecto2so.core.DirectoryNode;
import proyecto2so.core.FileNode;

public class NodeStructureTest {
    public static void main(String[] args) {
        System.out.println("ENTRANDO A NodeStructureTest");

        DirectoryNode root = new DirectoryNode("root", "admin", null);
        DirectoryNode docs = new DirectoryNode("docs", "iraia", root);
        root.addSubdirectory(docs);

        DirectoryNode privado = new DirectoryNode("privado", "iraia", docs);
        docs.addSubdirectory(privado);

        FileNode nota = new FileNode("nota.txt", "iraia", docs, 3, 0);
        docs.addFile(nota);

        if (!"/".equals(root.getPath())) {
            throw new RuntimeException("Error: la raíz debería tener path '/'.");
        }

        if (docs.getParent() != root) {
            throw new RuntimeException("Error: el padre de docs debería ser root.");
        }

        if (privado.getParent() != docs) {
            throw new RuntimeException("Error: el padre de privado debería ser docs.");
        }

        if (nota.getParent() != docs) {
            throw new RuntimeException("Error: el padre de nota.txt debería ser docs.");
        }

        if (!"/docs".equals(docs.getPath())) {
            throw new RuntimeException("Error: la ruta de docs debería ser '/docs'.");
        }

        if (!"/docs/privado".equals(privado.getPath())) {
            throw new RuntimeException("Error: la ruta de privado debería ser '/docs/privado'.");
        }

        if (!"/docs/nota.txt".equals(nota.getPath())) {
            throw new RuntimeException("Error: la ruta de nota.txt debería ser '/docs/nota.txt'.");
        }

        if (root.getSubdirectoryCount() != 1) {
            throw new RuntimeException("Error: root debería tener 1 subdirectorio.");
        }

        if (docs.getSubdirectoryCount() != 1) {
            throw new RuntimeException("Error: docs debería tener 1 subdirectorio.");
        }

        if (docs.getFileCount() != 1) {
            throw new RuntimeException("Error: docs debería tener 1 archivo.");
        }

        if (!"privado".equals(docs.getSubdirectories()[0].getName())) {
            throw new RuntimeException("Error: docs debería contener el subdirectorio privado.");
        }

        if (!"nota.txt".equals(docs.getFiles()[0].getName())) {
            throw new RuntimeException("Error: docs debería contener el archivo nota.txt.");
        }

        System.out.println("Jerarquía creada correctamente.");
        System.out.println("Rutas generadas correctamente.");
        System.out.println("Relaciones padre-hijo correctas.");
        System.out.println("Paso 7 completado correctamente.");
    }
}