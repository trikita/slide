package trikita.slide.ui;

import android.content.Context;
import android.view.MotionEvent;
import android.view.View;
import android.widget.EditText;

import trikita.jedux.Action;
import trikita.slide.ActionType;
import trikita.slide.App;

public class Editor extends EditText implements View.OnTouchListener {

    interface OnSelectionChangedListener {
        void onSelectionChanged(int position);
    }

    private OnSelectionChangedListener mOnSelectionChangedListener;

    public Editor(Context context) {
        super(context);
        this.setOnTouchListener(this);
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

    private float downX;
    private float downY;

    @Override
    public boolean onTouch(View v, MotionEvent event) {
        switch(event.getAction()){
        case MotionEvent.ACTION_DOWN:
            downX = event.getX();
            downY = event.getY();
            return false;

        case MotionEvent.ACTION_UP:
            float upX = event.getX();
            float upY = event.getY();

            float deltaX = downX - upX;
            float deltaY = downY - upY;

            //HORIZONTAL SCROLL
            if(Math.abs(deltaX) > Math.abs(deltaY))
            {
                int min_distance = 100;
                if(Math.abs(deltaX) > min_distance){
                    // left or right
                    if(deltaX < 0)
                    {
                        App.dispatch(new Action<>(ActionType.PREVIOUS_PRESENTATION, getContext()));
                        return true;
                    }
                    if(deltaX > 0) {
                        App.dispatch(new Action<>(ActionType.NEXT_PRESENTATION, getContext()));
                        return true;
                    }
                }
            }
        }
        return false;
    }
}
