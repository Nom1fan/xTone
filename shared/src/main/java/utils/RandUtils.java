package utils;

import java.util.concurrent.ThreadLocalRandom;

/**
 * Created by Mor on 28/03/2016.
 */
public abstract class RandUtils {

    public static int getRand(int min, int max) {

        return ThreadLocalRandom.current().nextInt(min, max + 1);
    }
}
