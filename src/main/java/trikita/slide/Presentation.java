package trikita.slide;

import android.content.Context;
import android.net.Uri;

import org.immutables.gson.Gson;
import org.immutables.value.Value;

@Value.Immutable
@Gson.TypeAdapters
public abstract class Presentation {
    public abstract String text();
    public abstract int cursor();

    public abstract int colorScheme();

    public abstract String plantUMLEndPoint();
    public abstract boolean plantUMLEnabled();

    public abstract String plantUMLTemplateBefore();
    public abstract String plantUMLTemplateAfter();

    public abstract String templateBefore();
    public abstract String templateAfter();

    public abstract int pdfResolution();

    public static boolean possibleLineBreak(String s) {
        return s.startsWith(".");
    }

    public static boolean isBackground(String line) {
        return line.startsWith("@");
    }

    public static String asBackground(Uri uri) {
        return "@" + uri.toString();
    }

    public String[] pages() {
        return text().split("(\n){2,}");
    }

    public int pageForCursor(int cursor) {
        String s = text();
        int p = 1;

        char prev = 0, prevprev = 0;
        for(int i = 0; i < s.length() && i <= cursor; ++i) {
            char cur = s.charAt(i);
            if(cur != '\n' && prev == '\n' && prevprev == '\n')
                ++p;
            prevprev = prev;
            prev = cur;
        }

        return p;
    }

    private int cursorForPage(int page) {
        String s = text();
        int c = 0;
        char prev = 0, prevprev = 0;

        for(int skipped = 0; skipped < page-1 && c < s.length(); ++c) {
            char cur = s.charAt(c);
            if(cur != '\n' && prev == '\n' && prevprev == '\n')
                ++skipped;
            prevprev = prev;
            prev = cur;
        }

        return Math.max(0,c-1);
    }

    public int pageTurn(int cursor, int diff) {
        return cursorForPage(Math.max(1, Math.min(pages().length, pageForCursor(cursor)+diff)));
    }

    public int getPdfWidth(Context context) {
        String[] r = context.getResources().getStringArray(R.array.pdf_widths);
        return Integer.parseInt(r[pdfResolution()]);
    }

    public int getPdfHeight(Context ctx) {
        return getPdfWidth(ctx) * 9 / 16;
    }

    public int numberOfPages() {
        return this.pages().length;
    }

    public Slide.Builder slideBuilder(int page, int width) {
        if(page <= 0 || page > numberOfPages())
            return null;

        final String text = this.pages()[page-1];
        return new Slide.Builder(text, this, width);
    }

    public static class Default {
        public static ImmutablePresentation build(Context c) {
            return ImmutablePresentation.builder()
                    .text(c.getString(R.string.tutorial_text))
                    .cursor(0)
                    .colorScheme(0)
                    .plantUMLEndPoint("https://plantuml.nitorio.us/png")
                    .plantUMLEnabled(false)
                    .plantUMLTemplateBefore("skinparam backgroundcolor transparent\nskinparam dpi 300\n")
                    .plantUMLTemplateAfter("")
                    .templateBefore("")
                    .templateAfter("")
                    .pdfResolution(1)
                    .build();
        }
    }
}
