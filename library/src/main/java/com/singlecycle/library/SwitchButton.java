package com.singlecycle.library;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.PaintFlagsDrawFilter;
import android.graphics.Rect;
import android.graphics.RectF;
import android.graphics.drawable.Drawable;
import android.support.annotation.NonNull;
import android.util.AttributeSet;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewConfiguration;
import android.view.animation.DecelerateInterpolator;
import android.widget.Scroller;

import java.lang.Math;
import java.lang.Override;


public class SwitchButton extends View {

    public enum STATUS {
        ON, OFF
    }

    private int[] mPressedState = new int[]{android.R.attr.state_pressed};
    private int[] mUnPressedState = new int[]{-android.R.attr.state_pressed};
    private int mCursorMoveDuration;
    private int mSelectedBackground;
    private int mUnselectedBackground;
    private int mTrackWidth;
    private int mTrackPadding;
    private int mCursorTouchExpand;
    private float mCursorLocation;
    private float mCursorRightBoundary;
    private float mCursorLeftBoundary;
    private float mTrackRadius;
    private int mLastX;
    private boolean mPressed;
    private boolean mClicked;
    private boolean mIsBeingDragged;

    private STATUS mStatus;
    private Drawable mCursor;
    private Rect mCursorRect;
    private Rect mCursorTouchRect;
    private RectF mTrackRectF;
    private RectF mTrackSelectedRectF;
    private Paint mPaint;
    private Scroller mScroll;
    private PaintFlagsDrawFilter mPaintFlagsDrawFilter;
    private OnStatusChangeListener mListener;

    private static int TOUCH_SLOP;
    private static final int DEFAULT_DURATION = 200;
    private static final int DEFAULT_SELECTED_BACKGROUND = Color.rgb(252, 87, 119);
    private static final int DEFAULT_UNSELECTED_BACKGROUND = Color.rgb(238, 238, 238);
    private static final int DEFAULT_TOUCH_EXPAND = 20;

    public SwitchButton(Context context) {
        this(context, null, 0);
    }

    public SwitchButton(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public SwitchButton(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);

        if (attrs == null) {
            return;
        }

        TypedArray array = context.obtainStyledAttributes(attrs, R.styleable.SwitchButton);

        mCursorMoveDuration = array.getInt(R.styleable.SwitchButton_moveDuration, DEFAULT_DURATION);
        mSelectedBackground = array.getColor(R.styleable.SwitchButton_selectedBackground, DEFAULT_SELECTED_BACKGROUND);
        mUnselectedBackground = array.getColor(R.styleable.SwitchButton_unselectedBackground, DEFAULT_UNSELECTED_BACKGROUND);
        mCursor = array.getDrawable(R.styleable.SwitchButton_cursor);
        if (null != mCursor)
            mTrackWidth = (int) array.getDimension(R.styleable.SwitchButton_trackWidth, mCursor.getIntrinsicWidth());
        mTrackPadding = (int) array.getDimension(R.styleable.SwitchButton_trackPadding, 0);
        mCursorTouchExpand = (int) array.getDimension(R.styleable.SwitchButton_cursorTouchExpand, DEFAULT_TOUCH_EXPAND);
        mStatus = STATUS.values()[array.getInt(R.styleable.SwitchButton_status, 0)];

        array.recycle();

        mCursorRect = new Rect();
        mCursorTouchRect = new Rect();
        mTrackRectF = new RectF();
        mTrackSelectedRectF = new RectF();

        mPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        mScroll = new Scroller(context, new DecelerateInterpolator());
        mPaintFlagsDrawFilter = new PaintFlagsDrawFilter(0, Paint.ANTI_ALIAS_FLAG | Paint.FILTER_BITMAP_FLAG);
        TOUCH_SLOP = ViewConfiguration.get(context).getScaledTouchSlop();

        setWillNotDraw(false);
        setFocusable(true);
        setClickable(true);
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        if (null == mCursor) {
            super.onMeasure(widthMeasureSpec, heightMeasureSpec);
            return;
        }
        final int paddingLeft = getPaddingLeft();
        final int paddingTop = getPaddingTop();
        final int paddingRight = getPaddingRight();
        final int paddingBottom = getPaddingBottom();
        final int cursorHeight = mCursor.getIntrinsicHeight();
        final int cursorWidth = mCursor.getIntrinsicWidth();
        int widthNeed = mTrackWidth + paddingLeft + paddingRight;
        int heightNeed = cursorHeight + paddingTop + paddingBottom + mTrackPadding * 2;
        mCursorLeftBoundary = paddingLeft + mTrackPadding;
        mCursorRightBoundary = paddingLeft + mTrackWidth - cursorWidth - mTrackPadding;
        mTrackRectF.left = paddingLeft;
        mTrackRectF.top = paddingTop;
        mTrackRectF.right = mTrackRectF.left + mTrackWidth;
        mTrackRectF.bottom = mTrackRectF.top + cursorHeight + mTrackPadding * 2;
        if (mStatus == STATUS.OFF) {
            mCursorLocation = mCursorLeftBoundary;
        } else {
            mCursorLocation = mCursorRightBoundary;
        }
        mTrackSelectedRectF.top = mTrackRectF.top;
        mTrackSelectedRectF.bottom = mTrackRectF.bottom;
        mTrackSelectedRectF.left = mTrackRectF.left;
        mCursorRect.top = paddingTop + mTrackPadding;
        mCursorRect.bottom = mCursorRect.top + cursorHeight;
        mCursorTouchRect.top = mCursorRect.top - mCursorTouchExpand;
        mCursorTouchRect.bottom = mCursorRect.bottom + mCursorTouchExpand;
        mTrackRadius = (mTrackRectF.bottom - mTrackRectF.top) / 2;
        widthMeasureSpec = MeasureSpec.makeMeasureSpec(widthNeed, MeasureSpec.EXACTLY);
        heightMeasureSpec = MeasureSpec.makeMeasureSpec(heightNeed, MeasureSpec.EXACTLY);
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        if (null == mCursor) return;
        canvas.setDrawFilter(mPaintFlagsDrawFilter);
        float trackRadius = mTrackRadius;
        mPaint.reset();
        if (mCursorLocation == mCursorLeftBoundary) {
            mPaint.setColor(mUnselectedBackground);
            canvas.drawRoundRect(mTrackRectF, trackRadius, trackRadius, mPaint);
        } else if (mCursorLocation == mCursorRightBoundary) {
            mPaint.setColor(mSelectedBackground);
            canvas.drawRoundRect(mTrackRectF, trackRadius, trackRadius, mPaint);
        } else {
            mPaint.setColor(mUnselectedBackground);
            canvas.drawRoundRect(mTrackRectF, trackRadius, trackRadius, mPaint);
            mTrackSelectedRectF.right = mCursorLocation + mCursor.getIntrinsicWidth();
            mPaint.setColor(mSelectedBackground);
            canvas.drawRoundRect(mTrackSelectedRectF, trackRadius, trackRadius, mPaint);
        }
        mCursorRect.left = (int) mCursorLocation;
        mCursorRect.right = (int) (mCursorLocation + mCursor.getIntrinsicWidth());
        mCursorTouchRect.left = mCursorRect.left - mCursorTouchExpand;
        mCursorTouchRect.right = mCursorRect.right + mCursorTouchExpand;
        mCursor.setBounds(mCursorRect);
        mCursor.draw(canvas);
        canvas.save();
    }

    @Override
    public boolean onTouchEvent(@NonNull MotionEvent event) {
        if (mCursor == null) {
            return super.onTouchEvent(event);
        }

        final int action = event.getAction();

        switch (action) {
            case MotionEvent.ACTION_DOWN:
                handleTouchDown(event);
                break;
            case MotionEvent.ACTION_MOVE:
                handleTouchMove(event);
                break;
            case MotionEvent.ACTION_CANCEL:
            case MotionEvent.ACTION_UP:
                handleTouchUp();
                mLastX = 0;
                mPressed = false;
                mClicked = false;
                mIsBeingDragged = false;
                break;
        }

        return super.onTouchEvent(event);
    }

    private void handleTouchDown(MotionEvent e) {
        final int x = (int) e.getX();
        final int y = (int) e.getY();
        mClicked = true;
        if (mCursorTouchRect.contains(x, y)) {
            mCursor.setState(mPressedState);
            mCursor.invalidateSelf();
            mPressed = true;

            if (mScroll.computeScrollOffset()) {
                mScroll.abortAnimation();
            }
            if (getParent() != null) {
                getParent().requestDisallowInterceptTouchEvent(true);
            }
        }
        mLastX = x;
        invalidate();
    }

    private void handleTouchMove(MotionEvent e) {
        final int x = (int) e.getX();
        if (!mPressed) {
            return;
        }
        final int deltaX = x - mLastX;
        mLastX = x;
        if (!mIsBeingDragged) {
            if (Math.abs(deltaX) > TOUCH_SLOP) {
                mIsBeingDragged = true;
                mClicked = false;
            } else {
                return;
            }
        }

        if (mCursorLocation + deltaX <= mCursorLeftBoundary) {
            mCursorLocation = mCursorLeftBoundary;
            invalidate();
            return;
        } else if (mCursorLocation + deltaX >= mCursorRightBoundary) {
            mCursorLocation = mCursorRightBoundary;
            invalidate();
            return;
        }

        mCursorLocation += deltaX;
        invalidate();
    }

    private void handleTouchUp() {
        mCursor.setState(mUnPressedState);
        mCursor.invalidateSelf();
        if (getParent() != null) {
            getParent().requestDisallowInterceptTouchEvent(false);
        }
        if (mClicked) {
            if (mStatus == STATUS.ON) {
                mScroll.startScroll((int) mCursorLocation, 0, (int) (mCursorLeftBoundary - mCursorLocation), mCursorMoveDuration);
                changeStatus();
            } else {
                mScroll.startScroll((int) mCursorLocation, 0, (int) (mCursorRightBoundary - mCursorLocation), mCursorMoveDuration);
                changeStatus();
            }

            invalidate();
            return;
        }

        final float cursorTrackWidth = mCursorRightBoundary - mCursorLeftBoundary;

        if (mCursorLocation == mCursorLeftBoundary) {
            changeStatus();
            return;
        } else if (mCursorLocation == mCursorRightBoundary) {
            changeStatus();
            return;
        }
        if (mCursorLocation < cursorTrackWidth / 2) {
            mScroll.startScroll((int) mCursorLocation, 0, (int) (mCursorLeftBoundary - mCursorLocation), mCursorMoveDuration);
            changeStatus();
        } else {
            mScroll.startScroll((int) mCursorLocation, 0, (int) (mCursorRightBoundary - mCursorLocation), mCursorMoveDuration);
            changeStatus();
        }

        invalidate();
    }

    private void changeStatus() {
        if (mStatus == STATUS.OFF) {
            if (mListener != null) {
                mListener.onChange(STATUS.ON);
            }
            mStatus = STATUS.ON;
        } else {
            if (mListener != null) {
                mListener.onChange(STATUS.OFF);
            }
            mStatus = STATUS.OFF;
        }
    }

    @Override
    public void computeScroll() {
        if (mScroll.computeScrollOffset()) {
            mCursorLocation = mScroll.getCurrX();
            invalidate();
        }

        super.computeScroll();
    }

    public void setOnStatusChangeListener(OnStatusChangeListener listener) {
        mListener = listener;
    }

    public void setStatus(STATUS status) {
        if (mStatus == status) {
            return;
        }
        if (mCursorRightBoundary > 0) {
            if (status == STATUS.ON) {
                mScroll.startScroll((int) mCursorLocation, 0, (int) (mCursorRightBoundary - mCursorLocation), mCursorMoveDuration);
            } else {
                mScroll.startScroll((int) mCursorLocation, 0, (int) (mCursorLeftBoundary - mCursorLocation), mCursorMoveDuration);
            }
        }

        mStatus = status;
        if (mListener != null) {
            mListener.onChange(status);
        }

        invalidate();
    }

    public static interface OnStatusChangeListener {
        void onChange(STATUS status);
    }
}
