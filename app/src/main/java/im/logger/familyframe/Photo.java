package im.logger.familyframe;

import java.util.Calendar;
import java.util.Date;

/**
 * Created by kinka on 5/1/15.
 */
public class Photo {
    public String url = "";
    public String time = "";

    public Photo(String url) {
        this.url = url;
        time = Calendar.getInstance().getTime().toString();
    }

    @Override
    public String toString() {
        return "url: " + url + " at: " + time;
    }

    public Photo() {

    }
}
