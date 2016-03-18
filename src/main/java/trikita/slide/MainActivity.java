package trikita.slide;

import android.app.Activity;
import android.content.Context;
import android.os.Bundle;
import android.view.View;
import android.view.Window;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.IOException;

import trikita.anvil.Anvil;

import trikita.jedux.Action;
import trikita.jedux.Store;
import trikita.jedux.Logger;

public class MainActivity extends Activity {

    static Store<Action<ActionType, ?>, State> store;

    private Runnable unsubscribe;

    @Override
    public void onCreate(Bundle b) {
        super.onCreate(b);

        store = new Store<>(Slide::reducer,
                initialState(),
                new Logger<>("Slide"),
                new PersistMiddleware(this),
                new WindowMiddleware(getWindow()));

        this.unsubscribe = store.subscribe(Anvil::render);

        setContentView(new MainLayout(this));
        getWindow().setStatusBarColor(store.getState().backgroundColor());
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        this.unsubscribe.run();
    }

    @Override
    public void onBackPressed() {
        if (store.getState().presentationMode()) {
            store.dispatch(new Action<>(ActionType.CLOSE_PRESENTATION));
        } else {
            super.onBackPressed();
        }
    }

    private State initialState() {
        if (getSharedPreferences("data", 0).contains("data")) {
            String json = getSharedPreferences("data", 0).getString("data", "");
            try {
                return new ObjectMapper().readValue(json, ImmutableState.class);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        return ImmutableState.builder()
                .text("")
                .page(0)
                .foregroundColor(MainLayout.COLOR_SCHEMES[0][0])
                .backgroundColor(MainLayout.COLOR_SCHEMES[0][1])
                .presentationMode(false)
                .toolbarShown(true)
                .build();
    }

    private static class WindowMiddleware implements Store.Middleware<Action<ActionType, ?>, State> {
        private final Window mWindow;
        public WindowMiddleware(Window w) {
            mWindow = w;
        }
        @Override
        public void dispatch(Store<Action<ActionType, ?>, State> store, Action<ActionType, ?> action,
                             Store.NextDispatcher<Action<ActionType, ?>> next) {
            next.dispatch(action);
            switch (action.type) {
                case SET_BACKGROUND:
                    mWindow.setStatusBarColor(store.getState().backgroundColor());
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

    private static class PersistMiddleware implements Store.Middleware<Action<ActionType, ?>, State> {
        private final Context mContext;
        private final ObjectMapper mObjectMapper;

        public PersistMiddleware(Context c) {
            mContext = c;
            mObjectMapper = new ObjectMapper();
        }
        @Override
        public void dispatch(Store<Action<ActionType, ?>, State> store, Action<ActionType, ?> action,
                             Store.NextDispatcher<Action<ActionType, ?>> next) {
            next.dispatch(action);
            String json = null;
            try {
                json = mObjectMapper.writeValueAsString(store.getState());
                mContext.getSharedPreferences("data", 0).edit().putString("data", json).apply();
            } catch (JsonProcessingException e) {
                e.printStackTrace();
            }
        }
    }
}
