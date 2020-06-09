package trikita.slide;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.Typeface;
import android.graphics.drawable.Drawable;
import android.text.Layout;
import android.text.SpannableStringBuilder;
import android.text.StaticLayout;
import android.text.TextPaint;
import android.text.style.RelativeSizeSpan;
import android.text.style.StyleSpan;
import android.text.style.TypefaceSpan;
import android.view.Gravity;

import com.squareup.picasso.Picasso;
import com.squareup.picasso.RequestCreator;
import com.squareup.picasso.Target;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import trikita.anvil.Anvil;
import trikita.slide.functions.LoadingScreenRenderer;
import trikita.slide.ui.Style;

public class Slide {

    private static class Background {
        private static final Map<String, Integer> GRAVITY = new HashMap<>();

        static {
            GRAVITY.put("left", Gravity.LEFT);
            GRAVITY.put("top", Gravity.TOP);
            GRAVITY.put("right", Gravity.RIGHT);
            GRAVITY.put("bottom", Gravity.BOTTOM);
            GRAVITY.put("center", Gravity.CENTER);
            GRAVITY.put("w", Gravity.LEFT);
            GRAVITY.put("n", Gravity.TOP);
            GRAVITY.put("e", Gravity.RIGHT);
            GRAVITY.put("s", Gravity.BOTTOM);
            GRAVITY.put("nw", Gravity.LEFT | Gravity.TOP);
            GRAVITY.put("sw", Gravity.LEFT | Gravity.BOTTOM);
            GRAVITY.put("ne", Gravity.RIGHT | Gravity.TOP);
            GRAVITY.put("se", Gravity.RIGHT | Gravity.BOTTOM);
        }

        private final String url;
        private final float scale;
        private final int gravity;

        Background(String bg) {
            int g = Gravity.CENTER;
            float scale = 1f;
            String url = null;
            for (String part : bg.split("\\s+")) {
                if (part.endsWith("%")) {
                    try {
                        scale = Float.parseFloat(part.substring(0, part.length() - 1)) / 100;
                    } catch (NumberFormatException ignored) {
                    }
                } else if (GRAVITY.containsKey(part)) {
                    g = GRAVITY.get(part);
                } else if (part.contains(":/")) {
                    url = part;
                }
            }
            this.url = url;
            this.scale = scale;
            this.gravity = g;
        }
    }

    private final List<Background> backgrounds = new ArrayList<>();
    private final Map<String,CacheTarget> bitmaps = new HashMap<>();
    private final SpannableStringBuilder text = new SpannableStringBuilder();

    private final int fg, bg;

    public Slide(String s, int fg, int bg) {
        int emSpanStart = -1;
        int codeSpanStart = -1;
        for (String line : s.split("\n")) {
            if (Presentation.isBackground(line)) {
                backgrounds.add(new Background(line.substring(1)));
            } else if (line.startsWith("#")) {
                int start = text.length();
                text.append(line.substring(1)).append('\n');
                text.setSpan(new RelativeSizeSpan(1.6f), start, text.length(), 0);
                text.setSpan(new StyleSpan(Typeface.BOLD), start, text.length(), 0);
            } else {
                if (Presentation.possibleLineBreak(line)) {
                    line = line.substring(1);
                }
                for (int i = 0; i < line.length(); i++) {
                    char c = line.charAt(i);
                    // Handle emphasis
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
                    // Handle inline code
                    } else if (c == '`') {
                        if (codeSpanStart == -1) {
                            codeSpanStart = text.length();
                        } else {
                            if (codeSpanStart != text.length()) {
                                text.setSpan(new TypefaceSpan("monospace"), codeSpanStart, text.length(), 0);
                            } else {
                                text.append('`');
                            }
                            codeSpanStart = -1;
                        }
                    } else {
                        text.append(c);
                    }
                }
                text.append('\n');
            }
        }
        if (text.length() > 0 && text.charAt(text.length() - 1) == '\n') {
            text.delete(text.length() - 1, text.length());
        }

        this.fg = fg;
        this.bg = bg;
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

        @Override
        public void onBitmapFailed(Exception e, Drawable errorDrawable) {
        }

        @Override
        public void onPrepareLoad(Drawable placeHolderDrawable) {
        }

        Bitmap getCacheBitmap() {
            return cacheBitmap;
        }
    }

    public void render(Canvas canvas, String typeface, boolean blocking) {
        final int width = canvas.getWidth();
        final int height = canvas.getHeight();
        boolean loadingImages = false;

        TextPaint textPaint = new TextPaint();
        canvas.drawColor(bg);
        textPaint.setColor(fg);
        textPaint.setAntiAlias(true);
        textPaint.setTypeface(Typeface.create(typeface, Typeface.NORMAL));
        textPaint.setFlags(textPaint.getFlags() | TextPaint.EMBEDDED_BITMAP_TEXT_FLAG);

        for (Background img : backgrounds) {
            if (img.url != null) {
                RequestCreator request = Picasso.get()
                        .load(img.url);
                if (img.scale > 0) {
                    request = request.resize((int) (width * img.scale), (int) (height * img.scale));
                } else {
                    request = request.resize(width, height);
                }
                request = request.centerInside();

                Bitmap b = null;
                if (blocking) {
                    try {
                        b = request.get();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                } else {
                    CacheTarget cacheTarget = bitmaps.get(img.url+width);
                    if(cacheTarget == null){
                        cacheTarget = new CacheTarget();
                        bitmaps.put(img.url+width, cacheTarget);
                        request.into(cacheTarget);
                    }
                    b = cacheTarget.getCacheBitmap();
                }
                if (b != null) {
                    Rect r = new Rect();
                    Gravity.apply(img.gravity, b.getWidth(), b.getHeight(),
                            new Rect(0, 0, width, height), r);
                    canvas.drawBitmap(b, r.left, r.top, textPaint);
                } else {
                    loadingImages = true;
                }
            }
        }

        if(loadingImages) {
            (new LoadingScreenRenderer(App.instance.getApplicationContext(), fg, bg, canvas, "Loading images..."))
                .run();
            return;
        }

        float margin = 0.1f;

        int w = (int) (width * (1 - margin * 2));
        int h = (int) (height * (1 - margin * 2));

        for (int textSize = height; textSize > 1; textSize--) {
            textPaint.setTextSize(textSize);
            float dw = StaticLayout.getDesiredWidth(text, textPaint);

            if (dw <= w) {
                StaticLayout layout = new StaticLayout(
                    text, textPaint, (int)dw, Layout.Alignment.ALIGN_NORMAL,
                    1.0f, 0.0f, false
                );

                if (layout.getHeight() >= h) {
                    continue;
                }

                int l = 0;
                for (int i = 0; i < layout.getLineCount(); i++) {
                    int m = (int) (width - layout.getLineWidth(i)) / 2;
                    if (i == 0 || m < l) {
                        l = m;
                    }
                }

                int t = (height - layout.getHeight()) / 2;
                canvas.translate(l, t);

                textPaint.setColor(bg);
                textPaint.setStyle(Paint.Style.STROKE);
                textPaint.setStrokeWidth(8);
                layout.draw(canvas);

                textPaint.setColor(fg);
                textPaint.setStyle(Paint.Style.FILL);
                textPaint.setStrokeWidth(0);
                layout.draw(canvas);

                canvas.translate(-l,-t);
                return;
            }
        }
    }

    public static class Builder {
        public final Presentation presentation;
        public final String text;
        public final int width;
        public final int height;

        public Builder(String text, Presentation p, int width) {
            this.text = text;
            this.presentation = p;
            this.width = width;
            this.height = width * 9 / 16;
        }

        public Builder withText(String text) {
            return new Builder(text, presentation, width);
        }

        public Slide build() {
            int[] cs = Style.COLOR_SCHEMES[presentation.colorScheme()];
            return new Slide(text, cs[0], cs[1]);
        }
    }
}
