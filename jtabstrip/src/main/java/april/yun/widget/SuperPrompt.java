package april.yun.widget;

import android.animation.ValueAnimator;
import android.annotation.SuppressLint;
import android.content.res.Resources;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.PointF;
import android.graphics.Rect;
import android.graphics.RectF;
import android.graphics.drawable.Drawable;
import android.support.annotation.Size;
import android.text.TextUtils;
import android.util.DisplayMetrics;
import android.util.Log;
import android.util.TypedValue;
import android.view.View;
import android.view.animation.BounceInterpolator;
import android.view.animation.DecelerateInterpolator;

/**
 * @another 江祖赟
 * @date 2017/9/11.
 */
public class SuperPrompt implements ValueAnimator.AnimatorUpdateListener {
    protected static final String TAG = "SuperPrompt";
    public static final String NOTIFY = "n";
    public static final String ALOT = "~";
    protected static final int SHOWTIME = 666;
    public static String MSGFORMART = "%d";
    protected Paint mBgPaint;
    protected Paint mNumPaint;
    protected int color_bg = Color.RED;
    protected int color_num = Color.WHITE;
    protected int num_size = 11;
    protected float mHalfW;
    protected float mNumHeight;
    protected String msg_str = "";
    protected PointF mPromptCenterPoint;
    protected RectF mMsgBg;
    //protected static final String ALOT = "...~~";
    protected String mLastMsg = "";
    protected ValueAnimator mShowAni;
    //是否要清楚消息
    protected boolean msgIs_dirty;
    protected boolean mIsAniShow;
    protected float[] mPromptOutOffset;
    protected View mView;
    protected float mPointCenterY;
    protected float mHalfH;
    protected float mHalfMsgBgW;
    protected float mHalfMsgBgH;

    public static float dp2px(float px){
        DisplayMetrics dm = Resources.getSystem().getDisplayMetrics();
        return TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, px, dm);
    }

    {
        mNumPaint = new Paint(Paint.ANTI_ALIAS_FLAG) {
            {
                setColor(color_num);
                setTextAlign(Paint.Align.CENTER);
            }
        };
        mBgPaint = new Paint(Paint.ANTI_ALIAS_FLAG) {
            {
                setColor(color_bg);
            }
        };
        mShowAni = ValueAnimator.ofFloat(0, 1);
        mShowAni.setDuration(SHOWTIME);
        //mShowAni.setInterpolator(new AccelerateDecelerateInterpolator());
        mShowAni.addUpdateListener(this);
    }

    public SuperPrompt(View view){
        mView = view;
    }

    public static float getFontHeight(Paint paint){
        Paint.FontMetrics fontMetrics = paint.getFontMetrics();
        return -fontMetrics.top-fontMetrics.bottom;
    }

    protected boolean haveCompoundDrawable(Drawable[] compoundDrawables){
        for(Drawable compoundDrawable : compoundDrawables) {
            if(compoundDrawable != null) {
                return true;
            }
        }
        return false;
    }

    protected void onSizeChanged(int w, int h, int oldw, int oldh){
        mHalfW = w/2f;
        mHalfH = h/2f;
        mNumPaint.setTextSize(dp2px(num_size));
        mNumHeight = getFontHeight(mNumPaint);
        refreshNotifyBg();
        startShowAni();
    }

    public SuperPrompt asNewMsgNums(){
        color_bg = Color.TRANSPARENT;
        color_num = Color.RED;
        mNumPaint.setColor(color_num);
        mBgPaint.setColor(color_bg);
        return this;
    }

    private float getHalfMsgBgW(){
        return calcutePosition(false, true);
    }

    private float getCenterPointX(boolean isX){
        return calcutePosition(isX, false);
    }

    private float calcutePosition(boolean isX, Boolean getHalfMsgBgW){
        float msgWidth = getTextWidth(mNumPaint, msg_str);
        //prompt背景和 prompt文字的offset
        float promptOffset = mNumHeight/2f;
        float centerY = mNumHeight;
        //prompt 背景的宽度
        float halfMsgBgW = msgWidth/2f+promptOffset;
        if(color_bg != Color.TRANSPARENT && !NOTIFY.equals(msg_str)) {
            halfMsgBgW = halfMsgBgW>mNumHeight ? halfMsgBgW : mNumHeight;
        }else {
            if(NOTIFY.equals(msg_str)) {
                centerY = halfMsgBgW = mNumHeight/2f;
            }else {
                halfMsgBgW = msgWidth/2f;
                centerY = mNumHeight/2f;
            }
        }
        if(getHalfMsgBgW) {
            return halfMsgBgW;
        }else if(isX) {
            return mHalfW*2-halfMsgBgW;
        }else {
            return centerY;
        }
    }

    /**
     * 更新 promptView的位置
     */
    protected void refreshNotifyBg(){
        float msgWidth = getTextWidth(mNumPaint, msg_str);
        //prompt背景和 prompt文字的offset
        float promptOffset = mNumHeight/2f;
        mHalfMsgBgW = msgWidth/2f+promptOffset;
        mHalfMsgBgH = mNumHeight;
        mPointCenterY = mNumHeight;
        if(color_bg != Color.TRANSPARENT && !NOTIFY.equals(msg_str)) {
            mHalfMsgBgW = mHalfMsgBgW>mNumHeight ? mHalfMsgBgW : mNumHeight;
            mPromptCenterPoint = new PointF(mHalfW*2-mHalfMsgBgW, mPointCenterY);
        }else {
            if(NOTIFY.equals(msg_str)) {
                mHalfMsgBgH = mPointCenterY = mHalfMsgBgW = mNumHeight/2f;
            }else {
                mHalfMsgBgW = msgWidth/2f;
                mHalfMsgBgH = mPointCenterY = mNumHeight/2f;
            }
            mPromptCenterPoint = new PointF(mHalfW*2-mHalfMsgBgW, mPointCenterY);
        }

        mMsgBg = new RectF(mPromptCenterPoint.x-mHalfMsgBgW, mPromptCenterPoint.y-mHalfMsgBgH,
                mPromptCenterPoint.x+mHalfMsgBgW, mPromptCenterPoint.y+mHalfMsgBgH);
        if(mPromptOutOffset != null) {
            mPromptCenterPoint.offset(-mPromptOutOffset[0], mPromptOutOffset[1]);
            mMsgBg.offset(-mPromptOutOffset[0], mPromptOutOffset[1]);
        }
        //防止画到屏幕外  右上角
        if(mMsgBg.right>2*mHalfW || mMsgBg.top<0) {
            //顺序不可变 因为mPromptCenterPoint依赖mMsgBg
            float offsetX = 2*mHalfW-mMsgBg.right;
            offsetX = offsetX<0 ? offsetX : 0;
            float offsetY = mMsgBg.top<0 ? -mMsgBg.top : 0;
            mPromptCenterPoint.offset(offsetX, offsetY);
            mMsgBg.offset(offsetX, offsetY);
        }
        mPointCenterY = mPromptCenterPoint.y;
    }


    public void onDraw(Canvas canvas){
        if(!TextUtils.isEmpty(msg_str)) {
            if(msg_str.equals(NOTIFY)) {
                //画提示圆点即可
                canvas.drawCircle(mPromptCenterPoint.x, mPromptCenterPoint.y, mNumHeight/2, mBgPaint);
            }else {
                if(color_bg != Color.TRANSPARENT) {
                    canvas.drawRoundRect(mMsgBg, mNumHeight, mNumHeight, mBgPaint);
                }
                canvas.drawText(msg_str, mPromptCenterPoint.x, mPromptCenterPoint.y+mNumHeight/2, mNumPaint);
            }
        }
    }

    public static int computeMaxStringWidth(int currentMax, String[] strings, Paint p){
        float maxWidthF = 0.0f;
        int len = strings.length;
        for(int i = 0; i<len; i++) {
            float width = p.measureText(strings[i]);
            maxWidthF = Math.max(width, maxWidthF);
        }
        int maxWidth = (int)( maxWidthF+0.5 );
        if(maxWidth<currentMax) {
            maxWidth = currentMax;
        }
        return maxWidth;
    }

    public static float getTextWidth(Paint paint, String str){
        //                Rect bounds = new Rect();
        //                paint.getTextBounds(str, 0, str.length(), bounds);
        //                return bounds.width();
        return paint.measureText(str);
    }

    /**
     * 获取单个文字的高度 比较准确
     *
     * @param paint
     * @param str
     * @return
     */
    public static int getTextHeight(Paint paint, String str){
        Rect bounds = new Rect();
        paint.getTextBounds(str, 0, str.length(), bounds);
        return bounds.bottom-bounds.top;
    }

    /**
     * 获取单个字符文字的高度
     *
     * @param paint
     * @param str
     * @return
     */
    public static float getTextHeight2(Paint paint, String str){
        Paint.FontMetrics fontMetrics = paint.getFontMetrics();
        return fontMetrics.bottom-fontMetrics.ascent;
    }


    /**
     * 当num的值小于0 显示提示小圆点
     * 等于0 不现实任何
     */
    public SuperPrompt setPromptMsg(String msg){
        if(mLastMsg.equals(msg)) {
            Log.e(TAG, "set the same num width last time");
            return this;
        }else if(TextUtils.isEmpty(msg)) {
            msgIs_dirty = true;
            if(!mIsAniShow) {
                mLastMsg = msg_str = "";
                invalidatePrompt();
                return this;
            }
        }else {
            msgIs_dirty = false;
            msg_str = msg;
        }

        Log.d(TAG, "msg: "+msg_str);
        if(mHalfW>0) {
            refreshNotifyBg();
            startShowAni();
        }
        mLastMsg = msg;
        return this;
    }

    @SuppressLint("DefaultLocale")
    public String getMsgByNum(int num){
        if(num>99) {
            return ALOT;
        }else if(num == 0) {
            return "";
        }else if(num<0) {
            return NOTIFY;
        }else {
            return String.format(MSGFORMART, num);
        }
    }


    protected void startShowAni(){
        //为空表示不显示提示信息，清除提示信息会把msg_str置为空但是在动画结束之后
        if(!TextUtils.isEmpty(msg_str) && mIsAniShow) {
            if(msgIs_dirty) {//移除消息
                Log.d(TAG, "remove prompt msg");
                mLastMsg = "";
                //有消息到没消息
                mShowAni.cancel();
                mShowAni.setInterpolator(new DecelerateInterpolator());
                mShowAni.start();
            }else if(TextUtils.isEmpty(mLastMsg) || msg_str.equals(mLastMsg)) {//没消息到 显示消息
                Log.d(TAG, "ani show prompt msg");
                //没消息到有消息
                mLastMsg = NOTIFY;
                mShowAni.cancel();
                mShowAni.setInterpolator(new BounceInterpolator());
                mShowAni.start();
            }
        }
        invalidatePrompt();
    }


    public SuperPrompt setColor_bg(int color_bg){
        this.color_bg = color_bg;
        mBgPaint.setColor(color_bg);
        return this;
    }


    public SuperPrompt setColor_num(int color_num){
        this.color_num = color_num;
        mNumPaint.setColor(color_num);
        return this;
    }


    public SuperPrompt setNum_size(int num_size){
        this.num_size = num_size;
        return this;
    }

    @Override
    public void onAnimationUpdate(ValueAnimator animation){
        float ratio = (float)animation.getAnimatedValue();
        if(msgIs_dirty && ratio == 1) {
            Log.d(TAG, "clear msg aready");
            msg_str = "";//动画结束后情空消息
        }
        ratio = TextUtils.isEmpty(mLastMsg) ? 1-ratio : ratio;
        mPromptCenterPoint.y = mPointCenterY*( 3*ratio/2f-1/2f );
        mMsgBg.bottom = mHalfMsgBgH+mPromptCenterPoint.y;
        mMsgBg.bottom = mMsgBg.bottom<mMsgBg.top ? mMsgBg.top : mMsgBg.bottom;
        invalidatePrompt();
    }

    private void invalidatePrompt(){
        mView.invalidate();
    }

    public SuperPrompt setPromptOutOffset(@Size(value = 2) float[] promptOutOffset){
        mPromptOutOffset = promptOutOffset;
        return this;
    }

    public SuperPrompt setPromptOutOffset(float promptOutOffset){
        mPromptOutOffset = new float[]{promptOutOffset, promptOutOffset};
        return this;
    }

}
