package edu.decision.stream;

import java.io.IOException;

import static clojure.java.api.Clojure.var;
import static clojure.lang.RT.loadResourceScript;

public class Main {

    public static void main(String[] args) {
        try {
            loadResourceScript("DecisionStream.clj");
            var("DecisionStream", "main").invoke(args);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

}
