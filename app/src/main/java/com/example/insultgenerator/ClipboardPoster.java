package com.example.insultgenerator;

import android.content.ClipData;
import android.content.ClipDescription;
import android.content.ClipboardManager;
import android.content.Context;

class ClipboardPoster {
    private final ClipboardManager manager;

    ClipboardPoster(Context context) {
        this.manager = (ClipboardManager) context.getSystemService(Context.CLIPBOARD_SERVICE);
    }

    /**
     * posts given string to clipboard as primary clip. This method is called only if "Auto-Copy
     * to Clipboard" is checked and an insult is generated, or when "copy to clipboard" is pressed
     *
     * @param insult the String to copy to the clipboard
     */
    void postToClipboard(CharSequence insult) {
        String[] mimeTypes = {ClipDescription.MIMETYPE_TEXT_PLAIN};
        ClipData clip = new ClipData("Insult", mimeTypes, new ClipData.Item(insult));

        this.manager.setPrimaryClip(clip);
    }
}
