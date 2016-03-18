package com.jarone.litterary.helpers;

import android.graphics.Bitmap;
import android.opengl.GLES30;
import android.opengl.GLSurfaceView;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;

import javax.microedition.khronos.egl.EGL10;
import javax.microedition.khronos.egl.EGLContext;
import javax.microedition.khronos.opengles.GL10;

/**
 * Created by vicvu on 16-03-02.
 */
public class ImageHelper {

    public static Bitmap runningBitmap;
    private static ByteBuffer runningByteBuffer;
    private static int[] pixelsBuffer;
    private static int height = -1;
    private static int width = -1;


    public interface BitmapCreatedCallback {
        void onBitmapCreated(Bitmap bitmap);
    }

    // Create a bitmap from the current surface frame.
    public static void createBitmapFromFrame(final BitmapCreatedCallback bitmapCreatedCallback, final GLSurfaceView surface) {
        if ((width == -1 || width == 0) || (height == 0 || height == -1)) {
            width = surface.getWidth();
            height = surface.getHeight();
        }

        surface.queueEvent(new Runnable() {
            @Override
            public void run() {
                GL10 gl;
                EGL10 egl = (EGL10) EGLContext.getEGL();
                gl = (GL10) egl.eglGetCurrentContext().getGL();
                Bitmap frame = ImageHelper.getBitmapFromGLSurface(width, height, gl);
                bitmapCreatedCallback.onBitmapCreated(frame);
            }
        });
    }


    public static Bitmap getBitmapFromGLSurface(int w, int h, GL10 gl) {

        if (w == 0 || h == 0) {
            return null;
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

        gl.glReadPixels(0, 0, w, h, GLES30.GL_RGBA, GLES30.GL_UNSIGNED_BYTE, runningByteBuffer);
        runningByteBuffer.asIntBuffer().get(pixelsBuffer);
        runningBitmap.setPixels(pixelsBuffer, (w * h) - w, -w, 0, 0, w, h);
        runningByteBuffer.clear();
        return runningBitmap;
    }

}
