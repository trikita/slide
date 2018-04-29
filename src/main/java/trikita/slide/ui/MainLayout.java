package trikita.slide.ui;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.net.Uri;
import android.util.Pair;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.PopupMenu;
import android.widget.Spinner;
import android.widget.TextView;

import trikita.anvil.Anvil;
import trikita.anvil.RenderableView;
import trikita.jedux.Action;
import trikita.slide.ActionType;
import trikita.slide.App;
import trikita.slide.Presentation;
import trikita.slide.R;

import static trikita.anvil.DSL.FILL;
import static trikita.anvil.DSL.START;
import static trikita.anvil.DSL.TOP;
import static trikita.anvil.DSL.WRAP;
import static trikita.anvil.DSL.background;
import static trikita.anvil.DSL.centerInParent;
import static trikita.anvil.DSL.frameLayout;
import static trikita.anvil.DSL.gravity;
import static trikita.anvil.DSL.init;
import static trikita.anvil.DSL.onClick;
import static trikita.anvil.DSL.onTextChanged;
import static trikita.anvil.DSL.relativeLayout;
import static trikita.anvil.DSL.size;
import static trikita.anvil.DSL.text;
import static trikita.anvil.DSL.textView;
import static trikita.anvil.DSL.v;

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
                text(App.getState().getCurrentPresentation().text());
                Style.Editor.textStyle();
                background(null);
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
                    Anvil.currentView().invalidate();
                });
            });
        });
    }

    private void presentation() {
        relativeLayout(() -> {
            size(FILL, FILL);
            Style.Preview.background();

            v(Preview.class, () -> {
                size(FILL, WRAP);
                centerInParent();
                Anvil.currentView().invalidate();
            });
        });
    }

    private void onOpenMenu(View v) {
        PopupMenu menu = new PopupMenu(v.getContext(), v);
        menu.getMenuInflater().inflate(R.menu.overflow_popup, menu.getMenu());
        menu.setOnMenuItemClickListener(item -> {
            if (item.getItemId() == R.id.menu_remove_presentation) {
                openRemoveDialog();
            } else if (item.getItemId() == R.id.menu_insert_image) {
                App.dispatch(new Action<>(ActionType.PICK_IMAGE, (Activity) v.getContext()));
            } else if (item.getItemId() == R.id.menu_style) {
                openStylePicker();
            } else if (item.getItemId() == R.id.menu_template) {
                openTemplateDialog();
            } else if (item.getItemId() == R.id.menu_export_pdf) {
                openExportPDFDialog();
            } else if (item.getItemId() == R.id.menu_config_plantuml) {
                openConfigPlantUMLDialog();
            } else if (item.getItemId() == R.id.menu_plantuml_docs) {
                openPlantUMLDocs();
            }
            return true;
        });
        menu.show();
    }

    private void openRemoveDialog() {
        new AlertDialog.Builder(getContext())
            .setTitle(getContext().getString(R.string.om_remove))
            .setCancelable(true)
            .setMessage(R.string.are_you_sure)
            .setPositiveButton(R.string.btn_ok, new DialogInterface.OnClickListener() {
                public void onClick(DialogInterface dialog, int whichButton) {
                    App.dispatch(new Action<>(ActionType.REMOVE_PRESENTATION, getContext()));
                }
            })
            .setNegativeButton(R.string.dialog_btn_cancel, new DialogInterface.OnClickListener() {
                public void onClick(DialogInterface dialog, int whichButton) {
                }
            })
            .show();
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

    private void openConfigPlantUMLDialog() {
        Presentation p = App.getState().getCurrentPresentation();
        LinearLayout contents = new LinearLayout(getContext());
        contents.setOrientation(LinearLayout.VERTICAL);

        TextView warning = new TextView(getContext());
        warning.setText(R.string.pm_warning);
        contents.addView(warning);

        CheckBox enable = new CheckBox(getContext());
        enable.setChecked(p.plantUMLEnabled());
        enable.setText(R.string.pm_enable);
        contents.addView(enable);

        EditText txtUrl = new EditText(getContext());
        txtUrl.setHint(R.string.pm_plantuml_endpoint);
        txtUrl.setText(p.plantUMLEndPoint());
        contents.addView(txtUrl);

        TextView templateBefore = new TextView(getContext());
        templateBefore.setText(R.string.pm_plantuml_template_before);
        contents.addView(templateBefore);

        EditText txtTemplateBefore = new EditText(getContext());
        txtTemplateBefore.setSingleLine(false);
        txtTemplateBefore.setText(p.plantUMLTemplateBefore());
        contents.addView(txtTemplateBefore);

        TextView templateAfter = new TextView(getContext());
        templateAfter.setText(R.string.pm_plantuml_template_after);
        contents.addView(templateAfter);

        EditText txtTemplateAfter = new EditText(getContext());
        txtTemplateAfter.setSingleLine(false);
        txtTemplateAfter.setText(p.plantUMLTemplateAfter());
        contents.addView(txtTemplateAfter);

        new AlertDialog.Builder(getContext())
            .setTitle(R.string.om_config_plantuml)
            .setView(contents)
            .setPositiveButton(R.string.btn_ok, new DialogInterface.OnClickListener() {
                public void onClick(DialogInterface dialog, int whichButton) {
                    App.dispatch(new Action<>(ActionType.CONFIGURE_PLANTUML,
                        new Pair<>(enable.isChecked(),
                            new Pair<>(txtUrl.getText().toString(),
                                new Pair<>(txtTemplateBefore.getText().toString(), txtTemplateAfter.getText().toString())))));
                }
            })
            .setNegativeButton(R.string.dialog_btn_cancel, new DialogInterface.OnClickListener() {
                public void onClick(DialogInterface dialog, int whichButton) {
                }
            })
            .show();
    }

    private void openPlantUMLDocs() {
        Intent i = new Intent(Intent.ACTION_VIEW);
        i.setData(Uri.parse("https://plantuml.com"));
        getContext().startActivity(i);
    }

    private void openTemplateDialog() {
        Presentation p = App.getState().getCurrentPresentation();
        LinearLayout contents = new LinearLayout(getContext());
        contents.setOrientation(LinearLayout.VERTICAL);

        TextView before = new TextView(getContext());
        before.setText(R.string.template_before);
        contents.addView(before);

        EditText txtBefore = new EditText(getContext());
        txtBefore.setSingleLine(false);
        txtBefore.setText(p.templateBefore());
        contents.addView(txtBefore);

        TextView after = new TextView(getContext());
        after.setText(R.string.template_after);
        contents.addView(after);

        EditText txtAfter = new EditText(getContext());
        txtAfter.setSingleLine(false);
        txtAfter.setText(p.templateAfter());
        contents.addView(txtAfter);

        new AlertDialog.Builder(getContext())
            .setTitle(R.string.om_template)
            .setView(contents)
            .setPositiveButton(R.string.btn_ok, new DialogInterface.OnClickListener() {
                public void onClick(DialogInterface dialog, int whichButton) {
                    App.dispatch(new Action<>(ActionType.SET_TEMPLATE,
                        new Pair<>(txtBefore.getText().toString(),
                                    txtAfter.getText().toString())));
                }
            })
            .setNegativeButton(R.string.dialog_btn_cancel, new DialogInterface.OnClickListener() {
                public void onClick(DialogInterface dialog, int whichButton) {
                }
            })
            .show();
    }

    private void openExportPDFDialog() {
        Presentation p = App.getState().getCurrentPresentation();
        LinearLayout contents = new LinearLayout(getContext());
        contents.setOrientation(LinearLayout.VERTICAL);

        Spinner spinner = new Spinner(getContext());
        ArrayAdapter<CharSequence> adapter = ArrayAdapter.createFromResource(getContext(),
            R.array.pdf_resolutions, android.R.layout.simple_spinner_item);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinner.setAdapter(adapter);
        spinner.setSelection(p.pdfResolution());
        contents.addView(spinner);

        new AlertDialog.Builder(getContext())
            .setTitle(R.string.select_resolution)
            .setView(contents)
            .setPositiveButton(R.string.btn_ok, new DialogInterface.OnClickListener() {
                public void onClick(DialogInterface dialog, int whichButton) {
                    int selected = spinner.getSelectedItemPosition();
                    App.dispatch(new Action<>(ActionType.SET_PDF_RESOLUTION, selected));
                    App.dispatch(new Action<>(ActionType.CREATE_PDF, (Activity) getContext()));
                }
            })
            .setNegativeButton(R.string.dialog_btn_cancel, new DialogInterface.OnClickListener() {
                public void onClick(DialogInterface dialog, int whichButton) {
                }
            })
            .show();
    }
}
