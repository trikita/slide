package trikita.slide.middleware;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.graphics.pdf.PdfDocument;
import android.net.Uri;
import android.os.AsyncTask;
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

public class Exporter implements Store.Middleware<Action<ActionType, ?>, State> {
    public static final int WRITE_REQUEST_CODE = 43;

    private final Context mContext;

    public Exporter(Context c) {
        mContext = c;
    }

    @Override
    public void dispatch(Store<Action<ActionType, ?>, State> store, Action<ActionType, ?> action, Store.NextDispatcher<Action<ActionType, ?>> next) {
        if (action.type == ActionType.CREATE_PDF) {
            createPdf((Activity) action.value);
            return;
        } else if (action.type == ActionType.EXPORT_PDF) {
            new PdfExportTask(store, (Uri) action.value).execute();
            return;
        }
        next.dispatch(action);
    }

    private void createPdf(Activity a) {
        Intent intent = new Intent(Intent.ACTION_CREATE_DOCUMENT);
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        intent.setType("application/pdf");
        intent.putExtra(Intent.EXTRA_TITLE, a.getString(R.string.pdf_name_prefix)+getTimestamp());
        a.startActivityForResult(intent, WRITE_REQUEST_CODE);
    }

    private class PdfExportTask extends AsyncTask<Void, Void, Boolean> {

        private final Store<Action<ActionType, ?>, State> store;
        private final Uri uri;

        public PdfExportTask(Store<Action<ActionType, ?>, State> store, Uri uri) {
            this.store = store;
            this.uri = uri;
        }

        @Override
        protected Boolean doInBackground(Void... params) {
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
                            Style.COLOR_SCHEMES[App.getState().colorScheme()][1],
                            true);
                    document.finishPage(page);
                }

                pfd = mContext.getContentResolver().openFileDescriptor(uri, "w");
                if (pfd != null) {
                    FileOutputStream fos = new FileOutputStream(pfd.getFileDescriptor());
                    document.writeTo(fos);
                    return true;
                } else {
                    return false;
                }
            } catch (IOException e) {
                e.printStackTrace();
                return false;
            } finally {
                document.close();
                if (pfd != null) {
                    try { pfd.close(); } catch (IOException ignored) {}
                }
            }
        }

        @Override
        protected void onPostExecute(Boolean ok) {
            super.onPostExecute(ok);
            if (!ok) {
                Toast.makeText(mContext, mContext.getString(R.string.failed_export_pdf), Toast.LENGTH_LONG).show();
            }
        }
    }


    private String getTimestamp() {
        DateFormat df = new SimpleDateFormat("yyyy-MM-dd-HHmm");
        return df.format(Calendar.getInstance(TimeZone.getDefault()).getTimeInMillis());
    }
}
