package trikita.slide;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Typeface;
import android.text.Layout;
import android.text.StaticLayout;
import android.text.TextPaint;
import android.view.View;

import trikita.jedux.Action;
import trikita.jedux.Store;

public class Preview extends View {

    private static Store<Action<ActionType, ?>, State> store;

    public Preview(Context context) {
        super(context);
        store = MainActivity.store;
    }

    private final TextPaint mTextPaint = new TextPaint();

    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        int w = MeasureSpec.getSize(widthMeasureSpec);
        int h = MeasureSpec.getSize(heightMeasureSpec);
        if ((w * 9/16) > h) {
            w = h * 16/9;
        } else {
            h = w * 9/16;
        }
        super.onMeasure(MeasureSpec.makeMeasureSpec(w, MeasureSpec.EXACTLY),
                MeasureSpec.makeMeasureSpec(h, MeasureSpec.EXACTLY));
    }

    protected void onDraw(Canvas canvas) {
        canvas.drawColor(store.getState().backgroundColor());
        mTextPaint.setColor(store.getState().foregroundColor());
        mTextPaint.setAntiAlias(true);
        mTextPaint.setTypeface(Typeface.create("sans-serif-light", 0));

        String text = Slide.pageText(store.getState().text(), store.getState().page()).trim();
        text = text.replaceAll("\n\\.", "\n");

        float margin = 0.1f;

        int w = (int) (canvas.getWidth() * (1 - margin * 2));
        int h = (int) (canvas.getHeight() * (1 - margin * 2));

        int lines = text.split("\n").length;

        for (int textSize = canvas.getHeight() / lines; textSize > 1 ; textSize--) {
            mTextPaint.setTextSize(textSize);
            if (StaticLayout.getDesiredWidth(text, mTextPaint) <= w) {
                StaticLayout layout = new StaticLayout(text, mTextPaint, w, Layout.Alignment.ALIGN_NORMAL, 1, 0, false);
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
