package trikita.slide.ui;

import android.app.Activity;
import android.os.Bundle;

import trikita.jedux.Action;
import trikita.slide.ActionType;
import trikita.slide.App;

public class MainActivity extends Activity {

    @Override
    public void onCreate(Bundle b) {
        super.onCreate(b);
        setContentView(new MainLayout(this));
        App.getWindowController().setWindow(getWindow());
    }

    @Override
    public void onResume() {
        super.onResume();
        if (App.getState().presentationMode()) {
            App.dispatch(new Action<>(ActionType.OPEN_PRESENTATION));
        }
    }

    @Override
    protected void onDestroy() {
        App.getWindowController().setWindow(null);
        super.onDestroy();
    }

    @Override
    public void onBackPressed() {
        if (App.getState().presentationMode()) {
            App.dispatch(new Action<>(ActionType.CLOSE_PRESENTATION));
        } else {
            super.onBackPressed();
        }
    }
}
