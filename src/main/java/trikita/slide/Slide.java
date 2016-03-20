package trikita.slide;

import android.graphics.Canvas;
import android.graphics.Typeface;
import android.text.Layout;
import android.text.StaticLayout;
import android.text.TextPaint;

public class Slide {
    public static int page(String s, int pos) {
        if (pos > s.length()-1) {
            pos = s.length()-1;
        }
        return paginate(s.substring(0, pos+1)).length-1;
    }

    public static String[] paginate(String text) {
        return text.split("(\n\\s*){2,}");
    }

    public static String pageText(String text, int page) {
        if (page < 0) {
            return "";
        }
        return paginate(text)[page];
    }

    public static void renderPage(String pageText, Canvas canvas, String typeface, int fg, int bg) {
        TextPaint textPaint = new TextPaint();
        canvas.drawColor(bg);
        textPaint.setColor(fg);
        textPaint.setAntiAlias(true);
        textPaint.setTypeface(Typeface.create(typeface, 0));

        String text = pageText.trim().replaceAll("\n\\.", "\n");

        float margin = 0.1f;

        int w = (int) (canvas.getWidth() * (1 - margin * 2));
        int h = (int) (canvas.getHeight() * (1 - margin * 2));

        int lines = text.split("\n").length;

        for (int textSize = canvas.getHeight() / lines; textSize > 1 ; textSize--) {
            textPaint.setTextSize(textSize);
            if (StaticLayout.getDesiredWidth(text, textPaint) <= w) {
                StaticLayout layout = new StaticLayout(text, textPaint, w, Layout.Alignment.ALIGN_NORMAL, 1, 0, false);
                if (layout.getHeight() >= h) {
                    continue;
                }
                int l = 0;
                for (int i = 0; i < layout.getLineCount(); i++) {
                    int m = (int) (canvas.getWidth() - layout.getLineWidth(i))/2;
                    if (i == 0 || m < l) {
                        l = m;
                    }
                }
                canvas.translate(l, (canvas.getHeight() - layout.getHeight())/2);
                layout.draw(canvas);
                return;
            }
        }
    }
}
