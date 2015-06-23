package cc.core.pullrefresh;

import android.content.Context;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;
import android.widget.ListAdapter;

/**
 * ClassName:IPullBase <br/>
 * Date: 2015��6��21�� ����11:12:20 <br/>
 * ˢ�¶���ӿ�
 * 
 * @author YuanChao
 */
public interface IPullBase<T extends View> {

	public T getRefreshView();

	public void setEmptyView(View view);

	public void setAdapter(ListAdapter adapter);

	public void startHeadAnim(float... value);

	public boolean readyPull(MotionEvent ev);

	public void doMove(MotionEvent ev);

	public void initHeader(Context context, AttributeSet attr);

	public void initFooter(Context context, AttributeSet attr);

	public void setOnRefreshListener(OnRefreshListener listener);
}
