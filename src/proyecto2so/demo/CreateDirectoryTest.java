package proyecto2so.demo;

import proyecto2so.core.DirectoryNode;
import proyecto2so.core.FileSystemService;

public class CreateDirectoryTest {
    public static void main(String[] args) {
        System.out.println("ENTRANDO A CreateDirectoryTest");

        FileSystemService fs = new FileSystemService();
        fs.initialize(12);

        fs.createDirectory("/", "usuarios", "admin");
        fs.createDirectory("/usuarios", "iraia", "iraia");
        fs.createDirectory("/usuarios/iraia", "docs", "iraia");

        DirectoryNode root = fs.getRoot();

        if (root.getSubdirectoryCount() != 1) {
            throw new RuntimeException("Error: root debería tener 1 subdirectorio.");
        }

        DirectoryNode usuarios = root.getSubdirectories()[0];

        if (!"usuarios".equals(usuarios.getName())) {
            throw new RuntimeException("Error: debería existir /usuarios.");
        }

        if (!"/usuarios".equals(usuarios.getPath())) {
            throw new RuntimeException("Error: la ruta de usuarios debería ser /usuarios.");
        }

        if (usuarios.getSubdirectoryCount() != 1) {
            throw new RuntimeException("Error: /usuarios debería tener 1 subdirectorio.");
        }

        DirectoryNode iraia = usuarios.getSubdirectories()[0];

        if (!"iraia".equals(iraia.getName())) {
            throw new RuntimeException("Error: debería existir /usuarios/iraia.");
        }

        if (!"/usuarios/iraia".equals(iraia.getPath())) {
            throw new RuntimeException("Error: la ruta de iraia debería ser /usuarios/iraia.");
        }

        if (iraia.getSubdirectoryCount() != 1) {
            throw new RuntimeException("Error: /usuarios/iraia debería tener 1 subdirectorio.");
        }

        DirectoryNode docs = iraia.getSubdirectories()[0];

        if (!"docs".equals(docs.getName())) {
            throw new RuntimeException("Error: debería existir /usuarios/iraia/docs.");
        }

        if (!"/usuarios/iraia/docs".equals(docs.getPath())) {
            throw new RuntimeException("Error: la ruta de docs debería ser /usuarios/iraia/docs.");
        }

        boolean controlledFailure = false;

        try {
            fs.createDirectory("/ruta/inexistente", "fail", "admin");
        } catch (IllegalStateException e) {
            controlledFailure = true;
        }

        if (!controlledFailure) {
            throw new RuntimeException("Error: crear en una ruta inexistente debería fallar.");
        }

        System.out.println("Creación jerárquica de directorios correcta.");
        System.out.println("Rutas correctas.");
        System.out.println("Fallo controlado en ruta inexistente correcto.");
        System.out.println("Paso 10 completado correctamente.");
    }
}