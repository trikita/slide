package trikita.slide.preprocessors;

import java.util.ArrayList;
import java.util.List;

import trikita.slide.ImmutablePresentation;
import trikita.slide.Presentation;

public class SlideTemplatePreprocessor implements PresentationPreprocessor {
    @Override
    public Presentation process(Presentation p) {
        List<String> finalPar = new ArrayList<>();
        for(String par : p.splitParagraphs()) {
            finalPar.add(p.templateBefore()+par+p.templateAfter());
        }
        return ImmutablePresentation.copyOf(p).withText(
            p.joinParagraphs(finalPar.toArray(new String[]{}))
        );
    }
}
