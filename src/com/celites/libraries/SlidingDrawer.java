/*
 * Copyright (C) 2008 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.celites.libraries;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Rect;
import android.os.Handler;
import android.os.Message;
import android.os.SystemClock;
import android.util.AttributeSet;
import android.util.Log;
import android.view.MotionEvent;
import android.view.SoundEffectConstants;
import android.view.VelocityTracker;
import android.view.View;
import android.view.ViewGroup;

import com.celites.libraries.customslidingdrawer.R;

/**
 * SlidingDrawer hides content out of the screen and allows the user to drag a
 * handle to bring the content on screen. SlidingDrawer can be used vertically
 * or horizontally.
 * 
 * A special widget composed of two children views: the handle, that the users
 * drags, and the content, attached to the handle and dragged with it.
 * 
 * SlidingDrawer should be used as an overlay inside layouts. This means
 * SlidingDrawer should only be used inside of a FrameLayout or a RelativeLayout
 * for instance. The size of the SlidingDrawer defines how much space the
 * content will occupy once slid out so SlidingDrawer should usually use
 * fill_parent for both its dimensions.
 * 
 * Inside an XML layout, SlidingDrawer must define the id of the handle and of
 * the content:
 * 
 * <pre class="prettyprint">
 * &lt;SlidingDrawer
 *     android:id="@+id/drawer"
 *     android:layout_width="fill_parent"
 *     android:layout_height="fill_parent"
 * 
 *     android:handle="@+id/handle"
 *     android:content="@+id/content"&gt;
 * 
 *     &lt;ImageView
 *         android:id="@id/handle"
 *         android:layout_width="88dip"
 *         android:layout_height="44dip" /&gt;
 * 
 *     &lt;GridView
 *         android:id="@id/content"
 *         android:layout_width="fill_parent"
 *         android:layout_height="fill_parent" /&gt;
 * 
 * &lt;/SlidingDrawer&gt;
 * </pre>
 * 
 * @attr ref android.R.styleable#SlidingDrawer_content
 * @attr ref android.R.styleable#SlidingDrawer_handle
 * @attr ref android.R.styleable#SlidingDrawer_topOffset
 * @attr ref android.R.styleable#SlidingDrawer_bottomOffset
 * @attr ref android.R.styleable#SlidingDrawer_orientation
 * @attr ref android.R.styleable#SlidingDrawer_allowSingleTap
 * @attr ref android.R.styleable#SlidingDrawer_animateOnClick
 */
public class SlidingDrawer extends ViewGroup {
	/**
	 * @author Prasham
	 * 
	 */

	public static final int DIRECTION_LEFT_TO_RIGHT = 0;
	public static final int DIRECTION_RIGHT_TO_LEFT = 1;
	public static final int DIRECTION_TOP_TO_BOTTOM = 2;
	public static final int DIRECTION_BOTTOM_TO_TOP = 3;

	public static final int ORIENTATION_HORIZONTAL = 0;
	public static final int ORIENTATION_VERTICAL = 1;

	private static final int TAP_THRESHOLD = 6;
	private static final float MAXIMUM_TAP_VELOCITY = 100.0f;
	private static final float MAXIMUM_MINOR_VELOCITY = 150.0f;
	private static final float MAXIMUM_MAJOR_VELOCITY = 200.0f;
	private static final float MAXIMUM_ACCELERATION = 2000.0f;
	private static final int VELOCITY_UNITS = 1000;
	private static final int MSG_ANIMATE = 1000;
	private static final int ANIMATION_FRAME_DURATION = 1000 / 60;

	private static final int EXPANDED_FULL_OPEN = -10001;
	private static final int COLLAPSED_FULL_CLOSED = -10002;

	private final int mHandleId;
	private final int mContentId;

	private View mHandle;
	private View mContent;

	private final Rect mFrame = new Rect();
	private final Rect mInvalidate = new Rect();
	private boolean mTracking;
	private boolean mLocked;

	private VelocityTracker mVelocityTracker;

	private boolean mVertical;
	private boolean mExpanded;
	private int mBottomOffset;
	private int mTopOffset;
	private int mHandleHeight;
	private int mHandleWidth;
	private int mDirection;

	private OnDrawerOpenListener mOnDrawerOpenListener;
	private OnDrawerCloseListener mOnDrawerCloseListener;
	private OnDrawerScrollListener mOnDrawerScrollListener;

	private final Handler mHandler = new SlidingHandler();
	private float mAnimatedAcceleration;
	private float mAnimatedVelocity;
	private float mAnimationPosition;
	private long mAnimationLastTime;
	private long mCurrentAnimationTime;
	private int mTouchDelta;
	private boolean mAnimating;
	private boolean mAllowSingleTap;
	private boolean mAnimateOnClick;

	private final int mTapThreshold;
	private final int mMaximumTapVelocity;
	private final int mMaximumMinorVelocity;
	private final int mMaximumMajorVelocity;
	private final int mMaximumAcceleration;
	private final int mVelocityUnits;
	private boolean mcanHandleOnClick;

	/**
	 * Callback invoked when the drawer is opened.
	 */
	public static interface OnDrawerOpenListener {
		/**
		 * Invoked when the drawer becomes fully open.
		 */
		public void onDrawerOpened();
	}

	/**
	 * Callback invoked when the drawer is closed.
	 */
	public static interface OnDrawerCloseListener {
		/**
		 * Invoked when the drawer becomes fully closed.
		 */
		public void onDrawerClosed();
	}

	/**
	 * Callback invoked when the drawer is scrolled.
	 */
	public static interface OnDrawerScrollListener {
		/**
		 * Invoked when the user starts dragging/flinging the drawer's handle.
		 */
		public void onScrollStarted();

		/**
		 * Invoked when the user stops dragging/flinging the drawer's handle.
		 */
		public void onScrollEnded();
	}

	/**
	 * Creates a new SlidingDrawer from a specified set of attributes defined in
	 * XML.
	 * 
	 * @param context
	 *            The application's environment.
	 * @param attrs
	 *            The attributes defined in XML.
	 */
	public SlidingDrawer(Context context, AttributeSet attrs) {
		this(context, attrs, 0);
	}

	@Override
	public boolean isInEditMode() {
		// TODO Auto-generated method stub
		return super.isInEditMode();
	}

	/**
	 * Creates a new SlidingDrawer from a specified set of attributes defined in
	 * XML.
	 * 
	 * @param context
	 *            The application's environment.
	 * @param attrs
	 *            The attributes defined in XML.
	 * @param defStyle
	 *            The style to apply to this widget.
	 */
	public SlidingDrawer(Context context, AttributeSet attrs, int defStyle) {
		super(context, attrs, defStyle);
		TypedArray a = context.obtainStyledAttributes(attrs,
				R.styleable.MySlider);

		int orientation = a.getInt(
				R.styleable.MySlider_SlidingDrawer_orientation,
				ORIENTATION_VERTICAL);
		mDirection = a.getInt(R.styleable.MySlider_SlidingDrawer_direction,
				DIRECTION_LEFT_TO_RIGHT);
		mVertical = (mDirection == DIRECTION_TOP_TO_BOTTOM || mDirection == DIRECTION_BOTTOM_TO_TOP);

		mBottomOffset = (int) a.getDimension(
				R.styleable.MySlider_SlidingDrawer_bottomOffset, 0.0f);
		mTopOffset = (int) a.getDimension(
				R.styleable.MySlider_SlidingDrawer_topOffset, 0.0f);
		int test = (int) a.getDimension(
				R.styleable.MySlider_SlidingDrawer_maxHeight, 0.0f);
		mAllowSingleTap = a.getBoolean(
				R.styleable.MySlider_SlidingDrawer_allowSingleTap, true);
		mAnimateOnClick = a.getBoolean(
				R.styleable.MySlider_SlidingDrawer_animateOnClick, true);
		mcanHandleOnClick = a.getBoolean(
				R.styleable.MySlider_SlidingDrawer_handleOnClick, true);

		int handleId = a.getResourceId(
				R.styleable.MySlider_SlidingDrawer_handle, 0);
		if (handleId == 0) {
			throw new IllegalArgumentException(
					"The handle attribute is required and must refer "
							+ "to a valid child.");
		}

		int contentId = a.getResourceId(
				R.styleable.MySlider_SlidingDrawer_content, 0);
		if (contentId == 0) {
			throw new IllegalArgumentException(
					"The handle attribute is required and must refer "
							+ "to a valid child.");
		}

		mHandleId = handleId;
		mContentId = contentId;

		final float density = getResources().getDisplayMetrics().density;
		mTapThreshold = (int) (TAP_THRESHOLD * density + 0.5f);
		mMaximumTapVelocity = (int) (MAXIMUM_TAP_VELOCITY * density + 0.5f);
		mMaximumMinorVelocity = (int) (MAXIMUM_MINOR_VELOCITY * density + 0.5f);
		mMaximumMajorVelocity = (int) (MAXIMUM_MAJOR_VELOCITY * density + 0.5f);
		mMaximumAcceleration = (int) (MAXIMUM_ACCELERATION * density + 0.5f);
		mVelocityUnits = (int) (VELOCITY_UNITS * density + 0.5f);

		a.recycle();

		setAlwaysDrawnWithCacheEnabled(false);
	}

	@Override
	protected void onFinishInflate() {
		mHandle = findViewById(mHandleId);
		if (mHandle == null) {
			throw new IllegalArgumentException(
					"The handle attribute is must refer to an"
							+ " existing child.");
		}
		if (mcanHandleOnClick)
			mHandle.setOnClickListener(new DrawerToggler());

		mContent = findViewById(mContentId);
		if (mContent == null) {
			throw new IllegalArgumentException(
					"The content attribute is must refer to an"
							+ " existing child.");
		}

		mContent.setVisibility(View.GONE);
	}

	@Override
	protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
		int widthSpecMode = MeasureSpec.getMode(widthMeasureSpec);
		int widthSpecSize = MeasureSpec.getSize(widthMeasureSpec);

		int heightSpecMode = MeasureSpec.getMode(heightMeasureSpec);
		int heightSpecSize = MeasureSpec.getSize(heightMeasureSpec);

		if (widthSpecMode == MeasureSpec.UNSPECIFIED
				|| heightSpecMode == MeasureSpec.UNSPECIFIED) {
			// throw new RuntimeException(
			// "SlidingDrawer cannot have UNSPECIFIED dimensions");
			Log.w("Custom Sliding Drawer",
					"SlidingDrawer cannot have UNSPECIFIED dimensions");
		}

		final View handle = mHandle;
		final View content = mContent;
		measureChild(handle, widthMeasureSpec, heightMeasureSpec);

		if (mVertical) {
			int height = heightSpecSize - handle.getMeasuredHeight()
					- mTopOffset;
			content.measure(widthMeasureSpec,
					MeasureSpec.makeMeasureSpec(height, heightSpecMode));
			heightSpecSize = handle.getMeasuredHeight() + mTopOffset
					+ content.getMeasuredHeight();
			widthSpecSize = content.getMeasuredWidth();
			if (handle.getMeasuredWidth() > widthSpecSize)
				widthSpecSize = handle.getMeasuredWidth();
		} else {
			int width = widthSpecSize - handle.getMeasuredWidth() - mTopOffset;
			getContent().measure(
					MeasureSpec.makeMeasureSpec(width, widthSpecMode),
					heightMeasureSpec);
			widthSpecSize = handle.getMeasuredWidth() + mTopOffset
					+ content.getMeasuredWidth();
			heightSpecSize = content.getMeasuredHeight();
			if (handle.getMeasuredHeight() > heightSpecSize)
				heightSpecSize = handle.getMeasuredHeight();
		}

		setMeasuredDimension(widthSpecSize, heightSpecSize);
	}

	@Override
	protected void dispatchDraw(Canvas canvas) {
		final long drawingTime = getDrawingTime();
		final View handle = mHandle;
		final boolean isVertical = mVertical;

		drawChild(canvas, handle, drawingTime);

		if (mTracking || mAnimating) {
			final Bitmap cache = mContent.getDrawingCache();
			if (cache != null) {

				if (mDirection == DIRECTION_BOTTOM_TO_TOP)
					canvas.drawBitmap(cache, 0, handle.getBottom(), null);
				else if (mDirection == DIRECTION_RIGHT_TO_LEFT)
					canvas.drawBitmap(cache, handle.getRight(), 0, null);

				else if (mDirection == DIRECTION_LEFT_TO_RIGHT)
					canvas.drawBitmap(cache,
							handle.getLeft() - cache.getWidth(), 0, null);
				else
					canvas.drawBitmap(cache, 0, handle.getTop()
							- (getBottom() - getTop()) + mHandleHeight, null);
			} else {
				canvas.save();

				if (mDirection == DIRECTION_BOTTOM_TO_TOP)
					canvas.translate(0, handle.getTop() - mTopOffset);
				else if (mDirection == DIRECTION_RIGHT_TO_LEFT)
					canvas.translate(handle.getLeft() - mTopOffset, 0);

				else if (mDirection == DIRECTION_LEFT_TO_RIGHT)
					canvas.translate(
							handle.getLeft() - mTopOffset
									- mContent.getMeasuredWidth(), 0);
				else
					canvas.translate(
							0,
							handle.getTop() - mTopOffset
									- mContent.getMeasuredHeight());

				drawChild(canvas, mContent, drawingTime);
				canvas.restore();
			}
		} else if (mExpanded) {
			drawChild(canvas, mContent, drawingTime);
		}
	}

	@Override
	protected void onLayout(boolean changed, int l, int t, int r, int b) {
		if (mTracking) {
			return;
		}

		final int width = r - l;
		final int height = b - t;

		final View handle = mHandle;

		int childWidth = handle.getMeasuredWidth();
		int childHeight = handle.getMeasuredHeight();

		int childLeft;
		int childTop;
		int childRight = 0;
		int childBottom = 0;

		final View content = mContent;

		if (mDirection == DIRECTION_BOTTOM_TO_TOP) {
			childLeft = (width - childWidth) / 2;
			childTop = mExpanded ? mTopOffset : height - childHeight
					+ mBottomOffset;
			content.layout(0, mTopOffset + childHeight,
					content.getMeasuredWidth(), mTopOffset + childHeight
							+ content.getMeasuredHeight());
		} else if (mDirection == DIRECTION_RIGHT_TO_LEFT) {
			childTop = (height - childHeight) / 2;
			childLeft = mExpanded ? mTopOffset : width - childWidth
					+ mBottomOffset;

			content.layout(mTopOffset + childWidth, 0, mTopOffset + childWidth
					+ content.getMeasuredWidth(), content.getMeasuredHeight());
		} else if (mDirection == DIRECTION_TOP_TO_BOTTOM) {
			childLeft = (width - childWidth) / 2;
			childTop = mExpanded ? height - mBottomOffset - childHeight
					: mTopOffset;

			content.layout(0, mTopOffset, content.getMeasuredWidth(),
					mTopOffset + content.getMeasuredHeight());

		} else {

			childTop = (height - childHeight) / 2;
			childLeft = mExpanded ? width - mBottomOffset - childWidth
					: mTopOffset;

			content.layout(mTopOffset, 0,
					mTopOffset + content.getMeasuredWidth(),
					content.getMeasuredHeight());

		}

		handle.layout(childLeft, childTop, childLeft + childWidth, childTop
				+ childHeight);
		mHandleHeight = handle.getHeight();
		mHandleWidth = handle.getWidth();
	}

	@Override
	public boolean onInterceptTouchEvent(MotionEvent event) {
		if (mLocked) {
			return false;
		}

		final int action = event.getAction();

		float x = event.getX();
		float y = event.getY();

		final Rect frame = mFrame;
		final View handle = mHandle;

		handle.getHitRect(frame);
		if (!mTracking && !frame.contains((int) x, (int) y)) {
			return false;
		}

		if (action == MotionEvent.ACTION_DOWN) {
			mTracking = true;

			handle.setPressed(true);
			// Must be called before prepareTracking()
			prepareContent();

			// Must be called after prepareContent()
			if (mOnDrawerScrollListener != null) {
				mOnDrawerScrollListener.onScrollStarted();
			}

			if (mVertical) {
				final int top = mHandle.getTop();
				mTouchDelta = (int) y - top;
				prepareTracking(top);
			} else {
				final int left = mHandle.getLeft();
				mTouchDelta = (int) x - left;
				prepareTracking(left);
			}
			mVelocityTracker.addMovement(event);
		}

		return true;
	}

	@Override
	public boolean onTouchEvent(MotionEvent event) {
		if (mLocked) {
			return true;
		}

		if (mTracking) {
			mVelocityTracker.addMovement(event);
			final int action = event.getAction();
			switch (action) {
			case MotionEvent.ACTION_MOVE:
				int position = (int) (mVertical ? event.getY() : event.getX())
						- mTouchDelta;
				moveHandle(position);
				break;
			case MotionEvent.ACTION_UP:
			case MotionEvent.ACTION_CANCEL: {
				final VelocityTracker velocityTracker = mVelocityTracker;
				velocityTracker.computeCurrentVelocity(mVelocityUnits);

				float yVelocity = velocityTracker.getYVelocity();
				float xVelocity = velocityTracker.getXVelocity();
				boolean negative;

				final boolean vertical = mVertical;

				if (mDirection == DIRECTION_BOTTOM_TO_TOP) {
					negative = yVelocity < 0;
					if (xVelocity < 0) {
						xVelocity = -xVelocity;
					}
					if (xVelocity > mMaximumMinorVelocity) {
						xVelocity = mMaximumMinorVelocity;
					}
				} else if (mDirection == DIRECTION_RIGHT_TO_LEFT) {
					negative = xVelocity < 0;
					if (yVelocity < 0) {
						yVelocity = -yVelocity;
					}
					if (yVelocity > mMaximumMinorVelocity) {
						yVelocity = mMaximumMinorVelocity;
					}
				} else if (mDirection == DIRECTION_LEFT_TO_RIGHT) {
					negative = xVelocity < 0;
					if (yVelocity < 0) {
						yVelocity = -yVelocity;
					}
					if (yVelocity < mMaximumMinorVelocity) {
						yVelocity = mMaximumMinorVelocity;
					}
				} else {
					negative = yVelocity < 0;
					if (xVelocity < 0) {
						xVelocity = -xVelocity;
					}
					if (xVelocity < mMaximumMinorVelocity) {
						xVelocity = mMaximumMinorVelocity;
					}
				}

				float velocity = (float) Math.hypot(xVelocity, yVelocity);
				if (negative) {
					velocity = -velocity;
				}
				boolean invert = (mDirection == DIRECTION_LEFT_TO_RIGHT || mDirection == DIRECTION_TOP_TO_BOTTOM);
				final int handleTop = mHandle.getTop();
				final int handleLeft = mHandle.getLeft();
				final int handleBottom = mHandle.getBottom();
				final int handleRight = mHandle.getRight();

				if (Math.abs(velocity) < mMaximumTapVelocity) {
					boolean c1 = false;
					boolean c2 = false;
					boolean c3 = false;
					boolean c4 = false;

					if (invert) {
						// /conditions for vertical - bottom to top
						c1 = (mExpanded && (getBottom() - handleBottom) < mTapThreshold
								+ mBottomOffset);
						// /conditions for vertical
						c2 = (!mExpanded && handleTop < mTopOffset
								+ mHandleHeight - mTapThreshold);

						// condition for horizontal
						c3 = (mExpanded && (getRight() - handleRight) < mTapThreshold
								+ mBottomOffset);
						// condition for horizontal
						c4 = (!mExpanded && handleLeft > mTopOffset
								+ mHandleWidth + mTapThreshold);
					} else {
						// /conditions for vertical top to bottom
						c1 = (mExpanded && handleTop < mTapThreshold
								+ mTopOffset);
						// /conditions for vertical
						c2 = (!mExpanded && handleTop > mBottomOffset
								+ getBottom() - getTop() - mHandleHeight
								- mTapThreshold);

						// condition for horizontal
						c3 = (mExpanded && handleLeft < mTapThreshold
								+ mTopOffset);
						// condition for horizontal
						c4 = (!mExpanded && handleLeft > mBottomOffset
								+ getRight() - getLeft() - mHandleWidth
								- mTapThreshold);
					}

					if (vertical ? c1 || c2 : c3 || c4) {

						if (mAllowSingleTap) {
							playSoundEffect(SoundEffectConstants.CLICK);

							if (mExpanded) {
								animateClose(vertical ? handleTop : handleLeft);
							} else {
								animateOpen(vertical ? handleTop : handleLeft);
							}
						} else {
							performFling(vertical ? handleTop : handleLeft,
									velocity, false);
						}
					} else {
						performFling(vertical ? handleTop : handleLeft,
								velocity, false);
					}
				} else {
					performFling(vertical ? handleTop : handleLeft, velocity,
							false);
				}
			}
				break;
			}
		}

		return mTracking || mAnimating || super.onTouchEvent(event);
	}

	private void animateClose(int position) {
		prepareTracking(position);
		performFling(position, mMaximumAcceleration, true);
	}

	private void animateOpen(int position) {
		prepareTracking(position);
		performFling(position, -mMaximumAcceleration, true);
	}

	private void performFling(int position, float velocity, boolean always) {
		mAnimationPosition = position;
		mAnimatedVelocity = velocity;

		boolean invert = (mDirection == DIRECTION_LEFT_TO_RIGHT || mDirection == DIRECTION_TOP_TO_BOTTOM);

		boolean c1;
		boolean c2;
		boolean c3;

		if (mExpanded) {
			int bottom = mVertical ? getBottom() : getRight();
			int handleHeight = mVertical ? mHandleHeight : mHandleWidth;

			c1 = invert ? velocity < mMaximumMajorVelocity
					: velocity > mMaximumMajorVelocity;
			c2 = invert ? (bottom - (position + handleHeight)) + mBottomOffset > handleHeight
					: position > mTopOffset
							+ (mVertical ? mHandleHeight : mHandleWidth);
			c3 = invert ? velocity < -mMaximumMajorVelocity
					: velocity > -mMaximumMajorVelocity;
			if (always || (c1 || (c2 && c3))) {
				// We are expanded, So animate to CLOSE!
				mAnimatedAcceleration = mMaximumAcceleration;
				if (invert) {
					if (velocity > 0) {
						mAnimatedVelocity = 0;
					}
				} else {
					if (velocity < 0) {
						mAnimatedVelocity = 0;
					}
				}
			} else {
				// We are expanded, but they didn't move sufficiently to cause
				// us to retract. Animate back to the expanded position. so
				// animate BACK to expanded!
				mAnimatedAcceleration = -mMaximumAcceleration;

				if (invert) {
					if (velocity < 0) {
						mAnimatedVelocity = 0;
					}
				} else {
					if (velocity > 0) {
						mAnimatedVelocity = 0;
					}
				}
			}
		} else {

			// WE'RE COLLAPSED

			c1 = invert ? velocity < mMaximumMajorVelocity
					: velocity > mMaximumMajorVelocity;
			c2 = invert ? (position < (mVertical ? getHeight() : getWidth()) / 2)
					: (position > (mVertical ? getHeight() : getWidth()) / 2);
			c3 = invert ? velocity < -mMaximumMajorVelocity
					: velocity > -mMaximumMajorVelocity;

			if (!always && (c1 || (c2 && c3))) {
				mAnimatedAcceleration = mMaximumAcceleration;

				if (invert) {
					if (velocity > 0) {
						mAnimatedVelocity = 0;
					}
				} else {
					if (velocity < 0) {
						mAnimatedVelocity = 0;
					}
				}
			} else {
				mAnimatedAcceleration = -mMaximumAcceleration;

				if (invert) {
					if (velocity < 0) {
						mAnimatedVelocity = 0;
					}
				} else {
					if (velocity > 0) {
						mAnimatedVelocity = 0;
					}
				}
			}
		}

		long now = SystemClock.uptimeMillis();
		mAnimationLastTime = now;
		mCurrentAnimationTime = now + ANIMATION_FRAME_DURATION;
		mAnimating = true;
		mHandler.removeMessages(MSG_ANIMATE);
		mHandler.sendMessageAtTime(mHandler.obtainMessage(MSG_ANIMATE),
				mCurrentAnimationTime);
		stopTracking();
	}

	private void prepareTracking(int position) {
		boolean invert = (mDirection == DIRECTION_LEFT_TO_RIGHT || mDirection == DIRECTION_TOP_TO_BOTTOM);

		mTracking = true;
		mVelocityTracker = VelocityTracker.obtain();
		boolean opening = !mExpanded;
		if (opening) {
			mAnimatedAcceleration = mMaximumAcceleration;
			mAnimatedVelocity = mMaximumMajorVelocity;
			if (invert)
				mAnimationPosition = mTopOffset;
			else
				mAnimationPosition = mBottomOffset
						+ (mVertical ? getHeight() - mHandleHeight : getWidth()
								- mHandleWidth);
			moveHandle((int) mAnimationPosition);
			mAnimating = true;
			mHandler.removeMessages(MSG_ANIMATE);
			long now = SystemClock.uptimeMillis();
			mAnimationLastTime = now;
			mCurrentAnimationTime = now + ANIMATION_FRAME_DURATION;
			mAnimating = true;
		} else {
			if (mAnimating) {
				mAnimating = false;
				mHandler.removeMessages(MSG_ANIMATE);
			}
			moveHandle(position);
		}
	}

	private void moveHandle(int position) {
		final View handle = mHandle;

		if (mDirection == DIRECTION_BOTTOM_TO_TOP) {
			if (position == EXPANDED_FULL_OPEN) {
				handle.offsetTopAndBottom(mTopOffset - handle.getTop());
				invalidate();
			} else if (position == COLLAPSED_FULL_CLOSED) {
				handle.offsetTopAndBottom(mBottomOffset + getBottom()
						- getTop() - mHandleHeight - handle.getTop());
				invalidate();
			} else {
				final int top = handle.getTop();
				int deltaY = position - top;
				if (position < mTopOffset) {
					deltaY = mTopOffset - top;
				} else if (deltaY > mBottomOffset + getBottom() - getTop()
						- mHandleHeight - top) {
					deltaY = mBottomOffset + getBottom() - getTop()
							- mHandleHeight - top;
				}
				handle.offsetTopAndBottom(deltaY);

				final Rect frame = mFrame;
				final Rect region = mInvalidate;

				handle.getHitRect(frame);
				region.set(frame);

				region.union(frame.left, frame.top - deltaY, frame.right,
						frame.bottom - deltaY);
				region.union(0, frame.bottom - deltaY, getWidth(), frame.bottom
						- deltaY + mContent.getHeight());

				invalidate(region);
			}

		} else if (mDirection == DIRECTION_RIGHT_TO_LEFT) {
			if (position == EXPANDED_FULL_OPEN) {
				handle.offsetLeftAndRight(mTopOffset - handle.getLeft());
				invalidate();
			} else if (position == COLLAPSED_FULL_CLOSED) {
				handle.offsetLeftAndRight(mBottomOffset + getRight()
						- getLeft() - mHandleWidth - handle.getLeft());
				invalidate();
			} else {
				final int left = handle.getLeft();
				int deltaX = position - left;
				if (position < mTopOffset) {
					deltaX = mTopOffset - left;
				} else if (deltaX > mBottomOffset + getRight() - getLeft()
						- mHandleWidth - left) {
					deltaX = mBottomOffset + getRight() - getLeft()
							- mHandleWidth - left;
				}
				handle.offsetLeftAndRight(deltaX);

				final Rect frame = mFrame;
				final Rect region = mInvalidate;

				handle.getHitRect(frame);
				region.set(frame);

				region.union(frame.left - deltaX, frame.top, frame.right
						- deltaX, frame.bottom);
				region.union(frame.right - deltaX, 0, frame.right - deltaX
						+ mContent.getWidth(), getHeight());

				invalidate(region);
			}
		} else if (mDirection == DIRECTION_LEFT_TO_RIGHT) {
			if (position == EXPANDED_FULL_OPEN) {
				handle.offsetLeftAndRight(mBottomOffset + getRight()
						- getLeft() - mHandleWidth);
				invalidate();
			} else if (position == COLLAPSED_FULL_CLOSED) {
				handle.offsetLeftAndRight(mTopOffset - handle.getLeft());
				invalidate();
			} else {
				final int left = handle.getLeft();
				int deltaX = position - left;
				if (position < mTopOffset) {
					deltaX = mTopOffset - left;
				} else if (deltaX > mBottomOffset + getRight() - getLeft()
						- mHandleWidth - left) {
					deltaX = mBottomOffset + getRight() - getLeft()
							- mHandleWidth - left;
				}
				handle.offsetLeftAndRight(deltaX);

				final Rect frame = mFrame;
				final Rect region = mInvalidate;

				handle.getHitRect(frame);
				region.set(frame);

				region.union(frame.left - deltaX, frame.top, frame.right
						- deltaX, frame.bottom);
				region.union(frame.right - deltaX, 0, frame.right - deltaX
						+ mContent.getWidth(), getHeight());

				invalidate(region);
			}
		} else {
			if (position == EXPANDED_FULL_OPEN) {
				handle.offsetTopAndBottom(mBottomOffset + getBottom()
						- getTop() - mHandleHeight);
				invalidate();
			} else if (position == COLLAPSED_FULL_CLOSED) {
				handle.offsetTopAndBottom(mTopOffset - handle.getTop());
				invalidate();
			} else {
				final int top = handle.getTop();
				int deltaY = position - top;
				if (position < mTopOffset) {
					deltaY = mTopOffset - top;
				} else if (deltaY > mBottomOffset + getBottom() - getTop()
						- mHandleHeight - top) {
					deltaY = mBottomOffset + getBottom() - getTop()
							- mHandleHeight - top;
				}

				handle.offsetTopAndBottom(deltaY);

				final Rect frame = mFrame;
				final Rect region = mInvalidate;

				handle.getHitRect(frame);
				region.set(frame);

				region.union(frame.left, frame.top - deltaY, frame.right,
						frame.bottom - deltaY);
				region.union(0, frame.bottom - deltaY, getWidth(), frame.bottom
						- deltaY + mContent.getHeight());

				invalidate(region);
			}

		}

	}

	private void prepareContent() {
		if (mAnimating) {
			return;
		}

		// Something changed in the content, we need to honor the layout request
		// before creating the cached bitmap
		final View content = mContent;
		if (content.isLayoutRequested()) {

			if (mDirection == DIRECTION_BOTTOM_TO_TOP) {
				final int childHeight = mHandleHeight;
				int height = getBottom() - getTop() - childHeight - mTopOffset;
				content.measure(MeasureSpec.makeMeasureSpec(getRight()
						- getLeft(), MeasureSpec.EXACTLY), MeasureSpec
						.makeMeasureSpec(height, MeasureSpec.EXACTLY));
				content.layout(0, mTopOffset + childHeight,
						content.getMeasuredWidth(), mTopOffset + childHeight
								+ content.getMeasuredHeight());
			} else if (mDirection == DIRECTION_RIGHT_TO_LEFT) {
				final int childWidth = mHandle.getWidth();
				int width = getRight() - getLeft() - childWidth - mTopOffset;
				content.measure(MeasureSpec.makeMeasureSpec(width,
						MeasureSpec.EXACTLY), MeasureSpec.makeMeasureSpec(
						getBottom() - getTop(), MeasureSpec.EXACTLY));
				content.layout(childWidth + mTopOffset, 0, mTopOffset
						+ childWidth + content.getMeasuredWidth(),
						content.getMeasuredHeight());
			} else if (mDirection == DIRECTION_LEFT_TO_RIGHT) {
				final int childWidth = mHandle.getWidth();
				int width = getRight() - getLeft() - childWidth - mTopOffset;
				content.measure(MeasureSpec.makeMeasureSpec(width,
						MeasureSpec.EXACTLY), MeasureSpec.makeMeasureSpec(
						getBottom() - getTop(), MeasureSpec.EXACTLY));
				content.layout(mTopOffset, 0,
						mTopOffset + content.getMeasuredWidth(),
						content.getMeasuredHeight());
			} else {
				final int childHeight = mHandleHeight;
				int height = getBottom() - getTop() - childHeight - mTopOffset;
				content.measure(MeasureSpec.makeMeasureSpec(getRight()
						- getLeft(), MeasureSpec.EXACTLY), MeasureSpec
						.makeMeasureSpec(height, MeasureSpec.EXACTLY));
				content.layout(0, mTopOffset, content.getMeasuredWidth(),
						mTopOffset + content.getMeasuredHeight());
			}

		}
		// Try only once... we should really loop but it's not a big deal
		// if the draw was cancelled, it will only be temporary anyway
		content.getViewTreeObserver().dispatchOnPreDraw();
		content.buildDrawingCache();

		content.setVisibility(View.GONE);
	}

	private void stopTracking() {
		mHandle.setPressed(false);
		mTracking = false;

		if (mOnDrawerScrollListener != null) {
			mOnDrawerScrollListener.onScrollEnded();
		}

		if (mVelocityTracker != null) {
			mVelocityTracker.recycle();
			mVelocityTracker = null;
		}
	}

	private void doAnimation() {
		boolean invert = (mDirection == DIRECTION_LEFT_TO_RIGHT || mDirection == DIRECTION_TOP_TO_BOTTOM);
		if (mAnimating) {
			incrementAnimation(invert);
			if (invert) {
				if (mAnimationPosition < mTopOffset) {
					mAnimating = false;
					closeDrawer();
				} else if (mAnimationPosition >= mTopOffset
						+ (mVertical ? getHeight() : getWidth()) - 1) {
					mAnimating = false;
					openDrawer();
				} else {
					moveHandle((int) mAnimationPosition);
					mCurrentAnimationTime += ANIMATION_FRAME_DURATION;
					mHandler.sendMessageAtTime(
							mHandler.obtainMessage(MSG_ANIMATE),
							mCurrentAnimationTime);
				}
			} else {
				if (mAnimationPosition >= mBottomOffset
						+ (mVertical ? getHeight() : getWidth()) - 1) {
					mAnimating = false;
					closeDrawer();
				} else if (mAnimationPosition < mTopOffset) {
					mAnimating = false;
					openDrawer();
				} else {
					moveHandle((int) mAnimationPosition);
					mCurrentAnimationTime += ANIMATION_FRAME_DURATION;
					mHandler.sendMessageAtTime(
							mHandler.obtainMessage(MSG_ANIMATE),
							mCurrentAnimationTime);
				}
			}
		}
	}

	private void incrementAnimation(boolean invert) {
		long now = SystemClock.uptimeMillis();
		float t = (now - mAnimationLastTime) / 1000.0f; // ms -> s
		final float position = mAnimationPosition;
		final float v = mAnimatedVelocity; // px/s
		final float a = (invert) ? mAnimatedAcceleration
				: mAnimatedAcceleration; // px/s/s
		mAnimationPosition = position + (v * t) + (0.5f * a * t * t); // px
		mAnimatedVelocity = v + (a * t); // px/s
		mAnimationLastTime = now; // ms
	}

	/**
	 * Toggles the drawer open and close. Takes effect immediately.
	 * 
	 * @see #open()
	 * @see #close()
	 * @see #animateClose()
	 * @see #animateOpen()
	 * @see #animateToggle()
	 */
	public void toggle() {
		if (!mExpanded) {
			openDrawer();
		} else {
			closeDrawer();
		}
		invalidate();
		requestLayout();
	}

	/**
	 * Toggles the drawer open and close with an animation.
	 * 
	 * @see #open()
	 * @see #close()
	 * @see #animateClose()
	 * @see #animateOpen()
	 * @see #toggle()
	 */
	public void animateToggle() {
		if (!mExpanded) {
			animateOpen();
		} else {
			animateClose();
		}
	}

	/**
	 * Opens the drawer immediately.
	 * 
	 * @see #toggle()
	 * @see #close()
	 * @see #animateOpen()
	 */
	public void open() {
		openDrawer();
		invalidate();
		requestLayout();
	}

	/**
	 * Closes the drawer immediately.
	 * 
	 * @see #toggle()
	 * @see #open()
	 * @see #animateClose()
	 */
	public void close() {
		closeDrawer();
		invalidate();
		requestLayout();
	}

	/**
	 * Closes the drawer with an animation.
	 * 
	 * @see #close()
	 * @see #open()
	 * @see #animateOpen()
	 * @see #animateToggle()
	 * @see #toggle()
	 */
	public void animateClose() {
		prepareContent();
		final OnDrawerScrollListener scrollListener = mOnDrawerScrollListener;
		if (scrollListener != null) {
			scrollListener.onScrollStarted();
		}
		boolean invert = (mDirection == DIRECTION_LEFT_TO_RIGHT || mDirection == DIRECTION_TOP_TO_BOTTOM);
		animateClose(mVertical ? (invert) ? mHandle.getTop() : mHandle
				.getBottom() : (invert) ? mHandle.getLeft() : mHandle
				.getRight());
		if (scrollListener != null) {
			scrollListener.onScrollEnded();
		}
	}

	/**
	 * Opens the drawer with an animation.
	 * 
	 * @see #close()
	 * @see #open()
	 * @see #animateClose()
	 * @see #animateToggle()
	 * @see #toggle()
	 */
	public void animateOpen() {
		prepareContent();
		final OnDrawerScrollListener scrollListener = mOnDrawerScrollListener;
		if (scrollListener != null) {
			scrollListener.onScrollStarted();
		}
		boolean invert = (mDirection == DIRECTION_LEFT_TO_RIGHT || mDirection == DIRECTION_TOP_TO_BOTTOM);
		animateOpen(mVertical ? (invert) ? mHandle.getTop() : mHandle
				.getBottom() : (invert) ? mHandle.getLeft() : mHandle
				.getRight());
		if (scrollListener != null) {
			scrollListener.onScrollEnded();
		}
	}

	private void closeDrawer() {
		moveHandle(COLLAPSED_FULL_CLOSED);
		mContent.setVisibility(View.GONE);
		mContent.destroyDrawingCache();

		if (!mExpanded) {
			return;
		}

		mExpanded = false;
		if (mOnDrawerCloseListener != null) {
			mOnDrawerCloseListener.onDrawerClosed();
		}
	}

	private void openDrawer() {
		moveHandle(EXPANDED_FULL_OPEN);
		mContent.setVisibility(View.VISIBLE);

		if (mExpanded) {
			return;
		}

		mExpanded = true;
		if (mOnDrawerOpenListener != null) {
			mOnDrawerOpenListener.onDrawerOpened();
		}
	}

	/**
	 * Sets the listener that receives a notification when the drawer becomes
	 * open.
	 * 
	 * @param onDrawerOpenListener
	 *            The listener to be notified when the drawer is opened.
	 */
	public void setOnDrawerOpenListener(
			OnDrawerOpenListener onDrawerOpenListener) {
		mOnDrawerOpenListener = onDrawerOpenListener;
	}

	/**
	 * Sets the listener that receives a notification when the drawer becomes
	 * close.
	 * 
	 * @param onDrawerCloseListener
	 *            The listener to be notified when the drawer is closed.
	 */
	public void setOnDrawerCloseListener(
			OnDrawerCloseListener onDrawerCloseListener) {
		mOnDrawerCloseListener = onDrawerCloseListener;
	}

	/**
	 * Sets the listener that receives a notification when the drawer starts or
	 * ends a scroll. A fling is considered as a scroll. A fling will also
	 * trigger a drawer opened or drawer closed event.
	 * 
	 * @param onDrawerScrollListener
	 *            The listener to be notified when scrolling starts or stops.
	 */
	public void setOnDrawerScrollListener(
			OnDrawerScrollListener onDrawerScrollListener) {
		mOnDrawerScrollListener = onDrawerScrollListener;
	}

	/**
	 * Returns the handle of the drawer.
	 * 
	 * @return The View reprenseting the handle of the drawer, identified by the
	 *         "handle" id in XML.
	 */
	public View getHandle() {
		return mHandle;
	}

	/**
	 * Returns the content of the drawer.
	 * 
	 * @return The View reprenseting the content of the drawer, identified by
	 *         the "content" id in XML.
	 */
	public View getContent() {
		return mContent;
	}

	/**
	 * Unlocks the SlidingDrawer so that touch events are processed.
	 * 
	 * @see #lock()
	 */
	public void unlock() {
		mLocked = false;
	}

	/**
	 * Locks the SlidingDrawer so that touch events are ignores.
	 * 
	 * @see #unlock()
	 */
	public void lock() {
		mLocked = true;
	}

	/**
	 * Indicates whether the drawer is currently fully opened.
	 * 
	 * @return True if the drawer is opened, false otherwise.
	 */
	public boolean isOpened() {
		return mExpanded;
	}

	/**
	 * Indicates whether the drawer is scrolling or flinging.
	 * 
	 * @return True if the drawer is scroller or flinging, false otherwise.
	 */
	public boolean isMoving() {
		return mTracking || mAnimating;
	}

	private class DrawerToggler implements OnClickListener {
		public void onClick(View v) {
			if (mLocked) {
				return;
			}
			// mAllowSingleTap isn't relevant here; you're *always*
			// allowed to open/close the drawer by clicking with the
			// trackball.

			if (mAnimateOnClick) {
				animateToggle();
			} else {
				toggle();
			}
		}
	}

	private class SlidingHandler extends Handler {
		public void handleMessage(Message m) {
			switch (m.what) {
			case MSG_ANIMATE:
				doAnimation();
				break;
			}
		}
	}
}
