package trikita.slide.ui;

import android.content.Context;
import android.widget.GridView;

import java.util.Arrays;

import trikita.anvil.RenderableAdapter;
import trikita.anvil.RenderableView;
import trikita.jedux.Action;
import trikita.slide.ActionType;
import trikita.slide.App;

import static trikita.anvil.DSL.CENTER;
import static trikita.anvil.DSL.FILL;
import static trikita.anvil.DSL.WRAP;
import static trikita.anvil.DSL.adapter;
import static trikita.anvil.DSL.dip;
import static trikita.anvil.DSL.gravity;
import static trikita.anvil.DSL.gridView;
import static trikita.anvil.DSL.numColumns;
import static trikita.anvil.DSL.onItemClick;
import static trikita.anvil.DSL.padding;
import static trikita.anvil.DSL.size;
import static trikita.anvil.DSL.stretchMode;
import static trikita.anvil.DSL.textView;
import static trikita.anvil.DSL.verticalSpacing;

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
            onItemClick((parent, v, pos, id) -> onStyleClicked(pos));
        });
    }

    private final RenderableAdapter mStyleAdapter = RenderableAdapter.withItems(Arrays.asList(Style.COLOR_SCHEMES), (index, item) -> {
        textView(() -> {
            Style.StylePicker.circle(item[0], item[1]);
            if (App.getState().getCurrentPresentation().colorScheme() == index) {
                Style.StylePicker.itemSelected();
            } else {
                Style.StylePicker.itemNormal();
            }
        });
    });

    private void onStyleClicked(int pos) {
        App.dispatch(new Action<>(ActionType.SET_COLOR_SCHEME, pos));
    }
}
