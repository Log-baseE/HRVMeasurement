package com.app.hrv;

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.graphics.SurfaceTexture;
import android.hardware.camera2.CameraAccessException;
import android.hardware.camera2.CameraCaptureSession;
import android.hardware.camera2.CameraCharacteristics;
import android.hardware.camera2.CameraDevice;
import android.hardware.camera2.CameraManager;
import android.hardware.camera2.CameraMetadata;
import android.hardware.camera2.CaptureRequest;
import android.os.Bundle;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.util.Size;
import android.view.Surface;
import android.view.View;
import android.widget.Button;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Semaphore;

public class MainActivity extends AppCompatActivity {
    private CameraManager cameraManager;
    private CameraCharacteristics cameraCharacteristics;

    private CameraDevice mCameraDevice;
    private CameraCaptureSession mSession;

    private CaptureRequest.Builder mBuilder;
    private Semaphore mCameraOpenCloseLock = new Semaphore(1);

    private Button on;
    private Button off;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        on = (Button) findViewById(R.id.on);
        off = (Button) findViewById(R.id.off);

        initCamera();
    }

    private void initCamera() {
        cameraManager = (CameraManager) getSystemService(Context.CAMERA_SERVICE);
        try {
            String[] id = cameraManager.getCameraIdList();
            if (id != null && id.length > 0) {
                cameraCharacteristics = cameraManager.getCameraCharacteristics(id[0]);
                boolean isFlash = cameraCharacteristics.get(CameraCharacteristics.FLASH_INFO_AVAILABLE);
                if (isFlash) {
                    if (ActivityCompat.checkSelfPermission(this, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
                        return;
                    }
                    cameraManager.openCamera(id[0], new MyCameraDeviceStateCallback(), null);
                }
            }
        } catch (CameraAccessException e) {
            e.printStackTrace();
        }
    }

    class MyCameraDeviceStateCallback extends CameraDevice.StateCallback {
        @Override
        public void onOpened(CameraDevice camera) {
            mCameraDevice = camera;
            try {
                mBuilder = camera.createCaptureRequest(CameraDevice.TEMPLATE_PREVIEW);
                List<Surface> list = new ArrayList<Surface>();
                SurfaceTexture mSurfaceTexture = new SurfaceTexture(1);
                Size size = getSmallestSize(mCameraDevice.getId());
                mSurfaceTexture.setDefaultBufferSize(size.getWidth(), size.getHeight());
                Surface mSurface = new Surface(mSurfaceTexture);
                list.add(mSurface);
                mBuilder.addTarget(mSurface);
                camera.createCaptureSession(list, new MyCameraCaptureSessionStateCallback(), null);
            }
            catch (CameraAccessException e) {
                e.printStackTrace();
            }
        }

        @Override
        public void onDisconnected(CameraDevice camera) {}

        @Override
        public void onError(CameraDevice camera, int error) {}
    }

    private Size getSmallestSize(String cameraId) throws CameraAccessException {
        Size[] outputSizes = cameraManager.getCameraCharacteristics(cameraId).get(CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP).getOutputSizes(SurfaceTexture.class);
        if (outputSizes == null || outputSizes.length == 0) {
            throw new IllegalStateException("Camera " + cameraId + "doesn't support any outputSize.");
        }
        Size chosen = outputSizes[0];
        for (Size s : outputSizes) {
            if (chosen.getWidth() >= s.getWidth() && chosen.getHeight() >= s.getHeight()) {
                chosen = s;
            }
        }
        return chosen;
    }

    class MyCameraCaptureSessionStateCallback extends CameraCaptureSession.StateCallback {
        @Override
        public void onConfigured(CameraCaptureSession session) {
            mSession = session;
            try {
                mSession.setRepeatingRequest(mBuilder.build(), null, null);
            }
            catch (CameraAccessException e) {
                e.printStackTrace();
            }
        }

        @Override
        public void onConfigureFailed(CameraCaptureSession session) {}
    }

    public void turnOnFlashLight(View view) {
        try {
            mBuilder.set(CaptureRequest.FLASH_MODE, CameraMetadata.FLASH_MODE_TORCH);
            mSession.setRepeatingRequest(mBuilder.build(), null, null);
        }
        catch (Exception e) {
            e.printStackTrace();
            Log.e("Flash", "On");
        }
    }

    public void turnOffFlashLight(View view) {
        try {
            mBuilder.set(CaptureRequest.FLASH_MODE, CameraMetadata.FLASH_MODE_OFF);
            mSession.setRepeatingRequest(mBuilder.build(), null, null);
        }
        catch (Exception e) {
            e.printStackTrace();
            Log.e("Flash", "Off");
        }
    }

    public void close() {
        if (mCameraDevice == null || mSession == null) {
            return;
        }
        mSession.close();
        mCameraDevice.close();
        mCameraDevice = null;
        mSession = null;
    }
}
