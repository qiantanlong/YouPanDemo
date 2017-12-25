package hongzhen.com.youpandemo;

import android.content.Context;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.TextView;

import com.github.mjdev.libaums.fs.UsbFile;

import java.util.List;

/**
 * Created by hongzhen on 2017/12/22.
 */

public class FileAdapter extends BaseAdapter {
    private Context context;
    private List<UsbFile> list;
    private TextView textView;

    public FileAdapter(Context context, List<UsbFile> list) {
        this.context = context;
        this.list = list;
    }

    @Override
    public int getCount() {
        return list != null ? list.size() : 0;
    }

    @Override
    public Object getItem(int i) {
        return list.get(i);
    }

    @Override
    public long getItemId(int i) {
        return i;
    }

    @Override
    public View getView(int i, View view, ViewGroup viewGroup) {
        ViewHolder viewHolder = null;
        if (view == null) {
            viewHolder = new ViewHolder();
            view = View.inflate(context, R.layout.item_filelistview, null);
            viewHolder.tvFile = view.findViewById(R.id.tv_file);
            view.setTag(viewHolder);
        } else {
            viewHolder = (ViewHolder) view.getTag();
        }
        if (list.get(i).isDirectory()) {
            viewHolder.tvFile.setText(list.get(i).getName() + "(文件夹)");
        } else {
            viewHolder.tvFile.setText(list.get(i).getName());
        }
        return view;
    }

    class ViewHolder {
        private TextView tvFile;
    }
}
