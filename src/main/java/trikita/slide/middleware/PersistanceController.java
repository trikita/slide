package trikita.slide.middleware;

import android.content.Context;
import android.content.SharedPreferences;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.IOException;

import trikita.jedux.Action;
import trikita.jedux.Store;
import trikita.slide.ActionType;
import trikita.slide.ImmutableState;
import trikita.slide.State;

public class PersistanceController implements Store.Middleware<Action<ActionType, ?>, State> {

    private final ObjectMapper mObjectMapper;
    private final SharedPreferences mPreferences;

    public PersistanceController(Context c) {
        mPreferences = c.getSharedPreferences("data", 0);
        mObjectMapper = new ObjectMapper();
    }

    public State getSavedState() {
        if (mPreferences.contains("data")) {
            String json = mPreferences.getString("data", "");
            try {
                return mObjectMapper.readValue(json, ImmutableState.class);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        return null;
    }

    @Override
    public void dispatch(Store<Action<ActionType, ?>, State> store, Action<ActionType, ?> action,
                         Store.NextDispatcher<Action<ActionType, ?>> next) {
        next.dispatch(action);
        try {
            String json = mObjectMapper.writeValueAsString(store.getState());
            mPreferences.edit().putString("data", json).apply();
        } catch (JsonProcessingException e) {
            e.printStackTrace();
        }
    }
}
