package proyecto2so.demo;

import proyecto2so.core.Block;

public class BlockTest {
    public static void main(String[] args) {
        Block block = new Block(0);

        System.out.println("=== PRUEBA PASO 1: BLOCK ===");

        if (block.getId() != 0) {
            throw new RuntimeException("Error: el id inicial no es 0.");
        }

        if (!block.isFree()) {
            throw new RuntimeException("Error: el bloque debería iniciar libre.");
        }

        if (block.getFileName() != null) {
            throw new RuntimeException("Error: fileName debería iniciar en null.");
        }

        if (block.getNextBlockId() != -1) {
            throw new RuntimeException("Error: nextBlockId debería iniciar en -1.");
        }

        System.out.println("Estado inicial correcto: " + block);

        block.occupy("A", 5);

        if (block.isFree()) {
            throw new RuntimeException("Error: el bloque no debería estar libre después de occupy().");
        }

        if (!"A".equals(block.getFileName())) {
            throw new RuntimeException("Error: fileName no se asignó correctamente.");
        }

        if (block.getNextBlockId() != 5) {
            throw new RuntimeException("Error: nextBlockId no se asignó correctamente.");
        }

        System.out.println("Estado ocupado correcto: " + block);

        block.release();

        if (!block.isFree()) {
            throw new RuntimeException("Error: el bloque debería volver a estar libre.");
        }

        if (block.getFileName() != null) {
            throw new RuntimeException("Error: fileName debería volver a null.");
        }

        if (block.getNextBlockId() != -1) {
            throw new RuntimeException("Error: nextBlockId debería volver a -1.");
        }

        System.out.println("Estado liberado correcto: " + block);
        System.out.println("✅ Paso 1 completado correctamente.");
    }
}