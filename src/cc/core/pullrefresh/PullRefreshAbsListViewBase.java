package cc.core.pullrefresh;

import cc.core.pullrefresh.extra.FooterLayout;
import cc.core.pullrefresh.extra.HeaderLayout;

import com.nineoldandroids.view.ViewHelper;

import android.animation.Animator;
import android.animation.ObjectAnimator;
import android.animation.ValueAnimator;
import android.animation.ValueAnimator.AnimatorUpdateListener;
import android.content.Context;
import android.os.AsyncTask;
import android.util.AttributeSet;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewTreeObserver;
import android.view.ViewTreeObserver.OnGlobalLayoutListener;
import android.widget.AbsListView;
import android.widget.AbsListView.OnScrollListener;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListAdapter;
import android.widget.TextView;

/**
 * ClassName:PullRefreshBase <br/>
 * Date: 2015��6��22�� ����3:04:43 <br/>
 * 
 * @author YuanChao
 */
public abstract class PullRefreshAbsListViewBase<T extends AbsListView> extends
		LinearLayout implements IPullBase<T>, OnScrollListener {

	/**
	 * �ڲ���ListView
	 */
	protected T listview;

	/**
	 * ����ˢ�µ�Header���������ظ����View
	 */
	protected FooterLayout footerView;
	protected HeaderLayout headerView;

	/**
	 * ��װListView������
	 */
	protected LinearLayout layoutBase;

	protected ImageView headerImg;

	protected int mLatPointId;
	protected int firstVisibleItem;
	protected int headerHeight;
	protected int newScrollValue;
	protected int lastVisiablePosition;
	protected int totalCount;
	private int scrollState;// ��ǰ������״̬
	protected int state, mode;

	protected float mLastMotionY;
	protected float mInitialMotionY;
	protected float mInitY;
	protected float mHeaderY;
	protected final float STICKY = 2.0f;

	protected boolean once;
	protected boolean headerScrolling;
	protected boolean isFirst = true;
	protected boolean isPulling;
	protected boolean refreshAnimStarted = false;// ����ˢ��ʱ������ض���������Header�ĸ߶�
	// �������޷������Ļʱ������ظ���
	private boolean canLoadMore = true;

	private AttributeSet attr;

	protected OnRefreshListener refreshListener;

	public T getListview() {
		return listview;
	}

	@Override
	public void setOnRefreshListener(OnRefreshListener listener) {
		this.refreshListener = listener;
	}

	public PullRefreshAbsListViewBase(Context context, AttributeSet attrs,
			int defStyleAttr) {
		super(context, attrs, defStyleAttr);
		layoutBase = new LinearLayout(context, attrs, defStyleAttr);
		initRefreshView(context, attrs, defStyleAttr);
		initView(context, attrs);
	}

	public PullRefreshAbsListViewBase(Context context, AttributeSet attrs) {
		super(context, attrs);
		layoutBase = new LinearLayout(context, attrs);
		initRefreshView(context, attrs, 0);
		initView(context, attrs);
	}

	public PullRefreshAbsListViewBase(Context context) {
		super(context);
		layoutBase = new LinearLayout(context);
		initRefreshView(context, null, 0);
		initView(context, null);

	}

	/**
	 * <h1>View �ĳ�ʼ��</h1>
	 * <p>
	 * ʵ����һ��LineLayout����{@link #layoutBase}����{@link #headerView}��
	 * {@link #listview} ��ӵ�{@code #layoutBase}
	 * </p>
	 * 
	 * @param context
	 */
	protected void initView(Context context, AttributeSet attr) {
		this.attr = attr;
		innerInit(context);
		setOrientation(VERTICAL);
		initHeader(context, attr);
		listview.setOnScrollListener(this);
		layoutBase.setOrientation(VERTICAL);
		layoutBase.addView(headerView);
		layoutBase.addView(listview);
		addView(layoutBase);
		LayoutParams lp = (LayoutParams) layoutBase.getLayoutParams();
		lp.setMargins(0, -headerHeight, 0, 0);
		layoutBase.setLayoutParams(lp);
		getViewTreeObserver().addOnGlobalLayoutListener(
				new ViewTreeObserver.OnGlobalLayoutListener() {

					public void onGlobalLayout() {
						if (once) {
							once = false;
						} else
							return;
						int[] lc = new int[2];
						headerView.getLocationInWindow(lc);
					}
				});
	}

	/**
	 * <p>
	 * ��ʼ��{@link #listview}�ֱ�������Ĳ�ͬ�����Ĺ��췽������ʹxml�����ʹ��{@link #listview} ������
	 * </p>
	 * 
	 * @param context
	 * @param attrs
	 * @param defStyleAttr
	 */
	protected abstract void initRefreshView(Context context,
			AttributeSet attrs, int defStyleAttr);

	private void measureHeight(View view) {
		int w = MeasureSpec.makeMeasureSpec(0, MeasureSpec.UNSPECIFIED);
		int h = MeasureSpec.makeMeasureSpec(0, MeasureSpec.UNSPECIFIED);
		view.measure(w, h);
		headerHeight = view.getMeasuredHeight();
	}

	@Override
	public void onScrollStateChanged(AbsListView absListView, int i) {
		this.scrollState = i;
		// �ж��Ƿ񻬶�����ײ�
		if (totalCount == lastVisiablePosition
				&& scrollState == SCROLL_STATE_IDLE && canLoadMore) {
			// ˵����������������һ��Item
			if (refreshListener != null) {
				if (mode == Mode.DISABLE || mode == Mode.PULL_FROM_TOP)
					return;
				if (state != State.FOOTER_REFRESHING) {// �Ѿ����ڼ��ظ���״̬����Ҫ�ٻص�
					state = State.FOOTER_REFRESHING;
					footerView.setVisibility(View.VISIBLE);
					refreshListener.onFooterRefresh();
					refreshListener.onFooterRefresh(footerView);
				}
			}
		}
	}

	@Override
	public void onScroll(AbsListView view, int firstVisibleItem,
			int visibleItemCount, int totalItemCount) {
		this.firstVisibleItem = firstVisibleItem;

		// �����ݲ������������Ļʱ��������FooterView
		if (totalItemCount <= visibleItemCount) {
			this.canLoadMore = false;
			if (footerView != null)
				footerView.setVisibility(GONE);
		} else {
			this.canLoadMore = true;
		}

		lastVisiablePosition = firstVisibleItem + visibleItemCount;
		totalCount = totalItemCount;
	}

	private int[] getLc(View view) {
		int[] lc = new int[2];
		view.getLocationInWindow(lc);
		return lc;
	}

	protected void innerInit(Context context) {
		listview.getViewTreeObserver().addOnGlobalLayoutListener(
				new OnGlobalLayoutListener() {

					@Override
					public void onGlobalLayout() {

						if (!isFirst)
							return;
						isFirst = !isFirst;
						if (listview.getCount() > 0) {
							mInitY = getLc(getChildAt(0))[1];
						}
					}
				});
		// ����ʾ�����͵ײ���קʱ����Ӱ
		// if (Integer.parseInt(Build.VERSION.SDK) >= 9) {
		// this.setOverScrollMode(View.OVER_SCROLL_NEVER);
		// }
	}

	/**
	 * ����{@link #headerView}
	 */
	@Override
	public void initHeader(Context context, AttributeSet attr) {
		headerView = new HeaderLayout(getContext(), attr);
		measureHeight(headerView);
	}

	/**
	 * ����{@link #footerView}
	 */
	@Override
	public void initFooter(Context context, AttributeSet attr) {
		footerView = new FooterLayout(getContext(), attr);
	}

	/**
	 * ���ô˷���ǰ��Ӧ���ȳ�ʼ��{@link #footerView}
	 * 
	 * @param listener
	 */
	public void setFooterClickListener(OnClickListener listener) {
		footerView.setOnClickListener(listener);
	}

	/**
	 * ���� footerView
	 * 
	 * @return
	 */
	public FooterLayout getFooterView() {
		return footerView;
	}

	/**
	 * ���� headerView
	 * 
	 * @return
	 */
	public HeaderLayout getHeaderView() {
		return headerView;
	}

	/**
	 * <p>
	 * ��������ˢ�µ�ģʽ����Ӧ����{@link #setAdapter(ListAdapter)}֮ǰ�����������
	 * </p>
	 * 
	 * @param mode
	 */
	public void setMode(int mode) {
		this.mode = mode;
		switch (mode) {
		case Mode.BOTH:
			footerView.setVisibility(View.VISIBLE);
			initFooter(getContext(), attr);
			break;
		case Mode.PULL_FROM_BOTTOM:
			initFooter(getContext(), attr);
			footerView.setVisibility(View.VISIBLE);
			break;
		}
	}

	/**
	 * ������룬Touch�¼�����
	 * 
	 * @param ev
	 * @return
	 */
	public boolean childTouchEvnet(MotionEvent ev) {

		if (mode == Mode.DISABLE)
			return false;
		switch (ev.getAction()) {
		case MotionEvent.ACTION_DOWN:
			mLatPointId = ev.getPointerId(0);
			mLastMotionY = mInitialMotionY = ev.getY();
			break;
		case MotionEvent.ACTION_MOVE:
			// doMove(ev);

			mLastMotionY = ev.getY();
			doMove(ev);
			break;
		case MotionEvent.ACTION_UP:
			if (isPulling) {
				if (refreshListener == null)
					startHeadAnim(0f);
				else {
					if (state == State.HEAD_RELEASEING) {
						if (state != State.FOOTER_REFRESHING
								&& state != State.HEAD_REFRESHING) {// �Ѿ�����ˢ�µ�״̬����Ҫ�ص�
							state = State.HEAD_REFRESHING;
							refreshAnimStarted = true;
							startHeadAnim(headerHeight);
							updateState();
							refreshListener.onHeaderRefresh();
							refreshListener.onHeaderRefresh(headerView);
						}
					} else {
						startHeadAnim(0f);
					}
				}
				isPulling = false;
			}
			break;
		}
		if (isPulling || headerScrolling)
			return true;
		else
			return false;
	}

	public void onRefreshComplete() {
		this.state = State.NORMAL;
		startHeadAnim(0f);
		updateState();
	}

	public void updateState() {
		switch (state) {
		case State.HEAD_REFRESHING:
			headerView.setRefreshing();
			break;
		case State.NORMAL:
		case State.HEAD_PULLING:
			headerView.setPullToRefresh();
			break;
		case State.HEAD_RELEASEING:
			headerView.setReleaseToRefresh();
			break;
		}
	}

	/**
	 * <h1>�����ľ���ʵ�֡�</h1>
	 * <p>
	 * ͨ�����������ָ�����ľ��룬���ϵظı�{@link #layoutBase}��Y�Դﵽ����Ч��
	 * </p>
	 */
	@Override
	public void doMove(MotionEvent ev) {
		final float initialMotionValue, lastMotionValue;

		initialMotionValue = mInitialMotionY;
		lastMotionValue = mLastMotionY;

		setPressed(false);
		newScrollValue = Math.round(Math.min(initialMotionValue
				- lastMotionValue, 0)
				/ STICKY);

		if (readyPull(ev)) {
			updateHead();
			ViewHelper.setTranslationY(layoutBase, -newScrollValue);
			headerView.setAnimValue(-newScrollValue);
			// layoutBase.scrollTo(0, newScrollValue);
			invalidate();
		}
	}

	/**
	 * ����{@link #headerView}��������ȫ�ֵ�״̬
	 */
	private void updateHead() {
		setPressed(false);
		if (refreshAnimStarted)
			return;
		mHeaderY = getLc(getChildAt(0))[1];
		// if (state == State.FOOTER_REFRESHING)
		// return;
		if ((mHeaderY - mInitY) >= headerHeight) {
			this.state = State.HEAD_RELEASEING;
			updateState();
		} else {
			this.state = State.HEAD_PULLING;
			updateState();
		}
	}

	/**
	 * ����λ�ã��ж��Ƿ�ﵽ����
	 */
	@Override
	public boolean readyPull(MotionEvent ev) {
		if (isPulling)
			return true;
		int[] lc = new int[2];
		getChildAt(0).getLocationInWindow(lc);
		if (-newScrollValue > 0 && firstVisibleItem == 0 && mInitY == lc[1]) {
			Log.i("CC", "newScrollValue:" + newScrollValue + " lc[1]:" + lc[1]
					+ ",mInitY:" + mInitY);
			mInitialMotionY = ev.getY();
			newScrollValue = 1;
			isPulling = true;
			return true;
		} else {
			isPulling = false;
			return false;
		}
	}

	/**
	 * ˢ�½�����ص�����
	 * 
	 * @param value
	 *            ��������������ֵ
	 */
	public void startHeadAnim(float... value) {
		float startValue, endValue;
		ObjectAnimator anim = null;
		if (value.length == 1) {
			startValue = value[0];
			anim = ObjectAnimator.ofFloat(layoutBase, "translationY",
					startValue).setDuration(300);
		} else {
			startValue = value[0];
			endValue = value[1];
			anim = ObjectAnimator.ofFloat(layoutBase, "translationY",
					startValue, endValue).setDuration(300);
		}
		Log.d("CC", "mInitY:" + mInitY + ",Y:" + ",headerHeight:"
				+ headerHeight);
		anim.addListener(new Animator.AnimatorListener() {
			boolean isRefreshAnim = false;

			public void onAnimationStart(Animator animator) {
				if (refreshAnimStarted)
					isRefreshAnim = true;
				refreshAnimStarted = false;
				headerScrolling = true;
				state = State.HEAD_RELEASEING;
			}

			public void onAnimationEnd(Animator animator) {
				if (isRefreshAnim) {
					isRefreshAnim = false;
					headerScrolling = true;
					state = State.HEAD_REFRESHING;
					updateState();
				} else {
					headerScrolling = false;
					state = State.NORMAL;
				}
				updateState();
			}

			public void onAnimationCancel(Animator animator) {
			}

			public void onAnimationRepeat(Animator animator) {
			}
		});
		anim.addUpdateListener(new AnimatorUpdateListener() {

			@Override
			public void onAnimationUpdate(ValueAnimator animation) {
				// ViewHelper.setRotation(headerImg,
				// (Float) animation.getAnimatedValue());
				headerView.setAnimValue((Float) animation.getAnimatedValue());
				updateHead();
			}
		});
		anim.start();
	}

	@Override
	public T getRefreshView() {
		return listview;
	}

	/**
	 * ����������ʱ{@link #listview}��EmptyViewt</br>
	 * 
	 * @param view
	 *            ��Ӧ����һ��FrameLayout</br>
	 */
	@Override
	public void setEmptyView(View view) {
		if (listview.getEmptyView() == null) {
			listview.setEmptyView(view);
		} else if (view instanceof FrameLayout) {
			((FrameLayout) view).removeAllViews();
			((FrameLayout) view).addView(view);
		}
	}

	/**
	 * ����Adapter
	 */
	@Override
	public void setAdapter(ListAdapter adapter) {
		listview.setAdapter(adapter);
	}

	public static class Mode {
		public static final int DISABLE = -1;
		public static final int PULL_FROM_TOP = 0;
		public static final int PULL_FROM_BOTTOM = 2;
		public static final int BOTH = 3;
	}

	public static class State {
		public static final int NORMAL = 0;
		public static final int HEAD_PULLING = 1;
		public static final int HEAD_REFRESHING = 2;
		public static final int HEAD_RELEASEING = 3;

		public static final int FOOTER_PULLING = 4;
		public static final int FOOTER_REFRESHING = 5;
		public static final int FOOTER_RELEASEING = 6;
	}

}
