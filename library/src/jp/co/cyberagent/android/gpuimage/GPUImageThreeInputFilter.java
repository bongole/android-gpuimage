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

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;

import jp.co.cyberagent.android.gpuimage.util.TextureRotationUtil;
import android.graphics.Bitmap;
import android.opengl.GLES20;

import static jp.co.cyberagent.android.gpuimage.util.TextureRotationUtil.TEXTURE_NO_ROTATION;

public class GPUImageThreeInputFilter extends GPUImageFilter {
    private static final String VERTEX_SHADER = "attribute vec4 position;\n" +
            "attribute vec4 inputTextureCoordinate;\n" +
            "attribute vec4 inputTextureCoordinate2;\n" +
            "attribute vec4 inputTextureCoordinate3;\n" +
            " \n" +
            "varying vec2 textureCoordinate;\n" +
            "varying vec2 textureCoordinate2;\n" +
            "varying vec2 textureCoordinate3;\n" +
            " \n" +
            "void main()\n" +
            "{\n" +
            "    gl_Position = position;\n" +
            "    textureCoordinate = inputTextureCoordinate.xy;\n" +
            "    textureCoordinate2 = inputTextureCoordinate2.xy;\n" +
            "    textureCoordinate3 = inputTextureCoordinate3.xy;\n" +
            "}";

    public int mFilterSecondTextureCoordinateAttribute;
    public int mFilterThirdTextureCoordinateAttribute;
    public int mFilterInputTextureUniform2;
    public int mFilterInputTextureUniform3;
    public int mFilterSourceTexture2 = OpenGlUtils.NO_TEXTURE;
    public int mFilterSourceTexture3 = OpenGlUtils.NO_TEXTURE;
    private FloatBuffer mTexture2CoordinatesBuffer;
    private FloatBuffer mTexture3CoordinatesBuffer;
    private Bitmap mBitmap;

    public GPUImageThreeInputFilter(String fragmentShader) {
        this(VERTEX_SHADER, fragmentShader);
    }

    public GPUImageThreeInputFilter(String vertexShader, String fragmentShader) {
        super(vertexShader, fragmentShader);
        setRotation(Rotation.NORMAL, false, false);
    }

    @Override
    public void onInit() {
        super.onInit();

        mFilterSecondTextureCoordinateAttribute = GLES20.glGetAttribLocation(getProgram(), "inputTextureCoordinate2");
        mFilterThirdTextureCoordinateAttribute = GLES20.glGetAttribLocation(getProgram(), "inputTextureCoordinate3");
        mFilterInputTextureUniform2 = GLES20.glGetUniformLocation(getProgram(), "inputImageTexture2"); // This does assume a name of "inputImageTexture2" for second input texture in the fragment shader
        mFilterInputTextureUniform3 = GLES20.glGetUniformLocation(getProgram(), "inputImageTexture3"); // This does assume a name of "inputImageTexture3" for third input texture in the fragment shader
        GLES20.glEnableVertexAttribArray(mFilterSecondTextureCoordinateAttribute);
        GLES20.glEnableVertexAttribArray(mFilterThirdTextureCoordinateAttribute);

        setBitmap(mBitmap);
    }

    public void setSecondTexture(int secondTexture, FloatBuffer secondTextureCoordinatesBuffer) {
        mFilterSourceTexture2 = secondTexture;
        mTexture2CoordinatesBuffer = secondTextureCoordinatesBuffer;
    }

    public void setBitmap(final Bitmap bitmap) {
        if ( bitmap == null || bitmap.isRecycled() ) {
            return;
        }

        mBitmap = bitmap;

        runOnDraw(new Runnable() {
            public void run() {
                if (mBitmap == null || mBitmap.isRecycled()) {
                    return;
                }

                GLES20.glActiveTexture(GLES20.GL_TEXTURE4);
                mFilterSourceTexture3 = OpenGlUtils.loadTexture(mBitmap, mFilterSourceTexture3, false);
            }
        });
    }

    public Bitmap getBitmap() {
        return mBitmap;
    }

    public void recycleBitmap() {
        if (mBitmap == null || mBitmap.isRecycled()) {
            return;
        }

        mBitmap.recycle();
        mBitmap = null;
    }

    public void onDestroy() {
        super.onDestroy();
        GLES20.glDeleteTextures(1, new int[]{ mFilterSourceTexture3 }, 0);
        mFilterSourceTexture2 = OpenGlUtils.NO_TEXTURE;
        mFilterSourceTexture3 = OpenGlUtils.NO_TEXTURE;
    }

    @Override
    protected void onDrawArraysPre() {
        GLES20.glEnableVertexAttribArray(mFilterSecondTextureCoordinateAttribute);
        GLES20.glActiveTexture(GLES20.GL_TEXTURE3);
        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, mFilterSourceTexture2);
        GLES20.glUniform1i(mFilterInputTextureUniform2, 3);

        mTexture2CoordinatesBuffer.position(0);
        GLES20.glVertexAttribPointer(mFilterSecondTextureCoordinateAttribute, 2, GLES20.GL_FLOAT, false, 0, mTexture2CoordinatesBuffer);

        GLES20.glEnableVertexAttribArray(mFilterThirdTextureCoordinateAttribute);
        GLES20.glActiveTexture(GLES20.GL_TEXTURE4);
        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, mFilterSourceTexture3);
        GLES20.glUniform1i(mFilterInputTextureUniform3, 4);

        mTexture3CoordinatesBuffer.position(0);
        GLES20.glVertexAttribPointer(mFilterThirdTextureCoordinateAttribute, 2, GLES20.GL_FLOAT, false, 0, mTexture3CoordinatesBuffer);
    }

    public void setRotation(final Rotation rotation, final boolean flipHorizontal, final boolean flipVertical) {
        mTexture3CoordinatesBuffer = ByteBuffer.allocateDirect(TEXTURE_NO_ROTATION.length * 4)
                .order(ByteOrder.nativeOrder())
                .asFloatBuffer();
        float[] buffer = TextureRotationUtil.getRotation(rotation, flipHorizontal, flipVertical);
        mTexture3CoordinatesBuffer.put(buffer).position(0);
    }
}
