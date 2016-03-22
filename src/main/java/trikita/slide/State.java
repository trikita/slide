package trikita.slide;

import org.immutables.gson.Gson;
import org.immutables.value.Value;

import trikita.jedux.Action;
import trikita.jedux.Store;
import trikita.slide.ui.MainLayout;
import android.content.Context;

@Value.Immutable
@Gson.TypeAdapters
public interface State {
    String text();
    int page();

    boolean presentationMode();
    boolean toolbarShown();

    int colorScheme();

    class Reducer implements Store.Reducer<Action<ActionType, ?>, State> {
        public State reduce(Action<ActionType, ?> a, State s) {
            switch (a.type) {
                case SET_TEXT:
                    return ImmutableState.copyOf(s).withText((String) a.value);
                case SET_PAGE:
                    return ImmutableState.copyOf(s).withPage((Integer) a.value);
                case NEXT_PAGE:
                    return ImmutableState.copyOf(s)
                            .withPage(Math.min(s.page()+1, Slide.paginate(s.text()).length-1));
                case PREV_PAGE:
                    return ImmutableState.copyOf(s)
                            .withPage(Math.max(s.page()-1, 0));
                case OPEN_PRESENTATION:
                    return ImmutableState.copyOf(s).withPresentationMode(true);
                case CLOSE_PRESENTATION:
                    return ImmutableState.copyOf(s).withPresentationMode(false);
                case TOGGLE_TOOLBAR:
                    return ImmutableState.copyOf(s).withToolbarShown(!s.toolbarShown());
                case SET_COLOR_SCHEME:
                    return ImmutableState.copyOf(s).withColorScheme((Integer) a.value);
            }
            return s;
        }
    }

    class Default {
        public static State build(Context c) {
            return ImmutableState.builder()
                    .text(c.getString(R.string.tutorial_text))
                    .page(0)
                    .colorScheme(0)
                    .presentationMode(false)
                    .toolbarShown(true)
                    .build();
        }
    }
}
