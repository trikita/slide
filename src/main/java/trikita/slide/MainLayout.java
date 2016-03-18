package trikita.slide;

import android.content.Context;
import android.graphics.Color;
import android.graphics.Typeface;
import android.view.View;

import trikita.anvil.Anvil;
import trikita.anvil.RenderableView;
import trikita.jedux.Action;
import trikita.jedux.Store;

import static trikita.anvil.DSL.*;

public class MainLayout extends RenderableView {

    final static int COLOR_SCHEMES[][] = {
            {0xffffffff, 0xff000000},
            {0xff000000, 0xffffffff},
            {0xff03a9f4, 0xff000000},
            {0xffecf0f1, 0xff34495e},
    };

    private static Store<Action<ActionType, ?>, State> store;

    private Editor mEditor;

    public MainLayout(Context c) {
        super(c);
        store = MainActivity.store;
    }

    public void view() {
        if (store.getState().presentationMode()) {
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
                text(store.getState().text());
                textColor(Color.BLACK);
                typeface(Typeface.create("sans-serif-light", 0));
                gravity(TOP | START);
                backgroundDrawable(null);
                init(() -> {
                    mEditor = Anvil.currentView();
                    mEditor.setOnSelectionChangedListener(pos -> {
                        store.dispatch(new Action<>(ActionType.SET_PAGE, Slide.page(store.getState().text(), pos)));
                    });
                });
                onTextChanged(chars -> {
                    String s = chars.toString();
                    store.dispatch(new Action<>(ActionType.SET_TEXT, s));
                    store.dispatch(new Action<>(ActionType.SET_PAGE, Slide.page(s, mEditor.getSelectionStart())));
                });
            });

            v(Preview.class, () -> {
                size(dip(144), WRAP);
                alignParentEnd();
                alignParentBottom();
                margin(dip(12));
                onClick(v -> {
                    store.dispatch(new Action<>(ActionType.OPEN_PRESENTATION));
                });
                Anvil.currentView().invalidate();
            });
        });
    }

    private void presentation() {
        relativeLayout(() -> {
            size(FILL, FILL);
            backgroundColor(store.getState().backgroundColor());

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
                    onClick(v -> { store.dispatch(new Action<>(ActionType.PREV_PAGE)); });
                });
                v(View.class, () -> {
                    size(0, FILL);
                    weight(1f);
                    onClick(v -> { store.dispatch(new Action<>(ActionType.TOGGLE_TOOLBAR)); });
                });
                v(View.class, () -> {
                    size(0, FILL);
                    weight(1f);
                    onClick(v -> { store.dispatch(new Action<>(ActionType.NEXT_PAGE)); });
                });
            });

            linearLayout(() -> {
                size(WRAP, dip(48));
                margin(0, 0, 0, dip(12));
                alignParentBottom();
                centerHorizontal();
                visibility(store.getState().toolbarShown());

                button(() -> {
                    text("X");
                    onClick(v -> {
                        store.dispatch(new Action<>(ActionType.CLOSE_PRESENTATION));
                    });
                });
                button(() -> {
                    text("C");
                    onClick(v -> {
                        for (int i = 0; i < COLOR_SCHEMES.length; i++) {
                            if (COLOR_SCHEMES[i][0] == store.getState().foregroundColor() &&
                                    COLOR_SCHEMES[i][1] == store.getState().backgroundColor()) {
                                i = (i + 1) % COLOR_SCHEMES.length;
                                store.dispatch(new Action<>(ActionType.SET_FOREGROUND, COLOR_SCHEMES[i][0]));
                                store.dispatch(new Action<>(ActionType.SET_BACKGROUND, COLOR_SCHEMES[i][1]));
                                return;
                            }
                        }
                        store.dispatch(new Action<>(ActionType.SET_FOREGROUND, COLOR_SCHEMES[0][0]));
                        store.dispatch(new Action<>(ActionType.SET_BACKGROUND, COLOR_SCHEMES[0][1]));
                    });
                });
            });
        });
    }
}
