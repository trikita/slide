package trikita.slide.ui;

import android.content.Context;
import android.graphics.Color;
import android.graphics.Typeface;
import android.view.View;

import trikita.anvil.Anvil;
import trikita.anvil.RenderableView;
import trikita.jedux.Action;
import trikita.slide.ActionType;
import trikita.slide.App;
import trikita.slide.R;
import trikita.slide.Slide;

import static trikita.anvil.DSL.*;

public class MainLayout extends RenderableView {

    public final static int COLOR_SCHEMES[][] = {
            {0xffffffff, 0xff000000},
            {0xff000000, 0xffffffff},
            {0xff03a9f4, 0xff000000},
            {0xffecf0f1, 0xff34495e},
    };

    private Editor mEditor;

    public MainLayout(Context c) {
        super(c);
    }

    public void view() {
        if (App.getState().presentationMode()) {
            presentation();
        } else {
            editor();
        }
    }

    private void editor() {
        relativeLayout(() -> {
            size(FILL, FILL);
            fitsSystemWindows(true);
            padding(dip(8));
            backgroundColor(Color.WHITE);

            v(Editor.class, () -> {
                size(FILL, FILL);
                text(App.getState().text());
                textColor(Color.BLACK);
                typeface(Typeface.create("sans-serif-light", 0));
                gravity(TOP | START);
                backgroundDrawable(null);
                init(() -> {
                    mEditor = Anvil.currentView();
                    mEditor.setOnSelectionChangedListener(pos ->
                            App.dispatch(new Action<>(ActionType.SET_PAGE, Slide.page(App.getState().text(), pos))));
                });
                onTextChanged(chars -> {
                    String s = chars.toString();
                    App.dispatch(new Action<>(ActionType.SET_TEXT, s));
                    App.dispatch(new Action<>(ActionType.SET_PAGE, Slide.page(s, mEditor.getSelectionStart())));
                });
            });

            v(Preview.class, () -> {
                size(dip(144), WRAP);
                alignParentEnd();
                alignParentBottom();
                margin(dip(12));
                onClick(v -> App.dispatch(new Action<>(ActionType.OPEN_PRESENTATION)));
                Anvil.currentView().invalidate();
            });
        });
    }

    private void presentation() {
        relativeLayout(() -> {
            size(FILL, FILL);
            backgroundColor(App.getState().backgroundColor());

            v(Preview.class, () -> {
                size(FILL, WRAP);
                centerInParent();
                Anvil.currentView().invalidate();
            });

            linearLayout(() -> {
                size(FILL, FILL);
                v(View.class, () -> {
                    size(0, FILL);
                    weight(1f);
                    onClick(v -> App.dispatch(new Action<>(ActionType.PREV_PAGE)));
                });
                v(View.class, () -> {
                    size(0, FILL);
                    weight(1f);
                    onClick(v -> App.dispatch(new Action<>(ActionType.TOGGLE_TOOLBAR)));
                });
                v(View.class, () -> {
                    size(0, FILL);
                    weight(1f);
                    onClick(v -> App.dispatch(new Action<>(ActionType.NEXT_PAGE)));
                });
            });

            linearLayout(() -> {
                size(WRAP, dip(48));
                margin(0, 0, 0, dip(12));
                alignParentBottom();
                centerHorizontal();
                visibility(App.getState().toolbarShown());

                imageButton(() -> {
                    imageResource(R.drawable.ic_exit_to_app_24dp);
                    onClick(v -> App.dispatch(new Action<>(ActionType.CLOSE_PRESENTATION)));
                });
                imageButton(() -> {
                    imageResource(R.drawable.ic_color_lens_24dp);
                    onClick(v -> {
                        for (int i = 0; i < COLOR_SCHEMES.length; i++) {
                            if (COLOR_SCHEMES[i][0] == App.getState().foregroundColor() &&
                                    COLOR_SCHEMES[i][1] == App.getState().backgroundColor()) {
                                i = (i + 1) % COLOR_SCHEMES.length;
                                App.dispatch(new Action<>(ActionType.SET_FOREGROUND, COLOR_SCHEMES[i][0]));
                                App.dispatch(new Action<>(ActionType.SET_BACKGROUND, COLOR_SCHEMES[i][1]));
                                return;
                            }
                        }
                        App.dispatch(new Action<>(ActionType.SET_FOREGROUND, COLOR_SCHEMES[0][0]));
                        App.dispatch(new Action<>(ActionType.SET_BACKGROUND, COLOR_SCHEMES[0][1]));
                    });
                });
                imageButton(() -> {
                    imageResource(R.drawable.ic_archive_24dp);
                    onClick(v -> App.dispatch(new Action<>(ActionType.SHARE)));
                });
            });
        });
    }
}
