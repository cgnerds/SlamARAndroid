package com.magic_ar.slamar_android;

import android.content.Context;
import android.opengl.GLSurfaceView;

import javax.microedition.khronos.egl.EGL10;
import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.egl.EGLContext;
import javax.microedition.khronos.egl.EGLDisplay;

import static android.opengl.EGL14.EGL_CONTEXT_CLIENT_VERSION;

/**
 * Created by cgnerds on 2017/8/3.
 */

public class GLView extends GLSurfaceView {
    public GLView(Context context) {
        super(context);
        setEGLContextFactory(new ContextFactory());
    }

    private static class ContextFactory implements GLSurfaceView.EGLContextFactory {
        private static int EGL_CONTEXT_CLIENT_VERSION = 0x3098;

        @Override
        public EGLContext createContext(EGL10 egl, EGLDisplay eglDisplay, EGLConfig eglConfig) {
            EGLContext context;
            int[] attrib = {EGL_CONTEXT_CLIENT_VERSION, 2, EGL10.EGL_NONE};
            context = egl.eglCreateContext(eglDisplay, eglConfig, EGL10.EGL_NO_CONTEXT, attrib);
            return context;
        }

        @Override
        public void destroyContext(EGL10 egl, EGLDisplay eglDisplay, EGLContext eglContext) {
            egl.eglDestroyContext(eglDisplay, eglContext);
        }
    }
}
