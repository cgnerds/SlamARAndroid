package com.magicar;

import android.content.Context;
import android.opengl.GLSurfaceView;
import android.os.Build;

import org.opencv.android.Camera2Renderer;
import org.opencv.android.CameraGLSurfaceView;

/**
 * Created by 38325 on 2017/8/9.
 */

public class MagicGLSurfaceView extends GLSurfaceView {
    MagicGLRendererBase mRenderer;

    public MagicGLSurfaceView(Context context) {
        super(context);

        if(Build.VERSION.SDK_INT >= 21) {
            // mRenderer = new Camera2Renderer(this);
            // mRenderer = new Camera2Renderer(this);
            // mRenderer = new Camera2Renderer(this);
        }
    }
}
