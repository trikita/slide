package trikita.slide;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Typeface;
import android.graphics.drawable.Drawable;
import android.text.Layout;
import android.text.SpannableStringBuilder;
import android.text.StaticLayout;
import android.text.TextPaint;
import android.text.style.RelativeSizeSpan;
import android.text.style.StyleSpan;
import android.text.style.TypefaceSpan;

import com.squareup.picasso.Target;
import com.squareup.picasso.Picasso;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import trikita.anvil.Anvil;

public class Slide {

    private final List<String> backgrounds = new ArrayList<>();
    private final Map<String, CacheTarget> bitmaps = new HashMap<>();
    private final SpannableStringBuilder text = new SpannableStringBuilder();

    private Slide(String s) {
        int emSpanStart = -1;
        for (String line : s.split("\n")) {
            if (line.startsWith("@")) {
                backgrounds.add(line.substring(1));
            } else if (line.startsWith("#")) {
                int start = text.length();
                text.append(line.substring(1)).append('\n');
                text.setSpan(new RelativeSizeSpan(1.6f), start, text.length(), 0);
                text.setSpan(new StyleSpan(Typeface.BOLD), start, text.length(), 0);
            } else if (line.startsWith("  ")) {
                int start = text.length();
                text.append(line.substring(2)).append('\n');
                text.setSpan(new TypefaceSpan("monospace"), start, text.length(), 0);
            } else {
                if (line.startsWith(".")) {
                    line = line.substring(1);
                }
                // Handle emphasis
                for (int i = 0; i < line.length(); i++) {
                    char c = line.charAt(i);
                    if (c == '*') {
                        if (emSpanStart == -1) {
                            emSpanStart = text.length();
                        } else {
                            if (emSpanStart != text.length()) {
                                text.setSpan(new StyleSpan(Typeface.BOLD), emSpanStart, text.length(), 0);
                            } else {
                                text.append('*');
                            }
                            emSpanStart = -1;
                        }
                    } else {
                        text.append(c);
                    }
                }
                text.append('\n');
            }
        }
    }

    private static class CacheTarget implements Target {
        private Bitmap cacheBitmap;
        @Override
        public void onBitmapLoaded(Bitmap bitmap, Picasso.LoadedFrom from) {
            cacheBitmap = bitmap;
            if (from != Picasso.LoadedFrom.MEMORY) {
                Anvil.render();
            }
        }
        @Override public void onBitmapFailed(Drawable errorDrawable) {}
        @Override public void onPrepareLoad(Drawable placeHolderDrawable) {}
        public Bitmap getCacheBitmap() {
            return cacheBitmap;
        }
    }

    public void render(Context c, Canvas canvas, String typeface, int fg, int bg) {
        TextPaint textPaint = new TextPaint();
        canvas.drawColor(bg);
        textPaint.setColor(fg);
        textPaint.setAntiAlias(true);
        textPaint.setTypeface(Typeface.create(typeface, 0));

        for (String uri : backgrounds) {
            if (uri.length() > 0) {
                CacheTarget cacheTarget = new CacheTarget();
                bitmaps.put(uri, cacheTarget);
                Picasso.with(c)
                        .load(uri)
                        .resize(canvas.getWidth(), canvas.getHeight())
                        .centerCrop()
                        .into(cacheTarget);
                Bitmap b = cacheTarget.getCacheBitmap();
                if (b != null) {
                    canvas.drawBitmap(b, 0, 0, textPaint);
                }
            }
        }

        float margin = 0.1f;

        int w = (int) (canvas.getWidth() * (1 - margin * 2));
        int h = (int) (canvas.getHeight() * (1 - margin * 2));

        for (int textSize = canvas.getHeight(); textSize > 1 ; textSize--) {
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

                textPaint.setColor(bg);
                textPaint.setStyle(Paint.Style.STROKE);
                textPaint.setStrokeWidth(8);
                layout = new StaticLayout(text, textPaint, w, Layout.Alignment.ALIGN_NORMAL, 1, 0, false);
                layout.draw(canvas);
                textPaint.setColor(fg);
                textPaint.setStyle(Paint.Style.FILL);
                textPaint.setStrokeWidth(0);
                layout = new StaticLayout(text, textPaint, w, Layout.Alignment.ALIGN_NORMAL, 1, 0, false);
                layout.draw(canvas);
                return;
            }
        }
    }

    public static List<Slide> parse(String s) {
        List<Slide> slides = new ArrayList<>();
        String[] paragraphs = s.split("(\n\\s*){2,}");
        for (String par : paragraphs) {
            slides.add(new Slide(par));
        }
        return slides;
    }
}
