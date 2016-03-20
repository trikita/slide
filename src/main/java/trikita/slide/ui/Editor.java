package trikita.slide.ui;

import android.content.Context;
import android.widget.EditText;

public class Editor extends EditText {

    interface OnSelectionChangedListener {
        void onSelectionChanged(int position);
    }

    private OnSelectionChangedListener mOnSelectionChangedListener;

    public Editor(Context context) {
        super(context);
    }

    public void setOnSelectionChangedListener(OnSelectionChangedListener l) {
        mOnSelectionChangedListener = l;
    }

    @Override
    protected void onSelectionChanged(int selStart, int selEnd) {
        super.onSelectionChanged(selStart, selEnd);
        if (mOnSelectionChangedListener != null) {
            mOnSelectionChangedListener.onSelectionChanged(selStart);
        }
    }
}
