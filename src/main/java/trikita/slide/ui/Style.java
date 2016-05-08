package trikita.slide.ui;

import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.ShapeDrawable;
import android.graphics.drawable.shapes.OvalShape;
import android.view.Gravity;
import android.view.View;

import static trikita.anvil.DSL.*;

import trikita.slide.R;

public class Style {
    public final static String SLIDE_FONT = "sans-serif-light";

    public final static int COLOR_SCHEMES[][] = {
        {0xff333333, 0xffeeeeee, R.drawable.dark_round_button, 0xffeeeeee},    // dark gray on white
        {0xfffefefe, 0xff333333, R.drawable.light_round_button, 0xff333333},    // white on dark gray
        {0xffffffff, 0xffe51c23, R.drawable.light_round_button, 0xff333333},    // white on red
        {0xffffffff, 0xffff5722, R.drawable.light_round_button, 0xff333333},    // white on deep orange
        {0xff222222, 0xffffeb3b, R.drawable.dark_round_button, 0xffeeeeee},    // black on yellow
        {0xff333333, 0xffcddc39, R.drawable.dark_round_button, 0xffeeeeee},    // black on lime
        {0xff222222, 0xff8bc34a, R.drawable.dark_round_button, 0xffeeeeee},    // black on light green
        {0xffffffff, 0xff009688, R.drawable.light_round_button, 0xff333333},    // white on teal
        {0xff333333, 0xff00bcd4, R.drawable.dark_round_button, 0xffeeeeee},    // black on cyan
        {0xffffffff, 0xff3f51b5, R.drawable.light_round_button, 0xff333333},    // white on indigo
        {0xffffffff, 0xff75507b, R.drawable.light_round_button, 0xff333333},    // white on plum
        {0xffffffff, 0xffe91e63, R.drawable.light_round_button, 0xff333333},    // white on pink
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

        public static void previewContainer() {
            size(WRAP, WRAP);
            alignParentEnd();
            alignParentBottom();
            margin(dip(12));
            padding(dip(1), dip(1), dip(2), dip(2));
            backgroundResource(R.drawable.preview_bg);
        }

        public static void menuButton() {
            size(dip(32), dip(32));
            alignParentEnd();
            alignParentTop();
            margin(dip(12));
            gravity(Gravity.CENTER);
            text("\ue5d4");
            typeface("MaterialIcons-Regular.ttf");
            textSize(sip(26));
            textColor(0xcc555555);
        }
    }

    public static class StylePicker {
        public static void circle(int fgColor, int bgColor) {
            ShapeDrawable circle = new ShapeDrawable(new OvalShape());
            circle.getPaint().setColor(bgColor);
            backgroundDrawable(circle);

            size(dip(48), dip(48));
            layoutGravity(CENTER_HORIZONTAL);
            gravity(CENTER);
            textColor(fgColor);
        }

        public static void itemNormal() {
            text("A");
            textSize(sip(20));
            typeface(Typeface.create(SLIDE_FONT, Typeface.NORMAL));
        }

        public static void itemSelected() {
            text("\ue876");
            textSize(sip(24));
            typeface("MaterialIcons-Regular.ttf");
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
