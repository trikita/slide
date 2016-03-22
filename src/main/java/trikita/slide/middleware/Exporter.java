package trikita.slide.middleware;

import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.Intent;
import android.graphics.Typeface;
import android.graphics.pdf.PdfDocument;
import android.net.Uri;
import android.os.Environment;
import android.text.TextPaint;
import android.webkit.MimeTypeMap;
import android.widget.Toast;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;

import trikita.jedux.Action;
import trikita.jedux.Store;
import trikita.slide.ActionType;
import trikita.slide.App;
import trikita.slide.Slide;
import trikita.slide.State;
import trikita.slide.ui.Style;

public class Exporter implements Store.Middleware<Action<ActionType, ?>, State> {

    private final Context mContext;

    public Exporter(Context c) {
        mContext = c;
    }

    @Override
    public void dispatch(Store<Action<ActionType, ?>, State> store, Action<ActionType, ?> action, Store.NextDispatcher<Action<ActionType, ?>> next) {
        if (action.type == ActionType.SHARE) {
            System.out.println("SHARE ");
            File f = savePdf(store, "slide.pdf");
            if (f != null) {
                if (!sharePdf(f)) {
                    Toast.makeText(mContext, "Saved to " + f.getAbsolutePath(), Toast.LENGTH_LONG).show();
                }
            }
            return;
        }
        next.dispatch(action);
    }

    private boolean sharePdf(File f) {
        String mime = MimeTypeMap.getSingleton().getMimeTypeFromExtension("pdf");
        Intent intent = new Intent();
        intent.setAction(Intent.ACTION_VIEW);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        intent.setDataAndType(Uri.fromFile(f), mime);
        try {
            mContext.startActivity(intent);
            return true;
        } catch (ActivityNotFoundException e) {
            e.printStackTrace();
            return false;
        }
    }

    private File savePdf(Store<Action<ActionType, ?>, State> store, String filename) {
        PdfDocument document = new PdfDocument();
        try {
            PdfDocument.PageInfo pageInfo = new PdfDocument.PageInfo.Builder(640, 640 * 9 / 16, 1).create();

            String fullText = store.getState().text();
            for (int i = 0; i < Slide.paginate(fullText).length; i++) {
                String text = Slide.pageText(fullText, i);
                System.out.println("render: " + text);

                PdfDocument.Page page = document.startPage(pageInfo);
                page.getCanvas().drawColor(Style.COLOR_SCHEMES[store.getState().colorScheme()][1]);
                Slide.renderPage(text,
                        page.getCanvas(),
                        Style.SLIDE_FONT,
                        Style.COLOR_SCHEMES[App.getState().colorScheme()][0],
                        Style.COLOR_SCHEMES[App.getState().colorScheme()][1]);
                document.finishPage(page);
            }

            File file = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOCUMENTS), filename);
            if (!file.getParentFile().exists()) {
                file = new File(Environment.getExternalStorageDirectory(), filename);
            }
            FileOutputStream fos = new FileOutputStream(file);
            document.writeTo(fos);
            return file;
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        } finally {
            document.close();
        }
    }
}
