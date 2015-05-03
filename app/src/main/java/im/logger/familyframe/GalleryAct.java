package im.logger.familyframe;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.drawable.BitmapDrawable;
import android.net.ConnectivityManager;
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

import java.io.InputStream;
import java.net.URL;
import java.util.ArrayList;
import java.util.Map;
import java.util.Timer;
import java.util.TimerTask;

public class GalleryAct extends Activity {

    Firebase ref;
    Photo currentPhoto;
    int currentIndex;
    ArrayList<Photo> photos = new ArrayList<>();
    ImageSwitcher imageSwitcher;
    ProgressBar progressBar;
    View contentView;
    TouchImageView touchImageView;

    @Override
    protected void onResume() {
        super.onResume();
        contentView.setSystemUiVisibility(View.SYSTEM_UI_FLAG_FULLSCREEN | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
                | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        requestWindowFeature(Window.FEATURE_NO_TITLE);

        setContentView(R.layout.act_gallery);

        final View controlsView = findViewById(R.id.fullscreen_content_controls);
        progressBar = (ProgressBar) findViewById(R.id.progressBar);

        progressBar.setVisibility(View.VISIBLE);

        contentView = findViewById(R.id.fullscreen_content);
        touchImageView = (TouchImageView) findViewById(R.id.touchimage);
        touchImageView.setVisibility(View.GONE);
        touchImageView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                touchImageView.setVisibility(View.GONE);
            }
        });

        findViewById(R.id.dummy_button).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
//                pushGallery();
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

        Storage.init(this);

        initGallery();
    }

    class ActHandler extends Handler {
        public final static int PHOTOLOADED = 0x1;
        public final static int TIMEOUT = 0x2;

        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case PHOTOLOADED:
                    Bitmap bitmap = (Bitmap) msg.getData().get("bitmap");
                    imageSwitcher.setImageDrawable(new BitmapDrawable(getResources(), bitmap));
                    progressBar.setVisibility(View.GONE);
                    break;
                case TIMEOUT:
                    Toast.makeText(GalleryAct.this, "好像没有了。。。", Toast.LENGTH_SHORT).show();
                    break;
            }

        }
    }
    Handler handler = new ActHandler();

    void initGallery() {
        ref = new Firebase("https://fam-ily.firebaseio.com/gallery");

        // 判断网络是否可用
        ConnectivityManager cm = (ConnectivityManager) this.getSystemService(CONNECTIVITY_SERVICE);
        if (cm != null) {
            if (cm.getActiveNetworkInfo() == null || !cm.getActiveNetworkInfo().isAvailable()) {
                loadOffline();
                return;
            }
        }

        // 不同的查询要添加不同的的事件监听器，以免混乱呀
        ref.addChildEventListener(new ChildEventListener() {
            @Override
            public void onChildAdded(DataSnapshot dataSnapshot, String s) {

            }

            @Override
            public void onChildChanged(DataSnapshot dataSnapshot, String s) {

            }

            @Override
            public void onChildRemoved(DataSnapshot dataSnapshot) {
                String id = dataSnapshot.getKey();

                for (int i=0, len = photos.size(); i<len; i++) {
                    if (photos.get(i).id.equals(id)) {
                        Storage.removeBitmap(id);
                        photos.remove(i);
                        if (!currentPhoto.id.equals(id))
                            break;
                        len = photos.size();
                        if (currentIndex == len) {
                            prev();
                        } else {
                            currentIndex--; // 删除一个元素后，currentIndex 其实刚刚好，这里要--是因为next()里会++
                            next();
                        }
                        break;
                    }
                }
            }

            @Override
            public void onChildMoved(DataSnapshot dataSnapshot, String s) {

            }

            @Override
            public void onCancelled(FirebaseError firebaseError) {

            }
        });

        // 当连续两次childAdd的时候，就会出现一次childRemove，这里的remove其实是因为查询结果被更新，而并非后台有删除
        Query queryRef = ref.orderByChild("time").limitToLast(1);
        queryRef.addChildEventListener(new ChildEventListener() {
            @Override
            public void onChildAdded(DataSnapshot dataSnapshot, String s) {
                Map<String, Object> map = (Map<String, Object>) dataSnapshot.getValue();
                currentPhoto = new Photo();
                currentPhoto.id = dataSnapshot.getKey();
                currentPhoto.url = (String) map.get("url");
                currentPhoto.time = (Long) map.get("time");
                System.out.println("latest " + currentPhoto);
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

    void loadOffline() {
        Toast.makeText(this, "离线状态。。。", Toast.LENGTH_SHORT).show();
        progressBar.setVisibility(View.GONE);
        String[] ids = Storage.listIdList();
        for (String id:ids) {
            currentPhoto = new Photo();
            currentPhoto.id = id;
            photos.add(currentPhoto);
        }
        loadPhoto();
    }

    public static Bitmap TheBitmap = null;
    void loadPhoto() {
        progressBar.setVisibility(View.VISIBLE);
        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    Bitmap bitmap;
                    if (Storage.exists(currentPhoto.id)) {
                        bitmap = Storage.getBitmap(currentPhoto.id);
                    } else {
                        InputStream inputStream = new URL(currentPhoto.url).openConnection().getInputStream();
                        bitmap = BitmapFactory.decodeStream(inputStream);
                        Storage.setBitmap(currentPhoto.id, bitmap);
                    }
                    TheBitmap = bitmap;
                    Message msg = new Message();
                    msg.what = ActHandler.PHOTOLOADED;
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
            loadPhoto();
        } else {
            // fetch more previous and load photo
            Toast.makeText(this, "正在加载中。。。", Toast.LENGTH_SHORT).show();
//            loadPhoto();
            // 超时定时器
            final Timer timer = new Timer();
            timer.schedule(new TimerTask() {
                @Override
                public void run() {
                    handler.sendEmptyMessage(ActHandler.TIMEOUT);
                }
            }, 3000);

            Query queryRef = ref.orderByChild("time").endAt(currentPhoto.time - 1).limitToLast(1); // 这里是假设了同一秒内没有相同的
            queryRef.addChildEventListener(new ChildEventListener() {
                @Override
                public void onChildAdded(DataSnapshot dataSnapshot, String s) {
                    timer.cancel();

                    Map<String, Object> map = (Map<String, Object>) dataSnapshot.getValue();
                    currentPhoto = new Photo();
                    currentPhoto.id = dataSnapshot.getKey();
                    currentPhoto.url = (String) map.get("url");
                    currentPhoto.time = (Long) map.get("time");
                    System.out.println("prev " + currentPhoto);
                    photos.add(0, currentPhoto);
                    currentIndex = 0;
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
    }

    void removePhoto() {
        ref.child(currentPhoto.id).removeValue();
    }

    class MyGestureDetector extends GestureDetector.SimpleOnGestureListener {
        @Override
        public boolean onSingleTapConfirmed(MotionEvent e) {
            return super.onSingleTapConfirmed(e);
        }

        @Override
        public void onLongPress(MotionEvent e) {
            new AlertDialog.Builder(GalleryAct.this).setCancelable(false)
                    .setPositiveButton("删除", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialogInterface, int i) {
                            removePhoto();
                        }
                    })
                    .setNegativeButton("取消", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialogInterface, int i) {

                        }
                    })
                    .setTitle("删除该照片吗？").show();
        }

        @Override
        public boolean onDown(MotionEvent e) {
            return true;
        }

        @Override
        public boolean onSingleTapUp(MotionEvent e) {
            if (TheBitmap == null)
                return false;
            touchImageView.setImageDrawable(new BitmapDrawable(getResources(), TheBitmap));
            touchImageView.setVisibility(View.VISIBLE);

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
