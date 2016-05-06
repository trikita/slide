package trikita.slide.ui;

import android.content.Context;

import trikita.anvil.Anvil;
import trikita.anvil.RenderableView;
import trikita.jedux.Action;
import trikita.slide.ActionType;
import trikita.slide.App;
import trikita.slide.Slide;
import trikita.slide.R;

import static trikita.anvil.DSL.*;

public class MainLayout extends RenderableView {
    private final static int CLOSE_BUTTON = 0;
    private final static int PALETTE_BUTTON = 1;
    private final static int SHARE_BUTTON = 2;

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
            Style.Editor.background();

            v(Editor.class, () -> {
                size(FILL, FILL);
                gravity(TOP | START);
                text(App.getState().text());
                Style.Editor.textStyle();
                backgroundDrawable(null);
                init(() -> {
                    mEditor = Anvil.currentView();
                    mEditor.setOnSelectionChangedListener(pos -> {
                        String s = App.getState().text();
                        s = s.substring(0, Math.min(pos+1, s.length()));
                        App.dispatch(new Action<>(ActionType.SET_PAGE, Slide.parse(s).size()-1));
                    });
                });
                onTextChanged(chars -> {
                    String s = chars.toString();
                    App.dispatch(new Action<>(ActionType.SET_TEXT, s));
                    s = s.substring(0, mEditor.getSelectionStart());
                    App.dispatch(new Action<>(ActionType.SET_PAGE, Slide.parse(s).size()-1));
                });
            });

            frameLayout(() -> {
                Style.Editor.previewContainer();

                v(Preview.class, () -> {
                    Style.Editor.previewSize();
                    onClick(v -> App.dispatch(new Action<>(ActionType.OPEN_PRESENTATION)));
                    Anvil.currentView().invalidate();
                });
            });
        });
    }

    private void presentation() {
        relativeLayout(() -> {
            size(FILL, FILL);
            Style.Preview.background(App.getState().colorScheme());

            v(Preview.class, () -> {
                size(FILL, WRAP);
                centerInParent();
                Anvil.currentView().invalidate();
            });

            linearLayout(() -> {
                size(FILL, FILL);

                Style.Preview.touchPlaceholder(v -> App.dispatch(new Action<>(ActionType.PREV_PAGE)));
                Style.Preview.touchPlaceholder(v -> App.dispatch(new Action<>(ActionType.TOGGLE_TOOLBAR)));
                Style.Preview.touchPlaceholder(v -> App.dispatch(new Action<>(ActionType.NEXT_PAGE)));
            });

            linearLayout(() -> {
                size(WRAP, WRAP);
                margin(0, 0, 0, dip(25));
                alignParentBottom();
                centerHorizontal();
                visibility(App.getState().toolbarShown());

                button(() -> {
                    Style.Preview.button(CLOSE_BUTTON, App.getState().colorScheme());
                    onClick(v -> App.dispatch(new Action<>(ActionType.CLOSE_PRESENTATION)));
                });
                button(() -> {
                    Style.Preview.button(PALETTE_BUTTON, App.getState().colorScheme());
                    onClick(v -> App.dispatch(new Action<>(
                                ActionType.SET_COLOR_SCHEME,
                                (App.getState().colorScheme() + 1) % Style.COLOR_SCHEMES.length)));
                });
                button(() -> {
                    Style.Preview.button(SHARE_BUTTON, App.getState().colorScheme());
                    onClick(v -> App.dispatch(new Action<>(ActionType.SHARE)));
                });
            });
        });
    }
}
