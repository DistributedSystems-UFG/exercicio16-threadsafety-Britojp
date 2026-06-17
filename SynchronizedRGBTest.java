import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicInteger;

public class SynchronizedRGBTest {

    static final int NUM_WRITERS = 4;
    static final int NUM_READERS = 4;
    static final int ITERATIONS  = 500_000;

    static final int RGB_RED   = (255 << 16) | (0 << 8) | 0;  
    static final int RGB_GREEN = (0 << 16) | (255 << 8) | 0;  

    public static void main(String[] args) throws InterruptedException {
        SynchronizedRGB color = new SynchronizedRGB(255, 0, 0, "Red");

        AtomicInteger partialStateErrors   = new AtomicInteger(0);
        AtomicInteger compoundActionErrors = new AtomicInteger(0);
        CountDownLatch latch = new CountDownLatch(1);

        Thread[] writers = new Thread[NUM_WRITERS];
        Thread[] readers = new Thread[NUM_READERS];

        for (int i = 0; i < NUM_WRITERS; i++) {
            writers[i] = new Thread(() -> {
                try { latch.await(); } catch (InterruptedException e) { Thread.currentThread().interrupt(); }
                for (int j = 0; j < ITERATIONS; j++) {
                    if (j % 2 == 0) color.set(255, 0, 0, "Red");
                    else             color.set(0, 255, 0, "Green");
                }
            }, "Writer-" + i);
        }

        for (int i = 0; i < NUM_READERS; i++) {
            readers[i] = new Thread(() -> {
                try { latch.await(); } catch (InterruptedException e) { Thread.currentThread().interrupt(); }
                for (int j = 0; j < ITERATIONS; j++) {
                    int rgb    = color.getRGB();    
                    String name = color.getName();  

                    if (rgb != RGB_RED && rgb != RGB_GREEN) {
                        partialStateErrors.incrementAndGet();
                    }

                    boolean isRed   = (rgb == RGB_RED);
                    boolean isGreen = (rgb == RGB_GREEN);
                    if ((isRed && !name.equals("Red")) || (isGreen && !name.equals("Green"))) {
                        compoundActionErrors.incrementAndGet();
                    }
                }
            }, "Reader-" + i);
        }

        for (Thread t : writers) t.start();
        for (Thread t : readers) t.start();
        latch.countDown(); // libera todas as threads ao mesmo tempo

        for (Thread t : writers) t.join();
        for (Thread t : readers) t.join();

        System.out.println("=== SynchronizedRGB — Teste de Thread Safety ===");
        System.out.printf("Configuracao: %d writers, %d readers, %d iteracoes cada%n%n",
            NUM_WRITERS, NUM_READERS, ITERATIONS);

        System.out.println("[Teste 1] Atomicidade interna de getRGB():");
        System.out.println("  Estado parcial detectado: " + partialStateErrors.get());
        System.out.println(partialStateErrors.get() == 0
            ? "  PASSOU - synchronized garante atomicidade de cada metodo individualmente."
            : "  FALHOU - estado parcial visto (inesperado).");

        System.out.println();
        System.out.println("[Teste 2] Consistencia de operacao composta (getRGB + getName):");
        System.out.println("  Inconsistencias detectadas: " + compoundActionErrors.get());
        System.out.println(compoundActionErrors.get() > 0
            ? "  FALHOU (esperado) - sem lock externo, outro thread muda estado entre as duas chamadas."
            : "  PASSOU - nenhuma race condition ocorreu nesta execucao (tente mais iteracoes).");
    }
}
