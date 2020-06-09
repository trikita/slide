package trikita.slide.functions;

import java.util.concurrent.Callable;

import trikita.slide.Slide;

public class SlideTemplateProcessor implements Callable<Slide.Builder> {
    protected final Slide.Builder p;

    public SlideTemplateProcessor(Slide.Builder p) {
        this.p = p;
    }

    @Override
    public Slide.Builder call() {
        return p.withText(p.presentation.templateBefore() + p.text + p.presentation.templateAfter());
    }
}
