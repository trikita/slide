package trikita.slide.middleware;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.graphics.pdf.PdfDocument;
import android.net.Uri;
import android.os.ParcelFileDescriptor;
import android.widget.Toast;

import java.io.FileOutputStream;
import java.io.IOException;

import trikita.jedux.Action;
import trikita.jedux.Store;
import trikita.slide.ActionType;
import trikita.slide.App;
import trikita.slide.R;
import trikita.slide.Slide;
import trikita.slide.State;
import trikita.slide.ui.Style;
import java.util.TimeZone;
import java.util.Calendar;
import java.text.SimpleDateFormat;
import java.text.DateFormat;

public class StorageController implements Store.Middleware<Action<ActionType, ?>, State> {
    public static final int PICK_IMAGE_REQUEST_CODE = 44;
    public static final int EXPORT_PDF_REQUEST_CODE = 46;

    private final Context mContext;

    public StorageController(Context c) {
        mContext = c;
    }

    @Override
    public void dispatch(Store<Action<ActionType, ?>, State> store, Action<ActionType, ?> action, Store.NextDispatcher<Action<ActionType, ?>> next) {
        if (action.type == ActionType.CREATE_PDF) {
            createPdf((Activity) action.value);
            return;
        } else if (action.type == ActionType.EXPORT_PDF) {
            if (!exportToPdf(store, (Uri) action.value)) {
                Toast.makeText(mContext, mContext.getString(R.string.failed_export_pdf), Toast.LENGTH_LONG).show();
            }
            return;
        } else if (action.type == ActionType.PICK_IMAGE) {
            pickImage((Activity) action.value);
            return;
        } else if (action.type == ActionType.INSERT_IMAGE) {
            String s = store.getState().text();
            int c = store.getState().cursor();
            String chunk = s.substring(0, c);
            int startOfLine = chunk.lastIndexOf("\n");
            if (startOfLine == -1) {
                startOfLine = 0;
                s = "@"+((Uri) action.value).toString()+"\n"+s;
            } else {
                s = s.substring(0, startOfLine+1)+"@"+((Uri) action.value).toString()+"\n"+s.substring(startOfLine+1);
            }
            App.dispatch(new Action<>(ActionType.SET_TEXT, s));
            System.out.println("INSERT IMAGE cursor="+startOfLine);
            App.dispatch(new Action<>(ActionType.SET_CURSOR, startOfLine));
            return;
        }
        next.dispatch(action);
    }

    private void createPdf(Activity a) {
        Intent intent = new Intent(Intent.ACTION_CREATE_DOCUMENT);
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        intent.setType("application/pdf");
        intent.putExtra(Intent.EXTRA_TITLE, a.getString(R.string.pdf_name_prefix)+getTimestamp());
        a.startActivityForResult(intent, EXPORT_PDF_REQUEST_CODE);
    }

    private boolean exportToPdf(Store<Action<ActionType, ?>, State> store, Uri uri) {
        PdfDocument document = new PdfDocument();
        ParcelFileDescriptor pfd = null;
        try {
            PdfDocument.PageInfo pageInfo = new PdfDocument.PageInfo.Builder(640, 640 * 9 / 16, 1).create();

            for (Slide slide : store.getState().slides()) {
                PdfDocument.Page page = document.startPage(pageInfo);
                page.getCanvas().drawColor(Style.COLOR_SCHEMES[store.getState().colorScheme()][1]);
                slide.render(mContext,
                        page.getCanvas(),
                        Style.SLIDE_FONT,
                        Style.COLOR_SCHEMES[App.getState().colorScheme()][0],
                        Style.COLOR_SCHEMES[App.getState().colorScheme()][1]);
                document.finishPage(page);

            }

            pfd = mContext.getContentResolver().openFileDescriptor(uri, "w");
            FileOutputStream fos = new FileOutputStream(pfd.getFileDescriptor());
            document.writeTo(fos);
            return true;
        } catch (IOException e) {
            e.printStackTrace();
            return false;
        } finally {
            document.close();
            if (pfd != null) {
                try { pfd.close(); } catch (IOException e) {}
            }
        }
    }

    private String getTimestamp() {
        DateFormat df = new SimpleDateFormat("yyyy-MM-dd-HHmm");
        return df.format(Calendar.getInstance(TimeZone.getDefault()).getTimeInMillis());
    }

    private void pickImage(Activity a) {
        Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        intent.setType("image/*");
        a.startActivityForResult(intent, PICK_IMAGE_REQUEST_CODE);
    }
}
