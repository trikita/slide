package trikita.slide.middleware;

import android.content.Context;
import android.content.SharedPreferences;

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

    public PersistanceController(Context c) {
        mPreferences = c.getSharedPreferences("data", 0);
        GsonBuilder gsonBuilder = new GsonBuilder();
        gsonBuilder.registerTypeAdapterFactory(new GsonAdaptersState());
        mGson = gsonBuilder.create();
    }

    public State getSavedState() {
        if (mPreferences.contains("data")) {
            String json = mPreferences.getString("data", "");
            try {
                return mGson.fromJson(json, ImmutableState.class);
            } catch (Exception unused) {}
        }
        return null;
    }

    @Override
    public void dispatch(Store<Action<ActionType, ?>, State> store, Action<ActionType, ?> action,
            Store.NextDispatcher<Action<ActionType, ?>> next) {
        next.dispatch(action);
        String json = mGson.toJson(store.getState());
        mPreferences.edit().putString("data", json).apply();
    }
}
