package trikita.slide.functions;

import android.app.Activity;
import android.graphics.Bitmap;
import android.net.Uri;
import android.text.TextUtils;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.CancellationException;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import trikita.slide.Presentation;
import trikita.slide.Slide;
import trikita.slide.ui.MathView;

public class MathTypeSetter implements Callable<Slide.Builder> {

    protected final Activity ctx;
    protected final Slide.Builder b;
    protected final int timeout;

    public MathTypeSetter(Activity ctx, int timeout, Slide.Builder b) {
        this.ctx = ctx;
        this.b = b;
        this.timeout = timeout;
    }

    @Override
    public Slide.Builder call() {
        return typesetMath(b);
    }

    protected static final String startMath = "@startmath";

    protected static boolean isStartMath(String s) {
        return s.startsWith(startMath);
    }

    protected static boolean isEndMath(String s) {
        return s.startsWith("@endmath");
    }

    protected Slide.Builder typesetMath(Slide.Builder b) {
        final String par = b.text;

        List<String> outLines = new ArrayList<>();

        boolean inMath = false;
        List<String> mathLines = null;
        String bgArgs = "";

        for (String line : par.split("\n")) {
            if (inMath) {
                if (isEndMath(line)) {
                    outLines.add(processMath(b, TextUtils.join("<br>", mathLines)) + bgArgs);
                    inMath = false;
                    mathLines = null;
                    bgArgs = "";
                } else {
                    if (Presentation.possibleLineBreak(line))
                        mathLines.add("");
                    else
                        mathLines.add(line);
                }
            } else {
                if (isStartMath(line)) {
                    mathLines = new ArrayList<>();
                    inMath = true;
                    bgArgs = line.trim().substring(startMath.length());
                    if (!bgArgs.isEmpty() && bgArgs.charAt(0) != ' ') bgArgs = " " + bgArgs;
                } else {
                    outLines.add(line);
                }
            }
        }

        if (mathLines != null) {
            outLines.add(processMath(b, TextUtils.join("<br>", mathLines)) + bgArgs);
        }

        return b.withText(TextUtils.join("\n", outLines));
    }

    protected String processMath(Slide.Builder b, String mathLines) {
        Future<Bitmap> bmpf = new MathView(this.ctx, b, mathLines.trim()).futureBitmap();

        try {
            Bitmap bmp = bmpf.get(timeout, TimeUnit.SECONDS);
            return Presentation.asBackground(bmpUri(bmp));
        } catch(TimeoutException e) {
            throw (CancellationException) new CancellationException().initCause(e);
        } catch (Exception ignored) {
        }

        return mathLines;
    }

    protected Uri bmpUri(Bitmap bmp) throws IOException {
        File bmpFile = File.createTempFile("bmp", ".png", this.ctx.getCacheDir());
        OutputStream os = new FileOutputStream(bmpFile);
        bmp.compress(Bitmap.CompressFormat.PNG, 100, os);
        return Uri.fromFile(bmpFile);
    }
}
