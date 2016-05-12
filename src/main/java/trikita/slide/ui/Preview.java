package trikita.slide.ui;

import android.content.Context;
import android.graphics.Canvas;
import android.view.View;

import java.util.List;

import trikita.slide.App;
import trikita.slide.Slide;

public class Preview extends View {

    public Preview(Context context) {
        super(context);
    }

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
        List<Slide> slides = App.getState().slides();
        int page = App.getState().page();
        if (page >= 0 && page < slides.size()) {
            slides.get(page).render(getContext(), canvas,
                    Style.SLIDE_FONT,
                    Style.COLOR_SCHEMES[App.getState().colorScheme()][0],
                    Style.COLOR_SCHEMES[App.getState().colorScheme()][1],
                    false);
        } else {
            canvas.drawColor(Style.COLOR_SCHEMES[App.getState().colorScheme()][1]);
        }
    }
}
