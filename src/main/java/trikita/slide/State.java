package trikita.slide;

import android.content.Context;
import android.os.Vibrator;
import android.util.Pair;

import org.immutables.gson.Gson;
import org.immutables.value.Value;

import java.util.Collections;
import java.util.List;
import java.util.Objects;

import trikita.jedux.Action;
import trikita.jedux.Store;

@Value.Immutable
@Gson.TypeAdapters
public abstract class State {

    public abstract boolean presentationMode();
    public abstract boolean toolbarShown();

    public abstract int currentPresentationIndex();
    public abstract List<ImmutablePresentation> presentations();

    public ImmutablePresentation getCurrentPresentation() {
        return presentations().get(currentPresentationIndex());
    }

    State withCurrentPresentation(ImmutablePresentation p) {
        ImmutableState.Builder b = ImmutableState.builder()
            .from(this)
            .presentations(Collections.emptyList());

        for(int i = 0; i < presentations().size(); ++i) {
            if (i == currentPresentationIndex())
                b.addPresentations(p);
            else
                b.addPresentations(presentations().get(i));
        }

        return b.build();
    }

    static class Reducer implements Store.Reducer<Action<ActionType, ?>, State> {
        private void vibrateOnPresentationChange(Context ctx) {
            ((Vibrator) Objects.requireNonNull(ctx.getSystemService(Context.VIBRATOR_SERVICE)))
                                .vibrate(50);
        }

        public State reduce(Action<ActionType, ?> a, State s) {
            switch (a.type) {
                case SET_CURSOR:
                    return ImmutableState.copyOf(s)
                            .withCurrentPresentation(s.getCurrentPresentation().withCursor((Integer)a.value));
                case SET_TEXT:
                    return ImmutableState.copyOf(s).withCurrentPresentation(s.getCurrentPresentation().withText((String)a.value));
                case NEXT_PAGE:
                    App.getMainLayout().cursor(s.getCurrentPresentation().pageTurn(App.getMainLayout().cursor(),1));
                    // state has changed, do not return s!!
                    return App.getState();
                case PREV_PAGE:
                    App.getMainLayout().cursor(s.getCurrentPresentation().pageTurn(App.getMainLayout().cursor(),-1));
                    // state has changed, do not return s!!
                    return App.getState();
                case OPEN_PRESENTATION:
                    return ImmutableState.copyOf(s).withPresentationMode(true);
                case CLOSE_PRESENTATION:
                    return ImmutableState.copyOf(s).withPresentationMode(false);
                case TOGGLE_TOOLBAR:
                    return ImmutableState.copyOf(s).withToolbarShown(!s.toolbarShown());
                case SET_COLOR_SCHEME:
                    return ImmutableState.copyOf(s).withCurrentPresentation(s.getCurrentPresentation().withColorScheme((Integer) a.value));
                case CONFIGURE_PLANTUML:
                    Pair<Boolean,Pair<String,Pair<String,String>>> configuration = (Pair<Boolean,Pair<String,Pair<String,String>>>)a.value;
                    boolean enabled = configuration.first;
                    String endPoint = configuration.second.first;
                    String templateBefore = configuration.second.second.first;
                    String templateAfter = configuration.second.second.second;
                    return ImmutableState.copyOf(s).withCurrentPresentation(s.getCurrentPresentation()
                        .withPlantUMLEnabled(enabled)
                        .withPlantUMLEndPoint(endPoint)
                        .withPlantUMLTemplateBefore(templateBefore)
                        .withPlantUMLTemplateAfter(templateAfter));
                case SET_TEMPLATE:
                    Pair<String,String> beforeAfter = (Pair<String,String>)a.value;
                    return ImmutableState.copyOf(s).withCurrentPresentation(s.getCurrentPresentation()
                        .withTemplateBefore(beforeAfter.first)
                        .withTemplateAfter(beforeAfter.second));
                case SET_PDF_RESOLUTION:
                    return ImmutableState.copyOf(s).withCurrentPresentation(s.getCurrentPresentation()
                        .withPdfResolution((Integer)a.value));
                case PREVIOUS_PRESENTATION:
                    if(s.currentPresentationIndex() == s.presentations().size()-1 && s.currentPresentationIndex() > 0
                       && s.getCurrentPresentation().text().equals(((Context)a.value).getString(R.string.tutorial_text))) {
                        this.vibrateOnPresentationChange((Context)a.value);
                        return ImmutableState.copyOf(s)
                                .withPresentations(s.presentations().subList(0, s.currentPresentationIndex()))
                                .withCurrentPresentationIndex(s.currentPresentationIndex()-1);
                    }
                    else {
                        int newIndex = Math.max(0, Math.min(s.currentPresentationIndex() - 1, s.presentations().size() - 1));
                        if(newIndex != s.currentPresentationIndex()) {
                            this.vibrateOnPresentationChange((Context) a.value);
                            return ImmutableState.copyOf(s)
                                    .withCurrentPresentationIndex(newIndex);
                        } else {
                            return ImmutableState.copyOf(s);
                        }
                    }
                case NEXT_PRESENTATION:
                    if(s.currentPresentationIndex() == s.presentations().size()-1
                        && !s.getCurrentPresentation().text().equals(((Context)a.value).getString(R.string.tutorial_text))) {
                        this.vibrateOnPresentationChange((Context)a.value);
                        return ImmutableState.builder().from(s)
                                .addPresentations(Presentation.Default.build((Context) a.value))
                                .currentPresentationIndex(s.currentPresentationIndex()+1)
                                .build();
                    }
                    else {
                        int newIndex = Math.max(0, Math.min(s.currentPresentationIndex() + 1, s.presentations().size() - 1));
                        if (newIndex != s.currentPresentationIndex()) {
                            this.vibrateOnPresentationChange((Context) a.value);
                            return ImmutableState.copyOf(s)
                                    .withCurrentPresentationIndex(newIndex);
                        } else {
                            return ImmutableState.copyOf(s);
                        }
                    }
                case REMOVE_PRESENTATION:
                    ImmutableState t = ImmutableState.builder().from(s)
                        .presentations(s.presentations().subList(0,s.currentPresentationIndex()))
                        .addAllPresentations(s.presentations().subList(s.currentPresentationIndex()+1, s.presentations().size()))
                        .currentPresentationIndex(0)
                        .build();
                    if(t.presentations().size() == 0)
                        t = ImmutableState.copyOf(s)
                            .withPresentations(Presentation.Default.build((Context)a.value));
                    return t;
            }
            return s;
        }
    }

    static class Default {
        public static State build(Context c) {
            return ImmutableState.builder()
                .presentationMode(false)
                .toolbarShown(true)
                .currentPresentationIndex(0)
                .addPresentations(Presentation.Default.build(c))
                .build();
        }
    }
}
