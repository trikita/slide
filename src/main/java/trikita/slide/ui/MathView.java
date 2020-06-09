package trikita.slide.ui;

import android.app.Activity;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.os.AsyncTask;
import android.os.Handler;
import android.os.Message;
import android.webkit.JavascriptInterface;
import android.webkit.WebView;

import java.util.concurrent.Callable;
import java.util.concurrent.Future;
import java.util.concurrent.FutureTask;

import trikita.slide.Slide;
import trikita.slide.functions.BitmapCropper;

public class MathView {

    private WebView webView;
    private Handler mHandler;
    private final Slide.Builder builder;
    private final String maths;
    private final DelayedBitmapCropper bitmapCropper;
    private final FutureTask<Bitmap> result;

    private static class DelayedBitmapCropper implements Callable<Bitmap> {
        protected BitmapCropper cropper;

        public DelayedBitmapCropper createCropper(Bitmap bmp, Integer fontSize) {
            this.cropper = new BitmapCropper(bmp, fontSize);
            return this;
        }

        @Override
        public Bitmap call() throws Exception {
            return cropper.call();
        }
    }

    public MathView(Activity act, Slide.Builder b, String maths) {
        this.builder = b;
        this.maths = maths;
        this.bitmapCropper = new DelayedBitmapCropper();
        this.result = new FutureTask<>(bitmapCropper);

        act.runOnUiThread(() -> {
            this.mHandler = new Handler(msg -> {
                switch (msg.what) {
                    case 0:
                        onLoaded();
                        break;
                    case 1:
                        onTypeset((Integer)msg.obj);
                        break;
                }
                return true;
            });

            this.webView = new WebView(act);
            this.webView.layout(0, 0, b.width, b.height);
            this.webView.getSettings().setJavaScriptEnabled(true);
            this.webView.addJavascriptInterface(new MathViewJS(this.mHandler), "Android");
            this.webView.loadUrl("file:///android_asset/www/mathview.html");
            this.webView.setBackgroundColor(Color.TRANSPARENT);
            this.webView.setLayerType(WebView.LAYER_TYPE_SOFTWARE, null);
        });
    }

    private void onTypeset(int fontSize) {
        final Bitmap bmp = Bitmap.createBitmap(this.webView.getWidth(), this.webView.getHeight(), Bitmap.Config.ARGB_8888);
        this.webView.draw(new Canvas(bmp));
        this.bitmapCropper.createCropper(bmp, fontSize);
        AsyncTask.THREAD_POOL_EXECUTOR.execute(this.result);
    }

    private void onLoaded() {
        int fg = Style.COLOR_SCHEMES[builder.presentation.colorScheme()][0];
        this.webView.loadUrl("javascript:typeset('"
            + Integer.toHexString(fg).substring(2) + "','"
            + maths.trim()
        + "')");
    }

    public Future<Bitmap> futureBitmap() {
        return result;
    }

    private static class MathViewJS {
        private final Handler mHandler;

        MathViewJS(Handler m) {
            this.mHandler = m;
        }

        @JavascriptInterface
        void onTypeset(int fontSize) {
            Message.obtain(this.mHandler, 1, fontSize).sendToTarget();
        }

        @JavascriptInterface
        void onLoaded() {
            Message.obtain(this.mHandler, 0).sendToTarget();
        }
    }

}
