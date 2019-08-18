package com.example.textdetectionsteps;

import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.graphics.Matrix;
import android.media.ExifInterface;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.Gravity;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;

import org.opencv.android.BaseLoaderCallback;
import org.opencv.android.LoaderCallbackInterface;
import org.opencv.android.OpenCVLoader;
import org.opencv.android.Utils;
import org.opencv.core.CvType;
import org.opencv.core.KeyPoint;
import org.opencv.core.Mat;
import org.opencv.core.MatOfInt;
import org.opencv.core.MatOfKeyPoint;
import org.opencv.core.MatOfPoint;
import org.opencv.core.MatOfPoint2f;
import org.opencv.core.Point;
import org.opencv.core.Rect;
import org.opencv.core.RotatedRect;
import org.opencv.core.Scalar;
import org.opencv.core.Size;
import org.opencv.features2d.MSER;
import org.opencv.imgproc.Imgproc;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import static java.lang.Math.pow;
import static java.lang.Math.sqrt;

public class ProcessActivity extends AppCompatActivity {

    private static final String TAG = ProcessActivity.class.getSimpleName();
    private Button btnCrop;
    private ImageView imgMSER, imgRemove;
    private LinearLayout layout;
    private LinearLayout.LayoutParams lparamsImg;
    private Bitmap bitmap;
    private Bitmap bitmapCrop;
    private String path;
    private Mat imageMat;
    private Mat imageMat2;
    private Mat croppedPart;
    private MatOfInt hull;
    private Scalar CONTOUR_COLOR = new Scalar(255, 255, 255);


    private BaseLoaderCallback mLoaderCallback = new BaseLoaderCallback(this) {
        @Override
        public void onManagerConnected(int status) {
            switch (status) {
                case LoaderCallbackInterface
                        .SUCCESS: {
                    imageMat = new Mat();
                    imageMat2 = new Mat();
                }

                break;

                default: {
                    super.onManagerConnected(status);
                }

                break;
            }

        }
    };

    @Override
    protected void onResume() {

        Log.e("onResume","inside");
        super.onResume();

    }


    public static Intent getIntent(Context context){
        return new Intent(context,ProcessActivity.class);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_process);

        if (!OpenCVLoader.initDebug()) {
            Log.d(TAG, "OpenCV problem");
            OpenCVLoader.initAsync(OpenCVLoader.OPENCV_VERSION, this, mLoaderCallback);
            Log.e("onResume","not init");

        } else {
            Log.d(TAG, "OpenCV initiated success");
            mLoaderCallback.onManagerConnected(LoaderCallbackInterface.SUCCESS);
            Log.e("onResume","init");
        }

        //btnCrop = findViewById(R.id.btn_crop);
        layout = findViewById(R.id.layout);
        imgMSER = findViewById(R.id.img_mser);
        imgRemove = findViewById(R.id.img_after_regionprop);

        lparamsImg = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        lparamsImg.setMargins(32,8,4,8);

        path = getIntent().getStringExtra("BitmapImage");

        ExifInterface exifInterface = null;

        BitmapFactory.Options options = new BitmapFactory.Options();
        options.inSampleSize = 2;

        bitmap = BitmapFactory.decodeFile(path, options);


        try {
            exifInterface = new ExifInterface(path);


        } catch (IOException e) {
            Log.d("Error", "IO problem");
        }

        int orientation = exifInterface.getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_UNDEFINED);
        switch (orientation) {
            case ExifInterface.ORIENTATION_ROTATE_90:
                bitmap = rotateImage(bitmap, 90);
                break;
            case ExifInterface.ORIENTATION_ROTATE_180:
                bitmap = rotateImage(bitmap, 180);
                break;
            case ExifInterface.ORIENTATION_ROTATE_270:
                bitmap = rotateImage(bitmap, 270);
                break;
            case ExifInterface.ORIENTATION_NORMAL:
            default:
                break;

        }

        Log.e("mat is null"," "+imageMat);
        Utils.bitmapToMat(bitmap, imageMat);
        detectText(imageMat);



//        btnCrop.setOnClickListener(new View.OnClickListener() {
//            @Override
//            public void onClick(View v) {
//                Intent intent = ProcessActivity.getIntent(getApplicationContext());
//                intent.putExtra("BitmapImage",path);
    //        }
 //       });

    }

    public void detectText(Mat mat) {

        Imgproc.cvtColor(imageMat, imageMat2, Imgproc.COLOR_RGB2GRAY);
        Mat mRgb = mat;
        Mat mRgbMSER = mRgb.clone();

        Mat mGray = imageMat2;

        MatOfKeyPoint keyPoint = new MatOfKeyPoint();

        List<KeyPoint> pointList;
        KeyPoint kPoint;

        Mat mask = Mat.zeros(mGray.size(), CvType.CV_8UC1);

        RotatedRect rectanf = null;

        int rectanX;
        int rectanY;
        int rectanW;
        int rectanH;


        List<MatOfPoint> contour = new ArrayList<>();
        Mat kernel = new Mat(1, 50, CvType.CV_8UC1, Scalar.all(255));
        Mat morbyte = new Mat();
        Mat hierarchy = new Mat();



//        @SuppressWarnings("deprecation")
//        FeatureDetector detector = FeatureDetector.create(FeatureDetector.MSER);
//        detector.detect(mGray, keyPoint);

        MSER mser = MSER.create(5,60,14400,0.15,0.2,200,1.10,0.003,5);
        mser.detect(mGray, keyPoint);
        //convert MatOfKeyPoint to list of KeyPoint
        pointList = keyPoint.toList();


        for (int i = 0; i < pointList.size(); i++) {
            kPoint = pointList.get(i);
            if (kPoint.size > 0) {
                rectanX = (int) (kPoint.pt.x - 0.5 * kPoint.size);
                rectanY = (int) (kPoint.pt.y - 0.5 * kPoint.size);
                rectanW = (int) (kPoint.size);
                rectanH = (int) (kPoint.size);
                if (rectanX <= 0)
                    rectanX = 1;
                if (rectanY <= 0)
                    rectanY = 1;
                if ((rectanX + rectanW) > mGray.width())
                    rectanW = mGray.width() - rectanX;
                if ((rectanY + rectanH) > mGray.height())
                    rectanH = mGray.height() - rectanY;

                Rect rectan = new Rect(rectanX, rectanY, rectanW, rectanH);
                Mat roi = new Mat(mask, rectan);
                Log.e("Area rect",i+" "+rectan.area());

                roi.setTo(CONTOUR_COLOR);
            }
        }

        Imgproc.morphologyEx(mask, morbyte, Imgproc.MORPH_DILATE, kernel);//Imgproc.RETR_EXTERNAL Imgproc.CHAIN_APPROX_NONE
        Imgproc.findContours(morbyte, contour, hierarchy, Imgproc.RETR_EXTERNAL, Imgproc.CHAIN_APPROX_NONE );

        croppedPart = new Mat();
        bitmapCrop = null;

        MatOfPoint2f mMOP2f = new MatOfPoint2f();
        for (int i = contour.size()-1 ; i >=0 ; i--) {
            contour.get(i).convertTo(mMOP2f, CvType.CV_32FC2);

            rectanf = Imgproc.minAreaRect(mMOP2f);

            mMOP2f.convertTo(contour.get(i), CvType.CV_32S);

            drawRect(rectanf, mRgbMSER);

            if ((rectanf.size.width * rectanf.size.height >= 1000)
                    && (eccentricity(mMOP2f) >= 0.9)    && (solidity(contour.get(i)) < 99) && extent(contour.get(i), rectanf) < 1) {
//0.9
                drawRect(rectanf, mRgb);
                rotateAndCropText(rectanf, mRgb);


            }
        }

        Bitmap bitmapMSER = Bitmap.createBitmap(mRgbMSER.cols(),mRgbMSER.rows(),Bitmap.Config.ARGB_8888);
        Utils.matToBitmap(mRgbMSER,bitmapMSER);
        imgMSER.setImageBitmap(bitmapMSER);

        Bitmap bitmapRemove = Bitmap.createBitmap(mRgb.cols(),mRgb.rows(),Bitmap.Config.ARGB_8888);
        Utils.matToBitmap(mRgb,bitmapRemove);
        imgRemove.setImageBitmap(bitmapRemove);


        LinearLayout.LayoutParams lparams = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        lparams.setMargins(4,64,4,16);
        lparams.gravity = Gravity.CENTER_HORIZONTAL;

        Button btnCapture = new Button(this);
        btnCapture.setLayoutParams(lparams);
        btnCapture.setPadding(4,4,4,4);
        btnCapture.setText("Capture again");
        btnCapture.setBackgroundColor(getResources().getColor(R.color.colorPrimary));
        btnCapture.setTextColor(Color.WHITE);
        layout.addView(btnCapture);

        btnCapture.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = ShowImageActivity.getIntent(getApplicationContext());
                startActivity(intent);
                finish();
            }
        });



    }

    public Bitmap rotateImage(Bitmap img, float angle) {
        Matrix matrix = new Matrix();
        matrix.postRotate(angle);
        return Bitmap.createBitmap(img, 0, 0, img.getWidth(), img.getHeight(), matrix, true);
    }

    public void drawRect(RotatedRect rect, Mat mRgb){

        rect.size.height += 10;
        rect.size.width += 5;
        Point[] pts = new Point[4];
        rect.points(pts);
        for (int j = 0; j < 4; j++) {
            Imgproc.line(mRgb, new Point(pts[j].x, (int) pts[j].y), new Point((int) pts[(j + 1) % 4].x, (int) pts[(j + 1) % 4].y), CONTOUR_COLOR, 2);
        }

    }

    public double eccentricity( MatOfPoint2f contour )
    {
        RotatedRect ellipse = Imgproc.fitEllipse(contour);
        double a = ellipse.size.height / 2;
        double b = ellipse.size.width / 2;
        double eccentricity = sqrt(pow(a, 2)-pow(b, 2));
        eccentricity = eccentricity/a;

        return eccentricity;
    }





    public double solidity( MatOfPoint contour ){

        hull = new MatOfInt();
        double area = Imgproc.contourArea(contour);

        Imgproc.convexHull(contour, hull);
        MatOfPoint mopHull = new MatOfPoint();
        mopHull.create((int) hull.size().height, 1, CvType.CV_32SC2);

        for (int j = 0; j < hull.size().height; j++) {
            int index = (int) hull.get(j, 0)[0];
            double[] point = new double[] { contour.get(index, 0)[0], contour.get(index, 0)[1] };
            mopHull.put(j, 0, point);
        }

        double solid = 100 * area / Imgproc.contourArea(mopHull);

        return solid;

    }

    public double extent(MatOfPoint contour, RotatedRect rect){

        return Imgproc.contourArea(contour) / (rect.size.width * rect.size.height) ;
    }

    public void rotateAndCropText(RotatedRect rect, Mat mRgb){

        try {

               //Rotate text

                    double angle = rect.angle;

                    if (rect.size.width < rect.size.height)
                        angle = 90 + angle;

                    Mat rot_mat = Imgproc.getRotationMatrix2D(rect.center, angle, 1);
                    Mat rotated = new Mat();
                    Mat rotated3 = new Mat();


                    Imgproc.warpAffine(mRgb, rotated, rot_mat, mRgb.size(), Imgproc.INTER_CUBIC);

                    //Crop image
                    Size box_size = rect.size;
                    if (rect.angle < -45.) {
                        //std::swap(box_size.width, box_size.height);
                        double aux = box_size.width;
                        box_size.width = box_size.height;
                        box_size.height = aux;
                    }
                    Imgproc.cvtColor(rotated, rotated3, Imgproc.COLOR_RGBA2RGB);

                    Imgproc.getRectSubPix(rotated3, box_size, rect.center, croppedPart);


                    Imgproc.cvtColor(croppedPart,croppedPart,Imgproc.COLOR_RGB2GRAY);

                    bitmapCrop = Bitmap.createBitmap(croppedPart.width(), croppedPart.height(), Bitmap.Config.ARGB_8888);
                    Utils.matToBitmap(croppedPart, bitmapCrop);



            ImageView imageview = new ImageView(this);
            imageview.setLayoutParams(lparamsImg);

            imageview.setImageBitmap(bitmapCrop);
            layout.addView(imageview);



                } catch (Exception e) {
                    Log.d(TAG, "cropped part data error " + e.getMessage());
                }

    }


}
