package com.magicar;

import android.graphics.SurfaceTexture;
import android.opengl.GLSurfaceView;
import android.view.SurfaceView;

/**
 * Created by 38325 on 2017/8/9.
 */

public abstract class MagicGLRendererBase implements GLSurfaceView.Renderer, SurfaceTexture.OnFrameAvailableListener{
    protected final String TAG = "MagicGLRendererBase";

    protected SurfaceTexture mSTex;
    // protected SurfaceTexture mSTex;
}
