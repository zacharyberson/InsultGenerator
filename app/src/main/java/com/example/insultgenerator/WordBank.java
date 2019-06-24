package com.example.insultgenerator;

import java.util.Random;

@SuppressWarnings("unused")
final class WordBank {
    static final int SHAKESPEARE = 0;
    static final int NUM_BANKS = 1;

    private enum Speech {ACTION, ADJECTIVE, COMMAND, COMPLETE, NOUN, START}

    private static final int FULL_INSULT_RANDOM_BOUND = 7;
    private static final int INSULT_LENGTH_BOUND = 40;
    private final boolean[] banks = new boolean[NUM_BANKS];
    private final Random random;

    WordBank() {
        for (int i = 0; i < this.banks.length; i++) {
            this.banks[i] = false;
        }
        System.out.println("Initialized banks");
        random = new Random();
    }

    WordBank(long seed) {
        for (int i = 0; i < this.banks.length; i++) {
            this.banks[i] = false;
        }
        System.out.println("Initialized banks");
        random = new Random(seed);
        System.out.println("Initialized randomizer with seed: " + Long.toString(seed));
    }

    WordBank(boolean[] banks) {
        int i = 0;
        for (boolean bool : banks) {
            this.banks[i++] = bool;
        }
        System.out.println("Initialized banks");
        random = new Random();
    }

    WordBank(boolean[] banks, long seed) {
        int i = 0;
        for (boolean bool : banks) {
            this.banks[i++] = bool;
        }
        System.out.println("Initialized banks");
        random = new Random(seed);
        System.out.println("Initialized randomizer with seed: " + Long.toString(seed));
    }

    /**
     * Randomize words and format insult, mixing bank sources if indicated
     *
     * @param mix: boolean, indicates whether to pull insults from one (false) or multiple (true) banks
     * @return String: the generated insult or default words
     * if there were no banks to pull from
     */
    @SuppressWarnings("ConstantConditions")
    String generateInsult(boolean mix) {
        //randomize bank, get random adj. default to "blanky" if no banks picked
        if (mix)
            return getMixedInsult();
        else {
            switch (getRandomBank()) {
                case SHAKESPEARE:
                    return getShakespeareInsult();
                default:
                    return "Blanky Blonky MacBlank-Blank";
            }
        }
    }

    /**
     * updates indicated bank, enabling or disabling it. This function is called whenever a bank
     * option is checked or unchecked from the MainActivity gui
     *
     * @param bank which bank to update
     * @param add  whether adding/enabling (true) or removing/disabling (false) bank
     */
    void updateBank(int bank, boolean add) {
        if (add) {
            addBank(bank);
        } else
            removeBank(bank);
    }

    private String getMixedInsult() {
        int selBank = getRandomBank();
        String[] parts;
        String insult;

        if (-1 == selBank) {
            return "Blanky Blonky MacBlank-Blank";
        } else {
            parts = new String[4];

            //System.out.println("randomized the bank of " + Integer.toString(selBank));
            parts[0] = getRandomWord(selBank, Speech.START);

            //re-randomize bank, keep getting second adj until different than first
            selBank = getRandomBank();
            //noinspection StatementWithEmptyBody
            parts[1] = getRandomWord(selBank, Speech.ADJECTIVE);

            //if mixing banks, re-randomize bank, keep getting second adj until different than first
            selBank = getRandomBank();
            //noinspection StatementWithEmptyBody
            while (parts[1].equals(parts[2] = getRandomWord(selBank, Speech.ADJECTIVE))) ;

            //if mixing banks, re-randomize bank, get random noun
            selBank = getRandomBank();
            parts[3] = getRandomWord(selBank, Speech.NOUN);
        }

        return parts[0] + " " + parts[1] + ", " + parts[2] + " " + parts[3];
    }

    private String getShakespeareInsult() {
        int length = 0;
        String preInsult1, preInsult2, start, noun, adj1, adj2, adj3, insult;

        // 1 in FULL_INSULT_RANDOM_BOUND chance to return full pre-made insult
        if (0 == random.nextInt(FULL_INSULT_RANDOM_BOUND))
            return getRandomWord(SHAKESPEARE, Speech.COMPLETE);

        adj2 = adj3 = preInsult1 = preInsult2 = null;
        insult = "";

        // get start
        start = getRandomWord(SHAKESPEARE, Speech.START);
        length += start.length();
        // get noun
        noun = getRandomWord(SHAKESPEARE, Speech.NOUN);
        length += noun.length();
        // get adj
        adj1 = getRandomWord(SHAKESPEARE, Speech.ADJECTIVE);
        length += adj1.length();

        // if really short, randomize whether to add preInsult or another adj
        if (INSULT_LENGTH_BOUND >= length)
            if (0 == random.nextInt(3)) {
                if (0 == random.nextInt(3)) {
                    preInsult1 = getRandomWord(SHAKESPEARE, Speech.ACTION);
                } else {
                    preInsult1 = getRandomWord(SHAKESPEARE, Speech.COMMAND);
                }
                length += preInsult1.length();
            } else {
                // get adj2, if same as 1 re-randomize until unique
                while (adj1.equals(adj2 = getRandomWord(SHAKESPEARE, Speech.ADJECTIVE))) ;
                length += adj2.length();
            }

        // if still too short, randomize whether to add preInsult (if not already added) or adj
        if (INSULT_LENGTH_BOUND >= length) {
            if (0 == random.nextInt(3)) {
                if (0 == random.nextInt(3)) {
                    if (null == preInsult1) {
                        preInsult1 = getRandomWord(SHAKESPEARE, Speech.ACTION);
                        //length += preInsult1.length();
                    } else {
                        while (preInsult1.equals(preInsult2 =
                                getRandomWord(SHAKESPEARE, Speech.ACTION))) ;
                        //length += preInsult2.length();
                    }
                } else {
                    if (null == preInsult1) {
                        preInsult1 = getRandomWord(SHAKESPEARE, Speech.COMMAND);
                        //length += preInsult1.length();
                    } else {
                        while (preInsult1.equals(preInsult2 =
                                getRandomWord(SHAKESPEARE, Speech.COMMAND))) ;
                        //length += preInsult2.length();
                    }
                }
            } else {
                if (null == adj2)
                    while (adj1.equals(adj2 = getRandomWord(SHAKESPEARE, Speech.ADJECTIVE))) ;
                    // get additional, unique adj
                else {
                    while (adj2.equals(adj3 = getRandomWord(SHAKESPEARE, Speech.ADJECTIVE))) ;
                    while (adj1.equals(adj3))
                        adj3 = getRandomWord(SHAKESPEARE, Speech.ADJECTIVE);
                }
                //length += preInsult.length();
            }
        }

        // compile insult
        if (null != preInsult1)
            insult += preInsult1 + ", ";

        if (null != preInsult2)
            insult += preInsult2 + ", ";

        // Change "a" to "an" if next word starts with vowel
        if (start.endsWith(" a") && adj1.substring(0, 1).matches("[aeiouAEIOU]"))
            start += "n";
        insult += start;
        //length += start.length();

        insult += " " + adj1;

        // unless previous adjective ends in " of", add comma
        if (null != adj2) {
            if (adj1.endsWith(" of"))
                insult += " " + adj2;
            else
                insult += ", " + adj2;
        }

        // unless previous adjective ends in " of", add comma
        if (null != adj3) {
            if (adj2.endsWith(" of"))
                insult += " " + adj3;
            else
                insult += ", " + adj3;
        }

        insult += " " + noun;

        return insult;
    }

    /**
     * Pick random word of indicated Speech from indicated bank
     *
     * @param bank,   which bank to pull word from
     * @param speech, type of speech bank to use i.e. adjective or noun
     * @return String, random word from bank
     * null if invalid bank or Speech
     */
    private String getRandomWord(int bank, Speech speech) {
        switch (bank) {
            case SHAKESPEARE:
                switch (speech) {
                    case ACTION:
                        return shakeAction[random.nextInt(shakeAction.length)];
                    case ADJECTIVE:
                        return shakeAdj[random.nextInt(shakeAdj.length)];
                    case COMMAND:
                        return shakeCommand[random.nextInt(shakeCommand.length)];
                    case COMPLETE:
                        return shakeComplete[random.nextInt(shakeComplete.length)];
                    case NOUN:
                        return shakeNoun[random.nextInt(shakeNoun.length)];
                    case START:
                        return shakeStart[random.nextInt(shakeStart.length)];
                    default:
                        System.err.println("error: WordBank.java.getRandomWord(bank, speech): " +
                                "invalid speech");
                        break;
                }
            default:
                System.err.println("error: WordBank.java.getRandomAdj(int bank, speech): invalid bank (" +
                        Integer.toString(bank) + ")");
                break;
        }
        return null;
    }

    /**
     * pick index of random, enabled bank
     *
     * @return int: random selected bank's index. Returns -1 if no enabled banks
     */
    private int getRandomBank() {
        int i = 0;
        int j = 0;
        int[] enabledBanks;

        //get number of enabled banks
        for (boolean bool : banks) {
            if (bool) i++;
        }

        // if no enabled banks
        if (0 == i) return -1;

        //get array of enabled bank indexes
        enabledBanks = new int[i];
        for (i = 0; i < banks.length; i++) {
            if (banks[i]) {
                enabledBanks[j++] = i;
                //System.out.println(Integer.toString(i) + " was found to be enabled");
            }
        }

        // pick and return random bank index from array of enabled bank indices
        return enabledBanks[random.nextInt(enabledBanks.length)];
    }

    private void addBank(int bank) {
        if (bank < NUM_BANKS && bank >= 0) this.banks[bank] = true;
        else System.err.println("error: adding bank " + Integer.toString(bank) +
                " outside bounds of available banks");
    }

    private void removeBank(int bank) {
        if (bank < NUM_BANKS && bank >= 0) this.banks[bank] = false;
        else System.err.println("error: removing bank " + Integer.toString(bank) +
                " outside bounds of available banks");
    }

    /**
     * Checks if two WordBank objects are equal
     * WordBank objects are considered equal iff their list of enabled banks match
     *
     * @param o: object to compare to this WordBank
     * @return boolean: True if both are WordBank objects with same bank list, false otherwise
     */
    @Override
    public boolean equals(Object o) {
        boolean[] oBanks;
        if (o.getClass() != this.getClass()) return false;

        oBanks = ((WordBank) o).banks;
        for (int i = 0; i < NUM_BANKS; i++) {
            if (this.banks[i] != oBanks[i]) return false;
        }
        return true;
    }

    private static final String[] shakeAction = {"You Lack Gall", "I Scorn You"};
    private static final String[] shakeAdj = {"Afraid", "Little", "Poor", "Sorry", "Unfortunate",
            "Drunken", "Three-Inch", "Froward", "Unable", "Lily-Liver'd", "Pigeon-Liver'd", "Sick",
            "Scurvy", "Swollen", "Fat-as-Butter", "Loathsome", "Poisonous Bunch-Backed", "Fat",
            "Cream Faced", "Clay-Brained", "Knotty-Pated", "Whoreson", "Obscene", "Greasy",
            "Damned", "Elvish-Mark'd", "Abortive", "Leathern-Jerkin", "Crystal-Button",
            "Knot-Pated", "Agatering", "Puke-Stocking", "Caddis-Garter", "Smooth-Tongue",
            "Lump of"};
    private static final String[] shakeCommand = {"Away", "Prick Thy Face", "Over-Red Thy Fear"};
    private static final String[] shakeComplete = {"Thy Wit's as Thick as a Tewkesbury Mustard",
            "I'll Beat Thee, but I Would Infect my Hands", "Methink'st Thou art a General " +
            "Offence and Every Man Should Beat Thee", "More of Your Conversation Would Infect " +
            "my Brain", "Thou art the Rankest Compound of Villainous Smell that Ever Offended " +
            "Nostril", "The Tartness of Thy Face Sours Ripe Grapes", "There’s No More Faith in " +
            "Thee than in a Stewed Prune", "Thine Face is Not Worth Sunburning", "Thou art an " +
            "Easy Glove, Thou Go Off and On at Pleasure", "Thou art Unfit for any Place but Hell"};
    private static final String[] shakeNoun = {"Knave", "Curr", "Coward",
            "Owner of No One Good Quality", "Most Notable Coward", "Infinite and Endless Liar",
            "Hourly Promise Breaker", "Starvelling", "Elf-Skin", "Dried Neat's-Tongue",
            "Fool", "Worm", "Boy", "Bull's-Pizzle", "Stock-Fish", "Guts",
            "Toad", "Trunk of Humours", "Bolting-Hutch of Beastliness",
            "Parcel of Dropsies", "Huge Bombard of Sack", "Stuffed Cloak-Bag of Guts",
            "Roasted Manningtree Ox with Pudding in Thy Belly", "Reverend Vice", "Grey Iniquity",
            "Father Ruffian", "Vanity in Years", "Boil", "Plague Sore", "Flesh-Monger", "Babe",
            "Loon", "Tallow-Catch", "Mountain Goat", "Rooting Hog", "Spanish Pouch",
            "Foul Deformity"};
    private static final String[] shakeStart = {"Thou", "You", "Thou art a"};
}
