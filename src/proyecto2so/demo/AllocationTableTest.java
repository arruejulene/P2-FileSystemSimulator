package proyecto2so.demo;

import proyecto2so.core.AllocationEntry;
import proyecto2so.core.FileSystemService;

public class AllocationTableTest {
    public static void main(String[] args) {
        System.out.println("ENTRANDO A AllocationTableTest");

        FileSystemService fs = new FileSystemService();
        fs.initialize(10);

        fs.createDirectory("/", "docs", "iraia");
        fs.createFile("/docs", "a.txt", "iraia", 3);
        fs.createFile("/docs", "b.txt", "iraia", 2);

        AllocationEntry[] table = fs.getAllocationTable();

        if (table.length != 2) {
            throw new RuntimeException("Error: la tabla debería tener 2 filas.");
        }

        if (!"a.txt".equals(table[0].getFileName())) {
            throw new RuntimeException("Error: la primera fila debería ser a.txt.");
        }

        if (table[0].getBlockCount() != 3) {
            throw new RuntimeException("Error: a.txt debería mostrar 3 bloques.");
        }

        if (table[0].getFirstBlockId() < 0) {
            throw new RuntimeException("Error: a.txt debería tener un firstBlockId válido.");
        }

        if (!"b.txt".equals(table[1].getFileName())) {
            throw new RuntimeException("Error: la segunda fila debería ser b.txt.");
        }

        if (table[1].getBlockCount() != 2) {
            throw new RuntimeException("Error: b.txt debería mostrar 2 bloques.");
        }

        if (table[1].getFirstBlockId() < 0) {
            throw new RuntimeException("Error: b.txt debería tener un firstBlockId válido.");
        }

        System.out.println("Tabla de asignación generada correctamente.");
        System.out.println("Datos de a.txt correctos.");
        System.out.println("Datos de b.txt correctos.");
        System.out.println("Paso 17 completado correctamente.");
    }
}