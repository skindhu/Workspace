package com.workspace;
import android.content.Context;
import android.content.res.TypedArray;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.VelocityTracker;
import android.view.View;
import android.view.ViewConfiguration;
import android.view.ViewParent;
import android.widget.FrameLayout;
import android.widget.Scroller;

public class Workspace extends FrameLayout{
	
	protected int defaultScreen;
	protected int currentScreen;
	protected int nextScreen;
	protected int destnationScreen;
	protected float lastMotionX, lastMotionY;
	protected int chilStartPostion;
	protected Scroller scroller;
	protected VelocityTracker velocityTracker;
	protected int touchSlop, minFlingVelocity, maxFlingVelocity;
	
	protected static final int INVALID_SCREEN = -1;
	
	protected static final int TOUCH_STATE_REST = 0;
	protected static final int TOUCH_STATE_SCROLLING = 1;
	
	protected static final int SCROLL_TYPE_HORIZONTAL = 3;
	protected static final int SCROLL_TYPE_VERTICAL = 4;
	
	protected static final double TAN30 = Math.tan(Math.toRadians(30));
	
	protected int touchState = TOUCH_STATE_SCROLLING;
	
	protected int scrollType;
	protected ScrollerManagerInterface scrollerManager; 

	private OnScreenChangeListener onScreenChangeListener;

	public interface OnScreenChangeListener {
		void onScreenChangeStart(int whichScreen);
		void onScrrenChangeEnd(int whichScreen);
	}
	
	public Workspace(Context context, AttributeSet attrs) {
		this(context, attrs, 0);
	}

	public Workspace(Context context, AttributeSet attrs, int defStyle) {
		super(context, attrs, defStyle);
		
		TypedArray a = context.obtainStyledAttributes(attrs, R.styleable.Workspace, defStyle, 0);
		scrollType = a.getInt(R.styleable.Workspace_orientation, SCROLL_TYPE_HORIZONTAL);
		a.recycle();
		initWorkspace();
	}
	
	private void initWorkspace() {
		scroller = new Scroller(getContext());
		currentScreen = defaultScreen;
		ViewConfiguration vc = ViewConfiguration.get(getContext());
		touchSlop = vc.getScaledTouchSlop();
		minFlingVelocity = vc.getScaledMinimumFlingVelocity();
		maxFlingVelocity = vc.getScaledMaximumFlingVelocity();
		if (scrollType == SCROLL_TYPE_HORIZONTAL) {
			scrollerManager = new HorizontalScrollerManager();
		} else if(scrollType == SCROLL_TYPE_VERTICAL) {
			scrollerManager = new VerticalScrollerManager();
		}
	}
	
	
	
	@Override
	public boolean onInterceptTouchEvent(MotionEvent e) {
		int action = e.getAction();
		if (action == MotionEvent.ACTION_MOVE
				&& touchState != TOUCH_STATE_REST) {
			return true;
		}
		float x = e.getX();
		float y = e.getY();
		switch (action) {
		case MotionEvent.ACTION_DOWN:
			lastMotionX = x;
			lastMotionY = y;
			touchState = scroller.isFinished() ? TOUCH_STATE_REST : TOUCH_STATE_SCROLLING;
			break;
		case MotionEvent.ACTION_MOVE:
			scrollerManager.touchMoveIntercept(x, y);
			break;
		case MotionEvent.ACTION_CANCEL:
		case MotionEvent.ACTION_UP:
			touchState = TOUCH_STATE_REST;
			break;
		}
		return touchState != TOUCH_STATE_REST;
	}
	
	@Override
	public boolean onTouchEvent(MotionEvent e) {
		if (getChildCount() < 0) {
			return true;
		}
		if (velocityTracker == null) {
			velocityTracker = VelocityTracker.obtain();
		}
		velocityTracker.addMovement(e);
		float x = e.getX();
		float y = e.getY();
		
		switch (e.getAction()) {
		case MotionEvent.ACTION_DOWN:
			if (!scroller.isFinished()) {
				scroller.abortAnimation();
			}
			final ViewParent parent = getParent();
			if (parent != null) {
				parent.requestDisallowInterceptTouchEvent(true);
			}
			lastMotionX = x;
			lastMotionY = y;
			break;
		case MotionEvent.ACTION_MOVE:
			if (touchState == TOUCH_STATE_SCROLLING && isInValidTouchArea(e.getX(), e.getY())) {
				scrollerManager.touchMove(x, y);
			}
			break;
		case MotionEvent.ACTION_UP:
			if (touchState == TOUCH_STATE_SCROLLING) {
				velocityTracker.computeCurrentVelocity(1000);
				int velocity = scrollerManager.getVelocity(velocityTracker);
				if (isValidPositiveVelocity(velocity) && currentScreen > 0) {
					snapToScreen(currentScreen - 1);
				} else if (isValidNegativeVelocity(velocity)) {
					snapToScreen(currentScreen + 1);
				} else {
					snapToDestination();
				}
				if (velocityTracker != null) {
					velocityTracker.recycle();
					velocityTracker = null;
				}
			}
			touchState = TOUCH_STATE_REST;
			break;
		}
		
		return true;
	}
	
	private boolean isValidPositiveVelocity(int velocity) {
		return velocity >= minFlingVelocity && velocity <= maxFlingVelocity;
	}
	
	private boolean isValidNegativeVelocity(int velocity) {
		return velocity >= -maxFlingVelocity && velocity <= -minFlingVelocity;
	}
	
	private boolean isInValidTouchArea(float x, float y) {
		float localX = x - getLeft();
		float localY = y - getTop();
		return localX >=0 && localX <= getWidth() && localY >= 0 && localY <= getHeight();
	}
	
	
	
	public void snapToScreen(int whichScreen) {
		whichScreen = Math.max(0, Math.min(whichScreen, getChildCount() - 1));
		
		if (destnationScreen != whichScreen) {
			destnationScreen = whichScreen;
			if (onScreenChangeListener != null) {
				onScreenChangeListener.onScreenChangeStart(whichScreen);
			}
		}
		nextScreen = whichScreen;
		scrollerManager.startScroll(whichScreen);
		invalidate();
	}
	
	private void snapToDestination() {
		snapToScreen(scrollerManager.getDestinationScreen());	
	}
	
	@Override
	public void computeScroll() {
		if (scroller.computeScrollOffset()) {
			scrollTo(scroller.getCurrX(), scroller.getCurrY());
			postInvalidate();
		} else if (nextScreen != INVALID_SCREEN) {
			currentScreen = Math.max(0, Math.min(nextScreen, getChildCount() - 1));
			destnationScreen = currentScreen;
			nextScreen = INVALID_SCREEN;
			if (onScreenChangeListener != null) {
				onScreenChangeListener.onScrrenChangeEnd(currentScreen);
			}
		}
	}
	
	@Override
	protected void onLayout(boolean changed, int left, int top, int right, int bottom) {
		chilStartPostion = 0;
		int count = getChildCount();
		for (int i = 0; i < count; i++) {
			View child = getChildAt(i);
			child.setClickable(true);
			if (child.getVisibility() != View.GONE) {
				scrollerManager.layout(child);
			}
		}
	}
	
	 public interface ScrollerManagerInterface {
		public void touchMoveIntercept(float x, float y);
		public void touchMove(float x, float y);
		public int getVelocity(VelocityTracker velocityTracker);
		public void layout(View child);
		public void startScroll(int whichScreen);
		public int getDestinationScreen();
	}
	
	 public class HorizontalScrollerManager implements ScrollerManagerInterface {

		@Override
		public void touchMoveIntercept(float x, float y) {
			int xDiff = (int) Math.abs(x - lastMotionX);
			int yDiff = (int) Math.abs(y - lastMotionY);
			boolean xMoved = xDiff > touchSlop;
			double tan = yDiff / (double) xDiff;
			if (xMoved && tan < TAN30) {
				touchState = TOUCH_STATE_SCROLLING;
				lastMotionX = x;
			}
		}

		@Override
		public void touchMove(float x, float y) {
			int deltaX = (int)(lastMotionX - x);
			if (deltaX > 0 && currentScreen == getChildCount() - 1) {
				if (onScreenChangeListener != null) {
					onScreenChangeListener.onScrrenChangeEnd(getChildCount());
				}
				return;
			}
			lastMotionX = x;
			if (deltaX < 0) {
				if (getScrollX() > 0) {
					scrollBy(Math.max(-getScrollX(), deltaX), 0);
				} else {
					scrollBy(deltaX / 3, 0);
				}
			} else if (deltaX > 0) {
				int avaliblerToScroll = getChildAt(getChildCount() - 1).getRight() - getScrollX() - getWidth();
				if (avaliblerToScroll > 0) {
					scrollBy(Math.min(avaliblerToScroll, deltaX), 0);
				} else {
					scrollBy(deltaX /3, 0);
				}
			}			
			
		}

		@Override
		public int getVelocity(VelocityTracker velocityTracker) {
			return (int) velocityTracker.getXVelocity();
		}

		@Override
		public void layout(View child) {
			int childWidth = child.getMeasuredWidth();
			child.layout(chilStartPostion, 0, chilStartPostion + childWidth, child.getMeasuredHeight());
			chilStartPostion += childWidth;		
		}

		@Override
		public void startScroll(int whichScreen) {
			int newX = whichScreen * getWidth();
			int delta = newX - getScrollX();
			scroller.startScroll(getScrollX(), 0, delta, 0, Math.abs(delta));			
		}

		@Override
		public int getDestinationScreen() {
			int screenWidth = getWidth();
			return (getScrollX() + (screenWidth / 2)) / screenWidth;	
		} 
	 }
	 
	 public class VerticalScrollerManager implements ScrollerManagerInterface {

		@Override
		public void touchMoveIntercept(float x, float y) {
			int xDiff = (int) Math.abs(x - lastMotionX);
			int yDiff = (int) Math.abs(y - lastMotionY);
			boolean yMoved = yDiff > touchSlop;
			double tan = xDiff / (double) yDiff;
			if (yMoved && tan < TAN30) {
				touchState = TOUCH_STATE_SCROLLING;
				lastMotionY = y;					
			}
		}

		@Override
		public void touchMove(float x, float y) {
			int deltaY = (int)(lastMotionY - y);
			if (deltaY > 0 && currentScreen == getChildCount() - 1) {
				if (onScreenChangeListener != null) {
					onScreenChangeListener.onScrrenChangeEnd(getChildCount());
				}
				return;
			}
			lastMotionY = y;
			if (deltaY < 0) {
				if (getScrollY() > 0) {
					scrollBy(0, Math.max(-getScrollY(), deltaY));
				} else {
					scrollBy(0, deltaY / 3);
				}
			} else if (deltaY > 0) {
				int avaliblerToScroll = getChildAt(getChildCount() - 1).getBottom() - getScrollY() - getHeight();
				if (avaliblerToScroll > 0) {
					scrollBy(0, Math.min(avaliblerToScroll, deltaY));
				} else {
					scrollBy(0, deltaY /3);
				}
			}			
		}

		@Override
		public int getVelocity(VelocityTracker velocityTracker) {
			return (int) velocityTracker.getYVelocity();
		}

		@Override
		public void layout(View child) {
			int childHeight = child.getMeasuredHeight();
			child.layout(0, chilStartPostion , child.getMeasuredWidth(), chilStartPostion+childHeight);
			chilStartPostion += childHeight;
		}

		@Override
		public void startScroll(int whichScreen) {
			int newY = whichScreen * getHeight();
			int delta = newY - getScrollY();
			scroller.startScroll(0, getScrollY(), 0, delta, Math.abs(delta));
			
		}

		@Override
		public int getDestinationScreen() {
			int screenHeight = getHeight();
			return (getScrollY() + (screenHeight / 2)) / screenHeight;
		}
		 
	 }

}
