package trikita.slide;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;

import org.immutables.value.Value;

@Value.Immutable
@JsonSerialize(as = ImmutableState.class)
@JsonDeserialize(as = ImmutableState.class)
interface State {
    String text();
    int page();

    boolean presentationMode();
    boolean toolbarShown();

    int backgroundColor();
    int foregroundColor();
}
