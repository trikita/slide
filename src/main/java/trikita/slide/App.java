package trikita.slide;

import android.app.Application;

import trikita.anvil.Anvil;
import trikita.jedux.Action;
import trikita.jedux.Logger;
import trikita.jedux.Store;
import trikita.slide.middleware.StorageController;
import trikita.slide.middleware.PersistanceController;
import trikita.slide.middleware.WindowController;

public class App extends Application {
    private static App instance;

    private Store<Action<ActionType, ?>, State> store;
    private WindowController windowController;

    @Override
    public void onCreate() {
        super.onCreate();
        App.instance = this;

        this.windowController = new WindowController();

        PersistanceController persistanceController = new PersistanceController(this);
        State initialState = persistanceController.getSavedState();
        if (initialState == null) {
            initialState = State.Default.build(this);
        }
        StorageController sc = new StorageController(this);

        this.store = new Store<>(new State.Reducer(),
                initialState,
//                new Logger<>("Slide"),
                persistanceController,
                this.windowController,
                sc);

        sc.dumpToFile(false);   // false - with no delay

        this.store.subscribe(Anvil::render);
    }

    public static WindowController getWindowController() {
        return instance.windowController;
    }

    public static State getState() {
        return instance.store.getState();
    }

    public static State dispatch(Action<ActionType, ?> action) {
        return instance.store.dispatch(action);
    }
}
