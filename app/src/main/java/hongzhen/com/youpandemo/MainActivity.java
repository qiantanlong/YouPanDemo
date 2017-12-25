package hongzhen.com.youpandemo;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.hardware.usb.UsbDevice;
import android.hardware.usb.UsbManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.support.v7.app.AppCompatActivity;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.github.mjdev.libaums.UsbMassStorageDevice;
import com.github.mjdev.libaums.fs.FileSystem;
import com.github.mjdev.libaums.fs.UsbFile;
import com.github.mjdev.libaums.fs.UsbFileInputStream;
import com.github.mjdev.libaums.fs.UsbFileOutputStream;
import com.github.mjdev.libaums.partition.Partition;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.util.ArrayList;

/**
 * @author hongzhen
 */
public class MainActivity extends AppCompatActivity implements View.OnClickListener {
    //输入的内容
    private EditText etContent;
    //写入到U盘
    private Button btnWrite;
    //从U盘读取
    private Button btnRead;
    //显示读取的内容
    private TextView tvContent;
    //自定义U盘读写权限
    private static final String ACTION_USB_PERMISSION = "com.android.example.USB_PERMISSION";
    //当前处接U盘列表
    private UsbMassStorageDevice[] storageDevices;
    //当前U盘所在文件目录
    private UsbFile cFolder;
    private final static String U_DISK_FILE_NAME = "str.txt";
    @SuppressLint("HandlerLeak")
    private Handler mHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case 100:
                    showToastMsg("保存成功");
                    break;
                case 101:
//                    String txt = msg.obj.toString();
                    StringBuilder txt = (StringBuilder) msg.obj;
                    if (!TextUtils.isEmpty(txt)) {
                        tvContent.setText("读取到的数据是：" + txt);
                    }
                    break;
                default:
            }
        }
    };
    private Button btnCopy2UPan;
    private Button btnGetFileUPan;
    private ListView listviewFile;
    private Button btnNewFile;
    private Button btnNewDir;
    private Button btnDelFile;
    private ArrayList<UsbFile> files;
    private Button btnClear;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        initViews();
        registerUDiskReceiver();
    }

    private void initViews() {
        etContent = (EditText) findViewById(R.id.u_disk_edt);
        btnWrite = (Button) findViewById(R.id.u_disk_write);
        btnRead = (Button) findViewById(R.id.u_disk_read);
        btnCopy2UPan = (Button) findViewById(R.id.btn_copy_2UPan);
        btnGetFileUPan = (Button) findViewById(R.id.btn_get_file_UPan);
        btnNewFile = (Button) findViewById(R.id.btn_new_file);
        btnNewDir = (Button) findViewById(R.id.btn_new_dir);
        btnDelFile = (Button) findViewById(R.id.btn_del_file);
        btnClear = (Button) findViewById(R.id.btn_clear);
        tvContent = (TextView) findViewById(R.id.tv_content);
        listviewFile = (ListView) findViewById(R.id.listview_file);
        btnWrite.setOnClickListener(this);
        btnRead.setOnClickListener(this);
        btnCopy2UPan.setOnClickListener(this);
        btnGetFileUPan.setOnClickListener(this);
        btnClear.setOnClickListener(this);
        btnNewFile.setOnClickListener(this);
        btnNewDir.setOnClickListener(this);
        btnDelFile.setOnClickListener(this);
    }

    @Override
    public void onClick(View view) {
        switch (view.getId()) {
            case R.id.u_disk_write:
                //向U盘文件写入数据str.txt
                final String content = etContent.getText().toString().trim();
                if (TextUtils.isEmpty(content)){
                    Toast.makeText(MainActivity.this, "请输入内容", Toast.LENGTH_LONG).show();
                }else {
                    mHandler.post(new Runnable() {
                        @Override
                        public void run() {
                            saveToUDisk(content);
                        }
                    });
                }
                break;
            case R.id.u_disk_read:
                //读取优盘中文件的数据str.txt
                mHandler.post(new Runnable() {
                    @Override
                    public void run() {
                        readFromUDisk();
                    }
                });

                break;
            case R.id.btn_copy_2UPan:
                //复制文件到U盘
                copyFile2Upan();
                break;
            case R.id.btn_get_file_UPan:
                //获取U盘的文件列表
                getFileListUPan(cFolder);
                break;
            case R.id.btn_new_file:
                //创建新文件
                String conFile = etContent.getText().toString().trim();
                if (TextUtils.isEmpty(conFile)) {
                    Toast.makeText(MainActivity.this, "请输入要创建的文件的名称", Toast.LENGTH_LONG).show();
                } else {
                    createNewFile(conFile);
                }
                break;
            case R.id.btn_new_dir:
                //创建新目录
                String conDir = etContent.getText().toString().trim();
                if (TextUtils.isEmpty(conDir)) {
                    Toast.makeText(MainActivity.this, "请输入要创建的文件的名称", Toast.LENGTH_LONG).show();
                } else {
                    createNewDir(conDir);
                }

                break;
            case R.id.btn_del_file:
                //删除文件
                delFileFromUPan();
                break;
            case R.id.btn_clear:
                etContent.setText("");
                break;
            default:
        }
    }

    /**
     * 新建目录
     *
     * @param conDir
     */
    private void createNewDir(String conDir) {
        if (cFolder == null) {
            getUPanDevsList();
        }
        try {
            UsbFile directory = cFolder.createDirectory(conDir);
            saveTxtToUDisk("新建目录并新建文件中内容",directory.createFile("new.txt"));
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * 删除文件或文件夹
     */
    private void delFileFromUPan() {
        Toast.makeText(MainActivity.this, "请在文件目录列表中长按对应文件（夹）", Toast.LENGTH_LONG).show();
    }

    /**
     * 创建新的文件
     *
     * @param con
     */
    private void createNewFile(String con) {
        if (cFolder == null) {
            getUPanDevsList();
        }
        try {
            cFolder.createFile(con);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * 获取优盘文件列表
     */
    private void getFileListUPan(UsbFile usbFileSrc) {
        UsbFile[] usbFiles = new UsbFile[0];
        try {
            if (usbFileSrc == null) {
                getUPanDevsList();//如果usbFileSrc为空，获取优盘的文件列表
            } else {
                usbFiles = usbFileSrc.listFiles();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        if (null != usbFiles && usbFiles.length > 0) {
            files = new ArrayList<>();
            for (UsbFile usbFile : usbFiles) {
                Log.i("GetName:", usbFile.getName());
                files.add(usbFile);
            }
            listviewFile.setAdapter(new FileAdapter(MainActivity.this, files));
            listviewFile.setOnItemClickListener(new AdapterView.OnItemClickListener() {
                @Override
                public void onItemClick(AdapterView<?> adapterView, View view, int i, long l) {
                    if (files.get(i).isDirectory()) {
                        tvContent.setText("当前目录：" + files.get(i).getName());
                        getFileListUPan(files.get(i));
                    } else {
                        tvContent.setText("文件名：" + files.get(i).getName() + "大小：" + getPrintSize(files.get(i).getLength()));
                    }
                }
            });
            listviewFile.setOnItemLongClickListener(new AdapterView.OnItemLongClickListener() {
                @Override
                public boolean onItemLongClick(AdapterView<?> adapterView, View view, int i, long l) {
                    showDelDialog(files.get(i).getName(), MainActivity.this, i);
                    return true;
                }
            });
        }else {
            Toast.makeText(MainActivity.this,"当前目录下没有文件！",Toast.LENGTH_LONG).show();
        }
    }

    /**
     * 将获取的文件大小从字节转换到MB
     * @param size
     * @return
     */
    public static String getPrintSize(long size) {
        //如果字节数少于1024，则直接以B为单位，否则先除于1024，后3位因太少无意义
        if (size < 1024) {
            return String.valueOf(size) + "B";
        } else {
            size = size / 1024;
        }
        //如果原字节数除于1024之后，少于1024，则可以直接以KB作为单位
        //因为还没有到达要使用另一个单位的时候
        //接下去以此类推
        if (size < 1024) {
            return String.valueOf(size) + "KB";
        } else {
            size = size / 1024;
        }
        if (size < 1024) {
            //因为如果以MB为单位的话，要保留最后1位小数，
            //因此，把此数乘以100之后再取余
            size = size * 100;
            return String.valueOf((size / 100)) + "."
                    + String.valueOf((size % 100)) + "MB";
        } else {
            //否则如果要以GB为单位的，先除于1024再作同样的处理
            size = size * 100 / 1024;
            return String.valueOf((size / 100)) + "."
                    + String.valueOf((size % 100)) + "GB";
        }
    }

    /**
     * 长按删除对话框
     */
    private void showDelDialog(String content, Activity activity, final int id) {
        final AlertDialog dialog = new AlertDialog.Builder(activity).create();
        View view = activity.getLayoutInflater().inflate(R.layout.dialog_upload_again, null);
        Button btnYes = (Button) view.findViewById(R.id.btn_del);
        Button btnNo = (Button) view.findViewById(R.id.btn_rename);
        final EditText tvContent = (EditText) view.findViewById(R.id.txt_content);
        TextView tvClose = (TextView) view.findViewById(R.id.tv_close);
        tvContent.setText(content);
        View.OnClickListener listener = new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                switch (v.getId()) {
                    case R.id.btn_del:
                        //删除
                        try {
                            files.get(id).delete();
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                        getFileListUPan(cFolder);
                        break;
                    case R.id.btn_rename:
                        //重命名
                        try {
                            files.get(id).setName(tvContent.getText().toString().trim());
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                        getFileListUPan(cFolder);
                        break;
                    case R.id.tv_close:
                        //关闭对话框
                        dialog.dismiss();
                        break;
                    default:
                        break;
                }
                dialog.dismiss();
            }
        };
        view.findViewById(R.id.btn_del).setOnClickListener(listener);
        view.findViewById(R.id.btn_rename).setOnClickListener(listener);
        view.findViewById(R.id.tv_close).setOnClickListener(listener);
        dialog.show();
        dialog.setContentView(view);

    }

    /**
     * 复制文件到优盘
     */
    private void copyFile2Upan() {
        String path = "/sdcard/SD卡.txt";
        File file1 = new File(path);
        if (!file1.exists()) {
            FileOutputStream outStream = null;
            try {
                outStream = new FileOutputStream(file1);
                outStream.write("SD卡上的测试文件，复制到优盘！".getBytes());
                outStream.close();
            } catch (FileNotFoundException e) {
                e.printStackTrace();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        try {
            if (cFolder == null) {
                getUPanDevsList();
            }
            UsbFile file = cFolder.createFile("SD卡.txt");
            FileInputStream input = new FileInputStream(path);
            OutputStream output = new UsbFileOutputStream(file);
            byte[] buffer = new byte[2048];
            while (input.read(buffer) > 0) {
                output.write(buffer);
            }
            input.close();
            output.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
        getFileListUPan(cFolder);
    }

    /**
     * 读取优盘数据
     */
    private void readFromUDisk() {
        UsbFile[] usbFiles = new UsbFile[0];
        try {
            usbFiles = cFolder.listFiles();
        } catch (IOException e) {
            e.printStackTrace();
        }
        if (null != usbFiles && usbFiles.length > 0) {
            for (UsbFile usbFile : usbFiles) {
                Log.i("GetName:", usbFile.getName());
                if (usbFile.getName().equals(U_DISK_FILE_NAME)) {
                    readTxtFromUDisk(usbFile);
                }
            }
        }
    }

    /**
     * 保存文本到优盘
     *
     * @param content
     */
    private void saveToUDisk(String content) {
        UsbFile[] usbFiles = new UsbFile[0];
        try {
            usbFiles = cFolder.listFiles();
        } catch (IOException e) {
            e.printStackTrace();
        }
        if (null != usbFiles && usbFiles.length > 0) {
            for (UsbFile usbFile : usbFiles) {
                Log.i("GetName:", usbFile.getName());
                if (usbFile.getName().equals(U_DISK_FILE_NAME)) {
                    saveTxtToUDisk(content, usbFile);
                } else {
                    try {
                        UsbFile file = cFolder.createFile(U_DISK_FILE_NAME);
                        saveTxtToUDisk(content, file);
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            }
        }
    }

    /**
     * 保存文本到文件
     *
     * @param toSaveString
     * @param usbFile
     */
    private void saveTxtToUDisk(String toSaveString, UsbFile usbFile) {
        UsbFile descFile = usbFile;
        //读取文件内容
        OutputStream out = new UsbFileOutputStream(descFile);
        try {
            out.write(toSaveString.getBytes());
            out.close();
            mHandler.sendEmptyMessage(100);
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }


    private void readTxtFromUDisk(UsbFile usbFile) {
        UsbFile descFile = usbFile;
        //读取文件内容
        InputStream is = new UsbFileInputStream(descFile);
        //读取秘钥中的数据进行匹配
        StringBuilder sb = new StringBuilder();
        BufferedReader bufferedReader = null;
        try {
            bufferedReader = new BufferedReader(new InputStreamReader(is));
            String read;
            while ((read = bufferedReader.readLine()) != null) {
                sb.append(read);
            }
            Message msg = mHandler.obtainMessage();
            msg.what = 101;
            msg.obj = sb;
            Log.i("Txt:", sb.toString());
            mHandler.sendMessage(msg);
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            try {
                if (bufferedReader != null) {
                    bufferedReader.close();
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    /**
     * 获取USB设备
     *
     * @param device
     */
    private void readDevice(UsbMassStorageDevice device) {
        try {
            Log.i("USB:", "readDevice");
            device.init();//初始化
            //设备分区
            Partition partition = device.getPartitions().get(0);
            //文件系统
            FileSystem currentFs = partition.getFileSystem();
            currentFs.getVolumeLabel();//可以获取到设备的标识
            //通过FileSystem可以获取当前U盘的一些存储信息，包括剩余空间大小，容量等等
            Log.i("Capacity: ", currentFs.getCapacity() + "");
            Log.i("Occupied Space: ", currentFs.getOccupiedSpace() + "");
            Log.i("Free Space: ", currentFs.getFreeSpace() + "");
            Log.i("Chunk size: ", currentFs.getChunkSize() + "");
            cFolder = currentFs.getRootDirectory();//设置当前文件对象为根目录
            getFileListUPan(cFolder);//获取优盘中的文件列表
            Log.i("USB:", cFolder.getName());
        } catch (Exception e) {
            e.printStackTrace();
            Log.i("USB:", e.getMessage());
        }
    }

    /**
     * @description OTG广播注册
     * @author hongzhen
     * @time 2017/12/11 17:20
     */
    private void registerUDiskReceiver() {
        //监听otg插入 拔出
        IntentFilter usbDeviceStateFilter = new IntentFilter();
        usbDeviceStateFilter.addAction(UsbManager.ACTION_USB_DEVICE_ATTACHED);
        usbDeviceStateFilter.addAction(UsbManager.ACTION_USB_DEVICE_DETACHED);
        registerReceiver(mOtgReceiver, usbDeviceStateFilter);
        //注册监听自定义广播
        IntentFilter filter = new IntentFilter(ACTION_USB_PERMISSION);
        registerReceiver(mOtgReceiver, filter);
    }

    /**
     * @description OTG广播，监听U盘的插入及拔出
     * @author hongzhen
     * @time 2017/12/11 17:20
     * @param
     */
    private BroadcastReceiver mOtgReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            switch (action) {
                case ACTION_USB_PERMISSION://接受到自定义广播
                    UsbDevice usbDevice = intent.getParcelableExtra(UsbManager.EXTRA_DEVICE);
                    //允许权限申请
                    if (intent.getBooleanExtra(UsbManager.EXTRA_PERMISSION_GRANTED, false)) {
                        if (usbDevice != null) {
                            //用户已授权，可以进行读取操作
                            Log.i("USB:", "已授权");
                            readDevice(getUsbMass(usbDevice));
                        } else {
                            showToastMsg("没有插入U盘");
                        }
                    } else {
                        showToastMsg("未获取到U盘权限");
                    }
                    break;
                case UsbManager.ACTION_USB_DEVICE_ATTACHED://接收到U盘设备插入广播
                    UsbDevice device_add = intent.getParcelableExtra(UsbManager.EXTRA_DEVICE);
                    if (device_add != null) {
                        //接收到U盘插入广播，尝试读取U盘设备数据
                        showToastMsg("U盘插入");
                        getUPanDevsList();
                    }
                    break;
                case UsbManager.ACTION_USB_DEVICE_DETACHED://接收到U盘设设备拔出广播
                    showToastMsg("U盘已拔出");
                    break;
                default:
            }
        }
    };

    /**
     * @description U盘设备读取
     * @author hongzhen
     * @time 2017/12/11 17:20
     */
    private void getUPanDevsList() {
        //设备管理器
        UsbManager usbManager = (UsbManager) getSystemService(Context.USB_SERVICE);
        //获取U盘存储设备
        storageDevices = UsbMassStorageDevice.getMassStorageDevices(this);
        PendingIntent pendingIntent = PendingIntent.getBroadcast(this, 0, new Intent(ACTION_USB_PERMISSION), 0);
        //一般手机只有1个OTG插口
        for (UsbMassStorageDevice device : storageDevices) {
            //读取设备是否有权限
            if (usbManager.hasPermission(device.getUsbDevice())) {
                readDevice(device);
            } else {
                //没有权限，进行申请
                usbManager.requestPermission(device.getUsbDevice(), pendingIntent);
            }
        }
        if (storageDevices.length == 0) {
            showToastMsg("请插入可用的U盘");
        }
    }

    private UsbMassStorageDevice getUsbMass(UsbDevice usbDevice) {
        for (UsbMassStorageDevice device : storageDevices) {
            if (usbDevice.equals(device.getUsbDevice())) {
                return device;
            }
        }
        return null;
    }


    private void showToastMsg(String msg) {
        Toast.makeText(this, msg, Toast.LENGTH_SHORT).show();
    }
}
