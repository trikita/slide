package trikita.slide.ui;

import android.content.Context;
import android.view.View;
import android.widget.AdapterView;
import android.widget.GridView;

import java.util.Arrays;

import trikita.anvil.RenderableAdapter;
import trikita.anvil.RenderableView;
import trikita.jedux.Action;
import trikita.slide.ActionType;
import trikita.slide.App;

import static trikita.anvil.DSL.*;

public class StylePicker extends RenderableView {

    public StylePicker(Context c) {
        super(c);
    }

    public void view() {
        mStyleAdapter.notifyDataSetChanged();
        gridView(() -> {
            size(FILL, WRAP);
            padding(dip(7), dip(15));
            gravity(CENTER);
            numColumns(4);
            verticalSpacing(dip(12));
            stretchMode(GridView.STRETCH_COLUMN_WIDTH);
            adapter(mStyleAdapter);
            onItemClick(this::onStyleClicked);
        });
    }

    private final RenderableAdapter mStyleAdapter = RenderableAdapter.withItems(Arrays.asList(Style.COLOR_SCHEMES), (index, item) -> {
        textView(() -> {
            Style.StylePicker.circle(item[0], item[1]);
            if (App.getState().colorScheme() == index) {
                Style.StylePicker.itemSelected();
            } else {
                Style.StylePicker.itemNormal();
            }
        });
    });

    private void onStyleClicked(AdapterView parent, View v, int pos, long id) {
        App.dispatch(new Action<>(ActionType.SET_COLOR_SCHEME, pos));
    }
}
