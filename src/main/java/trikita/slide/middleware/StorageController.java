package trikita.slide.middleware;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.graphics.pdf.PdfDocument;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.ParcelFileDescriptor;
import android.widget.Toast;

import java.io.BufferedReader;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.TimeZone;

import trikita.jedux.Action;
import trikita.jedux.Store;
import trikita.slide.ActionType;
import trikita.slide.App;
import trikita.slide.R;
import trikita.slide.Slide;
import trikita.slide.State;
import trikita.slide.ui.Style;

public class StorageController implements Store.Middleware<Action<ActionType, ?>, State> {
    public static final int OPEN_DOCUMENT_REQUEST_CODE = 43;
    public static final int PICK_IMAGE_REQUEST_CODE = 44;
    public static final int EXPORT_PDF_REQUEST_CODE = 46;

    private static final long FILE_WRITER_DELAY = 3000; // 3sec

    private Context mContext = null;
    private final Handler mHandler;

    private final Runnable mDocUpdater = () -> {
        if (App.getState().uri() != null) {
            try {
                int flags = (Intent.FLAG_GRANT_READ_URI_PERMISSION | Intent.FLAG_GRANT_WRITE_URI_PERMISSION);
                mContext.getContentResolver().takePersistableUriPermission(Uri.parse(App.getState().uri()), flags);
                saveDocument();
            } catch (SecurityException e) {
                e.printStackTrace();
                Toast.makeText(mContext, mContext.getString(R.string.failed_save_doc), Toast.LENGTH_LONG).show();
            }
        }
    };

    public StorageController(Context c) {
        mContext = c;
        HandlerThread ht = new HandlerThread("document_backup");
        ht.start();
        mHandler = new Handler(ht.getLooper());
    }

    @Override
    public void dispatch(Store<Action<ActionType, ?>, State> store, Action<ActionType, ?> action, Store.NextDispatcher<Action<ActionType, ?>> next) {
        if (action.type == ActionType.SET_TEXT) {
            dumpToFile(true);
        } else if (action.type == ActionType.CREATE_PDF) {
            dumpToFile(false);
            createPdf((Activity) action.value);
            return;
        } else if (action.type == ActionType.EXPORT_PDF) {
            new PdfExportTask(store, (Uri) action.value).execute();
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
                s = "@"+(action.value).toString()+"\n"+s;
            } else {
                s = s.substring(0, startOfLine+1)+"@"+(action.value).toString()+"\n"+s.substring(startOfLine+1);
            }
            App.dispatch(new Action<>(ActionType.SET_TEXT, s));
            App.dispatch(new Action<>(ActionType.SET_CURSOR, startOfLine));
            return;
        } else if (action.type == ActionType.OPEN_DOCUMENT) {
            dumpToFile(false);
            openDocument((Activity) action.value);
            return;
        } else if (action.type == ActionType.LOAD_DOCUMENT) {
            String doc = loadDocument((Uri) action.value);
            if (doc != null) {
                App.dispatch(new Action<>(ActionType.SET_TEXT, doc));
                App.dispatch(new Action<>(ActionType.SET_CURSOR, 0));
            } else {
                Toast.makeText(mContext, mContext.getString(R.string.failed_open_doc), Toast.LENGTH_LONG).show();
            }
        }
        next.dispatch(action);
    }

    public void dumpToFile(boolean withDelay) {
        mHandler.removeCallbacksAndMessages(null);
        if (withDelay) {
            mHandler.postDelayed(mDocUpdater, FILE_WRITER_DELAY);
        } else {
            mHandler.post(mDocUpdater);
        }
    }

    private void saveDocument() {
        ParcelFileDescriptor pfd = null;
        try {
            Uri uri = Uri.parse(App.getState().uri());
            pfd = mContext.getContentResolver().openFileDescriptor(uri, "w");
            FileOutputStream fos = null;
            if (pfd != null) {
                fos = new FileOutputStream(pfd.getFileDescriptor());
                fos.getChannel().truncate(0);
                fos.write((App.getState().text() + "\n").getBytes());
                fos.close();
            }
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            if (pfd != null) {
                try { pfd.close(); } catch (IOException e) {}
            }
        }
    }

    private void openDocument(Activity a) {
        Intent intent = new Intent(Intent.ACTION_CREATE_DOCUMENT);
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        intent.setType("text/plain");
        intent.putExtra(Intent.EXTRA_TITLE, a.getString(R.string.default_new_doc_name));
        a.startActivityForResult(intent, OPEN_DOCUMENT_REQUEST_CODE);
    }

    private String loadDocument(Uri uri) {
        InputStream is = null;
        try {
            is = mContext.getContentResolver().openInputStream(uri);
            BufferedReader reader = new BufferedReader(new InputStreamReader(is));
            StringBuilder builder = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                builder.append(line);
                builder.append("\n");
            }
            return builder.toString();
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        } finally {
            if (is != null) {
                try { is.close(); } catch (IOException e) {}
            }
        }
    }

    private void createPdf(Activity a) {
        Intent intent = new Intent(Intent.ACTION_CREATE_DOCUMENT);
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        intent.setType("application/pdf");
        intent.putExtra(Intent.EXTRA_TITLE, a.getString(R.string.pdf_name_prefix)+getTimestamp());
        a.startActivityForResult(intent, EXPORT_PDF_REQUEST_CODE);
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

    private void pickImage(Activity a) {
        Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        intent.setType("image/*");
        a.startActivityForResult(intent, PICK_IMAGE_REQUEST_CODE);
    }
}
