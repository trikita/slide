package trikita.slide.middleware;

import android.app.Activity;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.pdf.PdfDocument;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.ParcelFileDescriptor;
import android.support.v4.app.NotificationCompat;
import android.support.v4.app.NotificationManagerCompat;

import java.io.FileOutputStream;
import java.io.IOException;
import java.text.DateFormat;
import java.util.Calendar;
import java.util.TimeZone;
import java.util.concurrent.CancellationException;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.TimeoutException;

import trikita.jedux.Action;
import trikita.jedux.Store;
import trikita.slide.ActionType;
import trikita.slide.App;
import trikita.slide.Presentation;
import trikita.slide.R;
import trikita.slide.Slide;
import trikita.slide.State;
import trikita.slide.ui.Style;


public class StorageController implements Store.Middleware<Action<ActionType, ?>, State> {
    public static final int PICK_IMAGE_REQUEST_CODE = 44;
    public static final int EXPORT_PDF_REQUEST_CODE = 46;

    private final Context mContext;

    private static final int PDF_EXPORT_NOTIFICATION_ID = 0;
    private static final String PDF_EXPORT_NOTIFICATION_CHANNEL = "SLIDEPDFEXPORT";

    private final NotificationManagerCompat notificationManager;
    private final NotificationCompat.Builder mBuilder;

    public StorageController(Context c) {
        mContext = c;

        notificationManager = NotificationManagerCompat.from(mContext);
        mBuilder = new NotificationCompat.Builder(mContext, PDF_EXPORT_NOTIFICATION_CHANNEL);
        mBuilder.setContentTitle(c.getString(R.string.om_export_pdf))
            .setSmallIcon(R.mipmap.ic_launcher)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setAutoCancel(true);
    }

    @Override
    public void dispatch(Store<Action<ActionType, ?>, State> store, Action<ActionType, ?> action, Store.NextDispatcher<Action<ActionType, ?>> next) {
        next.dispatch(action);
        if (action.type == ActionType.CREATE_PDF) {
            createPdf((Activity)action.value);
        } else if (action.type == ActionType.EXPORT_PDF) {
            int PROGRESS_MAX = App.getState().getCurrentPresentation().pages().length;
            mBuilder.setProgress(PROGRESS_MAX, 0, false);
            notificationManager.notify(PDF_EXPORT_NOTIFICATION_ID, mBuilder.build());
            new PdfExportTask(store.getState().getCurrentPresentation(), (Uri) action.value, mContext, notificationManager, mBuilder).execute();
        } else if (action.type == ActionType.PICK_IMAGE) {
            pickImage((Activity) action.value);
        } else if (action.type == ActionType.INSERT_IMAGE) {
            Presentation p = store.getState().getCurrentPresentation();
            String s = p.text();
            int c = App.getMainLayout().cursor();
            String chunk = s.substring(0, c);
            int startOfLine = chunk.lastIndexOf("\n");
            if (startOfLine == -1) {
                s = "@"+(action.value).toString()+"\n"+s;
            } else {
                s = s.substring(0, startOfLine+1)+"@"+(action.value).toString()+"\n"+s.substring(startOfLine+1);
            }
            App.dispatch(new Action<>(ActionType.SET_TEXT, s));
        }
    }

    private static class PdfExportTask extends AsyncTask<Void, Integer, Boolean> {

        private final Context context;
        private final Presentation p;
        private final NotificationManagerCompat notificationManager;
        private final NotificationCompat.Builder mBuilder;
        private final Uri uri;

        PdfExportTask(Presentation p, Uri uri, Context ctx, NotificationManagerCompat notificationManager, NotificationCompat.Builder mBuilder) {
            this.context = ctx;
            this.p = p;
            this.notificationManager = notificationManager;
            this.mBuilder = mBuilder;
            this.uri = uri;
            mBuilder.setContentText(ctx.getString(R.string.notifications_exporting_pdf_progress));
        }

        @Override
        protected Boolean doInBackground(Void... params) {
            PdfDocument document = new PdfDocument();
            ParcelFileDescriptor pfd = null;
            try {
                PdfDocument.PageInfo pageInfo = new PdfDocument.PageInfo.Builder(
                        p.getPdfWidth(context), p.getPdfHeight(context), 1
                ).create();
                Bitmap bmp = Bitmap.createBitmap(pageInfo.getContentRect().width(), pageInfo.getContentRect().height(), Bitmap.Config.ARGB_8888);

                for (int i = 1; i <= p.numberOfPages(); ++i) {
                    Canvas c = new Canvas(bmp);

                    boolean doBuild = true;
                    int buildTimeout = 10;
                    int retries = 0;
                    final int maxRetries = 4;

                    final int maxProgress = p.numberOfPages()*(maxRetries+1)+1;

                    while(doBuild) {
                        publishProgress((i-1)*(maxRetries+1)+retries, maxProgress);

                        Future<Slide> future = App.getBuildController().build(
                            p, i, p.getPdfWidth(context), buildTimeout,
                            null, null
                        );
                        try {
                            Slide slide = future.get();
                            slide.render(c, Style.SLIDE_FONT, true);
                            doBuild = false;
                        } catch (ExecutionException e) {
                            if (e.getCause() instanceof CancellationException
                                    && e.getCause().getCause() instanceof TimeoutException
                                    && retries < maxRetries) {
                                buildTimeout += 20;
                                ++retries;
                            }
                            else
                                throw e;
                        }
                    }

                    PdfDocument.Page page = document.startPage(pageInfo);
                    page.getCanvas().drawBitmap(bmp, c.getClipBounds(), pageInfo.getContentRect(), null);
                    document.finishPage(page);

                    publishProgress(i*(maxRetries+1), maxProgress);
                }

                pfd = context.getContentResolver().openFileDescriptor(uri, "w");
                if (pfd != null) {
                    FileOutputStream fos = new FileOutputStream(pfd.getFileDescriptor());
                    document.writeTo(fos);
                    publishProgress(1, 1);
                    return true;
                } else {
                    return false;
                }
            } catch (Exception e) {
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
        protected void onProgressUpdate(Integer... progress) {
            mBuilder.setProgress(progress[1], progress[0], false);
            notificationManager.notify(PDF_EXPORT_NOTIFICATION_ID, mBuilder.build());
        }

        @Override
        protected void onPostExecute(Boolean ok) {
            super.onPostExecute(ok);
            if (!ok)
                mBuilder.setContentText(context.getString(R.string.failed_export_pdf));
            else {
                Intent target = new Intent(Intent.ACTION_VIEW);
                target.setFlags(Intent.FLAG_ACTIVITY_NO_HISTORY | Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                target.setData(uri);
                target.setFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
                mBuilder
                    .setContentText(context.getString(R.string.notifications_exporting_pdf_complete))
                    .setContentIntent(PendingIntent.getActivity(this.context, 0, target, PendingIntent.FLAG_UPDATE_CURRENT));
            }
            mBuilder.setProgress(0,0,false);
            notificationManager.notify(PDF_EXPORT_NOTIFICATION_ID, mBuilder.build());
        }
    }

    private String getTimestamp() {
        DateFormat df = DateFormat.getDateTimeInstance();
        return df.format(Calendar.getInstance(TimeZone.getDefault()).getTimeInMillis());
    }

    private void createPdf(Activity a) {
        Intent intent = new Intent(Intent.ACTION_CREATE_DOCUMENT);
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        intent.setType("application/pdf");
        intent.putExtra(Intent.EXTRA_TITLE, a.getApplicationContext().getString(R.string.pdf_file_name_template, getTimestamp()));
        a.startActivityForResult(intent, EXPORT_PDF_REQUEST_CODE);
    }

    private void pickImage(Activity a) {
        Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        intent.setType("image/*");
        a.startActivityForResult(intent, PICK_IMAGE_REQUEST_CODE);
    }
}
