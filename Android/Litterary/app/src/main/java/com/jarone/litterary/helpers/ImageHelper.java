package com.jarone.litterary.helpers;

import android.content.Context;
import android.graphics.Bitmap;
import android.opengl.GLES30;
import android.opengl.GLSurfaceView;
import android.util.TypedValue;

import com.jarone.litterary.R;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.ShortBuffer;

import javax.microedition.khronos.egl.EGL10;
import javax.microedition.khronos.egl.EGLContext;
import javax.microedition.khronos.opengles.GL10;

/**
 * Created by vicvu on 16-03-02.
 */
public class ImageHelper {

    public static Bitmap runningBitmap;
    private static ByteBuffer runningByteBuffer;
    private static short[] sBuffer;
    private static ShortBuffer sb;

    private static int[] pixelsBuffer;

    private static int height = -1;
    private static int width = -1;

    public static float getDP(Context c, int dp) {
        return TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, dp, c.getResources().getDisplayMetrics());
    }

    public interface BitmapCreatedCallback {
        void onBitmapCreated(Bitmap bitmap);
    }

    private static class CreateBitmapRunnable implements Runnable {

        private BitmapCreatedCallback bitmapCreatedCallback;

        public void setNewBitmap(BitmapCreatedCallback bitmapCreatedCallback) {
            this.bitmapCreatedCallback = bitmapCreatedCallback;
        }

        @Override
        public void run() {
            GL10 gl;
            EGL10 egl = (EGL10) EGLContext.getEGL();
            gl = (GL10) egl.eglGetCurrentContext().getGL();
            Bitmap frame = ImageHelper.getBitmapFromGLSurface(width, height, gl);
            bitmapCreatedCallback.onBitmapCreated(frame);
        }

    }

    private static CreateBitmapRunnable createBitmapRunnable = new CreateBitmapRunnable();


    // Create a bitmap from the current surface frame.
    public static void createBitmapFromFrame(final BitmapCreatedCallback bitmapCreatedCallback, final GLSurfaceView surface) {
        if ((width == -1 || width == 0) || (height == 0 || height == -1)) {
            width = surface.getWidth();
            height = surface.getHeight();
        }
        createBitmapRunnable.setNewBitmap(bitmapCreatedCallback);
        if (surface.isEnabled()) {
            surface.queueEvent(createBitmapRunnable);
        } else {
            bitmapCreatedCallback.onBitmapCreated(null);
        }
    }

    private static int surfaceHeight = -1;
    private static int surfaceWidth = -1;

    public static Bitmap getBitmapFromGLSurface(int w, int h, GL10 gl) {

        if (w == 0 || h == 0) {
            return null;
        }

        if (surfaceHeight <= 0 || surfaceWidth <= 0) {
            surfaceHeight = ContextManager.getMainActivityInstance().findViewById(R.id.surface_layout).getHeight();
            surfaceWidth = ContextManager.getMainActivityInstance().findViewById(R.id.surface_layout).getWidth();
        }


        if (runningBitmap == null) {
            runningBitmap = Bitmap.createBitmap(w, h, Bitmap.Config.RGB_565);
        }

        if (runningByteBuffer == null) {
            runningByteBuffer = ByteBuffer.allocateDirect(w * h * 4);
            runningByteBuffer = runningByteBuffer.order(ByteOrder.nativeOrder());
        }

        if (pixelsBuffer == null) {
            pixelsBuffer = new int[w * h];
        }

        if (sBuffer == null) {
            sBuffer = new short[w * h];
        }

        if (sb == null) {
            sb = ShortBuffer.wrap(sBuffer);
        }

        gl.glReadPixels(0, 0, w, h, GLES30.GL_RGBA, GLES30.GL_UNSIGNED_BYTE, runningByteBuffer);
        runningByteBuffer.asIntBuffer().get(pixelsBuffer);
        runningBitmap.setPixels(pixelsBuffer, (w * h) - w, -w, 0, 0, w, h);
        runningBitmap.copyPixelsToBuffer(sb);
        for (int i = 0; i < w * h; ++i) {
            short v = sBuffer[i];
            sBuffer[i] = (short) (((v & 0x1f) << 11) | (v & 0x7e0) | ((v & 0xf800) >> 11));
        }
        sb.rewind();
        runningByteBuffer.clear();
        runningBitmap.copyPixelsFromBuffer(sb);
        sb.clear();
        return Bitmap.createScaledBitmap(runningBitmap, surfaceWidth, surfaceHeight, false);
    }

}
