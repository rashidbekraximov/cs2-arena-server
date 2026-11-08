package uz.cs2arena.match;

/**
 * Jamoaviy Elo: jamoalarning o'rtacha ELO'si bo'yicha kutilgan natija hisoblanadi,
 * jamoadagi har bir o'yinchi bir xil o'zgarish oladi.
 */
public final class EloCalculator {

    static final int K = 30;

    private EloCalculator() {}

    /** A jamoasi uchun o'zgarish (B uchun — teskari belgi). scoreA: 1 g'alaba, 0.5 durang, 0 mag'lubiyat. */
    public static int deltaForA(double avgA, double avgB, double scoreA) {
        double expectedA = 1.0 / (1.0 + Math.pow(10, (avgB - avgA) / 400.0));
        return (int) Math.round(K * (scoreA - expectedA));
    }
}
