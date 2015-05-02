package im.logger.familyframe;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;

/**
 * Created by kinka on 5/1/15.
 */
public class Photo {
    public String id = "";
    public String url = "";
    public long time = 0;

    public Photo(String url) {
        this.url = url;
        time = System.currentTimeMillis();
    }

    @Override
    public String toString() {
        return "id=" + id + " url: " + url + " at: " + (new SimpleDateFormat("yyyy/MM/dd HH:mm:ss").format(new Date(time)));
    }

    public Photo() {

    }
}
