package com.schneider.mstt.test.utils;

import java.util.Random;

public class IdGenerator {

    private static final char[] symbols = new char[10];

    static {
        for (int idx = 0; idx < 10; ++idx) {
            symbols[idx] = (char) ('0' + idx);
        }
    }

    private static final Random RANDOM = new Random();

    // Retourne un id de la forme prefix + n digits
    // Exemple : test05678
    public static String getRandomId(String prefix, int nbDigits) {
        char[] buf = new char[nbDigits];

        for (int i = 0; i < nbDigits; i++) {
            buf[i] = symbols[RANDOM.nextInt(symbols.length)];
        }

        return prefix + new String(buf);

    }
}
