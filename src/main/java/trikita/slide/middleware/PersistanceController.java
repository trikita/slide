package trikita.slide.middleware;

import android.app.Application;
import android.content.SharedPreferences;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Message;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import trikita.jedux.Action;
import trikita.jedux.Store;
import trikita.slide.ActionType;
import trikita.slide.GsonAdaptersState;
import trikita.slide.ImmutableState;
import trikita.slide.State;

public class PersistanceController implements Store.Middleware<Action<ActionType, ?>, State> {

    private final SharedPreferences mPreferences;
    private final Gson mGson;
    private final Handler mHandler;

    public PersistanceController(Application c) {
        mPreferences = c.getSharedPreferences("data", 0);
        GsonBuilder gsonBuilder = new GsonBuilder();
        gsonBuilder.registerTypeAdapterFactory(new GsonAdaptersState());
        mGson = gsonBuilder.create();

        // the data can be quite large, so the saving process takes place in another thread to
        // prevent freezing the UI
        HandlerThread ht = new HandlerThread("data_persister");
        ht.start();
        mHandler = new Handler(ht.getLooper(), new Handler.Callback() {
            @Override
            public boolean handleMessage(Message msg) {
                saveState((Store<Action<ActionType, ?>, State>)(msg.obj));
                return true;
            }
        });
    }

    public State getSavedState() {
        if (mPreferences.contains("data")) {
            String json = mPreferences.getString("data", "");
            return mGson.fromJson(json, ImmutableState.class);
        }
        return null;
    }

    private void saveState(Store<Action<ActionType, ?>, State> store) {
        String json = mGson.toJson(store.getState());
        mPreferences.edit().putString("data", json).apply();
    }

    @Override
    public void dispatch(Store<Action<ActionType, ?>, State> store, Action<ActionType, ?> actionTypeAction, Store.NextDispatcher<Action<ActionType, ?>> nextDispatcher) {
        nextDispatcher.dispatch(actionTypeAction);
        mHandler.removeMessages(0);
        mHandler.sendMessageDelayed(mHandler.obtainMessage(0, store), 300);
    }
}
