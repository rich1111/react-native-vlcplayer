package com.yuanzhou.vlc.vlcplayer;

import android.annotation.SuppressLint;
import android.content.Context;
import android.graphics.SurfaceTexture;
import android.media.AudioManager;
import android.net.Uri;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.Surface;
import android.view.TextureView;
import android.view.View;

import com.facebook.react.bridge.LifecycleEventListener;
import com.facebook.react.uimanager.ThemedReactContext;

import org.videolan.libvlc.IVLCVout;
import org.videolan.libvlc.LibVLC;
import org.videolan.libvlc.Media;
import org.videolan.libvlc.MediaPlayer;

import java.util.ArrayList;
//import org.videolan.vlc.util.VLCInstance;

//import org.videolan.libvlc.util.VLCUtil;
//import org.videolan.vlc.VlcVideoView;

@SuppressLint("ViewConstructor")
class ReactVlcPlayerView extends TextureView implements
        LifecycleEventListener,
        TextureView.SurfaceTextureListener,
        AudioManager.OnAudioFocusChangeListener{

    private static final String TAG = "ReactVlcPlayerView";


    private final VideoEventEmitter eventEmitter;
    private LibVLC libvlc;
    private MediaPlayer mMediaPlayer = null;
    private TextureView surfaceView;
    private Surface surfaceVideo;//视频画布
    private boolean isSurfaceViewDestory;
    //资源路径
    private String src;
    //是否网络资源
    private  boolean netStrTag;

   /* private static final int SURFACE_BEST_FIT = 0;
    private static final int SURFACE_FIT_HORIZONTAL = 1;
    private static final int SURFACE_FIT_VERTICAL = 2;
    private static final int SURFACE_FILL = 3;
    private static final int SURFACE_16_9 = 4;
    private static final int SURFACE_4_3 = 5;
    private static final int SURFACE_ORIGINAL = 6;
    private int mCurrentSize = SURFACE_BEST_FIT;*/
    // Props from React
    /* private Uri srcUri;
    private String extension;
    private boolean repeat;*/
    private boolean disableFocus;
    private boolean playInBackground = false;
    // \ End props

    private int mVideoHeight = 0;
    private int mVideoWidth = 0;
    private int mVideoVisibleHeight = 0;
    private int mVideoVisibleWidth = 0;
    private int mSarNum = 0;
    private int mSarDen = 0;
    private int screenWidth = 0;
    private int screenHeight = 0;
    private boolean isPaused = true;
    private boolean isHostPaused = false;
    private int preVolume = 150;
    private boolean preMuted = false;
    private boolean autoAspectRatio = false;

    // React
    private final ThemedReactContext themedReactContext;
    private final AudioManager audioManager;




    public ReactVlcPlayerView(ThemedReactContext context) {
        super(context);
        this.eventEmitter = new VideoEventEmitter(context);
        this.themedReactContext = context;
        audioManager = (AudioManager) context.getSystemService(Context.AUDIO_SERVICE);
        themedReactContext.addLifecycleEventListener(this);
        DisplayMetrics dm = getResources().getDisplayMetrics();
        screenHeight = dm.heightPixels;
        screenWidth = dm.widthPixels;
        this.setSurfaceTextureListener(this);
        surfaceView = this;
        //surfaceView.setZOrderOnTop(false);
        //surfaceView.setZOrderMediaOverlay(false);
       // this.setZOrderOnTop(true);
       // this.getHolder().setFormat(PixelFormat.TRANSLUCENT);
        //this.setEGLConfigChooser(8, 8, 8, 8, 16, 0);
       //this.setZOrderMediaOverlay(true);
        //
        //不过中间那句是OpenGl的，视情况使用，无用可注释掉了，也能实现了透明，但是GLSurfaceView就必须使用

       // this.setZOrderMediaOverlay(false);
        this.addOnLayoutChangeListener(onLayoutChangeListener);
    }


    @Override
    public void setId(int id) {
        super.setId(id);
        eventEmitter.setViewId(id);
    }

    @Override
    protected void onAttachedToWindow() {
        super.onAttachedToWindow();
        //createPlayer();
    }

    @Override
    protected void onDetachedFromWindow() {
        super.onDetachedFromWindow();
        stopPlayback();
    }

    // LifecycleEventListener implementation

    @Override
    public void onHostResume() {
        Log.i("onHostResume","---------onHostResume------------>");
        if(mMediaPlayer != null && isSurfaceViewDestory && isHostPaused){
            IVLCVout vlcOut =  mMediaPlayer.getVLCVout();
            if(!vlcOut.areViewsAttached()){
               // vlcOut.setVideoSurface(this.getHolder().getSurface(), this.getHolder());
                vlcOut.attachViews(onNewVideoLayoutListener);
                isSurfaceViewDestory = false;
                isPaused = false;
               // this.getHolder().setKeepScreenOn(true);
                mMediaPlayer.play();
            }
        }
    }


    @Override
    public void onHostPause() {
        if(!isPaused && mMediaPlayer != null){
            isPaused = true;
            isHostPaused = true;
            mMediaPlayer.pause();
            // this.getHolder().setKeepScreenOn(false);
            eventEmitter.paused(true);
        }
        Log.i("onHostPause","---------onHostPause------------>");
    }



    @Override
    public void onHostDestroy() {
        stopPlayback();
    }

    public void cleanUpResources() {
        if(surfaceView != null){
            surfaceView.removeOnLayoutChangeListener(onLayoutChangeListener);
        }
        stopPlayback();
    }

    private void releasePlayer() {
        if (libvlc == null)
            return;
        mMediaPlayer.stop();
        final IVLCVout vout = mMediaPlayer.getVLCVout();
        vout.removeCallback(callback);
        vout.detachViews();
        //surfaceView.removeOnLayoutChangeListener(onLayoutChangeListener);
        libvlc.release();
        libvlc = null;
        //mVideoWidth = 0;
        //mVideoHeight = 0;
    }

    private void stopPlayback() {
        onStopPlayback();
        releasePlayer();
    }

    private void onStopPlayback() {
        setKeepScreenOn(false);
        audioManager.abandonAudioFocus(this);
    }

    // AudioManager.OnAudioFocusChangeListener implementation

    @Override
    public void onAudioFocusChange(int focusChange) {
    }

    public void setPlayInBackground(boolean playInBackground) {
        this.playInBackground = playInBackground;
    }

    public void setDisableFocus(boolean disableFocus) {
        this.disableFocus = disableFocus;
    }

    private void createPlayer(boolean autoplay) {
        releasePlayer();
        if(this.getSurfaceTexture() == null){
            Log.i(TAG,"getSurfaceTexture() is null");
            return;
        }
        try {
            // Create LibVLC
            ArrayList<String> options = new ArrayList<String>(50);
            // [bavv add start]
            options.add("--rtsp-tcp");
            options.add("-vv");
            // [bavv add end]

            /*
            options.add("--rtsp-tcp");
            options.add("--no-stats");
            options.add("--network-caching=300");
            options.add("--clock-jitter=110");
            options.add("--clock-synchro=1");
            options.add("0");
             */
            libvlc = new LibVLC(getContext(), options);
            //libvlc =  VLCInstance.get(getContext());

            // Create media player
            mMediaPlayer = new MediaPlayer(libvlc);
            mMediaPlayer.setEventListener(mPlayerListener);
            //surfaceView = this;
            //surfaceView.addOnLayoutChangeListener(onLayoutChangeListener);
            //this.getHolder().setKeepScreenOn(true);
            IVLCVout vlcOut =  mMediaPlayer.getVLCVout();
            if(mVideoWidth > 0 && mVideoHeight > 0){
                vlcOut.setWindowSize(mVideoWidth,mVideoHeight);
                if(autoAspectRatio){
                    mMediaPlayer.setAspectRatio(mVideoWidth + ":" + mVideoHeight);
                }
                //mMediaPlayer.setAspectRatio(mVideoWidth+":"+mVideoHeight);
            }
		/*
            if (!vlcOut.areViewsAttached()) {
                vlcOut.addCallback(callback);
                vlcOut.setVideoView(surfaceView);
                //vlcOut.setVideoSurface(this.getHolder().getSurface(), this.getHolder());
                vlcOut.attachViews(onNewVideoLayoutListener);
            }
		*/
            DisplayMetrics dm = getResources().getDisplayMetrics();
            Media m = null;
            if(netStrTag){
                Uri uri = Uri.parse(this.src);
                m = new Media(libvlc, uri);
            }else{
                m = new Media(libvlc, this.src);
            }
            m.setEventListener(mMediaListener);

            m.addOption(":rtsp-tcp");
            m.addOption(":no-stats");
            m.addOption(":quiet-synchro");
            //m.addOption(":network-caching=300");
            //m.addOption(":clock-jitter=110");
            //m.addOption(":clock-synchro=0");
            m.setHWDecoderEnabled(true, true);

            mMediaPlayer.setMedia(m);
            mMediaPlayer.setScale(0);

            if (!vlcOut.areViewsAttached()) {
                vlcOut.addCallback(callback);
               // vlcOut.setVideoSurface(this.getSurfaceTexture());
                //vlcOut.setVideoSurface(this.getHolder().getSurface(), this.getHolder());
                //vlcOut.attachViews(onNewVideoLayoutListener);
                vlcOut.setVideoSurface(this.getSurfaceTexture());
                vlcOut.attachViews(onNewVideoLayoutListener);
               // vlcOut.attachSurfaceSlave(surfaceVideo,null,onNewVideoLayoutListener);
                //vlcOut.setVideoView(this);
                //vlcOut.attachViews(onNewVideoLayoutListener);
            }
            if(autoplay){
                isPaused = false;
                mMediaPlayer.play();
            }
            this.setMutedModifier(this.preMuted);
            eventEmitter.loadStart();
        } catch (Exception e) {
           e.printStackTrace();
            //Toast.makeText(getContext(), "Error creating player!", Toast.LENGTH_LONG).show();
        }
    }


    /*************
     * Events  Listener
     *************/

    private View.OnLayoutChangeListener onLayoutChangeListener = new View.OnLayoutChangeListener(){

        @Override
        public void onLayoutChange(View view, int i, int i1, int i2, int i3, int i4, int i5, int i6, int i7) {
            if(view.getWidth() > 0 && view.getHeight() > 0 ){
                mVideoWidth = view.getWidth(); // 获取宽度
                mVideoHeight = view.getHeight(); // 获取高度
                if(mMediaPlayer != null) {
                    IVLCVout vlcOut = mMediaPlayer.getVLCVout();
                    vlcOut.setWindowSize(mVideoWidth, mVideoHeight);
                    if(autoAspectRatio){
                        mMediaPlayer.setAspectRatio(mVideoWidth + ":" + mVideoHeight);
                    }
                }
            }
        }
    };

    /**
     * 播放过程中的时间事件监听
     */
    private MediaPlayer.EventListener mPlayerListener = new MediaPlayer.EventListener(){
        long currentTime = 0;
        long totalLength = 0;
        @Override
        public void onEvent(MediaPlayer.Event event) {
            switch (event.type) {
                case MediaPlayer.Event.EndReached:
                    eventEmitter.end();
                    break;
                case MediaPlayer.Event.Playing:
                    eventEmitter.playing();
                    Log.i("Event.playing","Event.playing");
                    break;
                case MediaPlayer.Event.Opening:
                    Log.i("Event.Opening","Event.Opening");
                    eventEmitter.onOpen();
                    break;
                case MediaPlayer.Event.Paused:
                    eventEmitter.paused(true);
                    Log.i("Event.Paused","Event.Paused");
                    break;
                case MediaPlayer.Event.Buffering:
                    if(event.getBuffering()  >= 100){
                        eventEmitter.buffering(false, event.getBuffering());
                    }else{
                        eventEmitter.buffering(true,event.getBuffering());
                    }
                    break;
                case MediaPlayer.Event.Stopped:
                    eventEmitter.stopped();
                    break;
                case MediaPlayer.Event.EncounteredError:
                    break;
                case MediaPlayer.Event.TimeChanged:
                    //event.
                    currentTime = mMediaPlayer.getTime();
                    totalLength = mMediaPlayer.getLength();
                    eventEmitter.progressChanged(currentTime, totalLength);
                    break;
                default:
                    break;
            }
        }
    };


    private IVLCVout.OnNewVideoLayoutListener onNewVideoLayoutListener = new IVLCVout.OnNewVideoLayoutListener(){
        @Override
        public void onNewVideoLayout(IVLCVout vout, int width, int height, int visibleWidth, int visibleHeight, int sarNum, int sarDen) {
            if (width * height == 0)
                return;

            // store video size
            mVideoWidth = width;
            mVideoHeight = height;
            mVideoVisibleWidth  = visibleWidth;
            mVideoVisibleHeight = visibleHeight;
            mSarNum = sarNum;
            mSarDen = sarDen;
            Log.i("onNewVideoLayout","{" +
                    "mVideoWidth:"+mVideoWidth+",mVideoHeight:"+mVideoHeight +
                    "mVideoVisibleWidth:"+mVideoVisibleWidth+",mVideoVisibleHeight:"+mVideoVisibleHeight);
            eventEmitter.load(mMediaPlayer.getLength(),mMediaPlayer.getTime(),mVideoVisibleWidth,mVideoVisibleHeight);
        }
    };


    IVLCVout.Callback callback = new IVLCVout.Callback() {
        @Override
        public void onSurfacesCreated(IVLCVout ivlcVout) {
            isSurfaceViewDestory = false;
        }

        @Override
        public void onSurfacesDestroyed(IVLCVout ivlcVout) {
            //IVLCVout vlcOut =  mMediaPlayer.getVLCVout();
            //vlcOut.detachViews();
            isSurfaceViewDestory = true;
        }

    };

    /**
     *  视频进度调整
     * @param time
     */
    public void seekTo(long time) {
        if(mMediaPlayer != null){
            mMediaPlayer.setTime(time);
            mMediaPlayer.isSeekable();
        }
    }

    /**
     * 设置资源路径
     * @param uri
     * @param isNetStr
     */
    public void setSrc(String uri, boolean isNetStr) {
        this.src = uri;
        this.netStrTag = isNetStr;
        createPlayer(true);
    }

    /**
     * 改变播放速率
     * @param rateModifier
     */
    public void setRateModifier(float rateModifier) {
        if(mMediaPlayer != null){
            mMediaPlayer.setRate(rateModifier);
        }
    }


    /**
     * 改变声音大小
     * @param volumeModifier
     */
    public void setVolumeModifier(int volumeModifier) {
        this.preVolume = volumeModifier;
        if(mMediaPlayer != null){
            mMediaPlayer.setVolume(volumeModifier);
        }
    }

    /**
     * 改变静音状态
     * @param muted
     */
    public void setMutedModifier(boolean muted) {
        this.preMuted = muted;
        if(mMediaPlayer != null){
            if(muted){
                mMediaPlayer.setVolume(0);
            }else{
                mMediaPlayer.setVolume(this.preVolume);
            }
        }
        Log.i(TAG, "Muted " + muted + " Volume " + this.preVolume);
    }

    /**
     * 改变播放状态
     * @param paused
     */
    public void setPausedModifier(boolean paused){
        if(mMediaPlayer != null){
            if(paused){
                isPaused = true;
                mMediaPlayer.pause();
            }else{
                isPaused = false;
                mMediaPlayer.play();
            }
        }
    }

    /**
     * 重新加载视频
     * @param autoplay
     */
    public void doResume(boolean autoplay){
        createPlayer(autoplay);
    }

    public void setRepeatModifier(boolean repeat){
    }


    /**
     * 改变宽高比
     * @param aspectRatio
     */
    public void setAspectRatio(String aspectRatio){
        if(!autoAspectRatio && mMediaPlayer != null){
            mMediaPlayer.setAspectRatio(aspectRatio);
        }
    }


    @Override
    public void onSurfaceTextureAvailable(SurfaceTexture surface, int width, int height) {
        Log.i(TAG,"onSurfaceTextureAvailable");
        mVideoWidth = width;
        mVideoHeight = height;
        surfaceVideo = new Surface(surface);
        createPlayer(true);
    }

    @Override
    public void onSurfaceTextureSizeChanged(SurfaceTexture surface, int width, int height) {

    }

    @Override
    public boolean onSurfaceTextureDestroyed(SurfaceTexture surface) {
        return true;
    }

    @Override
    public void onSurfaceTextureUpdated(SurfaceTexture surface) {
        //Log.i(TAG,"onSurfaceTextureUpdated");
    }

    private final Media.EventListener mMediaListener = new Media.EventListener() {
        @Override
        public void onEvent(Media.Event event) {
            switch (event.type) {
                case Media.Event.MetaChanged:
                    Log.i(TAG, "Media.Event.MetaChanged:  =" + event.getMetaId());
                    break;
                case Media.Event.ParsedChanged:
                    Log.i(TAG, "Media.Event.ParsedChanged  =" + event.getMetaId());
                    break;
                case Media.Event.StateChanged:
                    Log.i(TAG, "StateChanged   =" + event.getMetaId());
                    break;
                default:
                    Log.i(TAG, "Media.Event.type=" + event.type + "   eventgetParsedStatus=" + event.getParsedStatus());
                    break;

            }
        }
    };

    /*private void changeSurfaceSize(boolean message) {

        if (mMediaPlayer != null) {
            final IVLCVout vlcVout = mMediaPlayer.getVLCVout();
            vlcVout.setWindowSize(screenWidth, screenHeight);
        }

        double displayWidth = screenWidth, displayHeight = screenHeight;

        if (screenWidth < screenHeight) {
            displayWidth = screenHeight;
            displayHeight = screenWidth;
        }

        // sanity check
        if (displayWidth * displayHeight <= 1 || mVideoWidth * mVideoHeight <= 1) {
            return;
        }

        // compute the aspect ratio
        double aspectRatio, visibleWidth;
        if (mSarDen == mSarNum) {
            *//* No indication about the density, assuming 1:1 *//*
            visibleWidth = mVideoVisibleWidth;
            aspectRatio = (double) mVideoVisibleWidth / (double) mVideoVisibleHeight;
        } else {
            *//* Use the specified aspect ratio *//*
            visibleWidth = mVideoVisibleWidth * (double) mSarNum / mSarDen;
            aspectRatio = visibleWidth / mVideoVisibleHeight;
        }

        // compute the display aspect ratio
        double displayAspectRatio = displayWidth / displayHeight;

        counter ++;

        switch (mCurrentSize) {
            case SURFACE_BEST_FIT:
                if(counter > 2)
                    Toast.makeText(getContext(), "Best Fit", Toast.LENGTH_SHORT).show();
                if (displayAspectRatio < aspectRatio)
                    displayHeight = displayWidth / aspectRatio;
                else
                    displayWidth = displayHeight * aspectRatio;
                break;
            case SURFACE_FIT_HORIZONTAL:
                Toast.makeText(getContext(), "Fit Horizontal", Toast.LENGTH_SHORT).show();
                displayHeight = displayWidth / aspectRatio;
                break;
            case SURFACE_FIT_VERTICAL:
                Toast.makeText(getContext(), "Fit Horizontal", Toast.LENGTH_SHORT).show();
                displayWidth = displayHeight * aspectRatio;
                break;
            case SURFACE_FILL:
                Toast.makeText(getContext(), "Fill", Toast.LENGTH_SHORT).show();
                break;
            case SURFACE_16_9:
                Toast.makeText(getContext(), "16:9", Toast.LENGTH_SHORT).show();
                aspectRatio = 16.0 / 9.0;
                if (displayAspectRatio < aspectRatio)
                    displayHeight = displayWidth / aspectRatio;
                else
                    displayWidth = displayHeight * aspectRatio;
                break;
            case SURFACE_4_3:
                Toast.makeText(getContext(), "4:3", Toast.LENGTH_SHORT).show();
                aspectRatio = 4.0 / 3.0;
                if (displayAspectRatio < aspectRatio)
                    displayHeight = displayWidth / aspectRatio;
                else
                    displayWidth = displayHeight * aspectRatio;
                break;
            case SURFACE_ORIGINAL:
                Toast.makeText(getContext(), "Original", Toast.LENGTH_SHORT).show();
                displayHeight = mVideoVisibleHeight;
                displayWidth = visibleWidth;
                break;
        }

        // set display size
        int finalWidth = (int) Math.ceil(displayWidth * mVideoWidth / mVideoVisibleWidth);
        int finalHeight = (int) Math.ceil(displayHeight * mVideoHeight / mVideoVisibleHeight);

        SurfaceHolder holder = this.getHolder();
        holder.setFixedSize(finalWidth, finalHeight);

        ViewGroup.LayoutParams lp = this.getLayoutParams();
        lp.width = finalWidth;
        lp.height = finalHeight;
        this.setLayoutParams(lp);
        this.invalidate();
    }*/
}
