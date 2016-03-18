package trikita.slide;

import trikita.jedux.Action;

public class Slide {
    static State reducer(Action<ActionType, ?> a, State s) {
        switch (a.type) {
            case SET_TEXT:
                return ImmutableState.copyOf(s).withText((String) a.value);
            case SET_PAGE:
                return ImmutableState.copyOf(s).withPage((Integer) a.value);
            case NEXT_PAGE:
                return ImmutableState.copyOf(s)
                        .withPage(Math.min(s.page()+1, paginate(s.text()).length-1));
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
    static int page(String s, int pos) {
        if (pos > s.length()-1) {
            pos = s.length()-1;
        }
        return paginate(s.substring(0, pos+1)).length-1;
    }

    static String[] paginate(String text) {
        return text.split("(\n\\s*){2,}");
    }

    static String pageText(String text, int page) {
        if (page < 0) {
            return "";
        }
        return paginate(text)[page];
    }
}
