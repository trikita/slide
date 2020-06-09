package trikita.slide.middleware;

import android.app.Activity;
import android.os.AsyncTask;
import android.os.Handler;
import android.os.Message;
import android.util.Pair;

import java.util.Map;
import java.util.Set;
import java.util.concurrent.CancellationException;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Future;
import java.util.concurrent.FutureTask;
import java.util.concurrent.TimeoutException;

import trikita.anvil.Anvil;
import trikita.jedux.Action;
import trikita.jedux.Store;
import trikita.slide.ActionType;
import trikita.slide.App;
import trikita.slide.Presentation;
import trikita.slide.Slide;
import trikita.slide.State;
import trikita.slide.functions.MathTypeSetter;
import trikita.slide.functions.PlantUMLProcessor;
import trikita.slide.functions.SlideTemplateProcessor;

public class BuildController implements Store.Middleware<Action<ActionType, ?>, State> {

    protected Activity ctx;
    protected final Map<Pair<Integer,Integer>, Future<Slide>> buildCache;
    protected final Handler buildHandler;

    private static final int REGENERATION_TIMEOUT = 300;

    public BuildController() {
        super();
        buildCache = new ConcurrentHashMap<>();
        buildHandler = new Handler(msg -> {
            switch(msg.what) {
            case 0:
                cancelBuildsAndInvalidateBuildCache(true);
                return true;
            }
            return false;
        });
    }

    private BuildController cancelBuildsAndInvalidateBuildCache(boolean now) {
        buildHandler.removeMessages(0);
        if(now) {
            Set<Pair<Integer, Integer>> keys = buildCache.keySet();
            for (Pair<Integer, Integer> key : keys) {
                Future<Slide> slide = buildCache.get(key);
                if (!slide.isDone()) slide.cancel(true);
                buildCache.remove(key);
            }
            Anvil.render();
        } else {
            buildHandler.sendMessageDelayed(
                Message.obtain(buildHandler, 0),
                REGENERATION_TIMEOUT
            );
        }
        return this;
    }

    private BuildController reportFailure(Pair<Integer,Integer> cacheKey) {
        buildCache.remove(cacheKey);
        return this;
    }

    public Future<Slide> build(Presentation p, int page, int width, int timeout, Runnable onDone, Runnable onTimeout) {
        final Pair<Integer,Integer> cacheKey = new Pair<>(page,width);

        if(buildCache.containsKey(cacheKey))
            return buildCache.get(cacheKey);

        final FutureTask<Slide> future = new FutureTask<>(() -> {
            try {
                Slide.Builder builder =
                    new MathTypeSetter(ctx, timeout,
                    new SlideTemplateProcessor(
                    p.slideBuilder(page, width)
                ).call()
                ).call();

                if (p.plantUMLEnabled())
                    builder = new PlantUMLProcessor(builder).call();

                return builder.build();
            } catch(Exception e) {
                App.getBuildController().reportFailure(cacheKey);
                if(e.getCause() instanceof CancellationException
                    && e.getCause().getCause() instanceof TimeoutException
                    && onTimeout != null)
                    onTimeout.run();
                throw e;
            } finally {
                if(onDone != null) onDone.run();
            }
        });
        AsyncTask.THREAD_POOL_EXECUTOR.execute(future);

        buildCache.put(cacheKey, future);
        return future;
    }

    public void setActivity(Activity act) { this.ctx = act; }

    @Override
    public void dispatch(Store<Action<ActionType, ?>, State> store, Action<ActionType, ?> action, Store.NextDispatcher<Action<ActionType, ?>> nextDispatcher) {
        nextDispatcher.dispatch(action);
        switch(action.type) {
            case SET_TEXT:
            case SET_COLOR_SCHEME:
            case CONFIGURE_PLANTUML:
            case SET_TEMPLATE:
                cancelBuildsAndInvalidateBuildCache(false);
                break;
        }
    }
}
