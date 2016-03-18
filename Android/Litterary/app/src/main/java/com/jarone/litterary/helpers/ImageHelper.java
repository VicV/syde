package com.jarone.litterary.helpers;

import android.graphics.Bitmap;
import android.opengl.GLSurfaceView;

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

        if (gl != null && w != 0 && h != 0) {
            int screenshotSize = w * h;
            ByteBuffer bb = ByteBuffer.allocateDirect(screenshotSize * 4);
            bb.order(ByteOrder.nativeOrder());
            gl.glReadPixels(0, 0, w, h, GL10.GL_RGBA, GL10.GL_UNSIGNED_BYTE, bb);
            int pixelsBuffer[] = new int[screenshotSize];
            bb.asIntBuffer().get(pixelsBuffer);
            Bitmap bitmap = Bitmap.createBitmap(w, h,
                    Bitmap.Config.RGB_565);

            bitmap.setPixels(pixelsBuffer, screenshotSize - w, -w, 0,
                    0, w, h);

            short sBuffer[] = new short[screenshotSize];
            ShortBuffer sb = ShortBuffer.wrap(sBuffer);
            bitmap.copyPixelsToBuffer(sb);

            // Making created bitmap (from OpenGL points) compatible with
            // Android bitmap
            for (int i = 0; i < screenshotSize; ++i) {
                short v = sBuffer[i];
                sBuffer[i] = (short) (((v & 0x1f) << 11) | (v & 0x7e0) | ((v & 0xf800) >> 11));
            }
            sb.rewind();
            bitmap.copyPixelsFromBuffer(sb);
            return bitmap.copy(Bitmap.Config.ARGB_8888, false);
        }
        return null;
    }
}


