package trikita.slide.ui;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.view.View;
import android.widget.PopupMenu;

import trikita.anvil.Anvil;
import trikita.anvil.RenderableView;
import trikita.jedux.Action;
import trikita.slide.ActionType;
import trikita.slide.App;
import trikita.slide.R;

import static trikita.anvil.DSL.*;

public class MainLayout extends RenderableView {

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
                        App.dispatch(new Action<>(ActionType.SET_CURSOR, pos));
                    });
                });
                onTextChanged(chars -> {
                    String s = chars.toString();
                    App.dispatch(new Action<>(ActionType.SET_TEXT, s));
                    App.dispatch(new Action<>(ActionType.SET_CURSOR, mEditor.getSelectionStart()));
                });
            });

            textView(() -> {
                Style.Editor.menuButton();
                onClick(this::onOpenMenu);
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

            button(() -> {
                Style.Preview.button(App.getState().colorScheme());
                margin(0, 0, 0, dip(25));
                alignParentBottom();
                centerHorizontal();
                visibility(App.getState().toolbarShown());
                onClick(v -> App.dispatch(new Action<>(ActionType.CLOSE_PRESENTATION)));
            });
        });
    }

    private void onOpenMenu(View v) {
        PopupMenu menu = new PopupMenu(v.getContext(), v);
        menu.getMenuInflater().inflate(R.menu.overflow_popup, menu.getMenu());
        menu.setOnMenuItemClickListener(item -> {
            if (item.getItemId() == R.id.menu_open) {
                App.dispatch(new Action<>(ActionType.OPEN_DOCUMENT, (Activity) v.getContext()));
            } else if (item.getItemId() == R.id.menu_insert_image) {
                App.dispatch(new Action<>(ActionType.PICK_IMAGE, (Activity) v.getContext()));
            } else if (item.getItemId() == R.id.menu_style) {
                openStylePicker();
            //} else if (item.getItemId() == R.id.menu_settings) {
            } else if (item.getItemId() == R.id.menu_export_pdf) {
                App.dispatch(new Action<>(ActionType.CREATE_PDF, (Activity) v.getContext()));
            }
            return true;
        });
        menu.show();
    }

    private void openStylePicker() {
        AlertDialog.Builder builder = new AlertDialog.Builder(getContext())
            .setTitle(getContext().getString(R.string.dlg_style_picker_title))
            .setCancelable(true)
            .setView(new StylePicker(getContext()))
            .setPositiveButton(getContext().getString(R.string.btn_ok), (arg0, arg1) -> {});
        AlertDialog dialog = builder.create();
        dialog.setCanceledOnTouchOutside(true);
        dialog.show();
    }
}
