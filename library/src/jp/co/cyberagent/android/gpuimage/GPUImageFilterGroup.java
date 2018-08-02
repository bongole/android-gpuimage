/*
 * Copyright (C) 2012 CyberAgent
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package jp.co.cyberagent.android.gpuimage;

import android.annotation.SuppressLint;
import android.opengl.GLES20;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static jp.co.cyberagent.android.gpuimage.GPUImageRenderer.CUBE;
import static jp.co.cyberagent.android.gpuimage.util.TextureRotationUtil.TEXTURE_NO_ROTATION;

/**
 * Resembles a filter that consists of multiple filters applied after each
 * other.
 */
public class GPUImageFilterGroup extends GPUImageFilter {

    private List<GPUImageFilter> mFilters = new ArrayList<>();
    private int[] mFrameBuffers;
    private int[] mFrameBufferTextures;
    private int mLastDrawnTexture = OpenGlUtils.NO_TEXTURE;

    private FloatBuffer mGLCubeBuffer;
    private FloatBuffer mGLTextureBuffer;

    public GPUImageFilterGroup(final String vertexShader, final String fragmentShader) {
        super(vertexShader, fragmentShader);
        setUp();
    }

    /**
     * Instantiates a new GPUImageFilterGroup with no filters.
     */
    public GPUImageFilterGroup() {
        setUp();
    }

    private void setUp(){
        mGLCubeBuffer = ByteBuffer.allocateDirect(CUBE.length * 4)
                .order(ByteOrder.nativeOrder())
                .asFloatBuffer();
        mGLCubeBuffer.put(CUBE).position(0);

        mGLTextureBuffer = ByteBuffer.allocateDirect(TEXTURE_NO_ROTATION.length * 4)
                .order(ByteOrder.nativeOrder())
                .asFloatBuffer();
        mGLTextureBuffer.put(TEXTURE_NO_ROTATION).position(0);
    }

    public void addFilter(GPUImageFilter aFilter) {
        if (aFilter == null) {
            return;
        }

        mFilters.add(aFilter);
    }

    public void removeFilter(GPUImageFilter aFilter) {
         if (aFilter == null) {
            return;
        }

        if( mFilters.remove(aFilter) ){
            aFilter.destroy();
        }
    }

    public void replaceFilter(GPUImageFilter aFilter) {
        int idx = mFilters.indexOf(aFilter);
        replaceFilter(idx, aFilter);
    }

    public void replaceFilter(int idx, GPUImageFilter aFilter) {
         if (aFilter == null || idx < 0 || mFilters.size() <= idx ) {
            return;
        }

        GPUImageFilter oldFilter = mFilters.set(idx, aFilter);
        if( oldFilter != null ){
            oldFilter.destroy();
        }
    }

    public void swapFilter(GPUImageFilter aFilter, GPUImageFilter bFilter) {
        int aIndex = mFilters.indexOf(aFilter);
        int bIndex = mFilters.indexOf(bFilter);
        if( aIndex < 0 || bIndex < 0 ){
            return;
        }

        Collections.swap(mFilters, aIndex, bIndex);
    }

    /*
     * (non-Javadoc)
     * @see jp.co.cyberagent.android.gpuimage.GPUImageFilter#onInit()
     */
    @Override
    public void onInit() {
        super.onInit();
        for (GPUImageFilter filter : mFilters) {
            filter.init();
        }
    }

    /*
     * (non-Javadoc)
     * @see jp.co.cyberagent.android.gpuimage.GPUImageFilter#onDestroy()
     */
    @Override
    synchronized public void onDestroy() {
        destroyFramebuffers();
        mLastDrawnTexture = OpenGlUtils.NO_TEXTURE;

        for (GPUImageFilter filter : mFilters) {
            filter.destroy();
        }
        super.onDestroy();
    }

    private void destroyFramebuffers() {
        if (mFrameBufferTextures != null) {
            GLES20.glDeleteTextures(mFrameBufferTextures.length, mFrameBufferTextures, 0);
            mFrameBufferTextures = null;
        }
        if (mFrameBuffers != null) {
            GLES20.glDeleteFramebuffers(mFrameBuffers.length, mFrameBuffers, 0);
            mFrameBuffers = null;
        }
    }

    /*
     * (non-Javadoc)
     * @see
     * jp.co.cyberagent.android.gpuimage.GPUImageFilter#onOutputSizeChanged(int,
     * int)
     */
    @Override
    synchronized public void onOutputSizeChanged(final int width, final int height) {
        int[] savedTexture2D = new int[1];
        int[] savedFrameBuffer = new int[1];
        GLES20.glGetIntegerv(GLES20.GL_TEXTURE_BINDING_2D, savedTexture2D, 0);
        GLES20.glGetIntegerv(GLES20.GL_FRAMEBUFFER_BINDING, savedFrameBuffer, 0);

        super.onOutputSizeChanged(width, height);

        if (mFrameBuffers != null) {
            destroyFramebuffers();
        }

        for ( GPUImageFilter filter : mFilters ){
            if( !filter.isInitialized() ){
                filter.init();
            }

            filter.onOutputSizeChanged(width, height);
        }

        mFrameBuffers = new int[2];
        mFrameBufferTextures = new int[2];

        GLES20.glGenFramebuffers(2, mFrameBuffers, 0);
        GLES20.glGenTextures(2, mFrameBufferTextures, 0);
        for( int i = 0; i < 2; i++ ){
            GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, mFrameBufferTextures[i]);
            GLES20.glTexImage2D(GLES20.GL_TEXTURE_2D, 0, GLES20.GL_RGBA, width, height, 0,
                    GLES20.GL_RGBA, GLES20.GL_UNSIGNED_BYTE, null);
            GLES20.glTexParameterf(GLES20.GL_TEXTURE_2D,
                    GLES20.GL_TEXTURE_MAG_FILTER, GLES20.GL_LINEAR);
            GLES20.glTexParameterf(GLES20.GL_TEXTURE_2D,
                    GLES20.GL_TEXTURE_MIN_FILTER, GLES20.GL_LINEAR);
            GLES20.glTexParameterf(GLES20.GL_TEXTURE_2D,
                    GLES20.GL_TEXTURE_WRAP_S, GLES20.GL_CLAMP_TO_EDGE);
            GLES20.glTexParameterf(GLES20.GL_TEXTURE_2D,
                    GLES20.GL_TEXTURE_WRAP_T, GLES20.GL_CLAMP_TO_EDGE);

            GLES20.glBindFramebuffer(GLES20.GL_FRAMEBUFFER, mFrameBuffers[i]);
            GLES20.glFramebufferTexture2D(GLES20.GL_FRAMEBUFFER, GLES20.GL_COLOR_ATTACHMENT0,
                    GLES20.GL_TEXTURE_2D, mFrameBufferTextures[i], 0);
        }

        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, savedTexture2D[0]);
        GLES20.glBindFramebuffer(GLES20.GL_FRAMEBUFFER, savedFrameBuffer[0]);
    }

    /*
     * (non-Javadoc)
     * @see jp.co.cyberagent.android.gpuimage.GPUImageFilter#onDraw(int,
     * java.nio.FloatBuffer, java.nio.FloatBuffer)
     */
    @SuppressLint("WrongCall")    
    @Override
    synchronized public void onDraw(final int textureId, final FloatBuffer cubeBuffer,
                       final FloatBuffer textureBuffer) {
        runPendingOnDrawTasks();
        if (!isInitialized() || mFrameBuffers == null || mFrameBufferTextures == null) {
            return;
        }

        int[] savedFrameBuffer = new int[1];
        float[] savedClearColor = new float[4];
        GLES20.glGetIntegerv(GLES20.GL_FRAMEBUFFER_BINDING, savedFrameBuffer, 0);
        GLES20.glGetFloatv(GLES20.GL_COLOR_CLEAR_VALUE, savedClearColor, 0);

        int i = 0;
        mLastDrawnTexture = textureId;
        FloatBuffer tb = textureBuffer;
        FloatBuffer cb = cubeBuffer;
        int w = getOutputWidth();
        int h = getOutputHeight();
        for ( GPUImageFilter filter : mFilters ){
             if( !filter.isInitialized() ){
                filter.init();
                filter.onOutputSizeChanged(w, h);
            }

            GLES20.glBindFramebuffer(GLES20.GL_FRAMEBUFFER, mFrameBuffers[ i % 2 ]);
            GLES20.glClearColor(savedClearColor[0], savedClearColor[1], savedClearColor[2], savedClearColor[3]);
            GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT | GLES20.GL_DEPTH_BUFFER_BIT);
            GLES20.glViewport(0, 0, w, h);

            filter.onDraw(mLastDrawnTexture, cb, tb);

            cb = mGLCubeBuffer;
            tb = mGLTextureBuffer;
            mLastDrawnTexture = mFrameBufferTextures[ i % 2 ];
            i++;
        }

        GLES20.glBindFramebuffer(GLES20.GL_FRAMEBUFFER, savedFrameBuffer[0]);
        GLES20.glClearColor(savedClearColor[0], savedClearColor[1], savedClearColor[2], savedClearColor[3]);
        GLES20.glViewport(0, 0, w, h);

        super.onDraw(mLastDrawnTexture, cb, tb);
    }

    public int getLastDrawnTexture() {
        return mLastDrawnTexture;
    }

    public FloatBuffer getLastDrawnCubeBuffer() {
        return mGLCubeBuffer;
    }

    public FloatBuffer getLastDrawnTextureBuffer() {
        return mGLTextureBuffer;
    }

    /**
     * Gets the filters.
     *
     * @return the filters
     */
    public List<GPUImageFilter> getFilters() {
        return mFilters;
    }

}
