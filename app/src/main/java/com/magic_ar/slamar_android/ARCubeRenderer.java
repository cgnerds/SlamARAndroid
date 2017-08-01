package com.magic_ar.slamar_android;

import android.opengl.GLSurfaceView;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;

import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.opengles.GL10;
import javax.microedition.khronos.opengles.GL11;

/**
 * Created by cgnerds on 2017/7/14.
 */

public class ARCubeRenderer implements GLSurfaceView.Renderer {
    public ImageDetector filter;
    public CameraProjectionAdapter cameraProjectionAdapter;
    public float scale = 1.5f; // for test

    private int mSurfaceWidth;
    private int mSurfaceHeight;

    private static final ByteBuffer VERTICES;
    private static final ByteBuffer COLORS;
    private static final ByteBuffer TRIANGLES;

    static {
        VERTICES = ByteBuffer.allocateDirect(96);
        VERTICES.order(ByteOrder.nativeOrder());
        VERTICES.asFloatBuffer().put(new float[]{
                // Front.
                -0.5f, -0.5f, 0.5f,
                0.5f, -0.5f, 0.5f,
                0.5f, 0.5f, 0.5f,
                -0.5f, 0.5f, 0.5f,
                // Back.
                -0.5f, -0.5f, -0.5f,
                0.5f, -0.5f, -0.5f,
                0.5f, 0.5f, -0.5f,
                -0.5f, 0.5f, -0.5f});
        VERTICES.position(0);

        COLORS = ByteBuffer.allocateDirect(32);
        final byte maxColor = (byte)255;
        COLORS.put(new byte[] {
                // Front.
                maxColor, 0, 0, maxColor,         // red
                maxColor, maxColor, 0, maxColor,  // yellow
                maxColor, maxColor, 0, maxColor,  // yellow
                maxColor, 0, 0, maxColor,         // red
                // Back.
                0, maxColor, 0, maxColor,         // green
                0, 0, maxColor, maxColor,         // blue
                0, 0, maxColor, maxColor,         // blue
                0, maxColor, 0, maxColor          // green
        });
        COLORS.position(0);

        TRIANGLES = ByteBuffer.allocateDirect(36);
        TRIANGLES.put(new byte[] {
                // Front.
                0, 1, 2, 2, 3, 0,
                3, 2, 6, 6, 7, 3,
                7, 6, 5, 5, 4, 7,
                // Back.
                4, 5, 1, 1, 0, 4,
                4, 0, 3, 3, 7, 4,
                1, 5, 6, 6, 2, 1
        });
        TRIANGLES.position(0);
    }

    @Override
    public void onSurfaceCreated(final GL10 gl10, final EGLConfig eglConfig) {
        gl10.glClearColor(0f, 0f, 0f, 0f); // transparent
        gl10.glEnable(GL10.GL_CULL_FACE);
    }

    @Override
    public void onSurfaceChanged(final GL10 gl10, int width, int height) {
        mSurfaceWidth = width;
        mSurfaceHeight = height;
    }

    @Override
    public void onDrawFrame(GL10 gl10) {
        gl10.glClear(GL10.GL_COLOR_BUFFER_BIT | GL10.GL_DEPTH_BUFFER_BIT);

        if(filter == null) {
            return;
        }

        if(cameraProjectionAdapter == null) {
            return;
        }

        float[] pose = filter.getGLPose();
        if(pose == null) {
            return;
        }

        final int adjustedWidth = (int)(mSurfaceHeight*cameraProjectionAdapter.getAspectRatio());
        final int marginX = (mSurfaceWidth - adjustedWidth) / 2;
        gl10.glViewport(marginX, 0, adjustedWidth, mSurfaceHeight);

        gl10.glMatrixMode(GL10.GL_PROJECTION);
        float[] projection = cameraProjectionAdapter.getProjectionGL();
        gl10.glLoadMatrixf(projection, 0);

        gl10.glMatrixMode(GL10.GL_MODELVIEW);
        gl10.glLoadMatrixf(pose, 0);
        gl10.glScalef(scale, scale, scale);

        // Move the cube forward so that it is not halfway inside the image.
        gl10.glTranslatef(0f, 0f, 0.5f);
        gl10.glVertexPointer(3, GL11.GL_FLOAT, 0, VERTICES);
        gl10.glColorPointer(4, GL11.GL_UNSIGNED_BYTE, 0, COLORS);

        gl10.glEnableClientState(GL10.GL_VERTEX_ARRAY);
        gl10.glEnableClientState(GL10.GL_COLOR_ARRAY);

        gl10.glDrawElements(GL10.GL_TRIANGLES,36, GL10.GL_UNSIGNED_BYTE, TRIANGLES);

        gl10.glDisableClientState(GL10.GL_VERTEX_ARRAY);
        gl10.glDisableClientState(GL10.GL_COLOR_ARRAY);
    }
}
