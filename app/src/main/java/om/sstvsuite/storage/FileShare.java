package om.sstvsuite.storage;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import androidx.core.content.FileProvider;
import java.io.File;

/* loaded from: /home/Gold/Documents/deepseek-harness/default-workspace/sstv/apk/classes7.dex */
public final class FileShare {
    public static final String AUTHORITY = "om.sstvsuite.fileprovider";

    private FileShare() {
    }

    public static Uri uriFor(Context context, File file) {
        return FileProvider.getUriForFile(context, AUTHORITY, file);
    }

    public static Intent imageShareIntent(Context context, File file) {
        Uri uriFor = uriFor(context, file);
        Intent intent = new Intent("android.intent.action.SEND");
        intent.setType("image/png");
        intent.putExtra("android.intent.extra.STREAM", uriFor);
        intent.addFlags(1);
        return intent;
    }

    public static Intent audioShareIntent(Context context, File file) {
        Uri uriFor = uriFor(context, file);
        Intent intent = new Intent("android.intent.action.SEND");
        intent.setType("audio/wav");
        intent.putExtra("android.intent.extra.STREAM", uriFor);
        intent.addFlags(1);
        return intent;
    }

    public static Uri cameraOutputUri(Context context, File file) {
        return uriFor(context, file);
    }
}
