package tests;

import java.net.URL;

/**
 * Created by Mor on 27/04/2016.
 */
public class Test {

    public static void main(String args[]) {

        ClassLoader classLoader = Test.class.getClassLoader();
        URL resource = classLoader.getResource("org/apache/http/conn/ssl/AllowAllHostnameVerifier.class");
        System.out.println(resource);
    }
}
