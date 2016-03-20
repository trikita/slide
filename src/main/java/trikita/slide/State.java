package trikita.slide;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;

import org.immutables.value.Value;

import trikita.jedux.Action;
import trikita.jedux.Store;
import trikita.slide.ui.MainLayout;

@Value.Immutable
@JsonSerialize(as = ImmutableState.class)
@JsonDeserialize(as = ImmutableState.class)
public interface State {
    String text();
    int page();

    boolean presentationMode();
    boolean toolbarShown();

    int backgroundColor();
    int foregroundColor();

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
                case SET_BACKGROUND:
                    return ImmutableState.copyOf(s).withBackgroundColor((Integer) a.value);
                case SET_FOREGROUND:
                    return ImmutableState.copyOf(s).withForegroundColor((Integer) a.value);
            }
            return s;
        }
    }

    class Default {
        public static State build() {
            return ImmutableState.builder()
                    .text("")
                    .page(0)
                    .foregroundColor(MainLayout.COLOR_SCHEMES[0][0])
                    .backgroundColor(MainLayout.COLOR_SCHEMES[0][1])
                    .presentationMode(false)
                    .toolbarShown(true)
                    .build();
        }
    }
}
