package im.logger.familyframe;

import android.annotation.TargetApi;
import android.app.Activity;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.view.animation.Interpolator;
import android.widget.ImageSwitcher;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.Toast;
import android.widget.ViewSwitcher;

import com.firebase.client.ChildEventListener;
import com.firebase.client.DataSnapshot;
import com.firebase.client.Firebase;
import com.firebase.client.FirebaseError;
import com.firebase.client.Query;

import java.net.URL;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class GalleryAct extends Activity {

    Firebase ref;
    Photo currentPhoto;
    int currentIndex;
    ArrayList<Photo> photos = new ArrayList<>();
    ImageSwitcher imageSwitcher;
    ProgressBar progressBar;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        requestWindowFeature(Window.FEATURE_NO_TITLE);

        setContentView(R.layout.act_gallery);

        final View controlsView = findViewById(R.id.fullscreen_content_controls);
        progressBar = (ProgressBar) findViewById(R.id.progressBar);

        progressBar.setVisibility(View.VISIBLE);

        final View contentView = findViewById(R.id.fullscreen_content);

        contentView.setSystemUiVisibility(View.SYSTEM_UI_FLAG_FULLSCREEN | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
                | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION);

        findViewById(R.id.dummy_button).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                pushGallery();
            }
        });

        Firebase.setAndroidContext(this);

        // http://www.tutorialspoint.com/android/android_imageswitcher.htm
        imageSwitcher = (ImageSwitcher) findViewById(R.id.imageSwitcher);
        imageSwitcher.setFactory(new ViewSwitcher.ViewFactory() {
            @Override
            public View makeView() {
                ImageView view = new ImageView(getApplicationContext());
                view.setScaleType(ImageView.ScaleType.FIT_CENTER);
                view.setLayoutParams(new ImageSwitcher.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
                return view;
            }
        });

        final GestureDetector gestureDetector = new GestureDetector(this, new MyGestureDetector());
        View.OnTouchListener gestureListener = new View.OnTouchListener() {
            @Override
            public boolean onTouch(View view, MotionEvent motionEvent) {
                if (gestureDetector.onTouchEvent(motionEvent))
                    return true;
                return false;
            }
        };
        imageSwitcher.setOnTouchListener(gestureListener);

        initGallery();
    }

    class ActHandler extends Handler {
        @Override
        public void handleMessage(Message msg) {
            Bitmap bitmap = (Bitmap) msg.getData().get("bitmap");
            imageSwitcher.setImageDrawable(new BitmapDrawable(getResources(), bitmap));
            progressBar.setVisibility(View.GONE);
        }
    }
    Handler handler = new ActHandler();

    void initGallery() {
        ref = new Firebase("https://fam-ily.firebaseio.com/gallery");

        Query queryRef = ref.orderByChild("time").limitToLast(1);
        queryRef.addChildEventListener(new ChildEventListener() {
            @Override
            public void onChildAdded(DataSnapshot dataSnapshot, String s) {
                Map<String, String> map = (Map<String, String>) dataSnapshot.getValue();
                currentPhoto = new Photo();
                currentPhoto.url = map.get("url");
                currentPhoto.time = map.get("time");
                System.out.println(currentPhoto);
                photos.add(currentPhoto);
                currentIndex = photos.size() - 1;

                loadPhoto();
            }

            @Override
            public void onChildChanged(DataSnapshot dataSnapshot, String s) {

            }

            @Override
            public void onChildRemoved(DataSnapshot dataSnapshot) {

            }

            @Override
            public void onChildMoved(DataSnapshot dataSnapshot, String s) {

            }

            @Override
            public void onCancelled(FirebaseError firebaseError) {

            }
        });
    }

    void loadPhoto() {
        // todo 双击放大
        progressBar.setVisibility(View.VISIBLE);
        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    // todo 缓存到本地
                    Bitmap bitmap = BitmapFactory.decodeStream(new URL(currentPhoto.url).openConnection().getInputStream());
                    Message msg = new Message();
                    msg.what = 0x1234;
                    Bundle data = new Bundle();
                    data.putParcelable("bitmap", bitmap);
                    msg.setData(data);
                    handler.sendMessage(msg);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }).start();
    }

    void pushGallery() {
        ref.push().setValue(new Photo("http://img5.douban.com/view/photo/photo/public/p2239830248.jpg"));
    }

    void next() {
        Animation in = AnimationUtils.loadAnimation(this, android.R.anim.slide_out_right);
        in.setInterpolator(new ReverseInterpolator()); // out_left
        Animation out = AnimationUtils.loadAnimation(this, android.R.anim.slide_in_left);
        out.setInterpolator(new ReverseInterpolator()); // in_right
        imageSwitcher.setInAnimation(in);
        imageSwitcher.setOutAnimation(out);

        if (currentIndex < photos.size() - 1) {
            currentIndex++;
            currentPhoto = photos.get(currentIndex);
            // load photo
            loadPhoto();
        } else {
            Toast.makeText(this, "没有更多了。。。", Toast.LENGTH_SHORT).show();
        }
    }

    class ReverseInterpolator implements Interpolator {
        @Override
        public float getInterpolation(float v) {
            return Math.abs(v - 1f);
        }
    }
    void prev() {
        imageSwitcher.setInAnimation(AnimationUtils.loadAnimation(this, android.R.anim.slide_in_left));
        imageSwitcher.setOutAnimation(AnimationUtils.loadAnimation(this, android.R.anim.slide_out_right));
        if (currentIndex > 0) {
            currentIndex--;
            currentPhoto = photos.get(currentIndex);
            // load photo
            loadPhoto();
        } else {
            // fetch more previous and load photo
            Toast.makeText(this, "正在加载中。。。", Toast.LENGTH_SHORT).show();
            loadPhoto();
        }
    }

    class MyGestureDetector extends GestureDetector.SimpleOnGestureListener {
        @Override
        public boolean onSingleTapConfirmed(MotionEvent e) {
            return super.onSingleTapConfirmed(e);
        }

        @Override
        public boolean onDown(MotionEvent e) {
            return true;
        }

        @Override
        public boolean onFling(MotionEvent e1, MotionEvent e2, float velocityX, float velocityY) {
            final int SWIPE_MIN_DIST = 100;
            final int SWIPE_MAX_OFF_PATH = 300;
            final int SWIPE_THRESHOLD_VECOCITY = 100;

            float deltaY = Math.abs(e1.getY() - e2.getY());
            float deltaX = e1.getX() - e2.getX();
//            System.out.println("dY " + deltaY + " dX " + deltaX);

            if (deltaY > SWIPE_MAX_OFF_PATH)
                return false;

            if (Math.abs(velocityX) > SWIPE_THRESHOLD_VECOCITY) {
                // right to left swipe
                if (e1.getX() - e2.getX() > SWIPE_MIN_DIST) {
                    next();
                } else if (e2.getX() - e1.getX() > SWIPE_MIN_DIST) {
                    prev();
                }
            }

            return false;
        }
    }
}
