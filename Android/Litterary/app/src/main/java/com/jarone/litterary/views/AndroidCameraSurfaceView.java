package com.jarone.litterary.views;

import android.content.Context;
import android.graphics.SurfaceTexture;
import android.hardware.Camera;
import android.opengl.GLES11Ext;
import android.opengl.GLES30;
import android.opengl.GLSurfaceView;
import android.util.AttributeSet;
import android.util.Log;
import android.view.SurfaceHolder;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;
import java.util.ArrayList;

import javax.microedition.khronos.opengles.GL10;

// View
public class AndroidCameraSurfaceView extends GLSurfaceView {
    AndroidCameraSurfaceRenderer mRenderer;
    private GL10 gl10;

    AndroidCameraSurfaceView(Context context) {
        super(context);
    }


    public AndroidCameraSurfaceView(Context context, AttributeSet attrs) {
        super(context, attrs);
    }


    public void surfaceCreated(SurfaceHolder holder) {
        super.surfaceCreated(holder);
    }

    public void setupSurfaceView() {
        mRenderer = new AndroidCameraSurfaceRenderer(this);
        setEGLContextClientVersion(2);
        setRenderer(mRenderer);
        setRenderMode(GLSurfaceView.RENDERMODE_WHEN_DIRTY);

    }

    public void surfaceDestroyed(SurfaceHolder holder) {
        if (mRenderer != null) {
            mRenderer.close();
        }
        super.surfaceDestroyed(holder);
    }

    public void surfaceChanged(SurfaceHolder holder, int format, int w, int h) {
        super.surfaceChanged(holder, format, w, h);
    }

    class AndroidCameraSurfaceRenderer implements GLSurfaceView.Renderer, SurfaceTexture.OnFrameAvailableListener {

        private int[] hTex;
        private FloatBuffer pVertex;
        private FloatBuffer pTexCoord;
        private int hProgram;

        private Camera mCamera;
        private SurfaceTexture mSTexture;

        private boolean mUpdateST = false;

        private AndroidCameraSurfaceView mView;

        AndroidCameraSurfaceRenderer(AndroidCameraSurfaceView view) {
            mView = view;
            float[] vtmp = {1.0f, -1.0f, -1.0f, -1.0f, 1.0f, 1.0f, -1.0f, 1.0f};
            float[] ttmp = {1.0f, 1.0f, 0.0f, 1.0f, 1.0f, 0.0f, 0.0f, 0.0f};
            pVertex = ByteBuffer.allocateDirect(8 * 4).order(ByteOrder.nativeOrder()).asFloatBuffer();
            pVertex.put(vtmp);
            pVertex.position(0);
            pTexCoord = ByteBuffer.allocateDirect(8 * 4).order(ByteOrder.nativeOrder()).asFloatBuffer();
            pTexCoord.put(ttmp);
            pTexCoord.position(0);
        }

        public void close() {
            mUpdateST = false;
            mSTexture.release();
            mCamera.stopPreview();
            mCamera = null;
            deleteTex();
        }


        public void onDrawFrame(GL10 unused) {
            GLES30.glClear(GLES30.GL_COLOR_BUFFER_BIT);

            synchronized (this) {
                if (mUpdateST) {
                    mSTexture.updateTexImage();
                    mUpdateST = false;
                }
            }

            GLES30.glUseProgram(hProgram);

            int ph = GLES30.glGetAttribLocation(hProgram, "vPosition");
            int tch = GLES30.glGetAttribLocation(hProgram, "vTexCoord");
            int th = GLES30.glGetUniformLocation(hProgram, "sTexture");

            GLES30.glActiveTexture(GLES30.GL_TEXTURE0);
            GLES30.glBindTexture(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, hTex[0]);
            GLES30.glUniform1i(th, 0);

            GLES30.glVertexAttribPointer(ph, 2, GLES30.GL_FLOAT, false, 4 * 2, pVertex);
            GLES30.glVertexAttribPointer(tch, 2, GLES30.GL_FLOAT, false, 4 * 2, pTexCoord);
            GLES30.glEnableVertexAttribArray(ph);
            GLES30.glEnableVertexAttribArray(tch);

            GLES30.glDrawArrays(GLES30.GL_TRIANGLE_STRIP, 0, 4);
//            ContextManager.getMainActivityInstance().setUpscalePreviewImage();
            GLES30.glFlush();
        }

        @Override
        public void onSurfaceCreated(GL10 gl, javax.microedition.khronos.egl.EGLConfig config) {
            //String extensions = GLES30.glGetString(GLES30.GL_EXTENSIONS);
            //Log.i("mr", "Gl extensions: " + extensions);
            //Assert.assertTrue(extensions.contains("OES_EGL_image_external"));

            initTex();
            mSTexture = new SurfaceTexture(hTex[0]);
            mSTexture.setOnFrameAvailableListener(this);

            mCamera = Camera.open();
            mCamera.setDisplayOrientation(180);
            try {
                mCamera.setPreviewTexture(mSTexture);
            } catch (IOException ioe) {
            }

            GLES30.glClearColor(1.0f, 1.0f, 0.0f, 1.0f);

            String vss = "attribute vec2 vPosition;\n" +
                    "attribute vec2 vTexCoord;\n" +
                    "varying vec2 texCoord;\n" +
                    "void main() {\n" +
                    "  texCoord = vTexCoord;\n" +
                    "  gl_Position = vec4 ( vPosition.x, vPosition.y, 0.0, 1.0 );\n" +
                    "}";
            String fss = "#extension GL_OES_EGL_image_external : require\n" +
                    "precision mediump float;\n" +
                    "uniform samplerExternalOES sTexture;\n" +
                    "varying vec2 texCoord;\n" +
                    "void main() {\n" +
                    "  gl_FragColor = texture2D(sTexture,texCoord);\n" +
                    "}";
            hProgram = loadShader(vss, fss);

        }

        public void onSurfaceChanged(GL10 unused, int width, int height) {
            GLES30.glViewport(0, 0, width, height);
            Camera.Parameters param = mCamera.getParameters();
            ArrayList<Camera.Size> psize = (ArrayList) param.getSupportedPreviewSizes();
            if (psize.size() > 0) {
                int i;
                for (i = 0; i < psize.size(); i++) {
                    if (psize.get(i).width < width || psize.get(i).width < height)
                        break;
                }
                if (i > 0)
                    i--;
                param.setPreviewSize(psize.get(i).width, psize.get(i).height);
                //Log.i("mr","ssize: "+psize.get(i).width+", "+psize.get(i).height);
            }
            param.set("orientation", "portrait");
            mCamera.setParameters(param);
            mCamera.setDisplayOrientation(180);
            mCamera.startPreview();
        }

        private void initTex() {
            hTex = new int[1];
            GLES30.glGenTextures(1, hTex, 0);
            GLES30.glBindTexture(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, hTex[0]);
            GLES30.glTexParameteri(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, GLES30.GL_TEXTURE_WRAP_S, GLES30.GL_CLAMP_TO_EDGE);
            GLES30.glTexParameteri(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, GLES30.GL_TEXTURE_WRAP_T, GLES30.GL_CLAMP_TO_EDGE);
            GLES30.glTexParameteri(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, GLES30.GL_TEXTURE_MIN_FILTER, GLES30.GL_NEAREST);
            GLES30.glTexParameteri(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, GLES30.GL_TEXTURE_MAG_FILTER, GLES30.GL_NEAREST);
        }

        private void deleteTex() {
            GLES30.glDeleteTextures(1, hTex, 0);
        }

        public synchronized void onFrameAvailable(SurfaceTexture st) {
            mUpdateST = true;
            mView.requestRender();
        }

        private int loadShader(String vss, String fss) {
            int vshader = GLES30.glCreateShader(GLES30.GL_VERTEX_SHADER);
            GLES30.glShaderSource(vshader, vss);
            GLES30.glCompileShader(vshader);
            int[] compiled = new int[1];
            GLES30.glGetShaderiv(vshader, GLES30.GL_COMPILE_STATUS, compiled, 0);
            if (compiled[0] == 0) {
                Log.e("Shader", "Could not compile vshader");
                Log.v("Shader", "Could not compile vshader:" + GLES30.glGetShaderInfoLog(vshader));
                GLES30.glDeleteShader(vshader);
                vshader = 0;
            }

            int fshader = GLES30.glCreateShader(GLES30.GL_FRAGMENT_SHADER);
            GLES30.glShaderSource(fshader, fss);
            GLES30.glCompileShader(fshader);
            GLES30.glGetShaderiv(fshader, GLES30.GL_COMPILE_STATUS, compiled, 0);
            if (compiled[0] == 0) {
                Log.e("Shader", "Could not compile fshader");
                Log.v("Shader", "Could not compile fshader:" + GLES30.glGetShaderInfoLog(fshader));
                GLES30.glDeleteShader(fshader);
                fshader = 0;
            }

            int program = GLES30.glCreateProgram();
            GLES30.glAttachShader(program, vshader);
            GLES30.glAttachShader(program, fshader);
            GLES30.glLinkProgram(program);

            return program;
        }
    }
}