package st.android.scanningui;

import android.content.Context;
import android.content.res.ColorStateList;
import android.content.res.TypedArray;
import android.graphics.Bitmap;
import android.graphics.Point;
import android.graphics.PorterDuff;
import android.graphics.drawable.Drawable;
import android.os.Handler;
import android.os.Message;
import android.support.annotation.NonNull;
import android.support.v4.graphics.drawable.DrawableCompat;
import android.text.TextUtils;
import android.util.AttributeSet;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.view.animation.AlphaAnimation;
import android.view.animation.Animation;
import android.view.animation.AnimationSet;
import android.view.animation.BounceInterpolator;
import android.view.animation.ScaleAnimation;
import android.view.animation.TranslateAnimation;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;
import android.widget.TextView;


public class ScanningUI extends RelativeLayout {

    /**
     * to slow down the corning's position changing
     */
    private final static long MAX_DELAY_TIME = 1000;
    private final static int MSG_POP_DELAY = 1000;
    private static final String TAG = ScanningUI.class.getSimpleName();
    private Context context;
    /**
     * top bar to show blur score process
     */
    private ProgressBar scanningBlurValueBar;
    /**
     * msg which jump to show "move closer", etc
     */
    private TextView jumpMsg;
    /**
     * 4 corner positions
     */
    private CornerPositions cornerPositions = new CornerPositions();
    /**
     * delay time when to reset corner position
     */
    private long cornersCurrentDelayTime = 0;
    /**
     * corner img size
     */
    private int cornerImgSize = 100;
    /**
     * fp img
     */
    private ImageView scanningResultImg;
    /**
     * include Fp img and loading view
     */
    private RelativeLayout resultImgAndLoadingLayout;
    private ImageView scanningThumb;
    private ProgressBar uploadingProgressBar;
    private Point msgTextViewPosition = new Point();
    private long msgPopStartTime = 0;
    private AnimationHandler animationHandler;
    /**
     * ProgressBar used to show the blur score
     */
    private RelativeLayout scanningBlurScoreProgressBarContainer;
    private LinearLayout scanningResultUploadingContainer;


    int blurScoreProgressbarProcessColor;
    int thumbBackgroundColor;
    int jumpMsgBackgroundColor;
    int uploadingContainerBackgroundColor;
    int uploadingProgressBarProcessColor;
    int progressBarProcessDrawable;
    int mainColor;

    ImageView conerLeftTop;
    ImageView conerLeftBottom;
    ImageView conerRightTop;
    ImageView conerRightBottom;

    public ScanningUI(Context context, AttributeSet attrs) {
        super(context, attrs);
        this.context = context;
        animationHandler = new AnimationHandler(this);

        try {
            LayoutInflater inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
            inflater.inflate(R.layout.scanning_ui_layout, this, true);

        } catch (Exception e) {
            e.printStackTrace();
        }

        getAttrSettings(context, attrs);

        initElements();
        grayLayoutInit();
        cornersPositionInit();
        scanningBlurScoreValueBarInit();
        scanningThumbInit();
        jumpMsgInit();
        resultImgInit();
        uploadingProgressBarInit();
    }

    private void getAttrSettings(Context context, AttributeSet attrs) {
        TypedArray ta = context.obtainStyledAttributes(attrs, R.styleable.scanningUI);

        blurScoreProgressbarProcessColor = ta.getResourceId(R.styleable.scanningUI_blurScoreProgressBarProcessColor, -1);
        thumbBackgroundColor = ta.getResourceId(R.styleable.scanningUI_thumbBackgroundColor, -1);
        jumpMsgBackgroundColor = ta.getResourceId(R.styleable.scanningUI_jumpMsgBackgroundColor, -1);
        uploadingContainerBackgroundColor = ta.getResourceId(R.styleable.scanningUI_uploadingContainerBackgroundColor, -1);
        uploadingProgressBarProcessColor = ta.getResourceId(R.styleable.scanningUI_uploadingProgressBarProcessColor, -1);
        progressBarProcessDrawable = ta.getResourceId(R.styleable.scanningUI_android_progressDrawable, -1);
        mainColor = ta.getResourceId(R.styleable.scanningUI_mainColor, -1);

        Log.e(TAG, "blurScoreProgressbarProcessColor = " + blurScoreProgressbarProcessColor);
        Log.e(TAG, "thumbBackgroundColor = " + thumbBackgroundColor);
        Log.e(TAG, "jumpMsgBackgroundColor = " + jumpMsgBackgroundColor);
        Log.e(TAG, "uploadingContainerBackgroundColor = " + uploadingContainerBackgroundColor);
        Log.e(TAG, "uploadingProgressBarProcessColor = " + uploadingProgressBarProcessColor);
        Log.e(TAG, "progressBarProcessDrawable = " + progressBarProcessDrawable);
        Log.e(TAG, "mainColor = " + mainColor);

        ta.recycle();
    }

    /**
     * init values of element
     */
    private void initElements() {
        try {
            uploadingProgressBar = (ProgressBar) findViewById(R.id.scanning_result_uploading_bar);
            scanningBlurScoreProgressBarContainer = (RelativeLayout) findViewById(R.id.scanning_blur_score_progress_container);
            scanningResultUploadingContainer = (LinearLayout) findViewById(R.id.scanning_result_uploading_container);
            resultImgAndLoadingLayout = (RelativeLayout) findViewById(R.id.scanning_result_img_and_loading);
            scanningResultImg = (ImageView) resultImgAndLoadingLayout.findViewById(R.id.scanning_result_img);
            scanningThumb = (ImageView) findViewById(R.id.scanning_progressbar_thumb);
            jumpMsg = (TextView) findViewById(R.id.scanning_status_msg);

            conerLeftTop = (ImageView) findViewById(R.id.corner_left_top);
            conerLeftBottom = (ImageView) findViewById(R.id.corner_left_bottom);
            conerRightTop = (ImageView) findViewById(R.id.corner_right_top);
            conerRightBottom = (ImageView) findViewById(R.id.corner_right_bottom);

            if (mainColor != -1) {

                setDrawableTint(scanningThumb);
                setDrawableTint(conerLeftBottom);
                setDrawableTint(conerLeftTop);
                setDrawableTint(conerRightTop);
                setDrawableTint(conerRightBottom);
                jumpMsg.setBackgroundColor(mainColor);

                scanningResultUploadingContainer.setBackgroundColor(context.getResources().getColor(mainColor));
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

    }

    private void setDrawableTint(ImageView view) {
        Drawable drawable = DrawableCompat.wrap(view.getDrawable());
        if (drawable != null) {
            DrawableCompat.setTint(drawable, context.getResources().getColor(mainColor));
            view.setImageDrawable(drawable);
        } else {
            Log.e(TAG, "scanningThumb.getDrawable() is null");
        }

    }

    /**
     * reset uploading progress bar
     */
    private void uploadingProgressBarInit() {
        if (progressBarProcessDrawable != -1) {
            uploadingProgressBar.setProgressDrawable(context.getResources().getDrawable(progressBarProcessDrawable));
        }
        Log.e(TAG, "uploadingProgressBar  ProcessDrawable " + uploadingProgressBar.getProgressDrawable());

        uploadingProgressBar.setVisibility(VISIBLE);
        uploadingProgressBar.setProgress(0);
    }

    /**
     * to calculate where to show the FP img and loading view
     */
    private void resultImgInit() {
        resultImgAndLoadingLayout.setX(cornerPositions.getLeftTop().x);
        resultImgAndLoadingLayout.setY(cornerPositions.getLeftTop().y);
        int wight = cornerPositions.getRightBottom().x - cornerPositions.getLeftTop().x + cornerImgSize;
        int height = cornerPositions.getRightBottom().y - cornerPositions.getLeftTop().y + cornerImgSize;
        resultImgAndLoadingLayout.setLayoutParams(new LayoutParams(wight, height));
    }


    /**
     * to show the scanning result img
     *
     * @param bitmap
     */
    public void resultImgSet(Bitmap bitmap) {
        scanningResultImg.setImageBitmap(bitmap);
    }

    public void resultContainerShowing(boolean needShow) {
        if (needShow) {
            resultImgAndLoadingLayout.setVisibility(VISIBLE);
            uploadingProgressBarInit();
        } else {
            resultImgAndLoadingLayout.setVisibility(GONE);
        }
    }

    /**
     * jump msg init
     */
    private void jumpMsgInit() {
        LayoutParams attributeSet = (LayoutParams) jumpMsg.getLayoutParams();
        int msgInitPosition = cornerPositions.getLeftTop().y + 100;
        attributeSet.setMargins(attributeSet.getMarginStart(), msgInitPosition, attributeSet.getMarginEnd(), 0);
        jumpMsg.setLayoutParams(attributeSet);
        msgTextViewPosition.set((int) jumpMsg.getX(), (int) jumpMsg.getY());
    }

    /**
     * jump msg and show the text as set
     *
     * @param msg which msg to show
     */
    public void jumpMsgStartJump(String msg) {
        if (!TextUtils.isEmpty(msg)
                && (System.currentTimeMillis() - msgPopStartTime > MSG_POP_DELAY)) {
            msgPopStartTime = System.currentTimeMillis();
            jumpMsg.setVisibility(VISIBLE);
            jumpMsg.setText(msg);

            TranslateAnimation jumpAnimation = new TranslateAnimation(0, 0, -300, 0);
            jumpAnimation.setFillAfter(true);
            jumpAnimation.setInterpolator(new BounceInterpolator());
            jumpAnimation.setDuration(500);

            AlphaAnimation alphaAnimation = new AlphaAnimation(1, 0);
            alphaAnimation.setDuration(1500);
            alphaAnimation.setFillEnabled(true);
            alphaAnimation.setAnimationListener(new Animation.AnimationListener() {
                @Override
                public void onAnimationStart(Animation animation) {

                }

                @Override
                public void onAnimationEnd(Animation animation) {
                    jumpMsg.setText("");
                    jumpMsg.setVisibility(INVISIBLE);
                }

                @Override
                public void onAnimationRepeat(Animation animation) {

                }
            });

            AnimationSet animationSet = new AnimationSet(false);
            animationSet.addAnimation(jumpAnimation);
            animationSet.addAnimation(alphaAnimation);
            jumpMsg.startAnimation(animationSet);
        }
    }

    /**
     * init the thumb icon
     */
    private void scanningThumbInit() {
        scanningThumb.setVisibility(GONE);
    }

    /**
     * to show the final status of the thumb
     */
    public void scanningThumbScale() {
        stopAnimationHandler();
        scanningBlurValueBar.setProgress(100);
        ScaleAnimation scaleAnimation = new ScaleAnimation(0f, 1f, 0f, 1f, Animation.RELATIVE_TO_SELF, 0.5f, Animation.RELATIVE_TO_SELF, 0.5f);
        scaleAnimation.setFillAfter(false);
        scaleAnimation.setInterpolator(new BounceInterpolator());
        scaleAnimation.setDuration(600);
        scanningThumb.setVisibility(VISIBLE);
        scanningThumb.startAnimation(scaleAnimation);
    }

    /**
     * init blur score progress bar
     */
    private void scanningBlurScoreValueBarInit() {
        LayoutParams layoutParams = (LayoutParams) scanningBlurScoreProgressBarContainer.getLayoutParams();
        layoutParams.setMargins(0, dp2Px(context, px2Dp(context, cornerPositions.leftTop.y) - 70), 0, 0);
        scanningBlurScoreProgressBarContainer.setLayoutParams(layoutParams);


        scanningBlurValueBar = (ProgressBar) findViewById(R.id.scanning_seekbar);
        if (progressBarProcessDrawable != -1) {
            scanningBlurValueBar.setProgressDrawable(context.getResources().getDrawable(progressBarProcessDrawable));
        }
        scanningBlurValueBar.setProgress(0);
        scanningBlurValueBar.setInterpolator(new BounceInterpolator());
    }

    /**
     * set current value of the progress bar
     *
     * @param progress
     */
    public void scanningBlurScoreBarSetProgress(int progress) {
        if (progress > scanningBlurValueBar.getProgress()) {
            scanningBlurValueBar.setProgress(progress);
            scanningBlurScoreBarDecrease();
        }
    }

    /**
     * decrease the blur score progress bar
     */
    private void scanningBlurScoreBarDecrease() {
        int currentScanningBlurScore = scanningBlurValueBar.getProgress();
        if (currentScanningBlurScore >= 1) {
            scanningBlurValueBar.setProgress(--currentScanningBlurScore);
            animationHandler.removeMessages(AnimationHandler.DECREASE_SCAN_BLURS_CORE_BAR);
            animationHandler.sendEmptyMessageDelayed(AnimationHandler.DECREASE_SCAN_BLURS_CORE_BAR, 60);
        }
    }

    @Override
    protected void onVisibilityChanged(@NonNull View changedView, int visibility) {
        super.onVisibilityChanged(changedView, visibility);
        scanningBlurScoreValueBarInit();
        cornersPositionInit();
        resultContainerShowing(false);
        scanningThumbInit();
        stopAnimationHandler();
        scanningResultImg.setImageBitmap(null);
    }

    /**
     * get current display metrics
     *
     * @param context context
     * @return current metrics
     */
    private DisplayMetrics getMetrics(Context context) {
        DisplayMetrics metrics = new DisplayMetrics();
        WindowManager manager = (WindowManager) context.getSystemService(Context.WINDOW_SERVICE);
        manager.getDefaultDisplay().getMetrics(metrics);
        return metrics;
    }


    private void grayLayoutInit() {
        int displayW = getMetrics(context).widthPixels;
        int displayH = getMetrics(context).heightPixels;
        int minProportion = 70;
        float placeholderProportion = Math.min(minProportion / 100.0f + 0.1f, 1);// set a 10% margin
        float sideMarginProportion = (1 - placeholderProportion) / 2.0f;
        int padw = (int) (displayW * sideMarginProportion);
        int squaresize = displayW - 2 * padw;
        int padh = (int) ((displayH - squaresize) / 2.0);
        int shift = displayH / 10;
        int yOffset = padh - shift;

        cornerPositions.setLeftTop(new Point(padw, yOffset));
        cornerPositions.setLeftBottom(new Point(padw, yOffset + squaresize - cornerImgSize));
        cornerPositions.setRightTop(new Point(displayW - padw - cornerImgSize, yOffset));
        cornerPositions.setRightBottom(new Point(displayW - padw - cornerImgSize, yOffset + squaresize - cornerImgSize));

        setGrayLayout(cornerImgSize, padw, padh, shift, yOffset);

    }

    /**
     * border gray layout
     *
     * @param angleImSize corner size
     * @param padw        width
     * @param padh        height
     * @param shift       shift
     * @param yOffset     offset
     */
    private void setGrayLayout(int angleImSize, int padw, int padh, int shift, int yOffset) {
        int margin = -40; //beside the corner and the edge of the gray layout
        setWidthAndHeight(findViewById(R.id.overlayT), -1, yOffset + margin);
        setWidthAndHeight(findViewById(R.id.overlayB), -1, padh + shift - angleImSize + 1 + margin);
        setWidthAndHeight(findViewById(R.id.overlayR), padw + margin, -1);
        setWidthAndHeight(findViewById(R.id.overlayL), padw + margin, -1);
    }

    /**
     * set gray width and Height
     *
     * @param view   which side
     * @param width  width
     * @param height height
     */
    private void setWidthAndHeight(View view, int width, int height) {
        ViewGroup.LayoutParams params = view.getLayoutParams();
        if (width >= 0) {
            params.width = width;
        }
        if (height >= 0) {
            params.height = height;
        }
    }

    /**
     * init corners position
     */
    private void cornersPositionInit() {
        setMarginLayout(findViewById(R.id.corner_left_top), cornerPositions.getLeftTop().x, cornerPositions.getLeftTop().y);
        setMarginLayout(findViewById(R.id.corner_left_bottom), cornerPositions.getLeftBottom().x, cornerPositions.getLeftBottom().y);
        setMarginLayout(findViewById(R.id.corner_right_top), cornerPositions.getRightTop().x, cornerPositions.getRightTop().y);
        setMarginLayout(findViewById(R.id.corner_right_bottom), cornerPositions.getRightBottom().x, cornerPositions.getRightBottom().y);
        cornersCurrentDelayTime = System.currentTimeMillis();
    }

    /**
     * move corners to focus status/position
     */
    public void cornersMovedToFocus() {
        int shift = context.getResources().getDisplayMetrics().densityDpi >= 640 ? 400 : 300;
        setMarginLayout(findViewById(R.id.corner_left_top), cornerPositions.getLeftTop().x + shift, cornerPositions.getLeftTop().y + shift);
        setMarginLayout(findViewById(R.id.corner_left_bottom), cornerPositions.getLeftBottom().x + shift, cornerPositions.getLeftBottom().y - shift);
        setMarginLayout(findViewById(R.id.corner_right_top), cornerPositions.getRightTop().x - shift, cornerPositions.getRightTop().y + shift);
        setMarginLayout(findViewById(R.id.corner_right_bottom), cornerPositions.getRightBottom().x - shift, cornerPositions.getRightBottom().y - shift);
    }

    /**
     * reset corners to init position
     */
    public void cornersResetPosition() {
        if ((System.currentTimeMillis() - cornersCurrentDelayTime) >= MAX_DELAY_TIME) {
            cornersPositionInit();
        }
    }

    /**
     * set layout of the gray area and corners
     *
     * @param view which view to set
     * @param x    position x
     * @param y    position y
     */
    private void setMarginLayout(View view, int x, int y) {
        MarginLayoutParams margin = new MarginLayoutParams(view.getLayoutParams());
        margin.setMargins(x, y, 0, 0);
        LayoutParams layoutParams = new LayoutParams(margin);
        view.setLayoutParams(layoutParams);
    }

    /**
     * set the process of the uploading progress bar
     *
     * @param processRate
     */
    public void uploadingProgressBarSet(int processRate) {
        uploadingProgressBar.setProgress(processRate);
    }

    /**
     * get the current rate of the progress bar
     *
     * @return current progress value
     */
    public int uploadingProgressBarGetRate() {
        return uploadingProgressBar.getProgress();
    }


    public void uploadingProgressBarStartUploading() {
        animationHandler.sendEmptyMessage(AnimationHandler.UPLOADING_PROGRESS_START);
    }

    /**
     * is uploading progress bar showing?
     *
     * @return current status
     */
    public boolean uploadingProgressBarIsShow() {
        return uploadingProgressBar.getVisibility() == VISIBLE;
    }

    public static int dp2Px(Context context, float dp) {
        final float scale = context.getResources().getDisplayMetrics().density;
        return (int) (dp * scale + 0.5f);
    }

    public static int px2Dp(Context context, float px) {
        final float scale = context.getResources().getDisplayMetrics().density;
        return (int) (px / scale + 0.5f);
    }

    /**
     * stop animation
     */
    public void stopAnimationHandler() {
        if (animationHandler != null) {
            animationHandler.removeMessages(AnimationHandler.DECREASE_SCAN_BLURS_CORE_BAR);
        }
    }

    public static class AnimationHandler extends Handler {

        private static final int DECREASE_SCAN_BLURS_CORE_BAR = 0;
        private static final int UPLOADING_PROGRESS_START = 1;
        private ScanningUI scanningUI = null;


        public AnimationHandler(@NonNull ScanningUI scanningUI) {
            this.scanningUI = scanningUI;
        }

        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case DECREASE_SCAN_BLURS_CORE_BAR:
                    scanningUI.scanningBlurScoreBarDecrease();
                    break;
                case UPLOADING_PROGRESS_START:
                    try {
                        postDelayed(new Runnable() {
                            @Override
                            public void run() {

                                try {
                                    if (scanningUI.uploadingProgressBarGetRate() > 99 || !scanningUI.uploadingProgressBarIsShow()) {
                                        return;
                                    }

                                    int rate = scanningUI.uploadingProgressBarGetRate();
                                    scanningUI.uploadingProgressBarSet(++rate);
                                    scanningUI.uploadingProgressBarStartUploading();
                                } catch (Exception e) {
                                    e.printStackTrace();
                                }
                            }
                        }, 50);

                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                    break;

                default:
                    break;
            }

            super.handleMessage(msg);
        }
    }

    /**
     * for easy to know the corner position
     */
    private class CornerPositions {
        private Point leftTop;
        private Point leftBottom;
        private Point rightTop;
        private Point rightBottom;

        Point getLeftTop() {
            return leftTop;
        }

        void setLeftTop(Point leftTop) {
            this.leftTop = leftTop;
        }

        Point getLeftBottom() {
            return leftBottom;
        }

        void setLeftBottom(Point leftBottom) {
            this.leftBottom = leftBottom;
        }

        Point getRightTop() {
            return rightTop;
        }

        void setRightTop(Point rightTop) {
            this.rightTop = rightTop;
        }

        Point getRightBottom() {
            return rightBottom;
        }

        void setRightBottom(Point rightBottom) {
            this.rightBottom = rightBottom;
        }

        @Override
        public String toString() {
            return "CornerPositions{" +
                    "leftTop=" + leftTop +
                    ", leftBottom=" + leftBottom +
                    ", rightTop=" + rightTop +
                    ", rightBottom=" + rightBottom +
                    '}';
        }
    }

}
