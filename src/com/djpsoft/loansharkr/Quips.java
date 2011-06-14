package com.djpsoft.loansharkr;

import java.util.Random;

public final class Quips {

    // Suppress default constructor for noninstantiability
    private Quips() {
        throw new AssertionError();
    }

    private static String[] quips = new String[]{
        "Time to break some kneecaps?",
        "This will not stand!",
        "Raaaaaaaargh!",
        "Time to intimidate and threaten?",
        "Call in the heavies.",
        "Cha-Ching! Its PAYDAY!",
    };

    public static String GetRandomOverdueLoanQuip() {
        Random rand = new Random();
        return quips[rand.nextInt(quips.length)];
    }

}
