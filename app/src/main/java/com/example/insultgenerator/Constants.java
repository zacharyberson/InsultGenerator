package com.example.insultgenerator;

final class Constants {
    static final int MS_IN_SECONDS = 1000;
    static final int PERIOD_DEFAULT_MS = 3000;
    static final double PERIOD_DEFAULT_SEC = 3.0;
    static final double PERIOD_MINIMUM_SEC = .5;
    static final double PERIOD_MAXIMUM_SEC = 20;
    static final float HALF_FLOAT = .5f;
    static final float FULL_FLOAT = 1f;

    interface NOTIFICATION {
        String GENERATED_INSULTS_CHANNEL = "12";
        int ID = 12;
    }

    interface KEYS {
        String SHAKESPEARE = "SHAKESPEARE";
        String AUTO_CLIP = "AUTO_CLIP";
        String AUTO_GEN = "AUTO_GEN";
        String MIX = "MIX";
        String SERVICE_RUNNING = "SERVICE_RUNNING";
        String INTERVAL_INPUT = "INTERVAL_INPUT";
        String INSULT_FIELD = "INSULT_FIELD";
        String BANKS = "BANKS";
        String MESSENGER = "MESSENGER";
        String PERIOD = "PERIOD";
    }

    interface ACTION {
        String MAIN_ACTION = "com.example.insultgenerator.action.main";
        String STOP_ACTION = "com.example.insultgenerator.action.stop";
    }
}
