package com.pyjtlk.radarview;

import android.animation.TypeEvaluator;
import android.animation.ValueAnimator;
import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.PointF;
import android.support.annotation.Nullable;
import android.util.AttributeSet;
import android.util.Log;
import android.view.View;
import android.view.ViewTreeObserver;
import java.util.ArrayList;

/**
 * Created by Administrator on 2018/12/11.
 */

/**
 * 雷达图控件
 */
public class RadarChart extends View{
    /**
     * 控件默认大小
     */
    public final static int VIEW_DEFAULT_SIZE = 50;

    /**
     * 雷达图线条默认颜色
     */
    public final static int LINE_DEFAULT_COLOR = Color.BLUE;

    /**
     * 雷达图线条默认透明度
     */
    public final static int LINE_DEFAULT_ALPHA = 250;

    /**
     * 雷达图线条默认宽度
     */
    public final static int LINE_DEFAULT_WIDTH = 10;

    /**
     * 雷达图内容默认颜色
     */
    public final static int CONTENT_DEFAULT_COLOR = Color.YELLOW;

    /**
     * 雷达图内容默认透明度
     */
    public final static int CONTENT_DEFAULT_ALPHA = 220;

    /**
     * 属性默认最大级别
     */
    public final static int ATTRIBUTE_DEFAULT_LEVEL = 1;

    /**
     * 属性数量的下限
     */
    public final static int ATTRIBUTE_MIN_COUNT = 3;

    /**
     * 动画效果默认时长,单位:毫秒
     */
    public final static int ANIM_DEFAULT_DURATION = 500;

    private float mWebXArray[][];
    private float mWebYArray[][];
    private float mContentXArray[];
    private float mContentYArray[];

    private Context mContext;

    private int mAttributeCount;
    private int mLineColor;
    private int mLineAlpha;
    private int mContentColor;
    private int mContentAlpha;
    private int mLineRadius;
    private boolean isShowWeb;
    private int mMaxLevel;
    private int mLineWidth;
    private int mAnimDuration;

    private int[] mAttributeLevels;

    private Paint mPaint;

    private ValueAnimator mAttrAnimator;

    /**
     * 属性值变化插值器,startList是各属性值的起始的坐标,endList是各属性值结束时的坐标,返回的列表是从起始坐标到结束坐标的过程的坐标列表
     */
    private TypeEvaluator<ArrayList<PointF>> mAttrEvaluator = new TypeEvaluator<ArrayList<PointF>>() {
        @Override
        public ArrayList<PointF> evaluate(float fraction, ArrayList<PointF> startList, ArrayList<PointF> endList) {
            ArrayList<PointF> resultList = new ArrayList<>();
            PointF startPoint,endPoint;
            for(int i = 0;i < startList.size();i++){
                startPoint = startList.get(i);
                endPoint = endList.get(i);
                resultList.add(new PointF(startPoint.x + fraction * (endPoint.x - startPoint.x),
                        startPoint.y + fraction * (endPoint.y - startPoint.y)));
            }
            return resultList;
        }
    };

    /**
     * 动画更新监听器,属性值坐标在此改变
     */
    private ValueAnimator.AnimatorUpdateListener mAttrUpdateListener = new ValueAnimator.AnimatorUpdateListener() {
        @Override
        public void onAnimationUpdate(ValueAnimator animation) {
            ArrayList<PointF> pointList = (ArrayList<PointF>) animation.getAnimatedValue();
            for(int i = 0;i < pointList.size();i++){
                mContentXArray[i] = pointList.get(i).x;
                mContentYArray[i] = pointList.get(i).y;
                postInvalidate();
            }
        }
    };

    /**
     * 雷达图的构造方法,但不推荐创建雷达图控件
     * @param context
     */
    public RadarChart(Context context) {
        this(context,null);
    }

    public RadarChart(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        mContext = context;

        int initLevel;

        TypedArray typedArray = mContext.obtainStyledAttributes(attrs,R.styleable.RadarChart);
        mAttributeCount = typedArray.getInteger(R.styleable.RadarChart_attributeCount,ATTRIBUTE_MIN_COUNT);
        mLineColor = typedArray.getColor(R.styleable.RadarChart_lineColor, LINE_DEFAULT_COLOR);
        mLineAlpha = typedArray.getInteger(R.styleable.RadarChart_lineAlpha,LINE_DEFAULT_ALPHA);
        mContentColor = typedArray.getColor(R.styleable.RadarChart_contentColor,CONTENT_DEFAULT_COLOR);
        mContentAlpha = typedArray.getInteger(R.styleable.RadarChart_contentAlpha,CONTENT_DEFAULT_ALPHA);
        isShowWeb = typedArray.getBoolean(R.styleable.RadarChart_showWeb,false);
        mMaxLevel = typedArray.getInteger(R.styleable.RadarChart_attributeLevel,ATTRIBUTE_DEFAULT_LEVEL);
        mLineWidth = typedArray.getDimensionPixelSize(R.styleable.RadarChart_lineWidth,LINE_DEFAULT_WIDTH);
        initLevel = typedArray.getInteger(R.styleable.RadarChart_initLevel,0);
        mAnimDuration = typedArray.getInteger(R.styleable.RadarChart_animDuration,ANIM_DEFAULT_DURATION);
        typedArray.recycle();

        if(mAttributeCount < ATTRIBUTE_MIN_COUNT){
            mAttributeCount = ATTRIBUTE_MIN_COUNT;
        }

        mAttributeLevels = new int[mAttributeCount];

        if(mLineAlpha < 0 || mLineAlpha > 255){
            mLineAlpha = LINE_DEFAULT_ALPHA;
        }

        if(mContentAlpha < 0 || mContentAlpha > 255){
            mContentAlpha = CONTENT_DEFAULT_ALPHA;
        }

        if(mMaxLevel < ATTRIBUTE_DEFAULT_LEVEL){
            mMaxLevel = ATTRIBUTE_DEFAULT_LEVEL;
        }

        if(initLevel >= 0 && initLevel <= mMaxLevel){
            for(int i = 0;i < mAttributeLevels.length;i++){
                mAttributeLevels[i] = initLevel;
            }
        }

        if(mAnimDuration < 0){
            mAnimDuration = -mAnimDuration;
        }

        mWebXArray = new float[mAttributeCount][mMaxLevel + 1];
        mWebYArray = new float[mAttributeCount][mMaxLevel + 1];

        mContentXArray = new float[mAttributeCount];
        mContentYArray = new float[mAttributeCount];

        mPaint = new Paint();
        mPaint.setStrokeCap(Paint.Cap.ROUND);
        mPaint.setAntiAlias(true);

        //mAttrAnimator = ValueAnimator.ofObject(mAttrEvaluator);
        //mAttrAnimator.addUpdateListener(mAttrUpdateListener);
        getViewTreeObserver().addOnGlobalLayoutListener(new ViewTreeObserver.OnGlobalLayoutListener() {
            @Override
            public void onGlobalLayout() {
                getViewTreeObserver().removeOnGlobalLayoutListener(this);
                setAttributes(mAttributeLevels);
            }
        });
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);
        int width = MeasureSpec.getSize(widthMeasureSpec);
        int height = MeasureSpec.getSize(heightMeasureSpec);
        int widthMode = MeasureSpec.getMode(widthMeasureSpec);
        int heightMode = MeasureSpec.getMode(heightMeasureSpec);

        //始终保持雷达图在控件的居中位置
        if(widthMode == MeasureSpec.AT_MOST && heightMode == MeasureSpec.AT_MOST){
            width = VIEW_DEFAULT_SIZE;
            height = VIEW_DEFAULT_SIZE;
            mLineRadius = width / 2;
        }else if(widthMode == MeasureSpec.EXACTLY && heightMode == MeasureSpec.AT_MOST){
            height = width;
            mLineRadius = width / 2;
        }else if(widthMode == MeasureSpec.AT_MOST && heightMode == MeasureSpec.EXACTLY){
            width = height;
            mLineRadius = height / 2;
        }else{
            mLineRadius = Math.min(width,height) / 2;
        }

        mLineRadius -= mLineWidth;

        setMeasuredDimension(width,height);
    }

    @Override
    protected void onLayout(boolean changed, int left, int top, int right, int bottom) {
        locatWebPosition();
    }

    /**
     * 确定雷达图网格线条的位置
     */
    protected void locatWebPosition(){
        float centerX = getWidth() / 2;
        float centerY = getHeight() / 2;

        float addDegree = 360 / mAttributeCount;
        float currentDegree = mAttributeCount % 2 == 0 ?
                0 : -90;

        float currentRadius;

        for(int i = 0;i < mAttributeCount;i++){
            for(int j = 0; j <= mMaxLevel; j++){
                currentRadius = mLineRadius / mMaxLevel * j;
                mWebXArray[i][j] = (float) (currentRadius * Math.cos(currentDegree * Math.PI / 180)) + centerX;
                mWebYArray[i][j] = (float) (currentRadius * Math.sin(currentDegree * Math.PI / 180)) + centerY;
            }
            currentDegree += addDegree;
        }
    }

    @Override
    protected void onDraw(Canvas canvas) {
        drawContent(canvas);
        if(isShowWeb) {
            drawWeb(canvas);
        }
        drawLines(canvas);
    }

    /**
     * 绘制雷达图的基础线条
     * @param canvas
     */
    protected void drawLines(Canvas canvas){
        mPaint.setStrokeWidth(mLineWidth);
        mPaint.setColor(mLineColor);
        mPaint.setAlpha(mLineAlpha);

        float addDegree = 360 / mAttributeCount;

        //若属性的数量为奇数,则从正90度开始绘制
        //若属性的数量为偶数,则从0度开始绘制
        //绘制方向为顺时方向
        float currentDegree = mAttributeCount % 2 == 0 ?
                0 : -90;

        float centerX = getWidth() / 2;
        float centerY = getHeight() / 2;
        float endX = 0,endY = 0;

        for(int i = 0;i < mAttributeCount;i++){
            endX = (float) (mLineRadius * Math.cos(currentDegree * Math.PI / 180)) + centerX;
            endY = (float) (mLineRadius * Math.sin(currentDegree * Math.PI / 180)) + centerY;

            canvas.drawLine(centerX,centerY,endX,endY,mPaint);
            currentDegree += addDegree;
        }
    }

    /**
     * 绘制雷达图的属性多边形
     * 若属性的数量为奇数,则从正90度开始绘制
     * 若属性的数量为偶数,则从0度开始绘制
     * 绘制的方向是顺时针方向
     * @param canvas
     */
    protected void drawContent(Canvas canvas) {
        mPaint.setColor(mContentColor);
        mPaint.setAlpha(mContentAlpha);

        float x,y;
        Path path = new Path();

        x = mContentXArray[0];
        y = mContentYArray[0];
        path.moveTo(x,y);

        for(int i = 1;i < mAttributeLevels.length;i++){
            x = mContentXArray[i];
            y = mContentYArray[i];
            path.lineTo(x,y);
        }

        canvas.drawPath(path,mPaint);
    }

    /**
     * 绘制雷达图的网格
     * @param canvas
     */
    protected void drawWeb(Canvas canvas){
        mPaint.setStrokeWidth(mLineWidth);
        mPaint.setColor(mLineColor);
        mPaint.setAlpha(mLineAlpha);

        for(int i = 1; i <= mMaxLevel; i++){
            for(int j = 0;j < mAttributeCount - 1;j++){
                canvas.drawLine(mWebXArray[j][i],mWebYArray[j][i],mWebXArray[j+1][i],mWebYArray[j+1][i],mPaint);
            }
            canvas.drawLine(mWebXArray[mAttributeCount - 1][i],mWebYArray[mAttributeCount - 1][i],mWebXArray[0][i],mWebYArray[0][i],mPaint);
        }
    }

    /**
     * 设置雷达图的其中一个属性的级别,播放属性值级别改变时的动画
     * @param attributeIndex    属性值的下标
     * @param level     属性的级别
     */
    public void setAttribute(int attributeIndex,int level){
        setAttribute(attributeIndex,level,true);
    }


    /**
     * 设置雷达图的其中一个属性的级别
     * @param attributeIndex    属性值的下标
     * @param level     属性的级别
     * @param isAnim    是否播放属性值级别改变时的动画
     */
    public void setAttribute(int attributeIndex,int level,boolean isAnim){
        if(level < 0 || level > mMaxLevel){
            throw new IllegalArgumentException("this level < 0 || > maxLevel");
        }
        mAttributeLevels[attributeIndex] = level;
        setAttributes(mAttributeLevels,isAnim);
    }

    /**
     * 设置所有属性的级别
     * @param levels 所有属性的新级别,注意输入的数量要与雷达图属性数一致,且级别不能大于此雷达图的最高级别
     */
    public void  setAttributes(int levels[]) {
        setAttributes(levels,true);
    }

    @Override
    protected void onAttachedToWindow() {
        super.onAttachedToWindow();
        setAttributes(mAttributeLevels);
        Log.d("PYJTLK", "setAttribute: ");
    }

    /**
     * 设置所有属性的级别,播放属性值级别改变时的动画
     * @param levels 所有属性的新级别,注意输入的数量要与雷达图属性数一致,且级别不能大于此雷达图的最高级别
     * @param isAnim 是否播放属性值级别改变时的动画
     */
    public void setAttributes(int levels[],boolean isAnim){
        if(levels.length != mAttributeCount){
            throw new IllegalArgumentException("input levels count != attribute count");
        }

        ArrayList<PointF> startPoints = new ArrayList<>();
        ArrayList<PointF> endPoints = new ArrayList<>();

        int level;
        for(int i = 0;i < mAttributeCount;i++){
            startPoints.add(new PointF(mContentXArray[i],mContentYArray[i]));
            level = levels[i];
            if(level < 0 || level > mMaxLevel){
                throw new IllegalArgumentException("this level < 0 || > maxLevel");
            }
            mAttributeLevels[i] = level;
            endPoints.add(new PointF(mWebXArray[i][level],mWebYArray[i][level]));
        }

        if(mAttrAnimator != null && mAttrAnimator.isRunning()){
            mAttrAnimator.cancel();
            mAttrAnimator.removeAllUpdateListeners();
        }

        mAttrAnimator = ValueAnimator.ofObject(mAttrEvaluator,startPoints,endPoints);
        mAttrAnimator.addUpdateListener(mAttrUpdateListener);
        mAttrAnimator.setObjectValues(startPoints,endPoints);
        mAttrAnimator.setDuration(isAnim ? mAnimDuration : 0);
        mAttrAnimator.start();
    }

    /**
     * 在控件脱离Window时（也就是Activity销毁时），取消正在执行的动画，避免内存泄漏
     */
    @Override
    protected void onDetachedFromWindow() {
        super.onDetachedFromWindow();
        mAttrAnimator.cancel();
        mAttrAnimator.removeAllUpdateListeners();
    }

    /**
     * 获取雷达图属性数量
     * @return
     */
    public int getAttributeCount() {
        return mAttributeCount;
    }

    /**
     * 获取雷达图基本线条的颜色值
     * @return
     */
    public int getLineColor() {
        return mLineColor;
    }

    /**
     * 设置雷达图基本线条
     * @param lineColor
     */
    public void setLineColor(int lineColor) {
        mLineColor = lineColor;
        invalidate();
    }

    /**
     * 获取雷达图基本线条透明度
     * @return
     */
    public int getLineAlpha() {
        return mLineAlpha;
    }

    /**
     * 设置雷达图基本线条的透明度
     * @param lineAlpha
     */
    public void setLineAlpha(int lineAlpha) {
        mLineAlpha = lineAlpha;
        invalidate();
    }

    /**
     * 获取雷达图属性多边形的颜色值
     * @return
     */
    public int getContentColor() {
        return mContentColor;
    }

    /**
     * 设置雷达图属性多边形的颜色
     * @param contentColor
     */
    public void setContentColor(int contentColor) {
        mContentColor = mContentColor;
        invalidate();
    }

    /**
     * 获取雷达图属性多边形的透明度
     * @return
     */
    public int getContentAlpha() {
        return mContentAlpha;
    }

    /**
     * 设置雷达图属性多边形的透明度
     * @param contentAlpha
     */
    public void setContentAlpha(int contentAlpha) {
        mContentAlpha = contentAlpha;
        invalidate();
    }

    /**
     * 返回网格是否显示
     * @return
     */
    public boolean isShowWeb() {
        return isShowWeb;
    }

    /**
     * 是否允许显示网格
     * @param showWeb
     */
    public void setShowWeb(boolean showWeb) {
        isShowWeb = showWeb;
        invalidate();
    }

    /**
     * 获取雷达图属性的级别上限
     * @return
     */
    public int getMaxLevel() {
        return mMaxLevel;
    }

    /**
     * 设置网格线的宽度
     * @param lineWidth
     */
    public void setmLineWidth(int lineWidth) {
        mLineWidth = lineWidth;
        invalidate();
    }

    /**
     * 获取属性变化的动画时长
     * @return
     */
    public int getAnimDuration() {
        return mAnimDuration;
    }

    /**
     * 设置属性变化的动画时长
     * @param maAnimDuration
     */
    public void setAnimDuration(int maAnimDuration) {
        mAnimDuration = mAnimDuration;
    }

    /**
     * 获取当前的属性级别的数组
     * @return
     */
    public int[] getAttributeLevels() {
        return mAttributeLevels;
    }
}
