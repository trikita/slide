package trikita.slide.middleware;

import android.os.Build;
import android.view.View;
import android.view.Window;
import android.view.WindowInsets;
import android.view.WindowInsetsController;

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
                    hideSystemBars();
                    break;
                case CLOSE_PRESENTATION:
                    showSystemBars();
                    break;
            }
        }
    }

    private void hideSystemBars() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            mWindow.setDecorFitsSystemWindows(false);
            WindowInsetsController controller = mWindow.getInsetsController();
            if (controller != null) {
                controller.hide(WindowInsets.Type.systemBars());
                controller.setSystemBarsBehavior(
                        WindowInsetsController.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE);
            }
        } else {
            int uiOptions = View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                    | View.SYSTEM_UI_FLAG_FULLSCREEN
                    | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY;
            mWindow.getDecorView().setSystemUiVisibility(uiOptions);
        }
    }

    private void showSystemBars() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            mWindow.setDecorFitsSystemWindows(true);
            WindowInsetsController controller = mWindow.getInsetsController();
            if (controller != null) {
                controller.show(WindowInsets.Type.systemBars());
            }
        } else {
            mWindow.getDecorView().setSystemUiVisibility(0);
        }
    }
}
