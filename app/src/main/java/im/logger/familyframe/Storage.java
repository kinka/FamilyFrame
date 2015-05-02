package im.logger.familyframe;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Environment;

import java.io.File;
import java.io.FileOutputStream;

/**
 * Created by kinka on 5/2/15.
 */
public class Storage {
    private static Context ctx;
    private static String rootPath = "";

    public static void init(Context ctx) {
        Storage.ctx = ctx;
        rootPath = Environment.getExternalStorageDirectory().getAbsolutePath() + "/" + ctx.getPackageName() + "/";
        // create directory
        File dir = new File(rootPath);
        if (!dir.exists())
            dir.mkdir();
        System.out.println("rootPath = " + rootPath);
    }

    public static boolean exists(String id) {
        return new File(rootPath + id).exists();
    }
    public static void setBitmap(String id, Bitmap bitmap) {
        FileOutputStream out = null;
        try {
            System.out.println("id file " + rootPath + id);
            File file = new File(rootPath + id);
            if (!file.exists())
                file.createNewFile();
            out = new FileOutputStream(file);
            bitmap.compress(Bitmap.CompressFormat.JPEG, 100, out);
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            try {
                if (out != null)
                    out.close();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    public static Bitmap getBitmap(String id) {
        Bitmap bitmap = null;
        try {
            bitmap = BitmapFactory.decodeFile(rootPath + id);
        } catch (Exception e) {
            e.printStackTrace();
        }
        System.out.println("get bitmap " + (bitmap != null));
        return bitmap;
    }
}
