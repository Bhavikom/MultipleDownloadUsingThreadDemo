package com.constraint.multipledownloadusingthreaddemo;

import android.Manifest;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Message;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.TextView;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.ByteArrayOutputStream;
import java.io.Closeable;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URL;
import java.net.URLConnection;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

public class MainActivity extends Activity {

    int timeout = 90000;
    boolean isRunning=true;
    private ListView lstView;
    private ImageAdapter imageAdapter;
    private Handler handler = new Handler();;

    public static final int DIALOG_DOWNLOAD_THUMBNAIL_PROGRESS = 0;
    private ProgressDialog mProgressDialog;

    ArrayList<HashMap<String, Object>> MyArrList = new ArrayList<HashMap<String, Object>>();

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_main);


        new LoadContentFromServer().execute();
    }


    public void ShowThumbnailData()
    {
        // ListView and imageAdapter
        lstView = (ListView) findViewById(R.id.listView1);
        lstView.setClipToPadding(false);
        imageAdapter = new ImageAdapter(getApplicationContext());
        lstView.setAdapter(imageAdapter);
    }


    public void startDownload(final int position) {

        isRunning=true;
        Runnable runnable = new Runnable() {
            int Status = 0;
            public void run() {
                String urlDownload = MyArrList.get(position).get("ImagePathFull").toString();
                int count = 0;
                try {

                    URL url = new URL(urlDownload);
                    URLConnection connection = url.openConnection();
                    connection.setDoInput(true);
                    connection.setDoOutput(true);
                    connection.setReadTimeout(timeout);
                    connection.setConnectTimeout(timeout);
                    connection.connect();

                    ActivityCompat.requestPermissions(MainActivity.this,
                            new String[] {
                                    Manifest.permission.READ_EXTERNAL_STORAGE,
                                    Manifest.permission.WRITE_EXTERNAL_STORAGE
                            },
                            100);
                    String storagePath = Environment.getExternalStorageDirectory().getAbsolutePath();
                    String rootPath = storagePath + "/test2";
                    String fileName = urlDownload.substring(urlDownload.lastIndexOf('/')+1, urlDownload.length() );

                    File root = new File(rootPath);
                    if(!root.mkdirs()) {
                        Log.i("Test", "This path is already exist: " + root.getAbsolutePath());
                    }
                    File file = new File(rootPath + fileName);
                    try {
                        int permissionCheck = ContextCompat.checkSelfPermission(MainActivity.this, android.Manifest.permission.WRITE_EXTERNAL_STORAGE);
                        if (permissionCheck == PackageManager.PERMISSION_GRANTED) {
                            if (!file.createNewFile()) {
                                Log.i("Test", "This file is already exist: " + file.getAbsolutePath());
                            }
                        }

                    } catch (Exception e) {
                        e.printStackTrace();
                    }

                    //OutputStream output = new FileOutputStream(file);
                    OutputStream output;
                    // Get File Name from URL
                    int lenghtOfFile =0;
                    // new code
                    //long downloadedLength = 0;
                    int totalLengthOffile = 0;

                        totalLengthOffile = connection.getContentLength();
                        Log.d("ANDRO_ASYNC", "Lenght of file: " + lenghtOfFile);
                        output = new BufferedOutputStream(new FileOutputStream(file));
                        //totalLengthOffile = connection.getContentLength();
                        //totalLengthOffile = lenghtOfFile;


                    // end new code
                    InputStream input = new BufferedInputStream(url.openStream());

                    byte data[] = new byte[1024];
                    long total = 0;

                    while ((count = input.read(data)) != -1) {

                        if(!isRunning){
                            break;
                        }
                        //lenghtOfFile += count;
                        total += count;
                        Status = (int)((total*100)/totalLengthOffile);
                        output.write(data, 0, count);

                        // Update ProgressBar
                        //final int finalLenghtOfFile = lenghtOfFile;
                        handler.post(new Runnable() {
                            public void run() {
                                updateStatus(position, Status);
                            }
                        });

                    }

                    output.flush();
                    output.close();
                    input.close();

                } catch (Exception e) {
                    Log.e(""," exception while download : "+e.toString());
                }


            }
        };
        new Thread(runnable).start();
    }

    private void updateStatus(int index,int Status){

        View v = lstView.getChildAt(index - lstView.getFirstVisiblePosition());

        // Update ProgressBar
        ProgressBar progress = (ProgressBar)v.findViewById(R.id.progressBar);
        progress.setProgress(Status);

        // Update Text to ColStatus
        TextView txtStatus = (TextView)v.findViewById(R.id.ColStatus);
        txtStatus.setPadding(10, 0, 0, 0);
        txtStatus.setText("Load : " + String.valueOf(Status)+"%");

        // Enabled Button View
        if(Status >= 100)
        {
            Button btnView = (Button)v.findViewById(R.id.btnView);
            btnView.setTextColor(Color.RED);
            btnView.setEnabled(true);
        }
    }


    @Override
    protected Dialog onCreateDialog(int id) {
        switch (id) {
            case DIALOG_DOWNLOAD_THUMBNAIL_PROGRESS:
                mProgressDialog = new ProgressDialog(this);
                mProgressDialog.setMessage("Downloading Thumbnail.....");
                mProgressDialog.setProgressStyle(ProgressDialog.STYLE_SPINNER);
                mProgressDialog.setCancelable(true);
                mProgressDialog.show();
                return mProgressDialog;
            default:
                return null;
        }
    }

    class LoadContentFromServer extends AsyncTask<Object, Integer, Object> {

        protected void onPreExecute() {
            super.onPreExecute();
            showDialog(DIALOG_DOWNLOAD_THUMBNAIL_PROGRESS);
        }

        @Override
        protected Object doInBackground(Object... params) {

            HashMap<String, Object> map;
            Bitmap bm;

            map = new HashMap<String, Object>();
            map.put("ImageID", "1");
            map.put("ImageName", "Image 1");
            map.put("ImagePathThum", "http://www.thaicreate.com/android/img1_thum.jpg");
            bm = loadBitmap("http://www.thaicreate.com/android/img1_thum.jpg");
            map.put("ImageThumBitmap", bm);
            map.put("ImagePathFull", "https://www.gstatic.com/webp/gallery/4.sm.jpg");
            MyArrList.add(map);

            map = new HashMap<String, Object>();
            map.put("ImageID", "2");
            map.put("ImageName", "Image 2");
            map.put("ImagePathThum", "http://www.thaicreate.com/android/img2_thum.jpg");
            bm = loadBitmap("http://www.thaicreate.com/android/img2_thum.jpg");
            map.put("ImageThumBitmap", bm);
            map.put("ImagePathFull", "https://www.gstatic.com/webp/gallery/4.sm.jpg");
            MyArrList.add(map);

            map = new HashMap<String, Object>();
            map.put("ImageID", "3");
            map.put("ImageName", "Image 3");
            map.put("ImagePathThum", "http://www.thaicreate.com/android/img3_thum.jpg");
            bm = loadBitmap("http://www.thaicreate.com/android/img3_thum.jpg");
            map.put("ImageThumBitmap", bm);
            map.put("ImagePathFull", "https://www.gstatic.com/webp/gallery/4.sm.jpg");
            MyArrList.add(map);

            map = new HashMap<String, Object>();
            map.put("ImageID", "4");
            map.put("ImageName", "Image 4");
            map.put("ImagePathThum", "http://www.thaicreate.com/android/img4_thum.jpg");
            bm = loadBitmap("http://www.thaicreate.com/android/img4_thum.jpg");
            map.put("ImageThumBitmap", bm);
            map.put("ImagePathFull", "https://www.gstatic.com/webp/gallery/4.sm.jpg");
            MyArrList.add(map);

            map = new HashMap<String, Object>();
            map.put("ImageID", "5");
            map.put("ImageName", "Image 5");
            map.put("ImagePathThum", "http://www.thaicreate.com/android/img5_thum.jpg");
            bm = loadBitmap("http://www.thaicreate.com/android/img5_thum.jpg");
            map.put("ImageThumBitmap", bm);
            map.put("ImagePathFull", "https://www.gstatic.com/webp/gallery/4.sm.jpg");
            MyArrList.add(map);

            map = new HashMap<String, Object>();
            map.put("ImageID", "6");
            map.put("ImageName", "Image 6");
            map.put("ImagePathThum", "http://www.thaicreate.com/android/img6_thum.jpg");
            bm = loadBitmap("http://www.thaicreate.com/android/img6_thum.jpg");
            map.put("ImageThumBitmap", bm);
            map.put("ImagePathFull", "http://www.sample-videos.com/video/mp4/720/big_buck_bunny_720p_20mb.mp4");
            MyArrList.add(map);

            return null;
        }


        @Override
        protected void onPostExecute(Object result) {
            ShowThumbnailData();
            dismissDialog(DIALOG_DOWNLOAD_THUMBNAIL_PROGRESS);
            removeDialog(DIALOG_DOWNLOAD_THUMBNAIL_PROGRESS);

            ActivityCompat.requestPermissions(MainActivity.this,
                    new String[] {
                            Manifest.permission.READ_EXTERNAL_STORAGE,
                            Manifest.permission.WRITE_EXTERNAL_STORAGE
                    },
                    100);
        }
    }


    class ImageAdapter extends BaseAdapter {

        private Context mContext;

        public ImageAdapter(Context context) {
            mContext = context;
        }

        public int getCount() {
            return MyArrList.size();
        }

        public Object getItem(int position) {
            return MyArrList.get(position);
        }

        public long getItemId(int position) {
            return position;
        }

        public View getView(final int position, View convertView, ViewGroup parent) {
            // TODO Auto-generated method stub

            LayoutInflater inflater = (LayoutInflater) mContext
                    .getSystemService(Context.LAYOUT_INFLATER_SERVICE);


            if (convertView == null) {
                convertView = inflater.inflate(R.layout.activity_column, null);
            }

            // ColImage
            ImageView imageView = (ImageView) convertView.findViewById(R.id.ColImgPath);
            imageView.getLayoutParams().height = 110;
            imageView.getLayoutParams().width = 110;
            imageView.setPadding(10, 10, 10, 10);
            imageView.setScaleType(ImageView.ScaleType.CENTER_CROP);
            try
            {
                imageView.setImageBitmap((Bitmap)MyArrList.get(position).get("ImageThumBitmap"));
            } catch (Exception e) {
                // When Error
                imageView.setImageResource(android.R.drawable.ic_menu_report_image);
            }

            // ColImgID
            TextView txtImgID = (TextView) convertView.findViewById(R.id.ColImgID);
            txtImgID.setPadding(10, 0, 0, 0);
            txtImgID.setText("ID : " + MyArrList.get(position).get("ImageID").toString());

            // ColImgName
            TextView txtPicName = (TextView) convertView.findViewById(R.id.ColImgName);
            txtPicName.setPadding(10, 0, 0, 0);
            txtPicName.setText("Name : " + MyArrList.get(position).get("ImageName").toString());

            // ColStatus
            TextView txtStatus = (TextView) convertView.findViewById(R.id.ColStatus);
            txtStatus.setPadding(10, 0, 0, 0);
            txtStatus.setText("...");

            //btnDownload
            final Button btnDownload = (Button) convertView.findViewById(R.id.btnDownload);
            btnDownload.setTextColor(Color.RED);
            btnDownload.setOnClickListener(new View.OnClickListener() {
                public void onClick(View v) {
                    // Download
                    btnDownload.setEnabled(false);
                    btnDownload.setTextColor(Color.GRAY);

                    startDownload(position);
                }
            });

            //btnView
            Button btnView = (Button) convertView.findViewById(R.id.btnView);
            btnView.setEnabled(false);
            btnView.setTextColor(Color.GRAY);
            btnView.setOnClickListener(new View.OnClickListener() {
                public void onClick(View v) {
                    ViewImageSDCard(position);
                }
            });

            // progressBar
            ProgressBar progress = (ProgressBar) convertView.findViewById(R.id.progressBar);
            progress.setPadding(10, 0, 0, 0);

            return convertView;

        }

    }

    // View Image from SD Card
    public void ViewImageSDCard(int position) {
        final AlertDialog.Builder imageDialog = new AlertDialog.Builder(this);
        final LayoutInflater inflater = (LayoutInflater) this
                .getSystemService(LAYOUT_INFLATER_SERVICE);

        View layout = inflater.inflate(R.layout.custom_fullimage_dialog,
                (ViewGroup) findViewById(R.id.layout_root));
        ImageView image = (ImageView) layout.findViewById(R.id.fullimage);

        String urlDownload = MyArrList.get(position).get("ImagePathFull").toString();

        // Get File Name from URL
        String fileName = urlDownload.substring(urlDownload.lastIndexOf('/')+1, urlDownload.length() );

        String strPath = "/mnt/sdcard/mydata/" + fileName;
        Bitmap bm = BitmapFactory.decodeFile(strPath); // Path from SDCard
        image.setScaleType(ImageView.ScaleType.CENTER_CROP);

        image.setImageBitmap(bm);

        String strName = MyArrList.get(position).get("ImageName").toString();
        imageDialog.setIcon(android.R.drawable.btn_star_big_on);
        imageDialog.setTitle("View : " + strName);
        imageDialog.setView(layout);
        imageDialog.setPositiveButton(android.R.string.ok,
                new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int which) {
                        dialog.dismiss();
                    }
                });
        imageDialog.create();
        imageDialog.show();
    }



    /***** Get Image Resource from URL (Start) *****/
    private static final String TAG = "Image";
    private static final int IO_BUFFER_SIZE = 4 * 1024;
    public static Bitmap loadBitmap(String url) {
        Bitmap bitmap = null;
        InputStream in = null;
        BufferedOutputStream out = null;

        try {
            in = new BufferedInputStream(new URL(url).openStream(), IO_BUFFER_SIZE);

            final ByteArrayOutputStream dataStream = new ByteArrayOutputStream();
            out = new BufferedOutputStream(dataStream, IO_BUFFER_SIZE);
            copy(in, out);
            out.flush();

            final byte[] data = dataStream.toByteArray();
            BitmapFactory.Options options = new BitmapFactory.Options();
            //options.inSampleSize = 1;

            bitmap = BitmapFactory.decodeByteArray(data, 0, data.length,options);
        } catch (IOException e) {
            Log.e(TAG, "Could not load Bitmap from: " + url);
        } finally {
            closeStream(in);
            closeStream(out);
        }

        return bitmap;
    }

    private static void closeStream(Closeable stream) {
        if (stream != null) {
            try {
                stream.close();
            } catch (IOException e) {
                android.util.Log.e(TAG, "Could not close stream", e);
            }
        }
    }

    private static void copy(InputStream in, OutputStream out) throws IOException {
        byte[] b = new byte[IO_BUFFER_SIZE];
        int read;
        while ((read = in.read(b)) != -1) {
            out.write(b, 0, read);
        }
    }
    /***** Get Image Resource from URL (End) *****/

}