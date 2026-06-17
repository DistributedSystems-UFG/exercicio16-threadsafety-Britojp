import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicInteger;

public class ImmutableRGBTest {

    static final int NUM_READERS   = 4;
    static final int NUM_INVERTERS = 4;
    static final int ITERATIONS    = 500_000;

    public static void main(String[] args) throws InterruptedException {
        final ImmutableRGB original = new ImmutableRGB(255, 0, 0, "Red");

        final int    expectedRGB          = original.getRGB();    // 0xFF0000 = 16711680
        final String expectedName         = original.getName();   // "Red"
        final int    expectedInvertedRGB  = (0 << 16) | (255 << 8) | 255; // 0x00FFFF
        final String expectedInvertedName = "Inverse of Red";

        AtomicInteger readErrors   = new AtomicInteger(0);
        AtomicInteger invertErrors = new AtomicInteger(0);
        CountDownLatch latch = new CountDownLatch(1);

        Thread[] readers   = new Thread[NUM_READERS];
        Thread[] inverters = new Thread[NUM_INVERTERS];

        for (int i = 0; i < NUM_READERS; i++) {
            readers[i] = new Thread(() -> {
                try { latch.await(); } catch (InterruptedException e) { Thread.currentThread().interrupt(); }
                for (int j = 0; j < ITERATIONS; j++) {
                    int rgb    = original.getRGB();
                    String name = original.getName();
                    if (rgb != expectedRGB || !name.equals(expectedName)) {
                        readErrors.incrementAndGet();
                    }
                }
            }, "Reader-" + i);
        }

        for (int i = 0; i < NUM_INVERTERS; i++) {
            inverters[i] = new Thread(() -> {
                try { latch.await(); } catch (InterruptedException e) { Thread.currentThread().interrupt(); }
                for (int j = 0; j < ITERATIONS; j++) {
                    ImmutableRGB inv = original.invert(); 

                    if (inv.getRGB() != expectedInvertedRGB || !inv.getName().equals(expectedInvertedName)) {
                        invertErrors.incrementAndGet();
                    }
                    if (original.getRGB() != expectedRGB) {
                        readErrors.incrementAndGet();
                    }
                }
            }, "Inverter-" + i);
        }

        for (Thread t : readers)   t.start();
        for (Thread t : inverters) t.start();
        latch.countDown();

        for (Thread t : readers)   t.join();
        for (Thread t : inverters) t.join();

        System.out.println("=== ImmutableRGB — Teste de Thread Safety ===");
        System.out.printf("Configuracao: %d readers, %d inverters, %d iteracoes cada%n%n",
            NUM_READERS, NUM_INVERTERS, ITERATIONS);

        System.out.println("[Teste 1] Estabilidade do objeto original (leituras compostas sem lock):");
        System.out.println("  Leituras incorretas: " + readErrors.get());
        System.out.println(readErrors.get() == 0
            ? "  PASSOU - objeto imutavel e sempre consistente, sem necessidade de sincronizacao."
            : "  FALHOU - estado alterado inesperadamente.");

        System.out.println();
        System.out.println("[Teste 2] Corretude de invert() sob concorrencia:");
        System.out.println("  Resultados incorretos de invert(): " + invertErrors.get());
        System.out.println(invertErrors.get() == 0
            ? "  PASSOU - invert() sempre produz novo objeto correto sem afetar o original."
            : "  FALHOU - resultado de invert() incorreto.");
    }
}
