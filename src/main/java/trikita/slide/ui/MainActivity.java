package trikita.slide.ui;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;

import trikita.jedux.Action;
import trikita.promote.Promote;
import trikita.slide.ActionType;
import trikita.slide.App;
import trikita.slide.middleware.StorageController;

public class MainActivity extends Activity {

    @Override
    public void onCreate(Bundle b) {
        super.onCreate(b);
        App.setMainActivity(this);
        MainLayout l = new MainLayout(this);
        setContentView(l);
        App.dispatch(new Action<>(ActionType.SET_WINDOW, getWindow()));
        App.setMainLayout(l);
    }

    @Override
    public void onResume() {
        super.onResume();
        if (App.getState().presentationMode()) {
            App.dispatch(new Action<>(ActionType.OPEN_PRESENTATION));
        }
        Promote.after(3).times().every(10).times().rate(this);
        Promote.after(5).times().every(5).times().share(this,
                Promote.FACEBOOK_TWITTER,
                "https://github.com/trikita/slide",
                "Slide: Minimal presentation maker");
    }

    @Override
    protected void onDestroy() {
        App.setMainLayout(null);
        App.dispatch(new Action<>(ActionType.SET_WINDOW, null));
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

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (resultCode != RESULT_OK || data == null) {
            return;
        }

        if (requestCode == StorageController.EXPORT_PDF_REQUEST_CODE) {
            Uri uri = data.getData();
            App.dispatch(new Action<>(ActionType.EXPORT_PDF, uri));
        } else if (requestCode == StorageController.PICK_IMAGE_REQUEST_CODE) {
            Uri uri = data.getData();
            App.dispatch(new Action<>(ActionType.INSERT_IMAGE, uri));
        }
    }
}
