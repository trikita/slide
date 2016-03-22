package trikita.slide.middleware;

import android.os.Build;
import android.view.View;
import android.view.Window;

import trikita.jedux.Action;
import trikita.jedux.Store;
import trikita.slide.ActionType;
import trikita.slide.App;
import trikita.slide.State;
import trikita.slide.ui.Style;

public class WindowController implements Store.Middleware<Action<ActionType, ?>, State> {
    private Window mWindow;

    public void setWindow(Window w) {
        mWindow = w;
        if (w != null && Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            w.setStatusBarColor(Style.COLOR_SCHEMES[App.getState().colorScheme()][1]);
        }
    }

    @Override
    public void dispatch(Store<Action<ActionType, ?>, State> store, Action<ActionType, ?> action,
                         Store.NextDispatcher<Action<ActionType, ?>> next) {
        next.dispatch(action);
        if (mWindow != null) {
            switch (action.type) {
                case SET_COLOR_SCHEME:
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                        mWindow.setStatusBarColor(Style.COLOR_SCHEMES[store.getState().colorScheme()][1]);
                    }
                    break;
                case OPEN_PRESENTATION:
                    int uiOptions = View.SYSTEM_UI_FLAG_HIDE_NAVIGATION | View.SYSTEM_UI_FLAG_FULLSCREEN | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY;
                    mWindow.getDecorView().setSystemUiVisibility(uiOptions);
                    break;
                case CLOSE_PRESENTATION:
                    mWindow.getDecorView().setSystemUiVisibility(0);
                    break;
            }
        }
    }
}
