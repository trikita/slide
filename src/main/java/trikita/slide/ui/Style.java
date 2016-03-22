package trikita.slide.ui;

import android.graphics.Color;
import android.graphics.Typeface;
import android.view.View;

import static trikita.anvil.DSL.*;

import trikita.slide.R;

public class Style {
    public final static String SLIDE_FONT = "sans-serif-light";

    public final static int COLOR_SCHEMES[][] = {
            {0xffffffff, 0xff000000, R.drawable.light_round_button, 0xff333333},
            {0xff000000, 0xffffffff, R.drawable.dark_round_button, 0xffeeeeee},
            {0xff03a9f4, 0xff000000, R.drawable.light_round_button, 0xff333333},
            {0xffecf0f1, 0xff34495e, R.drawable.light_round_button, 0xff333333},
    };

    private final static String[] ICONS = { "\ue879", "\ue40a", "\ue149" };

    public static class Editor {
        public static void background() {
            size(FILL, FILL);
            fitsSystemWindows(true);
            padding(dip(8));
            backgroundColor(Color.WHITE);
        }

        public static void textStyle() {
            textColor(Color.BLACK);
            typeface(Typeface.create("sans-serif-light", 0));
        }
        
        public static void previewSize() {
            size(dip(144), WRAP);
        }
    }

    public static class Preview {
        public static void background(int scheme) {
            backgroundColor(COLOR_SCHEMES[scheme][1]);
        }

        public static void touchPlaceholder(View.OnClickListener l) {
            v(View.class, () -> {
                size(0, FILL);
                weight(1f);
                onClick(l);
            });
        }

        public static void button(int pos, int scheme) {
            size(dip(54), dip(54));
            margin(dip(10), 0);
            text(ICONS[pos]);
            textSize(sip(30));
            textColor(COLOR_SCHEMES[scheme][3]);
            typeface("MaterialIcons-Regular.ttf");
            backgroundResource(COLOR_SCHEMES[scheme][2]);
        }
    }
}
